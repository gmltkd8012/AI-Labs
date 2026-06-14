package org.ucceditor

import kotlinx.coroutines.runBlocking
import org.ucceditor.agents.EditorAgent

fun main() = runBlocking {
    if (System.getenv("GOOGLE_API_KEY").isNullOrBlank()) {
        println("GOOGLE_API_KEY 환경변수가 설정되지 않았습니다.")
        println("터미널에서 'export GOOGLE_API_KEY=your_key' 를 실행한 뒤 다시 시작하세요.")
        return@runBlocking
    }

    val agent = EditorAgent()

    println("UccEditor — 동영상 편집 AI 에이전트 (종료: 'exit')")
    println("편집할 영상 경로와 원하는 편집을 말씀해 주세요.")
    println("━".repeat(48))

    try {
        while (true) {
            print("\n사용자: ")
            val input = readlnOrNull()?.trim() ?: break

            if (input.lowercase() == "exit") {
                println("종료합니다.")
                break
            }
            if (input.isEmpty()) continue

            val response = agent.chat(input)
            println("UccEditor: $response")
        }
    } finally {
        agent.close()
    }
}
