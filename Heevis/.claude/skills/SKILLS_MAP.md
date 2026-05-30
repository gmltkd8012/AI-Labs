# Agent 스킬 매핑

각 Sub-Agent가 참조해야 하는 스킬 파일 목록입니다.

---

## code-agent

코드 작성 Agent — Kotlin Koog 기반 Agent 코드를 생성합니다.

| 스킬 | 이유 |
|-----|------|
| `kotlin.md` | Kotlin 언어 문법, 타입 시스템, 표준 라이브러리 |
| `coroutines.md` | suspend 함수, Flow, CoroutineScope — Agent 비동기 처리 필수 |
| `koog.md` | AIAgent 생성, Tool 정의, executor 패턴, 전체 Koog API |

---

## review-agent

코드 리뷰 Agent — code-agent가 작성한 코드를 검토합니다.

| 스킬 | 이유 |
|-----|------|
| `kotlin.md` | Kotlin 관용 표현, 안티패턴 식별 |
| `coroutines.md` | 코루틴 취소 처리, structured concurrency 준수 여부 확인 |
| `koog.md` | Koog API 올바른 사용 여부, executor 리소스 관리 확인 |

---

## spec-agent

설계 Agent — 사용자 요구사항을 분석해 Agent 명세를 작성합니다.

| 스킬 | 이유 |
|-----|------|
| `koog.md` | 설계 가능한 Agent 유형, Tool, 전략 파악 |
| `llm-models.md` | 적절한 LLM 모델 선택 기준 (Ollama 기본 + 유료 모델 옵션) |

> spec-agent는 Kotlin 문법 세부 사항보다 **무엇을 만들 수 있는지** 알면 충분합니다.

---

## 공유 스킬 요약

```
kotlin.md       → code-agent, review-agent
coroutines.md   → code-agent, review-agent
koog.md         → code-agent, review-agent, spec-agent
llm-models.md   → spec-agent
```
