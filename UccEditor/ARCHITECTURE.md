# UccEditor 아키텍처 (초안)

> 초기 골격 단계. 1차 목표는 "요청 → 편집 실행" 루프를 동작시키는 것이고,
> 2차로 "예시 분석 → 패턴 학습 → 패턴 적용"을 얹는다.

## 큰 그림

```
사용자 입력
   │
   ▼
EditorAgent (Koog AIAgent)
   │  system prompt + 대화 히스토리
   │  편집 패턴 지식 (숏폼 훅 / 점프컷 / 하이라이트 …)
   ▼
ToolRegistry → FFmpegTools (@Tool)
   probe · trim · concat · changeSpeed · overlayText · extractAudio
   │  ProcessBuilder("ffmpeg"/"ffprobe")
   ▼
workspace/ 결과 파일
```

## 핵심 컴포넌트

| 컴포넌트 | 책임 |
|---------|------|
| `EditorAgent` | 의도 해석 → 도구 호출 오케스트레이션 → 결과 보고 |
| `FFmpegTools` | LLM에 노출되는 실제 편집 동작. 입력 검증 + 프로세스 실행 + 에러 전달 |
| `EditPlan` / `EditStep` | 편집 계획·패턴의 직렬화 표현 (학습된 패턴 저장/재사용) |
| `LLMConfig` | Provider 추상화 (Ollama 기본, Anthropic/OpenAI/Google 옵션) |

## 설계 원칙
- **편집 엔진은 검증된 CLI(FFmpeg)를 그대로 호출** — JNI/네이티브 바인딩보다 안정적.
- **도구는 작고 합성 가능하게** — Agent가 조합해 복잡한 편집을 구성.
- **실패를 삼키지 않는다** — exit code/stderr를 LLM에 그대로 전달해 재시도 유도.
- **출력 격리** — 모든 산출물은 `workspace/` 하위.

## 로드맵
1. **(현재) 편집 실행 루프** — REPL + FFmpegTools 기본 동작.
2. **패턴 학습(pattern-agent)** — 예시 영상/편집 사례 → `EditPlan` 추출(Structured Output).
3. **패턴 라이브러리** — 학습한 `EditPlan`을 JSON으로 저장하고 이름으로 재적용.
4. **다단계 전략** — probe→계획→실행→검증(re-probe) 그래프 기반 워크플로우.
