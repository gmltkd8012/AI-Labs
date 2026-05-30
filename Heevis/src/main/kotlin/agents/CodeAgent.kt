package org.example.agents

import ai.koog.agents.core.agent.AIAgent
import kotlinx.serialization.json.Json
import org.example.llm.LLMConfig
import org.example.model.AgentSpec
import org.example.model.CodeFile
import org.example.model.GeneratedCode

class CodeAgent(private val config: LLMConfig = LLMConfig.Ollama(modelId = "qwen2.5-coder:7b")) {

    private val systemPrompt = """
        당신은 JetBrains Koog 프레임워크 전문 Kotlin 코드 작성 Agent입니다.

        코딩 원칙:
        - Koog의 AIAgent, ToolSet, @Tool, @LLMDescription 패턴을 사용합니다
        - kotlinx.coroutines의 structured concurrency를 준수합니다
        - executor는 반드시 .use { } 블록으로 관리해 리소스를 해제합니다
        - 불필요한 주석 없이 명확한 네이밍으로 자기 설명적 코드를 작성합니다
        - suspend 함수로 코루틴 친화적으로 설계합니다

        응답은 반드시 순수 JSON만 반환하세요 (마크다운 코드블록 없이).
    """.trimIndent()

    suspend fun generate(spec: AgentSpec): GeneratedCode {
        val prompt = buildString {
            appendLine("다음 설계 명세에 따라 Kotlin Koog Agent 코드를 작성하세요.")
            appendLine()
            appendLine("Agent 이름: ${spec.agentName}")
            appendLine("역할: ${spec.role}")
            appendLine("시스템 프롬프트: ${spec.systemPrompt}")
            appendLine("필요 Tool: ${spec.requiredTools.joinToString(", ")}")
            appendLine("권장 모델: ${spec.recommendedModel}")
            appendLine("아키텍처 안내: ${spec.architectureNotes}")
            appendLine()
            appendLine("반환 JSON 형식:")
            appendLine("""{
  "spec": ${ Json.encodeToString(AgentSpec.serializer(), spec) },
  "files": [
    {
      "path": "src/main/kotlin/agents/${spec.agentName}.kt",
      "content": "실제 Kotlin 코드 전체"
    },
    {
      "path": "src/main/kotlin/agents/${spec.agentName}Tools.kt",
      "content": "ToolSet 구현 코드 (Tool이 필요한 경우)"
    }
  ]
}""")
        }

        config.createExecutor().use { executor ->
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = config.model,
                systemPrompt = systemPrompt,
                temperature = 0.2
            )
            val raw = agent.run(prompt) ?: error("CodeAgent 응답 없음")
            val jsonStart = raw.indexOf('{')
            val jsonEnd = raw.lastIndexOf('}') + 1
            check(jsonStart >= 0 && jsonEnd > jsonStart) { "CodeAgent가 유효한 JSON을 반환하지 않았습니다:\n$raw" }
            return Json { ignoreUnknownKeys = true }.decodeFromString(raw.substring(jsonStart, jsonEnd))
        }
    }
}
