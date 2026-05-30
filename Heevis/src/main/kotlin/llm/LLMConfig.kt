package org.example.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

sealed class LLMConfig {
    abstract fun createExecutor(): MultiLLMPromptExecutor
    abstract val model: LLModel

    data class Ollama(
        val modelId: String = "qwen2.5-coder:7b",
        val baseUrl: String = "http://localhost:11434"
    ) : LLMConfig() {
        override fun createExecutor() = simpleOllamaAIExecutor(baseUrl)
        override val model = LLModel(
            provider = LLMProvider.Ollama,
            id = modelId,
            capabilities = listOf(
                LLMCapability.Completion,
                LLMCapability.Tools,
                LLMCapability.Temperature
            )
        )
    }

    data class Anthropic(val variant: AnthropicVariant = AnthropicVariant.SONNET_4_6) : LLMConfig() {
        override fun createExecutor() = simpleAnthropicExecutor(
            apiKey = System.getenv("ANTHROPIC_API_KEY")
                ?: error("ANTHROPIC_API_KEY 환경변수가 설정되지 않았습니다")
        )
        override val model = when (variant) {
            AnthropicVariant.HAIKU_4_5  -> AnthropicModels.Haiku_4_5
            AnthropicVariant.SONNET_4_6 -> AnthropicModels.Sonnet_4_6
            AnthropicVariant.OPUS_4_7   -> AnthropicModels.Opus_4_7
        }
    }

    data class OpenAI(val variant: OpenAIVariant = OpenAIVariant.GPT4O_MINI) : LLMConfig() {
        override fun createExecutor() = simpleOpenAIExecutor(
            apiToken = System.getenv("OPENAI_API_KEY")
                ?: error("OPENAI_API_KEY 환경변수가 설정되지 않았습니다")
        )
        override val model = when (variant) {
            OpenAIVariant.GPT4O      -> OpenAIModels.Chat.GPT4o
            OpenAIVariant.GPT4O_MINI -> OpenAIModels.Chat.GPT4oMini
        }
    }

    data class Google(val modelId: String = "gemini-2.0-flash") : LLMConfig() {
        override fun createExecutor() = simpleGoogleAIExecutor(
            apiKey = System.getenv("GOOGLE_API_KEY")
                ?: error("GOOGLE_API_KEY 환경변수가 설정되지 않았습니다")
        )
        override val model = LLModel(
            provider = LLMProvider.Google,
            id = modelId,
            capabilities = listOf(
                LLMCapability.Completion,
                LLMCapability.Tools,
                LLMCapability.Temperature
            )
        )
    }
}

enum class AnthropicVariant { HAIKU_4_5, SONNET_4_6, OPUS_4_7 }
enum class OpenAIVariant { GPT4O, GPT4O_MINI }
