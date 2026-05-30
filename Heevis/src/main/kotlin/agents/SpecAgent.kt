package org.example.agents

import ai.koog.agents.core.agent.AIAgent
import kotlinx.serialization.json.Json
import org.example.llm.LLMConfig
import org.example.model.AgentSpec
import org.example.model.UserRequirement

class SpecAgent(private val config: LLMConfig = LLMConfig.Ollama(modelId = "llama3.1:8b")) {

    private val systemPrompt = """
        당신은 AI Agent 설계 전문가입니다.
        사용자 요구사항을 분석하여 JetBrains Koog 프레임워크 기반의 Kotlin AI Agent 설계 명세를 작성합니다.

        설계 원칙:
        - 기본 LLM은 항상 Ollama(로컬 무료)를 권장합니다
        - 사용자가 고성능을 요청할 때만 유료 모델(Claude, GPT, Gemini)을 제안합니다
        - Tool은 최소한으로 정의하고, 역할을 명확히 분리합니다
        - Koog의 ToolSet + @Tool + @LLMDescription 어노테이션 패턴을 활용합니다

        응답은 반드시 순수 JSON만 반환하세요 (마크다운 코드블록 없이).
    """.trimIndent()

    suspend fun design(requirement: UserRequirement): AgentSpec {
        val prompt = buildString {
            appendLine("다음 요구사항에 맞는 AI Agent 설계 명세를 JSON으로 작성하세요.")
            appendLine()
            appendLine("요구사항: ${requirement.description}")
            if (requirement.features.isNotEmpty()) {
                appendLine("필요 기능: ${requirement.features.joinToString(", ")}")
            }
            appendLine()
            appendLine("반환 JSON 형식:")
            appendLine("""{
  "agentName": "agent 클래스 이름 (PascalCase)",
  "role": "agent 역할 한 줄 설명",
  "systemPrompt": "agent에게 주입할 시스템 프롬프트",
  "requiredTools": ["Tool 이름 목록"],
  "recommendedModel": "권장 Ollama 모델 ID (예: qwen2.5-coder:7b)",
  "architectureNotes": "구현 시 주의사항 및 Koog 패턴 안내"
}""")
        }

        config.createExecutor().use { executor ->
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = config.model,
                systemPrompt = systemPrompt,
                temperature = 0.3
            )
            val raw = agent.run(prompt) ?: error("SpecAgent 응답 없음")
            val jsonStart = raw.indexOf('{')
            val jsonEnd = raw.lastIndexOf('}') + 1
            check(jsonStart >= 0 && jsonEnd > jsonStart) { "SpecAgent가 유효한 JSON을 반환하지 않았습니다:\n$raw" }
            return Json { ignoreUnknownKeys = true }.decodeFromString(raw.substring(jsonStart, jsonEnd))
        }
    }
}
