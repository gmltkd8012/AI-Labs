package org.example.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
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

    private val history = mutableListOf<Pair<String, String>>()
    private val executor = config.createExecutor()

    suspend fun chat(userMessage: String): String {
        val agentConfig = AIAgentConfig(
            prompt = prompt(id = "chat", params = LLMParams(temperature = 0.7)) {
                system(systemPrompt)
                history.forEach { (user, assistant) ->
                    user(user)
                    assistant(assistant)
                }
            },
            model = config.model,
            maxAgentIterations = 50
        )

        val agent = AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry
        )

        val response = try {
            agent.run(userMessage) ?: "응답을 받지 못했습니다."
        } catch (e: Exception) {
            if (isOllamaConnectionError(e)) {
                "Ollama 서버에 연결할 수 없습니다. 별도 터미널에서 'ollama serve' 를 먼저 실행해주세요."
            } else {
                "오류가 발생했습니다: ${e.message}"
            }
        }
        history.add(userMessage to response)
        return response
    }

    fun close() = executor.close()

    private fun isOllamaConnectionError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        val cause = e.cause?.message?.lowercase() ?: ""
        return listOf("connection refused", "connect timed out", "failed to connect", "localhost:11434")
            .any { it in msg || it in cause }
    }
}
