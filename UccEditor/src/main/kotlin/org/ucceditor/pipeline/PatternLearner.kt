package org.ucceditor.pipeline

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import org.ucceditor.llm.LLMConfig
import org.ucceditor.media.MediaProbe
import org.ucceditor.media.aspectOf
import org.ucceditor.model.StyleProfile
import java.io.File

/** 학습용 영상 쌍: `<name>_full.*` ↔ `<name>_edit.*` */
data class TrainingPair(val name: String, val full: String, val edit: String)

/**
 * 편집 패턴 학습기.
 *
 * 1) 폴더에서 `_full` / `_edit` 쌍을 찾고
 * 2) 각 쌍을 ffprobe로 분석해 정량 지표를 집계하고
 * 3) Gemini로 스타일을 한국어로 요약해 [StyleProfile] 을 만듭니다.
 */
class PatternLearner(
    private val geminiModelId: String = "gemini-2.0-flash",
) {
    private val videoExt = setOf("mp4", "mov", "mkv", "avi", "m4v", "webm")

    /** 폴더에서 학습 쌍을 찾아 이름순으로 반환합니다. */
    fun discoverPairs(folderPath: String): List<TrainingPair> {
        val dir = File(folderPath)
        if (!dir.isDirectory) return emptyList()
        val files = dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in videoExt } ?: return emptyList()

        return files
            .filter { it.nameWithoutExtension.endsWith("_full") }
            .mapNotNull { fullFile ->
                val prefix = fullFile.nameWithoutExtension.removeSuffix("_full")
                val editFile = files.firstOrNull { it.nameWithoutExtension == "${prefix}_edit" }
                editFile?.let { TrainingPair(prefix, fullFile.absolutePath, it.absolutePath) }
            }
            .sortedBy { it.name }
    }

    /** 쌍들을 분석해 스타일 프로파일을 추출합니다. */
    suspend fun learn(pairs: List<TrainingPair>): StyleProfile {
        val ratios = mutableListOf<Double>()
        val editWidths = mutableListOf<Int>()
        val editHeights = mutableListOf<Int>()
        var reframeVotes = 0

        for (pair in pairs) {
            val full = MediaProbe.probe(pair.full) ?: continue
            val edit = MediaProbe.probe(pair.edit) ?: continue
            if (full.durationSec > 0) ratios.add(edit.durationSec / full.durationSec)
            editWidths.add(edit.width)
            editHeights.add(edit.height)
            if (full.aspect != edit.aspect) reframeVotes++
        }

        val avgRatio = if (ratios.isNotEmpty()) ratios.average().coerceIn(0.05, 1.0) else 0.5
        val w = editWidths.filter { it > 0 }.takeIf { it.isNotEmpty() }?.let { median(it) } ?: 1080
        val h = editHeights.filter { it > 0 }.takeIf { it.isNotEmpty() }?.let { median(it) } ?: 1920
        val reframed = pairs.isNotEmpty() && reframeVotes * 2 >= pairs.size

        val base = StyleProfile(
            sampleCount = pairs.size,
            avgDurationRatio = avgRatio,
            targetWidth = w,
            targetHeight = h,
            aspect = aspectOf(w, h),
            reframed = reframed,
        )
        return base.copy(summary = summarize(base))
    }

    private fun median(values: List<Int>): Int =
        values.sorted()[values.size / 2]

    /** Gemini로 정성 요약 생성. 실패/키 없음 시 템플릿 요약으로 폴백. */
    private suspend fun summarize(p: StyleProfile): String {
        val facts = "샘플 ${p.sampleCount}개, 평균 길이 압축률 ${"%.0f".format(p.avgDurationRatio * 100)}%, " +
            "대표 해상도 ${p.targetWidth}x${p.targetHeight}(${p.aspect}), 리프레임 ${if (p.reframed) "있음" else "없음"}."
        val fallback = "원본을 약 ${"%.0f".format(p.avgDurationRatio * 100)}% 길이로 압축하고 " +
            "${p.aspect} 화면비로 ${if (p.reframed) "리프레임하는" else "유지하는"} 스타일입니다. ($facts)"

        return try {
            val config = LLMConfig.Google(modelId = geminiModelId)
            val executor = config.createExecutor()
            try {
                val agentConfig = AIAgentConfig(
                    prompt = prompt(id = "style-summary") {
                        system(
                            "너는 영상 편집 분석가다. 주어진 수치를 바탕으로 편집 스타일의 특징을 " +
                                "2~3문장 한국어로 자연스럽게 요약하라. 수치를 그대로 나열하지 말고 해석해서 설명하라."
                        )
                    },
                    model = config.model,
                    maxAgentIterations = 3
                )
                val agent = AIAgent(
                    promptExecutor = executor,
                    agentConfig = agentConfig,
                    toolRegistry = ToolRegistry { }
                )
                agent.run(facts).ifBlank { fallback }
            } finally {
                executor.close()
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
