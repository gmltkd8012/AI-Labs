package org.ucceditor.pipeline

import org.ucceditor.media.MediaProbe
import org.ucceditor.model.StyleProfile
import org.ucceditor.tools.FFmpegTools
import java.util.Locale

/**
 * 학습된 [StyleProfile] 을 새 영상에 적용해 편집본을 생성합니다.
 *
 * v1 적용 규칙(정량 지표 기반):
 *  1. 길이 압축 — 원본 길이 × avgDurationRatio 만큼 앞에서 trim
 *  2. 리프레임 — 프로파일이 화면비 변경 스타일이면 targetWidth×Height 로 reframe
 *
 * 더 정교한 컷/자막/속도 적용은 이후 단계에서 EditPlan 기반으로 확장합니다.
 */
class StyleEditor(
    private val tools: FFmpegTools = FFmpegTools(),
) {
    /** 편집을 수행하고 최종 산출물의 절대경로를 반환합니다. */
    suspend fun apply(targetPath: String, profile: StyleProfile): String {
        val info = MediaProbe.probe(targetPath)
            ?: error("대상 영상을 분석할 수 없습니다: $targetPath (ffmpeg 설치 및 경로 확인)")

        // 1) 길이 압축
        val targetLen = (info.durationSec * profile.avgDurationRatio)
            .coerceIn(1.0, info.durationSec)
        val trimmedName = "step1_trim.mp4"
        val trimMsg = tools.trim(targetPath, "0", fmt(targetLen), trimmedName)
        check(!trimMsg.startsWith("[ERROR]")) { "trim 실패: $trimMsg" }
        var current = tools.pathOf(trimmedName)

        // 2) 리프레임
        if (profile.reframed) {
            val reframedName = "step2_reframe.mp4"
            val reframeMsg = tools.reframe(current, profile.targetWidth, profile.targetHeight, reframedName)
            check(!reframeMsg.startsWith("[ERROR]")) { "reframe 실패: $reframeMsg" }
            current = tools.pathOf(reframedName)
        }

        return current
    }

    private fun fmt(seconds: Double): String = String.format(Locale.US, "%.2f", seconds)
}
