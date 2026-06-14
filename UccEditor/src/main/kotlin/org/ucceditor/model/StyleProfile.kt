package org.ucceditor.model

import kotlinx.serialization.Serializable

/**
 * 학습된 편집 스타일 프로파일.
 *
 * `full ↔ edit` 쌍들을 분석해 추출한 **정량 지표 + 정성 요약**입니다.
 * 새 영상에 편집을 적용할 때 [org.ucceditor.pipeline.StyleEditor] 가 이 값을 사용합니다.
 */
@Serializable
data class StyleProfile(
    /** 학습에 사용한 쌍 개수 */
    val sampleCount: Int,
    /** 편집본/풀영상 길이 비의 평균 (0~1, 작을수록 많이 압축) */
    val avgDurationRatio: Double,
    /** 편집본 대표 가로 해상도 */
    val targetWidth: Int,
    /** 편집본 대표 세로 해상도 */
    val targetHeight: Int,
    /** 대표 화면비 (예: 9:16, 16:9, 1:1) */
    val aspect: String,
    /** 풀영상 대비 화면비가 바뀌는지 (리프레임 필요 여부) */
    val reframed: Boolean,
    /** Gemini가 생성한 스타일 설명 (정성) */
    val summary: String = ""
)
