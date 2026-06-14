package org.ucceditor.model

import kotlinx.serialization.Serializable

/**
 * 하나의 편집 작업 단위.
 *
 * Agent가 사용자 요청/학습된 패턴을 해석해 생성하고, 각 [op] 는
 * [org.ucceditor.tools.FFmpegTools] 의 실제 편집 동작으로 매핑됩니다.
 */
@Serializable
data class EditStep(
    /** 동작 종류: trim | concat | overlayText | changeSpeed | extractAudio | scale 등 */
    val op: String,
    /** 동작별 파라미터 (예: start, end, text, factor) */
    val params: Map<String, String> = emptyMap(),
    /** 이 단계를 적용하는 이유 — 편집 패턴 학습/설명에 사용 */
    val rationale: String = ""
)

/**
 * 편집 계획 = 편집 단계의 순서 있는 묶음.
 *
 * "편집 패턴"의 직렬화 표현이기도 하며, 학습된 패턴을 JSON으로 저장/재사용할 수 있습니다.
 */
@Serializable
data class EditPlan(
    val source: String,
    val output: String,
    val steps: List<EditStep>,
    /** 이 계획이 따르는 편집 패턴 이름 (예: "유튜브 숏폼 훅", "브이로그 점프컷") */
    val pattern: String = ""
)

/** 단일 편집 동작 실행 결과. */
@Serializable
data class EditResult(
    val ok: Boolean,
    val message: String,
    val outputPath: String? = null
)
