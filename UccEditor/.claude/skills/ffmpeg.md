# FFmpeg 동영상 편집 레퍼런스 스킬

> UccEditor의 **편집 엔진** — 실제 영상 편집은 FFmpeg 프로세스 호출로 수행합니다.
> 도구 구현: `src/main/kotlin/org/ucceditor/tools/FFmpegTools.kt`
> 공식 문서: https://ffmpeg.org/ffmpeg.html

---

## 설치 / 확인

```bash
brew install ffmpeg      # macOS
ffmpeg -version          # 설치 확인
ffprobe -version
```

UccEditor는 시스템 PATH의 `ffmpeg` / `ffprobe` 를 `ProcessBuilder`로 호출합니다.
별도 JNI 바인딩 없이, 검증된 CLI를 그대로 사용하는 것이 가장 안정적입니다.

---

## 소스 분석 (probe)

편집 전 항상 길이/해상도/코덱을 확인합니다.

```bash
ffprobe -v error \
  -show_entries format=duration,size:stream=codec_name,width,height,r_frame_rate \
  -of default=noprint_wrappers=1 input.mp4
```

---

## 핵심 편집 동작

| 동작 | 핵심 명령 | 비고 |
|------|----------|------|
| 자르기(trim) | `-ss <start> -to <end> -c copy` | `-c copy`는 재인코딩 없이 빠름(키프레임 단위) |
| 정밀 자르기 | `-ss <start> -to <end>` (copy 생략) | 프레임 정확하지만 재인코딩 |
| 결합(concat) | `-f concat -safe 0 -i list.txt -c copy` | 같은 코덱/해상도일 때만 안정적 |
| 속도 변경 | `-filter_complex "[0:v]setpts=N*PTS[v];[0:a]atempo=F[a]"` | `setpts=1/F`, `atempo`는 0.5~2.0 범위 |
| 자막/텍스트 | `-vf "drawtext=text='...':x=...:y=..."` | 폰트/박스 옵션으로 가독성 확보 |
| 오디오 추출 | `-vn -q:a 2 out.mp3` | `-vn` = 비디오 제거 |
| 해상도 변경 | `-vf scale=1080:1920` | 숏폼 9:16 = `1080:1920` |
| GIF 변환 | `-vf "fps=12,scale=480:-1" out.gif` | 미리보기용 |

### concat 리스트 파일 형식
```
file '/abs/path/clip1.mp4'
file '/abs/path/clip2.mp4'
```

### 속도 변경 주의
- `atempo` 필터는 0.5~2.0만 지원. 그 밖의 배율은 `atempo`를 체이닝: 4배속 = `atempo=2.0,atempo=2.0`.
- 영상(`setpts`)과 오디오(`atempo`)를 항상 같이 조정해 싱크를 맞춥니다.

---

## 편집 패턴 → 명령 매핑

이 프로젝트의 목표는 **편집 패턴 학습 후 실제 편집**입니다. 대표 패턴:

| 패턴 | 구성 | FFmpegTools 조합 |
|------|------|-----------------|
| 유튜브 숏폼 훅 | 앞 3~5초 강조 + 9:16 + 훅 자막 | `trim` → `scale` → `overlayText` |
| 브이로그 점프컷 | 정적 구간 제거 후 빠른 전환 | 다회 `trim` → `concat` |
| 하이라이트 릴 | 핵심 장면만 모아 BGM | 다회 `trim` → `concat` → 오디오 믹스 |
| 속도 강약 | 지루한 구간 빠르게 | `changeSpeed(factor>1)` |

> 패턴은 `EditPlan`(`model/EditPlan.kt`)으로 직렬화해 저장/재사용할 수 있습니다.
> 새 편집 동작이 필요하면 먼저 여기 표에 추가하고 `FFmpegTools`에 `@Tool`로 구현하세요.

---

## 안정성 체크리스트 (도구 구현 시)

- [ ] 입력 파일 존재 여부를 먼저 검사한다.
- [ ] 출력은 `workspace/` 하위로 격리하고 절대경로로 반환한다.
- [ ] `redirectErrorStream(true)`로 stderr까지 수집해 실패 원인을 LLM에 전달한다.
- [ ] exit code != 0 이면 마지막 로그를 잘라 `[ERROR]`로 반환한다(예외 삼키지 않기).
- [ ] 장시간 작업은 `Dispatchers.IO`에서 실행한다.

---

## 참고 링크
- FFmpeg 필터 문서: https://ffmpeg.org/ffmpeg-filters.html
- drawtext: https://ffmpeg.org/ffmpeg-filters.html#drawtext
- concat demuxer: https://trac.ffmpeg.org/wiki/Concatenate
