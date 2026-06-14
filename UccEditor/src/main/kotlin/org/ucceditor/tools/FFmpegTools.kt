package org.ucceditor.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 실제 동영상 편집을 수행하는 도구 모음.
 *
 * 편집 엔진으로 시스템에 설치된 **FFmpeg**(`ffmpeg`, `ffprobe`)를 프로세스로 호출합니다.
 * Agent는 이 도구들을 조합해 사용자의 편집 요청 / 학습된 편집 패턴을 실제 파일로 구현합니다.
 *
 * 설치 확인: `ffmpeg -version`  (macOS: `brew install ffmpeg`)
 */
class FFmpegTools(
    private val workDir: File = File(System.getProperty("user.dir"), "workspace"),
) : ToolSet {

    init {
        if (!workDir.exists()) workDir.mkdirs()
    }

    @Tool
    @LLMDescription("동영상 메타데이터(길이, 해상도, 코덱 등)를 조회합니다. 편집 전 소스 분석에 사용하세요.")
    suspend fun probe(
        @LLMDescription("분석할 동영상 파일 경로") input: String
    ): String {
        if (!File(input).exists()) return "파일을 찾을 수 없습니다: $input"
        return run(
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration,size:stream=codec_name,width,height,r_frame_rate",
            "-of", "default=noprint_wrappers=1",
            input
        )
    }

    @Tool
    @LLMDescription("동영상을 지정 구간으로 자릅니다(trim). start~end 구간만 남깁니다.")
    suspend fun trim(
        @LLMDescription("입력 파일 경로") input: String,
        @LLMDescription("시작 시각 (예: 00:00:05 또는 5)") start: String,
        @LLMDescription("종료 시각 (예: 00:00:12 또는 12)") end: String,
        @LLMDescription("출력 파일 이름 (workspace 기준, 예: clip.mp4)") output: String
    ): String {
        if (!File(input).exists()) return "파일을 찾을 수 없습니다: $input"
        val out = resolveOutput(output)
        return run(
            "ffmpeg", "-y", "-i", input,
            "-ss", start, "-to", end,
            "-c", "copy", out
        ).ifBlank { "trim 완료 → $out" }
    }

    @Tool
    @LLMDescription("재생 속도를 변경합니다. factor>1 이면 빠르게, <1 이면 느리게 (예: 2.0 = 2배속).")
    suspend fun changeSpeed(
        @LLMDescription("입력 파일 경로") input: String,
        @LLMDescription("속도 배율 (0.5 ~ 4.0 권장)") factor: Double,
        @LLMDescription("출력 파일 이름 (workspace 기준)") output: String
    ): String {
        if (!File(input).exists()) return "파일을 찾을 수 없습니다: $input"
        if (factor <= 0) return "factor는 0보다 커야 합니다."
        val out = resolveOutput(output)
        val pts = 1.0 / factor
        return run(
            "ffmpeg", "-y", "-i", input,
            "-filter_complex", "[0:v]setpts=$pts*PTS[v];[0:a]atempo=$factor[a]",
            "-map", "[v]", "-map", "[a]", out
        ).ifBlank { "속도 ${factor}x 적용 완료 → $out" }
    }

    @Tool
    @LLMDescription("동영상 위에 자막/텍스트를 오버레이합니다. 숏폼 훅 텍스트 등에 사용.")
    suspend fun overlayText(
        @LLMDescription("입력 파일 경로") input: String,
        @LLMDescription("표시할 텍스트") text: String,
        @LLMDescription("출력 파일 이름 (workspace 기준)") output: String
    ): String {
        if (!File(input).exists()) return "파일을 찾을 수 없습니다: $input"
        val out = resolveOutput(output)
        val safe = text.replace(":", "\\:").replace("'", "")
        return run(
            "ffmpeg", "-y", "-i", input,
            "-vf", "drawtext=text='$safe':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=h-120:box=1:boxcolor=black@0.5:boxborderw=10",
            "-c:a", "copy", out
        ).ifBlank { "텍스트 오버레이 완료 → $out" }
    }

    @Tool
    @LLMDescription("여러 동영상을 순서대로 이어붙입니다(concat). 같은 코덱/해상도일 때 가장 안정적입니다.")
    suspend fun concat(
        @LLMDescription("이어붙일 파일 경로들 (쉼표로 구분, 순서대로)") inputsCsv: String,
        @LLMDescription("출력 파일 이름 (workspace 기준)") output: String
    ): String {
        val inputs = inputsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (inputs.size < 2) return "concat 하려면 최소 2개 파일이 필요합니다."
        val missing = inputs.filterNot { File(it).exists() }
        if (missing.isNotEmpty()) return "파일을 찾을 수 없습니다: ${missing.joinToString()}"

        val listFile = File(workDir, "concat_list.txt").apply {
            writeText(inputs.joinToString("\n") { "file '${File(it).absolutePath}'" })
        }
        val out = resolveOutput(output)
        return run(
            "ffmpeg", "-y", "-f", "concat", "-safe", "0",
            "-i", listFile.absolutePath, "-c", "copy", out
        ).ifBlank { "${inputs.size}개 클립 결합 완료 → $out" }
    }

    @Tool
    @LLMDescription("동영상에서 오디오 트랙만 추출합니다(mp3).")
    suspend fun extractAudio(
        @LLMDescription("입력 파일 경로") input: String,
        @LLMDescription("출력 파일 이름 (workspace 기준, 예: audio.mp3)") output: String
    ): String {
        if (!File(input).exists()) return "파일을 찾을 수 없습니다: $input"
        val out = resolveOutput(output)
        return run("ffmpeg", "-y", "-i", input, "-vn", "-q:a", "2", out)
            .ifBlank { "오디오 추출 완료 → $out" }
    }

    private fun resolveOutput(name: String): String {
        val f = File(name)
        return if (f.isAbsolute) name else File(workDir, name).absolutePath
    }

    /**
     * 외부 프로세스를 실행하고 결과를 반환합니다.
     * 성공 시 stdout(또는 빈 문자열), 실패 시 "[ERROR] ..." 형태의 메시지를 돌려줍니다.
     */
    private suspend fun run(vararg command: String): String = withContext(Dispatchers.IO) {
        try {
            val proc = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            if (code == 0) {
                output.trim()
            } else {
                "[ERROR] 명령 실패 (exit=$code): ${output.takeLast(800)}"
            }
        } catch (e: Exception) {
            "[ERROR] FFmpeg 실행 실패: ${e.message}. ffmpeg가 설치되어 있는지 확인하세요 (brew install ffmpeg)."
        }
    }
}
