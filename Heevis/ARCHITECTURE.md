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

### RouterAgent 대화 히스토리
- `RouterAgent`는 `executor`를 인스턴스 수명 동안 유지하고, 매 `chat()` 호출마다 이전 (user, assistant) 쌍을 `prompt { }` DSL에 주입하여 **멀티턴 대화**를 지원합니다.
- Ollama 연결 실패(`connection refused` 등) 시 `ollama serve` 실행 안내 메시지를 반환합니다.
- 종료 시 `Main.kt`의 `finally` 블록에서 `router.close()`로 executor를 해제합니다.

### SpecializedTools
| Tool | 파일 | 위임 모델 | 동작 | 필요 환경변수 |
|------|------|---------|------|-------------|
| `generateImage` | `tools/SpecializedTools.kt` | Gemini (`gemini-2.0-flash-preview-image-generation`) | Ktor HTTP Client로 직접 호출 → base64 응답을 `generated_images/`에 파일 저장 | `GOOGLE_API_KEY` |
| `buildAgent` | `tools/SpecializedTools.kt` | Claude Sonnet 4.6 (우선) → GPT-4o (대체) | Orchestrator 파이프라인 실행 → 생성 코드를 디스크에 `.kt` 파일로 저장 | `ANTHROPIC_API_KEY` 또는 `OPENAI_API_KEY` |
| `deepAnalysis` | `tools/SpecializedTools.kt` | Claude (`Sonnet 4.6`) | 단일 Agent 실행으로 분석 답변 반환 | `ANTHROPIC_API_KEY` |

### Agent 파이프라인 (buildAgent 내부)
| 순서 | Agent | 파일 | 역할 | 사용 모델 | 입력 → 출력 |
|------|-------|------|------|---------|------------|
| 1 | SpecAgent | `agents/SpecAgent.kt` | 요구사항 분석 및 설계 | Claude Sonnet 4.6 / GPT-4o | `UserRequirement` → `AgentSpec` |
| 2 | CodeAgent | `agents/CodeAgent.kt` | Kotlin Koog 코드 생성 | Claude Sonnet 4.6 / GPT-4o | `AgentSpec` → `GeneratedCode` |
| 3 | ReviewAgent | `agents/ReviewAgent.kt` | 코드 품질 리뷰 | Claude Sonnet 4.6 / GPT-4o | `GeneratedCode` → `ReviewResult` |

> 모델 우선순위: `ANTHROPIC_API_KEY` 있으면 Claude, 없으면 `OPENAI_API_KEY`로 GPT-4o 사용
> ReviewAgent가 승인하지 않으면 CodeAgent 재실행 (최대 2회)
> **재시도 시 직전 리뷰 이슈를 `[심각도] 위치: 설명 → 제안` 형식으로 CodeAgent 프롬프트에 피드백 주입** → 같은 오류 반복 방지

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
   ├── issues: List<ReviewIssue>   ──┐ 미승인 시 피드백으로
   └── suggestions: List<String>     │ CodeAgent에 재주입 (최대 2회)
                                     ┘
      ▼ (승인 시)
  buildAgent 가 GeneratedCode.files 를 디스크에 .kt 파일로 저장
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
├── Main.kt                      진입점, 대화 루프 (finally에서 router.close())
├── agents/
│   ├── RouterAgent.kt           사용자 인터페이스 Agent (Ollama) + 대화 히스토리
│   ├── SpecAgent.kt             설계 명세 생성
│   ├── CodeAgent.kt             Kotlin 코드 생성 (리뷰 피드백 입력 지원)
│   └── ReviewAgent.kt           코드 리뷰
├── tools/
│   └── SpecializedTools.kt      유료 모델 위임 Tool 모음 (이미지 생성/파일 저장)
├── orchestrator/
│   └── Orchestrator.kt          spec→code→review 파이프라인 (피드백 루프)
├── model/
│   └── AgentContract.kt         Agent 간 데이터 계약 (data class)
└── llm/
    └── LLMConfig.kt             LLM Provider 설정 및 executor 팩토리

src/test/kotlin/
├── AgentContractTest.kt         데이터 계약 직렬화·심각도 정렬 테스트 (6)
└── OrchestratorFeedbackTest.kt  리뷰 피드백 포맷·재시도 로직 테스트 (3)

generated_images/                generateImage 결과물 저장 디렉토리 (런타임 생성)

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
ktor-client-cio   : 3.3.3            // generateImage 직접 HTTP 호출 (CIO 엔진)
ktor-client-content-negotiation : 3.3.3  // JSON 협상
ktor-serialization-kotlinx-json : 3.3.3  // Ktor JSON 직렬화
kotlinx-coroutines: 1.11.0           // 비동기 처리
kotlinx-serialization-json: 1.8.1   // JSON 직렬화
Kotlin            : 2.3.21
JVM               : 21

// 테스트
kotlin-test                    // 단위 테스트
kotlinx-coroutines-test: 1.11.0
```
