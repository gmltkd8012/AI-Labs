# LLM 모델 레퍼런스 스킬

> spec-agent 전용 — Agent 설계 시 모델 선택 기준으로 활용
> 기본 전략: **Ollama(무료) 우선**, 사용자 요청 시 유료 모델 API 추가

---

## 기본 전략: Ollama (로컬 무료)

개발 및 기본 실행은 Ollama를 기본으로 사용합니다.
API 비용 없이 로컬에서 실행되므로 개발·테스트에 최적입니다.

```kotlin
// Executor 생성 (koog-ktor 포함 시 httpClientFactory 생략 가능)
simpleOllamaAIExecutor(baseUrl = "http://localhost:11434")

// 모델 정의 (Koog는 Ollama 모델 상수 없음 — LLModel 직접 생성)
LLModel(
    provider = LLMProvider.Ollama,
    id = "qwen2.5-coder:7b",
    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools, LLMCapability.Temperature)
)
```

### 추천 Ollama 모델

| 모델 ID | 크기 | 용도 | 권장 Agent |
|--------|------|------|-----------|
| `qwen2.5-coder:7b` | 7B | 코드 생성/수정 최특화 | code-agent, review-agent |
| `qwen2.5-coder:14b` | 14B | 더 정확한 코드 생성 | code-agent (고품질) |
| `llama3.1:8b` | 8B | 범용 추론, 설계 | spec-agent |
| `deepseek-coder-v2:16b` | 16B | GPT-4 수준 코딩 | code-agent (고성능) |
| `phi4:14b` | 14B | 수학·논리 추론 | spec-agent (복잡 설계) |
| `qwen2.5:7b` | 7B | 다국어 범용 | spec-agent |

**설치:** `ollama pull qwen2.5-coder:7b`
**실행 확인:** `ollama list`

---

## 유료 모델 — 사용자 요청 시 활성화

### Anthropic Claude

> 환경변수: `ANTHROPIC_API_KEY`

```kotlin
simpleAnthropicExecutor(apiKey = System.getenv("ANTHROPIC_API_KEY"))
```

| Koog 상수 | API ID | 컨텍스트 | 특징 | 가격 (입력/출력 MTok) |
|---------|-------|---------|------|-------------------|
| `AnthropicModels.Haiku_4_5` | `claude-haiku-4-5` | 200K | 가장 빠름, 경량 | $1 / $5 |
| `AnthropicModels.Sonnet_4_6` | `claude-sonnet-4-6` | 1M | 속도·지능 균형, **Agent 권장** | $3 / $15 |
| `AnthropicModels.Opus_4_7` | `claude-opus-4-7` | 1M | 최고 지능, 복잡 추론 | $5 / $25 |

```kotlin
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
// 사용: AnthropicModels.Sonnet_4_6
```

---

### OpenAI / GPT

> 환경변수: `OPENAI_API_KEY`

```kotlin
simpleOpenAIExecutor(apiKey = System.getenv("OPENAI_API_KEY"))
```

| Koog 상수 | API ID | 컨텍스트 | 특징 | 가격 (입력/출력 MTok) |
|---------|-------|---------|------|-------------------|
| `OpenAIModels.Chat.GPT4oMini` | `gpt-4o-mini` | 128K | 빠르고 저렴 | ~$0.15 / $0.60 |
| `OpenAIModels.Chat.GPT4o` | `gpt-4o` | 128K | 고성능 범용 | ~$2.50 / $10 |

> **Codex 관련:** OpenAI Codex는 현재 클라우드 에이전트 제품입니다.
> API를 통한 코딩 작업에는 `gpt-4o` 또는 `gpt-4o-mini` 사용을 권장합니다.

```kotlin
import ai.koog.prompt.executor.clients.openai.OpenAIModels
// 사용: OpenAIModels.Chat.GPT4o
```

---

### Google Gemini

> 환경변수: `GOOGLE_API_KEY`

```kotlin
simpleGoogleAIExecutor(apiKey = System.getenv("GOOGLE_API_KEY"))
```

| 모델 ID (문자열) | 컨텍스트 | 특징 | 가격 |
|--------------|---------|------|------|
| `gemini-2.0-flash` | 1M | 빠른 응답, 범용 | 무료 등급 있음 |
| `gemini-2.5-pro` | 1M | 고성능 추론 | 유료 |
| `gemini-2.5-flash` | 1M | 속도·비용 균형 | 저렴 |

```kotlin
// Google은 Koog에서 LLModel 직접 생성
LLModel(
    provider = LLMProvider.Google,
    id = "gemini-2.0-flash",
    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools, LLMCapability.Temperature)
)
```

---

## Agent별 권장 모델 매핑

| Agent | 기본 (Ollama) | 고품질 요청 시 |
|-------|-------------|--------------|
| `spec-agent` | `llama3.1:8b` | `AnthropicModels.Sonnet_4_6` |
| `code-agent` | `qwen2.5-coder:7b` | `AnthropicModels.Sonnet_4_6` 또는 `OpenAIModels.Chat.GPT4o` |
| `review-agent` | `qwen2.5-coder:7b` | `AnthropicModels.Opus_4_7` |

---

## Executor import 경로

```kotlin
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMCapability
```
