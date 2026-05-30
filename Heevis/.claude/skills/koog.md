# JetBrains Koog 레퍼런스 스킬

> 기준 버전: **Koog 1.0.0** (2026-05-21 릴리즈, 최신 안정화)
> 공식 문서: https://docs.koog.ai/
> GitHub: https://github.com/JetBrains/koog

---

## Gradle 의존성

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.koog:koog-agents:1.0.0")
    implementation("ai.koog:koog-agents-additions:1.0.0-beta")  // 확장 기능 (베타)

    // Spring Boot 사용 시
    implementation("ai.koog:koog-spring-boot-starter:1.0.0")

    // Ktor 사용 시
    implementation("ai.koog:koog-ktor:1.0.0")
}
```

---

## 핵심 아키텍처

### Agent 유형
| Agent 유형 | 설명 |
|-----------|------|
| **Basic Agent** | 사전 정의된 전략으로 동작하는 기본 에이전트 |
| **Functional Agent** | 람다 함수로 정의하는 경량 에이전트 |
| **Graph-based Agent** | 커스텀 워크플로우 그래프 기반 에이전트 |
| **Planner Agent** | 계획을 반복적으로 수립·실행하는 에이전트 |

---

## LLM Provider 설정

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.llm.OllamaLLMClient
import ai.koog.prompt.llm.AnthropicLLMClient
import ai.koog.prompt.llm.OpenAILLMClient

// Anthropic Claude
val anthropicClient = AnthropicLLMClient(apiKey = System.getenv("ANTHROPIC_API_KEY"))

// OpenAI
val openaiClient = OpenAILLMClient(apiKey = System.getenv("OPENAI_API_KEY"))

// Ollama (로컬)
val ollamaClient = OllamaLLMClient(baseUrl = "http://localhost:11434")
```

---

## Tool 정의

### 어노테이션 기반 (권장)
```kotlin
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.ToolParam

class WeatherTools {
    @Tool("get_weather")
    suspend fun getWeather(
        @ToolParam("도시 이름") city: String,
        @ToolParam("온도 단위 (celsius/fahrenheit)", required = false) unit: String = "celsius"
    ): String {
        // 실제 날씨 API 호출
        return "서울: 22°C"
    }

    @Tool("search_web")
    suspend fun searchWeb(
        @ToolParam("검색 쿼리") query: String
    ): String {
        return "검색 결과..."
    }
}
```

### Tool Registry 등록
```kotlin
val toolRegistry = ToolRegistry {
    tools(WeatherTools())
}
```

---

## Agent 생성 & 실행

### Basic Agent
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.agent

val agent = agent {
    config = AIAgentConfig(
        prompt = "당신은 날씨 정보를 제공하는 도우미입니다.",
        model = "claude-opus-4-8",
        tools = toolRegistry
    )
    llmClient = anthropicClient
}

// 단순 실행
val response = agent.run("서울 날씨 알려줘")
println(response)

// 스트리밍
agent.runStreaming("서울 날씨 알려줘").collect { chunk ->
    print(chunk)
}
```

### Functional Agent
```kotlin
val functionalAgent = functionalAgent(llmClient = anthropicClient) {
    systemPrompt = "당신은 코딩 도우미입니다."
    model = "claude-sonnet-4-6"
    tools(WeatherTools())
}

val result = functionalAgent.run("Kotlin coroutine 예제 작성해줘")
```

---

## Graph-based Agent (워크플로우)

```kotlin
import ai.koog.agents.core.dsl.graph.agentGraph
import ai.koog.agents.core.strategy.AgentStrategy

val graphAgent = agentGraph {
    // 노드 정의
    val inputNode  = node("input")  { input -> process(input) }
    val llmNode    = llmNode("llm")
    val outputNode = node("output") { result -> format(result) }

    // 엣지 연결 (워크플로우 흐름 정의)
    inputNode  edgeTo llmNode
    llmNode    edgeTo outputNode

    // 조건부 분기
    llmNode.onCondition({ it.contains("오류") }) { errorNode }
}
```

---

## Prompt 구성

```kotlin
import ai.koog.prompt.dsl.prompt

val myPrompt = prompt {
    system("당신은 AI Agent 개발 전문가입니다.")

    user("Kotlin으로 간단한 에이전트 만드는 방법 알려줘")

    // 멀티모달 (이미지 포함)
    user {
        text("이 이미지를 분석해줘")
        image(imageBytes, mimeType = "image/png")
    }
}
```

---

## History Compression (토큰 최적화)

```kotlin
val agent = agent {
    config = AIAgentConfig(
        prompt = "시스템 프롬프트",
        model = "claude-sonnet-4-6",
        historyCompression = HistoryCompression(
            strategy = CompressionStrategy.SUMMARIZE,
            threshold = 8000  // 토큰 수 임계값
        )
    )
    llmClient = anthropicClient
}
```

---

## Agent Persistence (상태 저장/복원)

```kotlin
import ai.koog.agents.core.persistence.AgentPersistence
import ai.koog.agents.core.persistence.FileSystemPersistence

val persistence = FileSystemPersistence(path = "./agent-state")

val agent = agent {
    config = AIAgentConfig(prompt = "...", model = "...")
    llmClient = anthropicClient
    persistence = persistence
}

// 상태 저장 후 복원
val sessionId = agent.run("작업 시작")
val restored  = agent.restore(sessionId)
```

---

## Structured Output

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class AnalysisResult(
    val sentiment: String,
    val score: Double,
    val keywords: List<String>
)

val result: AnalysisResult = agent.runStructured(
    "이 텍스트의 감정을 분석해줘: '오늘 정말 좋은 날이야'",
    outputSchema = AnalysisResult::class
)
println("감정: ${result.sentiment}, 점수: ${result.score}")
```

---

## OpenTelemetry 모니터링

```kotlin
import ai.koog.agents.core.observability.OpenTelemetryObservability

val observability = OpenTelemetryObservability(
    serviceName = "heevis-agent",
    endpoint = "http://localhost:4317"
)

val agent = agent {
    config = AIAgentConfig(prompt = "...", model = "...")
    llmClient = anthropicClient
    observability = observability
}
```

---

## 지원 LLM Provider

| Provider | 클래스 | 환경변수 |
|---------|-------|---------|
| Anthropic | `AnthropicLLMClient` | `ANTHROPIC_API_KEY` |
| OpenAI | `OpenAILLMClient` | `OPENAI_API_KEY` |
| Google | `GoogleLLMClient` | `GOOGLE_API_KEY` |
| DeepSeek | `DeepSeekLLMClient` | `DEEPSEEK_API_KEY` |
| Ollama | `OllamaLLMClient` | - (로컬 URL) |
| AWS Bedrock | `BedrockLLMClient` | AWS 자격증명 |

---

## 참고 링크
- 공식 문서: https://docs.koog.ai/
- GitHub: https://github.com/JetBrains/koog
- Maven Central: https://central.sonatype.com/search?q=ai.koog
