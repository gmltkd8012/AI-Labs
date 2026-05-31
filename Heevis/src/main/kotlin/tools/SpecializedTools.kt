package org.example.tools

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.llm.AnthropicVariant
import org.example.llm.LLMConfig
import org.example.model.UserRequirement
import org.example.orchestrator.Orchestrator

class SpecializedTools : ToolSet {

    @Tool
    @LLMDescription("사용자가 이미지 생성을 요청할 때 사용. Gemini API를 통해 처리합니다.")
    suspend fun generateImage(
        @LLMDescription("생성할 이미지 설명 (최대한 구체적으로)") prompt: String
    ): String {
        val apiKey = System.getenv("GOOGLE_API_KEY")
            ?: return "GOOGLE_API_KEY가 설정되지 않았습니다. 터미널에서 'export GOOGLE_API_KEY=your_key' 를 실행해주세요."

        return try {
            val config = LLMConfig.Google("gemini-2.0-flash")
            config.createExecutor().use { executor ->
                val agent = AIAgent(
                    promptExecutor = executor,
                    llmModel = config.model,
                    systemPrompt = "당신은 이미지 생성 전문가입니다. 요청받은 이미지를 상세히 묘사하고, 실제 이미지 생성 API 호출 결과를 반환합니다."
                )
                agent.run("이미지 생성 요청: $prompt") ?: "Gemini 응답 없음"
            }
        } catch (e: Exception) {
            "이미지 생성 오류: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("AI Agent 설계 및 구현이 필요할 때 사용. Claude(우선) 또는 GPT-4o를 통해 Kotlin Koog 기반 Agent 코드를 생성합니다.")
    suspend fun buildAgent(
        @LLMDescription("만들고자 하는 Agent의 역할과 기능 설명") requirement: String
    ): String {
        val (orchestrator, modelName) = when {
            System.getenv("ANTHROPIC_API_KEY") != null ->
                Orchestrator.withPaidModel("claude") to "Claude Sonnet 4.6"
            System.getenv("OPENAI_API_KEY") != null ->
                Orchestrator.withPaidModel("openai") to "GPT-4o"
            else -> return "Agent 생성에는 유료 모델이 필요합니다.\n" +
                "Claude: export ANTHROPIC_API_KEY=your_key\n" +
                "GPT-4o: export OPENAI_API_KEY=your_key"
        }

        return try {
            println("[buildAgent] $modelName 사용")
            val result = orchestrator.run(UserRequirement(description = requirement))
            buildString {
                appendLine("Agent 생성 완료: ${result.generatedCode.spec.agentName}")
                appendLine("사용 모델: $modelName")
                appendLine("역할: ${result.generatedCode.spec.role}")
                appendLine("리뷰 결과: ${if (result.reviewResult.approved) "승인" else "미승인"} (${result.reviewResult.score}/100점)")
                appendLine("생성 파일:")
                result.generatedCode.files.forEach { appendLine("  - ${it.path}") }
            }.trimEnd()
        } catch (e: Exception) {
            "Agent 생성 오류: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("복잡한 분석, 심층 연구, 전문적 판단이 필요할 때 Claude를 통해 처리합니다.")
    suspend fun deepAnalysis(
        @LLMDescription("분석이 필요한 내용 또는 질문") query: String
    ): String {
        System.getenv("ANTHROPIC_API_KEY")
            ?: return "ANTHROPIC_API_KEY가 설정되지 않았습니다. 터미널에서 'export ANTHROPIC_API_KEY=your_key' 를 실행해주세요."

        return try {
            val config = LLMConfig.Anthropic(AnthropicVariant.SONNET_4_6)
            config.createExecutor().use { executor ->
                val agent = AIAgent(
                    promptExecutor = executor,
                    llmModel = config.model,
                    systemPrompt = "당신은 심층 분석 전문가입니다. 복잡한 질문에 상세하고 정확한 답변을 제공합니다."
                )
                agent.run(query) ?: "Claude 응답 없음"
            }
        } catch (e: Exception) {
            "분석 오류: ${e.message}"
        }
    }
}
