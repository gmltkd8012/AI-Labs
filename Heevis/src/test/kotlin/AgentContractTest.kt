import kotlinx.serialization.json.Json
import org.example.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentContractTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `AgentSpec serializes and deserializes correctly`() {
        val spec = AgentSpec(
            agentName = "WeatherAgent",
            role = "날씨 정보 제공",
            systemPrompt = "You are a weather assistant.",
            requiredTools = listOf("getWeather", "getForecast"),
            recommendedModel = "qwen2.5-coder:7b",
            architectureNotes = "Koog ToolSet 패턴 사용"
        )
        val encoded = json.encodeToString(AgentSpec.serializer(), spec)
        val decoded = json.decodeFromString<AgentSpec>(encoded)
        assertEquals(spec, decoded)
    }

    @Test
    fun `ReviewResult approved when no CRITICAL issues`() {
        val result = ReviewResult(
            approved = true,
            score = 85,
            issues = listOf(
                ReviewIssue(
                    severity = IssueSeverity.MINOR,
                    location = "WeatherAgent.kt:10",
                    description = "네이밍 개선 가능",
                    suggestion = "camelCase 사용 권장"
                )
            ),
            suggestions = listOf("전반적으로 양호")
        )
        assertTrue(result.approved)
        assertEquals(85, result.score)
        assertFalse(result.issues.any { it.severity == IssueSeverity.CRITICAL })
    }

    @Test
    fun `ReviewResult not approved when CRITICAL issue exists`() {
        val result = ReviewResult(
            approved = false,
            score = 40,
            issues = listOf(
                ReviewIssue(
                    severity = IssueSeverity.CRITICAL,
                    location = "WeatherAgent.kt:25",
                    description = "executor가 .use{} 없이 사용됨 (리소스 누수)",
                    suggestion = "executor.use { } 블록으로 감싸세요"
                )
            ),
            suggestions = emptyList()
        )
        assertFalse(result.approved)
        assertTrue(result.issues.any { it.severity == IssueSeverity.CRITICAL })
    }

    @Test
    fun `GeneratedCode contains spec and files`() {
        val spec = AgentSpec(
            agentName = "TestAgent",
            role = "테스트",
            systemPrompt = "test",
            requiredTools = emptyList(),
            recommendedModel = "llama3.1:8b",
            architectureNotes = ""
        )
        val code = GeneratedCode(
            spec = spec,
            files = listOf(
                CodeFile("src/main/kotlin/agents/TestAgent.kt", "package org.example"),
                CodeFile("src/main/kotlin/agents/TestAgentTools.kt", "package org.example")
            )
        )
        assertEquals("TestAgent", code.spec.agentName)
        assertEquals(2, code.files.size)
    }

    @Test
    fun `IssueSeverity ordering CRITICAL before MAJOR before MINOR`() {
        val issues = listOf(
            ReviewIssue(IssueSeverity.MINOR, "file:1", "minor", "fix"),
            ReviewIssue(IssueSeverity.CRITICAL, "file:2", "critical", "fix"),
            ReviewIssue(IssueSeverity.MAJOR, "file:3", "major", "fix"),
        )
        val sorted = issues.sortedBy { it.severity }
        assertEquals(IssueSeverity.CRITICAL, sorted[0].severity)
        assertEquals(IssueSeverity.MAJOR, sorted[1].severity)
        assertEquals(IssueSeverity.MINOR, sorted[2].severity)
    }

    @Test
    fun `UserRequirement with features serializes correctly`() {
        val req = UserRequirement(
            description = "날씨 Agent 만들어줘",
            features = listOf("실시간 날씨", "주간 예보", "알림 기능")
        )
        val encoded = json.encodeToString(UserRequirement.serializer(), req)
        val decoded = json.decodeFromString<UserRequirement>(encoded)
        assertEquals(req, decoded)
        assertEquals(3, decoded.features.size)
    }
}
