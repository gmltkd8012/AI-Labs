package org.example.tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.serialization.json.*
import org.example.llm.AnthropicVariant
import org.example.llm.LLMConfig
import org.example.model.UserRequirement
import org.example.orchestrator.Orchestrator
import java.io.File
import java.util.Base64

class SpecializedTools : ToolSet {

    @Tool
    @LLMDescription("사용자가 이미지 생성을 요청할 때 사용. Gemini API를 통해 이미지를 생성하고 파일로 저장합니다.")
    suspend fun generateImage(
        @LLMDescription("생성할 이미지 설명 (최대한 구체적으로)") prompt: String
    ): String {
        val apiKey = System.getenv("GOOGLE_API_KEY")
            ?: return "GOOGLE_API_KEY가 설정되지 않았습니다. 터미널에서 'export GOOGLE_API_KEY=your_key' 를 실행해주세요."

        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        return try {
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject { put("text", prompt) }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    putJsonArray("responseModalities") {
                        add("IMAGE")
                        add("TEXT")
                    }
                }
            }

            val response: JsonObject = client.post(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-preview-image-generation:generateContent"
            ) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }.body()

            val parts = response["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray
                ?: return "이미지 생성 실패: 응답에서 콘텐츠를 찾을 수 없습니다."

            val imagePart = parts.firstOrNull { it.jsonObject.containsKey("inlineData") }
                ?: return "이미지 생성 실패: 응답에 이미지 데이터가 없습니다.\n" +
                    parts.joinToString("\n") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }

            val base64Data = imagePart.jsonObject["inlineData"]!!
                .jsonObject["data"]!!.jsonPrimitive.content
            val mimeType = imagePart.jsonObject["inlineData"]!!
                .jsonObject["mimeType"]!!.jsonPrimitive.content

            val extension = mimeType.substringAfter("/").replace("jpeg", "jpg")
            val outputDir = File("generated_images").also { it.mkdirs() }
            val timestamp = System.currentTimeMillis()
            val outputFile = File(outputDir, "image_$timestamp.$extension")
            outputFile.writeBytes(Base64.getDecoder().decode(base64Data))

            "이미지 생성 완료: ${outputFile.absolutePath}"
        } catch (e: Exception) {
            "이미지 생성 오류: ${e.message}"
        } finally {
            client.close()
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

            val savedPaths = result.generatedCode.files.mapNotNull { codeFile ->
                runCatching {
                    val file = File(codeFile.path).also { it.parentFile?.mkdirs() }
                    file.writeText(codeFile.content)
                    codeFile.path
                }.getOrNull()
            }

            buildString {
                appendLine("Agent 생성 완료: ${result.generatedCode.spec.agentName}")
                appendLine("사용 모델: $modelName")
                appendLine("역할: ${result.generatedCode.spec.role}")
                appendLine("리뷰 결과: ${if (result.reviewResult.approved) "승인" else "미승인"} (${result.reviewResult.score}/100점)")
                appendLine("저장된 파일 (${savedPaths.size}개):")
                savedPaths.forEach { appendLine("  - $it") }
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
