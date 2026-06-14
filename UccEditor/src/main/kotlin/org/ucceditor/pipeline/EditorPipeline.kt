package org.ucceditor.pipeline

import org.ucceditor.tools.FFmpegTools
import java.io.File

/**
 * 단계형 편집 파이프라인.
 *
 *   시작 → 학습 폴더 입력 → 패턴 학습 → 편집 대상 입력 → 편집 → 저장 → 종료
 *
 * 자유 대화(EditorAgent)와 달리 정해진 순서를 따르는 상태머신입니다.
 */
class EditorPipeline(
    private val tools: FFmpegTools = FFmpegTools(),
) {
    private val learner = PatternLearner()
    private val editor = StyleEditor(tools)

    suspend fun run() {
        println("UccEditor — 동영상 편집 AI 에이전트")
        println("━".repeat(48))

        // ── Step 1. 학습 ────────────────────────────────────────────
        println("\n[1] 기존 편집 패턴을 학습할 동영상 폴더 경로를 입력하세요.")
        println("    (full/edit 쌍이 5개 정도 필요 — 예: example_full.mp4 / example_edit.mp4)")
        print("> ")
        val folder = readlnOrNull()?.trim().orEmpty()

        val pairs = learner.discoverPairs(folder)
        if (pairs.isEmpty()) {
            println("\n학습할 쌍을 찾지 못했습니다. '<이름>_full.mp4' 와 '<이름>_edit.mp4' 형식인지 확인하세요.")
            return
        }

        println("\n발견된 학습 쌍 (${pairs.size}개):")
        pairs.forEachIndexed { i, p ->
            println("${i + 1}. ${File(p.full).name} ↔ ${File(p.edit).name}")
        }
        if (pairs.size < 5) {
            println("(권장 5쌍보다 적지만 진행합니다.)")
        }

        println("\n패턴 파악중 ..")
        val profile = learner.learn(pairs)
        println("학습 완료")
        println("→ ${profile.summary}")

        // ── Step 2. 편집 ────────────────────────────────────────────
        println("\n[2] 편집하고 싶은 동영상 경로를 입력하세요.")
        print("1. > ")
        val target = readlnOrNull()?.trim().orEmpty()
        if (target.isEmpty() || !File(target).exists()) {
            println("\n파일을 찾을 수 없습니다: $target")
            return
        }

        println("\n편집 중 ..")
        val edited = try {
            editor.apply(target, profile)
        } catch (e: Exception) {
            println("편집 실패: ${e.message}")
            return
        }
        println("편집 완료")

        // ── Step 3. 저장 ────────────────────────────────────────────
        println("\n저장 중 ..")
        val finalPath = finalize(edited, target)
        println("저장 완료 → $finalPath")

        println("\n모든 작업이 종료되었습니다.")
    }

    /** 중간 산출물을 사람이 알아보기 쉬운 최종 파일명으로 정리합니다. */
    private fun finalize(editedPath: String, targetPath: String): String {
        val base = File(targetPath).nameWithoutExtension
        val finalFile = File(tools.pathOf("edited_$base.mp4"))
        File(editedPath).copyTo(finalFile, overwrite = true)
        return finalFile.absolutePath
    }
}
