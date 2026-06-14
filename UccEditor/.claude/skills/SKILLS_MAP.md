# Agent 스킬 매핑 (UccEditor)

각 작업/Agent가 참조해야 하는 스킬 파일 목록입니다.
UccEditor는 **동영상 편집 패턴 학습 + 실제 편집** Agent이므로, 공통 Kotlin/Koog 스킬에
도메인 스킬 `ffmpeg.md` 가 추가됩니다.

---

## editor-agent

편집 실행 Agent — 사용자 요청/패턴을 해석해 FFmpeg 도구로 실제 편집을 수행합니다.

| 스킬 | 이유 |
|-----|------|
| `koog.md` | AIAgent 생성, ToolSet/@Tool 정의, executor 패턴 |
| `kotlin.md` | Kotlin 문법, 데이터 모델(EditPlan) 직렬화 |
| `coroutines.md` | suspend 도구, 프로세스 실행을 Dispatchers.IO로 처리 |
| `ffmpeg.md` | **실제 편집 명령** — trim/concat/속도/자막/오디오, 패턴→명령 매핑 |

---

## pattern-agent (확장 예정)

패턴 학습 Agent — 예시 영상/편집 사례를 분석해 재사용 가능한 `EditPlan` 패턴을 추출합니다.

| 스킬 | 이유 |
|-----|------|
| `ffmpeg.md` | 편집 패턴이 어떤 FFmpeg 동작으로 구현되는지 파악 |
| `koog.md` | Structured Output으로 EditPlan 추출 |
| `llm-models.md` | 패턴 분석에 적합한 모델 선택 기준 |

---

## 공유 스킬 요약

```
koog.md         → editor-agent, pattern-agent
kotlin.md       → editor-agent
coroutines.md   → editor-agent
ffmpeg.md       → editor-agent, pattern-agent   (도메인 핵심)
llm-models.md   → pattern-agent
```
