package org.example.agents

import ai.koog.agents.core.agent.AIAgent
import kotlinx.serialization.json.Json
import org.example.llm.LLMConfig
import org.example.model.GeneratedCode
import org.example.model.ReviewResult

class ReviewAgent(private val config: LLMConfig = LLMConfig.Ollama(modelId = "qwen2.5-coder:7b")) {

    private val systemPrompt = """
        당신은 JetBrains Koog 프레임워크 전문 Kotlin 코드 리뷰 Agent입니다.

        리뷰 기준:
        1. Koog API 올바른 사용 (executor .use{} 블록, AIAgent 생성 패턴)
        2. Kotlin 코딩 컨벤션 및 관용 표현 (data class, sealed class, scope functions)
        3. 코루틴 올바른 사용 (structured concurrency, CancellationException 처리)
        4. 리소스 누수 여부 (executor, closeable 객체)
        5. 오류 처리 적절성 (null 처리, error() vs require() 구분)
        6. 보안 (API 키 하드코딩 없음, 환경변수 사용)

        심각도 기준:
        - CRITICAL: 런타임 오류, 리소스 누수, 보안 취약점
        - MAJOR: 컨벤션 위반, 불필요한 복잡성, 잘못된 코루틴 패턴
        - MINOR: 네이밍 개선, 스타일 제안

        응답은 반드시 순수 JSON만 반환하세요 (마크다운 코드블록 없이).
    """.trimIndent()

    suspend fun review(generatedCode: GeneratedCode): ReviewResult {
        val codeContent = generatedCode.files.joinToString("\n\n---\n\n") { file ->
            "// File: ${file.path}\n${file.content}"
        }

        val prompt = buildString {
            appendLine("다음 코드를 설계 명세 대비 리뷰하고 JSON으로 결과를 반환하세요.")
            appendLine()
            appendLine("=== 설계 명세 ===")
            appendLine("Agent: ${generatedCode.spec.agentName}")
            appendLine("역할: ${generatedCode.spec.role}")
            appendLine("필요 Tool: ${generatedCode.spec.requiredTools.joinToString(", ")}")
            appendLine()
            appendLine("=== 코드 ===")
            appendLine(codeContent)
            appendLine()
            appendLine("반환 JSON 형식:")
            appendLine("""{
  "approved": true 또는 false (CRITICAL 이슈 없을 때 true),
  "score": 0~100 점수,
  "issues": [
    {
      "severity": "CRITICAL|MAJOR|MINOR",
      "location": "파일명:라인 또는 함수명",
      "description": "문제 설명",
      "suggestion": "개선 방법"
    }
  ],
  "suggestions": ["전반적인 개선 제안 목록"]
}""")
        }

        config.createExecutor().use { executor ->
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = config.model,
                systemPrompt = systemPrompt,
                temperature = 0.1
            )
            val raw = agent.run(prompt) ?: error("ReviewAgent 응답 없음")
            val jsonStart = raw.indexOf('{')
            val jsonEnd = raw.lastIndexOf('}') + 1
            check(jsonStart >= 0 && jsonEnd > jsonStart) { "ReviewAgent가 유효한 JSON을 반환하지 않았습니다:\n$raw" }
            return Json { ignoreUnknownKeys = true }.decodeFromString(raw.substring(jsonStart, jsonEnd))
        }
    }
}
