package org.ucceditor.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 동영상 기본 메타데이터. */
data class MediaInfo(val durationSec: Double, val width: Int, val height: Int) {
    val aspect: String get() = aspectOf(width, height)
}

/**
 * `ffprobe` 로 동영상 메타데이터를 구조화해 읽어옵니다.
 * 도구(@Tool)가 아니라 파이프라인 내부 분석용 헬퍼입니다.
 */
object MediaProbe {

    suspend fun probe(path: String): MediaInfo? = withContext(Dispatchers.IO) {
        val duration = exec(
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "csv=p=0", path
        )?.trim()?.toDoubleOrNull() ?: return@withContext null

        val wh = exec(
            "ffprobe", "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height",
            "-of", "csv=p=0:s=x", path
        )?.trim()?.split("x") ?: return@withContext null

        val w = wh.getOrNull(0)?.toIntOrNull() ?: 0
        val h = wh.getOrNull(1)?.toIntOrNull() ?: 0
        MediaInfo(duration, w, h)
    }

    private fun exec(vararg cmd: String): String? = try {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (p.waitFor() == 0) out else null
    } catch (e: Exception) {
        null
    }
}

/** 가로:세로 비율을 대표 화면비 문자열로 변환. */
fun aspectOf(w: Int, h: Int): String {
    if (w == 0 || h == 0) return "unknown"
    val ratio = w.toDouble() / h
    return when {
        ratio < 0.85 -> "9:16"
        ratio > 1.5  -> "16:9"
        else         -> "1:1"
    }
}
