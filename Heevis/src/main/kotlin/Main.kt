package org.example

import kotlinx.coroutines.runBlocking
import org.example.llm.LLMConfig
import org.example.model.UserRequirement
import org.example.orchestrator.Orchestrator

fun main() = runBlocking {
    // 기본 실행: Ollama 로컬 모델 사용
    val orchestrator = Orchestrator()

    // 유료 모델 사용 시: Orchestrator.withPaidModel("claude")
    // 환경변수 ANTHROPIC_API_KEY / OPENAI_API_KEY / GOOGLE_API_KEY 필요

    val requirement = UserRequirement(
        description = "날씨 정보를 검색하고 사용자에게 알려주는 Agent",
        features = listOf("도시별 현재 날씨 조회", "날씨 기반 옷차림 추천")
    )

    val result = orchestrator.run(requirement)

    println("\n===== 파이프라인 결과 =====")
    println("Agent 이름: ${result.generatedCode.spec.agentName}")
    println("시도 횟수: ${result.attempts}")
    println("리뷰 승인: ${result.reviewResult.approved}")
    println("점수: ${result.reviewResult.score}/100")

    if (result.reviewResult.issues.isNotEmpty()) {
        println("\n이슈 목록:")
        result.reviewResult.issues.forEach { issue ->
            println("  [${issue.severity}] ${issue.location}: ${issue.description}")
        }
    }

    println("\n생성된 파일:")
    result.generatedCode.files.forEach { file ->
        println("  - ${file.path}")
    }
}
