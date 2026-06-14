package org.ucceditor

import kotlinx.coroutines.runBlocking
import org.ucceditor.pipeline.EditorPipeline

fun main() = runBlocking {
    if (System.getenv("GOOGLE_API_KEY").isNullOrBlank()) {
        println("GOOGLE_API_KEY 환경변수가 설정되지 않았습니다.")
        println("터미널에서 'export GOOGLE_API_KEY=your_key' 를 실행한 뒤 다시 시작하세요.")
        return@runBlocking
    }

    EditorPipeline().run()
}
