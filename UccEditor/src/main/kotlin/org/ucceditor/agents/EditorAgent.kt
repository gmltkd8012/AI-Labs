package org.ucceditor.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import org.ucceditor.llm.LLMConfig
import org.ucceditor.tools.FFmpegTools

/**
 * 동영상 편집 Agent.
 *
 * 역할:
 *  1. 사용자의 편집 요청과 (학습된) 편집 패턴을 해석한다.
 *  2. [FFmpegTools] 도구를 조합해 실제 편집을 수행한다.
 *  3. 수행 결과를 사용자에게 한국어로 보고한다.
 *
 * 이 클래스는 초기 골격입니다. 패턴 학습(예시 영상 분석 → EditPlan 추출)과
 * 다단계 전략은 이후 단계에서 확장합니다. 설계 메모는 ARCHITECTURE.md 참고.
 *
 * LLM은 **Google Gemini로 고정**합니다(`GOOGLE_API_KEY` 환경변수 필요).
 * Heevis와 달리 Provider 스위칭을 두지 않고, 모델 변형만 선택할 수 있습니다.
 */
class EditorAgent(
    geminiModelId: String = "gemini-2.0-flash",
) {
    private val config: LLMConfig = LLMConfig.Google(modelId = geminiModelId)

    private val systemPrompt = """
        당신은 UccEditor, 동영상 편집 전문 AI 에이전트입니다.

        목표:
        - 사용자의 편집 의도를 파악하고, 제공된 편집 도구를 사용해 실제 영상 파일을 편집합니다.
        - 편집 전에는 probe 도구로 소스를 분석해 길이/해상도를 확인하세요.
        - 자르기(trim), 결합(concat), 속도 변경(changeSpeed), 자막(overlayText),
          오디오 추출(extractAudio) 도구를 적절히 조합하세요.

        편집 패턴:
        - "숏폼/유튜브 훅": 앞부분을 짧게 trim 하고 상단/하단에 강조 텍스트를 overlayText.
        - "브이로그 점프컷": 불필요한 구간을 잘라 여러 클립으로 만든 뒤 concat.
        - 패턴을 적용할 때는 왜 그 편집을 했는지 한 줄로 설명하세요.

        규칙:
        - 파일이 없으면 추측하지 말고 사용자에게 경로를 물어보세요.
        - 결과 파일 경로를 항상 사용자에게 알려주세요.
        - 항상 한국어로 응답하세요.
    """.trimIndent()

    private val toolRegistry = ToolRegistry {
        tools(FFmpegTools().asTools())
    }

    private val history = mutableListOf<Pair<String, String>>()
    private val executor = config.createExecutor()

    suspend fun chat(userMessage: String): String {
        val agentConfig = AIAgentConfig(
            prompt = prompt(id = "uccedit", params = LLMParams(temperature = 0.3)) {
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
            agent.run(userMessage)
        } catch (e: Exception) {
            if (isAuthError(e)) {
                "Gemini 인증에 실패했습니다. GOOGLE_API_KEY가 올바른지 확인하세요."
            } else {
                "오류가 발생했습니다: ${e.message}"
            }
        }
        history.add(userMessage to response)
        return response
    }

    fun close() = executor.close()

    private fun isAuthError(e: Exception): Boolean {
        val msg = "${e.message} ${e.cause?.message}".lowercase()
        return listOf("401", "403", "unauthenticated", "permission", "api key", "api_key")
            .any { it in msg }
    }
}
