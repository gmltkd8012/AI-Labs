package org.example.orchestrator

import org.example.agents.CodeAgent
import org.example.agents.ReviewAgent
import org.example.agents.SpecAgent
import org.example.llm.LLMConfig
import org.example.model.GeneratedCode
import org.example.model.ReviewResult
import org.example.model.UserRequirement

class Orchestrator(
    private val specAgent: SpecAgent = SpecAgent(),
    private val codeAgent: CodeAgent = CodeAgent(),
    private val reviewAgent: ReviewAgent = ReviewAgent(),
    private val maxRetries: Int = 2
) {
    data class PipelineResult(
        val generatedCode: GeneratedCode,
        val reviewResult: ReviewResult,
        val attempts: Int
    )

    suspend fun run(requirement: UserRequirement): PipelineResult {
        println("[Orchestrator] 요구사항 수신: ${requirement.description}")

        println("[SpecAgent] 설계 명세 생성 중...")
        val spec = specAgent.design(requirement)
        println("[SpecAgent] 완료 — Agent: ${spec.agentName}, 권장 모델: ${spec.recommendedModel}")

        var lastCode: GeneratedCode? = null
        var lastReview: ReviewResult? = null
        var feedback: List<String> = emptyList()

        repeat(maxRetries) { attempt ->
            println("[CodeAgent] 코드 생성 중... (시도 ${attempt + 1}/$maxRetries)")
            val code = codeAgent.generate(spec, feedback)
            println("[CodeAgent] 완료 — 파일 ${code.files.size}개 생성")

            println("[ReviewAgent] 코드 리뷰 중...")
            val review = reviewAgent.review(code)
            println("[ReviewAgent] 완료 — 승인: ${review.approved}, 점수: ${review.score}/100")

            lastCode = code
            lastReview = review

            if (review.approved) {
                println("[Orchestrator] 파이프라인 완료 (${attempt + 1}회 시도)")
                return PipelineResult(code, review, attempt + 1)
            }

            val issuesForFeedback = review.issues
                .sortedBy { it.severity }
                .map { "[${it.severity}] ${it.location}: ${it.description} → ${it.suggestion}" }
            feedback = issuesForFeedback

            val criticalCount = review.issues.count { it.severity.name == "CRITICAL" }
            if (criticalCount > 0) {
                println("[Orchestrator] CRITICAL 이슈 ${criticalCount}개 발견, 피드백 포함 재시도...")
                issuesForFeedback.forEach { println("  - $it") }
            }
        }

        println("[Orchestrator] 최대 재시도 초과, 마지막 결과 반환")
        return PipelineResult(lastCode!!, lastReview!!, maxRetries)
    }

    companion object {
        fun withPaidModel(provider: String): Orchestrator {
            val config: LLMConfig = when (provider.lowercase()) {
                "claude", "anthropic" -> LLMConfig.Anthropic()
                "openai", "gpt"       -> LLMConfig.OpenAI()
                "google", "gemini"    -> LLMConfig.Google()
                else -> error("지원하지 않는 provider: $provider (claude / openai / google)")
            }
            return Orchestrator(
                specAgent   = SpecAgent(config),
                codeAgent   = CodeAgent(config),
                reviewAgent = ReviewAgent(config)
            )
        }
    }
}
