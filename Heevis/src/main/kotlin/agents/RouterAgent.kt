package org.example.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import org.example.llm.LLMConfig
import org.example.tools.SpecializedTools

class RouterAgent(private val config: LLMConfig = LLMConfig.Ollama(modelId = "llama3.1:8b")) {

    private val systemPrompt = """
        당신은 Heevis AI 어시스턴트입니다.
        사용자 요청을 분석하여 직접 답변하거나 적절한 도구를 사용합니다.

        도구 사용 기준:
        - 이미지 생성/그림 그려줘 → generateImage 도구
        - Agent 만들어줘/개발해줘/설계해줘 → buildAgent 도구
        - 복잡한 분석/연구/전문적 판단 → deepAnalysis 도구
        - 그 외 일반 대화와 질문 → 직접 답변

        항상 한국어로 응답하세요.
    """.trimIndent()

    private val toolRegistry = ToolRegistry {
        tools(SpecializedTools().asTools())
    }

    suspend fun chat(userMessage: String): String {
        config.createExecutor().use { executor ->
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = config.model,
                systemPrompt = systemPrompt,
                toolRegistry = toolRegistry,
                temperature = 0.7
            )
            return agent.run(userMessage) ?: "응답을 받지 못했습니다."
        }
    }
}
