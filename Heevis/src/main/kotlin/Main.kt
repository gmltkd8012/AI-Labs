package org.example

import kotlinx.coroutines.runBlocking
import org.example.agents.RouterAgent

fun main() = runBlocking {
    val router = RouterAgent()

    println("Heevis AI 어시스턴트 (종료: 'exit')")
    println("━".repeat(40))

    try {
        while (true) {
            print("\n사용자: ")
            val input = readln().trim()

            if (input.lowercase() == "exit") {
                println("종료합니다.")
                break
            }
            if (input.isEmpty()) continue

            val response = router.chat(input)
            println("Heevis: $response")
        }
    } finally {
        router.close()
    }
}
