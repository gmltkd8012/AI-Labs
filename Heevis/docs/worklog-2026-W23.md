# 주간 작업 로그 — 2026-W23 (6/1 ~ 6/7)

## 개요

ARCHITECTURE.md 설계와 실제 구현 사이의 간극을 좁히는 작업. RouterAgent → SpecializedTools → Orchestrator 파이프라인의 골격은 지난주(~5/31)에 완성되었고, 이번 주는 **"설계상 존재하나 실제로는 동작하지 않던 기능"을 실동작 수준으로 끌어올리고 품질 안전망(테스트)을 추가**했다.

우선순위 순으로 6개 항목을 진행했으며, 전 항목 빌드·테스트 통과로 마무리했다.

---

## 작업 항목

### 1. Orchestrator 재시도 피드백 전달 🔴
**문제**: ReviewAgent가 CRITICAL 이슈를 발견해도 CodeAgent가 동일한 `spec`으로 재실행되어 같은 오류를 반복할 가능성.

**조치**:
- `CodeAgent.generate(spec, reviewFeedback: List<String> = emptyList())`로 시그니처 확장.
- Orchestrator가 리뷰 이슈를 `[심각도] 위치: 설명 → 제안` 형식 문자열로 변환해 다음 시도의 프롬프트에 주입.

**파일**: `agents/CodeAgent.kt`, `orchestrator/Orchestrator.kt`

---

### 2. generateImage 실제 이미지 생성 🔴
**문제**: 기존 구현은 Google LLM으로 텍스트 응답만 받고 실제 이미지 파일을 만들지 않음.

**조치**:
- Ktor `HttpClient(CIO)`로 `gemini-2.0-flash-preview-image-generation:generateContent` REST API 직접 호출.
- `responseModalities: [IMAGE, TEXT]` 요청 → base64 `inlineData`를 디코딩해 `generated_images/image_<timestamp>.<ext>`로 저장.
- 응답에 이미지가 없으면 텍스트 파트를 모아 사유를 반환, `finally`에서 client 해제.

**파일**: `tools/SpecializedTools.kt`, `build.gradle.kts`(Ktor client 3종 추가)

---

### 3. buildAgent 생성 코드 디스크 저장 🟡
**문제**: 생성된 코드의 경로/요약만 출력하고 실제 `.kt` 파일을 쓰지 않음.

**조치**:
- `GeneratedCode.files`를 `File.writeText()`로 실제 저장(부모 디렉토리 자동 생성).
- 저장에 성공한 경로만 집계해 결과 메시지에 표시.

**파일**: `tools/SpecializedTools.kt`

---

### 4. RouterAgent 대화 히스토리 🟡
**문제**: `chat()` 호출마다 새 AIAgent를 만들어 이전 대화를 기억하지 못함.

**조치**:
- `executor`를 인스턴스 필드로 승격(매 호출 재생성 제거).
- `prompt { }` DSL에 `system → (user/assistant)* → 신규 user` 순으로 히스토리 주입.
- `close()` 추가, `Main.kt`는 `try/finally`로 종료 시 executor 해제.

**파일**: `agents/RouterAgent.kt`, `Main.kt`

---

### 5. 테스트 코드 작성 🟢
**문제**: test 의존성만 있고 테스트 전무.

**조치**: 총 9개 단위 테스트 추가, 전체 통과.
- `AgentContractTest` (6): 데이터 계약 직렬화/역직렬화, ReviewResult 승인 규칙, 심각도 정렬, GeneratedCode 구조, UserRequirement features.
- `OrchestratorFeedbackTest` (3): 리뷰 이슈 → 피드백 문자열 포맷, 심각도 정렬 우선순위, 승인 시 빈 피드백.

**파일**: `src/test/kotlin/AgentContractTest.kt`, `src/test/kotlin/OrchestratorFeedbackTest.kt`

---

### 6. Ollama 미실행 에러 처리 🟢
**문제**: Ollama 서버 미가동 시 연결 오류 스택트레이스가 그대로 노출.

**조치**: `RouterAgent.isOllamaConnectionError()`로 `connection refused`/`connect timed out`/`localhost:11434` 등을 감지 → `'ollama serve' 먼저 실행` 안내 메시지로 대체.

**파일**: `agents/RouterAgent.kt`

---

## 부가 발견 및 수정

- **`IssueSeverity` 정렬 버그**: enum ordinal이 `CRITICAL=0, MAJOR=1, MINOR=2`라 `sortedByDescending`이 오히려 MINOR를 앞에 놓았다. 테스트 작성 중 적발, `sortedBy`로 통일 (코드/테스트 모두 수정).

---

## 문서 동기화 (ARCHITECTURE.md)

코드 발전을 따라가지 못한 ARCHITECTURE.md를 갱신:
1. SpecializedTools 표에 "동작" 열 추가 — 이미지 모델명(`gemini-2.0-flash-preview-image-generation`)·파일 저장 명시.
2. RouterAgent 대화 히스토리/연결 에러 처리 섹션 신설.
3. 데이터 흐름에 리뷰 피드백 루프·파일 저장 단계 반영.
4. 의존성 섹션에 Ktor client 3종 + 테스트 의존성 추가.
5. 파일 구조에 `src/test/`, `generated_images/` 반영.

---

## 변경 규모

```
build.gradle.kts             |  5 +
Main.kt                      | 24 +-
agents/CodeAgent.kt          |  7 +-
agents/RouterAgent.kt        | 52 +++-
orchestrator/Orchestrator.kt | 18 +-
tools/SpecializedTools.kt    | 85 +++++-
src/test/kotlin/*            | 신규 2파일 (9 테스트)
```

검증: `./gradlew test` → **BUILD SUCCESSFUL (9 tests passed)**

---

## 남은 과제 / 다음 주 후보

- [ ] generateImage·buildAgent의 **실제 API 통합 테스트**(현재 단위 테스트는 순수 로직 한정, 외부 호출 미포함).
- [ ] 대화 히스토리 **토큰 누적 대비** — Koog `HistoryCompression` 적용 검토.
- [ ] 이번 주 변경분 **커밋 분리**(현재 working tree 미커밋 상태).
- [ ] buildAgent 저장 경로의 **상대경로 기준점**(현재 CWD 기준) 명확화 — 덮어쓰기 방지 가드 검토.
