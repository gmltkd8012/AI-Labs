# Heevis AI Architecture

## 전체 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                         사용자 (터미널)                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │ 입력 / 출력
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      RouterAgent                                │
│                   모델: Ollama (무료·로컬)                        │
│              항상 사용자와 직접 소통하는 유일한 창구               │
│                                                                 │
│  판단 기준                                                       │
│  ├── 일반 대화/질문   → 직접 답변 (Ollama)                        │
│  ├── 이미지 생성      → generateImage Tool                       │
│  ├── Agent 개발 요청  → buildAgent Tool                          │
│  └── 복잡한 분석      → deepAnalysis Tool                        │
└──────┬──────────────────┬───────────────────────┬───────────────┘
       │                  │                       │
       ▼                  ▼                       ▼
┌─────────────┐  ┌────────────────────┐  ┌──────────────────────┐
│generateImage│  │    buildAgent      │  │    deepAnalysis      │
│    Tool     │  │      Tool          │  │       Tool           │
│             │  │                    │  │                      │
│ Gemini API  │  │  Orchestrator      │  │   Claude API         │
│(GOOGLE_API  │  │  (파이프라인 실행)   │  │ (ANTHROPIC_API_KEY) │
│   _KEY)     │  │ Claude 우선        │  │                      │
│             │  │ GPT-4o 대체        │  │                      │
└─────────────┘  └────────┬───────────┘  └──────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   ┌─────────────┐ ┌───────────┐ ┌──────────────┐
   │  SpecAgent  │ │ CodeAgent │ │ ReviewAgent  │
   │             │ │           │ │              │
   │ 요구사항 분석│ │코드 생성  │ │ 코드 리뷰    │
   │ → 설계 명세 │→│→ Kotlin   │→│ → 승인/이슈  │
   │             │ │   코드    │ │              │
   │  Claude     │ │  Claude   │ │   Claude     │
   │ Sonnet 4.6  │ │Sonnet 4.6 │ │ Sonnet 4.6   │
   │  또는 GPT-4o│ │또는 GPT-4o│ │ 또는 GPT-4o  │
   └─────────────┘ └───────────┘ └──────────────┘
```

---

## 컴포넌트 설명

### RouterAgent
| 항목 | 내용 |
|------|------|
| 파일 | `agents/RouterAgent.kt` |
| 모델 | `Ollama llama3.1:8b` |
| 역할 | 사용자와 항상 직접 소통. 요청 유형을 판단하여 적절한 Tool 호출 또는 직접 답변 |
| 비용 | 무료 (로컬 실행) |

### SpecializedTools
| Tool | 파일 | 위임 모델 | 필요 환경변수 |
|------|------|---------|-------------|
| `generateImage` | `tools/SpecializedTools.kt` | Gemini (`gemini-2.0-flash`) | `GOOGLE_API_KEY` |
| `buildAgent` | `tools/SpecializedTools.kt` | Claude Sonnet 4.6 (우선) → GPT-4o (대체) | `ANTHROPIC_API_KEY` 또는 `OPENAI_API_KEY` |
| `deepAnalysis` | `tools/SpecializedTools.kt` | Claude (`Sonnet 4.6`) | `ANTHROPIC_API_KEY` |

### Agent 파이프라인 (buildAgent 내부)
| 순서 | Agent | 파일 | 역할 | 사용 모델 | 입력 → 출력 |
|------|-------|------|------|---------|------------|
| 1 | SpecAgent | `agents/SpecAgent.kt` | 요구사항 분석 및 설계 | Claude Sonnet 4.6 / GPT-4o | `UserRequirement` → `AgentSpec` |
| 2 | CodeAgent | `agents/CodeAgent.kt` | Kotlin Koog 코드 생성 | Claude Sonnet 4.6 / GPT-4o | `AgentSpec` → `GeneratedCode` |
| 3 | ReviewAgent | `agents/ReviewAgent.kt` | 코드 품질 리뷰 | Claude Sonnet 4.6 / GPT-4o | `GeneratedCode` → `ReviewResult` |

> 모델 우선순위: `ANTHROPIC_API_KEY` 있으면 Claude, 없으면 `OPENAI_API_KEY`로 GPT-4o 사용
> ReviewAgent가 CRITICAL 이슈 발견 시 CodeAgent 재실행 (최대 2회)

---

## 데이터 흐름

```
UserRequirement
      │
      ▼ SpecAgent
  AgentSpec
      │
      ▼ CodeAgent
  GeneratedCode
   ├── AgentSpec
   └── List<CodeFile>
      │
      ▼ ReviewAgent
  ReviewResult
   ├── approved: Boolean
   ├── score: Int (0~100)
   ├── issues: List<ReviewIssue>
   └── suggestions: List<String>
```

---

## LLM 구성

```
LLMConfig (sealed class)
├── Ollama(modelId, baseUrl)         → simpleOllamaAIExecutor  [무료·기본]
├── Anthropic(variant)               → simpleAnthropicExecutor [ANTHROPIC_API_KEY]
│    ├── HAIKU_4_5
│    ├── SONNET_4_6  ← 기본
│    └── OPUS_4_7
├── OpenAI(variant)                  → simpleOpenAIExecutor    [OPENAI_API_KEY]
│    ├── GPT4O
│    └── GPT4O_MINI  ← 기본
└── Google(modelId)                  → simpleGoogleAIExecutor  [GOOGLE_API_KEY]
     └── gemini-2.0-flash  ← 기본
```

---

## 파일 구조

```
src/main/kotlin/
├── Main.kt                      진입점, 대화 루프
├── agents/
│   ├── RouterAgent.kt           사용자 인터페이스 Agent (Ollama)
│   ├── SpecAgent.kt             설계 명세 생성
│   ├── CodeAgent.kt             Kotlin 코드 생성
│   └── ReviewAgent.kt           코드 리뷰
├── tools/
│   └── SpecializedTools.kt      유료 모델 위임 Tool 모음
├── orchestrator/
│   └── Orchestrator.kt          spec→code→review 파이프라인
├── model/
│   └── AgentContract.kt         Agent 간 데이터 계약 (data class)
└── llm/
    └── LLMConfig.kt             LLM Provider 설정 및 executor 팩토리

.claude/skills/
├── kotlin.md                    Kotlin 언어 레퍼런스
├── coroutines.md                Coroutines 레퍼런스
├── koog.md                      Koog 프레임워크 레퍼런스
├── llm-models.md                LLM 모델 선택 기준 (spec-agent용)
└── SKILLS_MAP.md                Agent별 스킬 참조 매핑
```

---

## 실행 방법

```bash
# 1. Ollama 서버 시작 (별도 터미널 유지)
ollama serve

# 2. 모델 다운로드 (최초 1회)
ollama pull llama3.1:8b
ollama pull qwen2.5-coder:7b

# 3. 유료 모델 환경변수 설정 (선택)
export ANTHROPIC_API_KEY=your_key   # buildAgent (우선), deepAnalysis
export OPENAI_API_KEY=your_key      # buildAgent (ANTHROPIC 없을 때 대체)
export GOOGLE_API_KEY=your_key      # generateImage

# 4. 실행
./gradlew run
```

---

## 의존성

```kotlin
koog-agents       : 1.0.0-preview7   // Koog 핵심 프레임워크
koog-ktor         : 1.0.0-beta-preview7  // JVM HTTP 클라이언트
kotlinx-coroutines: 1.11.0           // 비동기 처리
kotlinx-serialization-json: 1.8.1   // JSON 직렬화
Kotlin            : 2.3.21
JVM               : 21
```
