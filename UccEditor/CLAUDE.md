# UccEditor — 동영상 편집 AI Agent

JetBrains [Koog](https://github.com/JetBrains/koog) 기반 Kotlin AI Agent.
**동영상 편집 패턴을 학습하고, 실제 편집을 수행**하는 것이 목표입니다.

## 스택
- **언어**: Kotlin 2.3.21 (JVM 21)
- **프레임워크**: JetBrains Koog (`koog-agents` 1.0.0-preview7)
- **LLM**: Google Gemini **고정** (`gemini-2.0-flash`, `GOOGLE_API_KEY` 필요)
- **편집 엔진**: FFmpeg (시스템 설치 필요 — `brew install ffmpeg`)
- **빌드**: Gradle (Kotlin DSL)

## 구조
```
src/main/kotlin/org/ucceditor/
  Main.kt                  # REPL 진입점
  llm/LLMConfig.kt         # LLM Provider 설정 (기본 Ollama, 유료 모델 옵션)
  model/EditPlan.kt        # 편집 계획/패턴 직렬화 모델
  tools/FFmpegTools.kt     # 실제 편집 도구 (ToolSet, @Tool)
  agents/EditorAgent.kt    # 편집 실행 Agent
```

## 실행
```bash
export GOOGLE_API_KEY=your_key   # Gemini 사용에 필수

./gradlew run         # REPL 시작
./gradlew test        # 테스트
```
편집 결과물은 `workspace/` 하위에 저장됩니다.

## Harness (`.claude/skills/`)
작업 시 참조하는 레퍼런스. 매핑은 `SKILLS_MAP.md` 참고.
- `koog.md` · `kotlin.md` · `coroutines.md` — 공통
- `ffmpeg.md` — **도메인 핵심** (편집 명령 / 패턴→명령 매핑)
- `llm-models.md` — 모델 선택 기준

## 컨벤션
- LLM은 **Google Gemini로 고정** (Heevis의 Provider 스위칭 구조와 다름). 모델 변형만 `EditorAgent(geminiModelId=...)` 로 선택.
- 새 편집 동작: `ffmpeg.md` 패턴 표에 먼저 추가 → `FFmpegTools`에 `@Tool` 구현.
- 응답은 한국어.
