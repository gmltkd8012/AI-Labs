import org.example.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrchestratorFeedbackTest {

    @Test
    fun `review issues are formatted as feedback strings`() {
        val issues = listOf(
            ReviewIssue(
                severity = IssueSeverity.CRITICAL,
                location = "TestAgent.kt:15",
                description = "executor 리소스 누수",
                suggestion = "executor.use { } 블록 적용"
            ),
            ReviewIssue(
                severity = IssueSeverity.MAJOR,
                location = "TestAgent.kt:30",
                description = "에러 처리 누락",
                suggestion = "try-catch 추가"
            )
        )

        val feedback = issues
            .sortedBy { it.severity }
            .map { "[${it.severity}] ${it.location}: ${it.description} → ${it.suggestion}" }

        assertEquals(2, feedback.size)
        assertTrue(feedback[0].startsWith("[CRITICAL]"))
        assertTrue(feedback[1].startsWith("[MAJOR]"))
        assertTrue(feedback[0].contains("executor 리소스 누수"))
        assertTrue(feedback[0].contains("executor.use { } 블록 적용"))
    }

    @Test
    fun `CRITICAL issues have highest sort priority`() {
        val severities = listOf(IssueSeverity.MINOR, IssueSeverity.CRITICAL, IssueSeverity.MAJOR)
        val sorted = severities.sortedBy { it }
        assertEquals(IssueSeverity.CRITICAL, sorted[0])
    }

    @Test
    fun `approved review produces empty feedback`() {
        val review = ReviewResult(
            approved = true,
            score = 90,
            issues = listOf(
                ReviewIssue(IssueSeverity.MINOR, "file:1", "minor issue", "suggestion")
            ),
            suggestions = listOf("좋은 코드입니다")
        )

        val feedback = if (!review.approved) {
            review.issues
                .sortedByDescending { it.severity }
                .map { "[${it.severity}] ${it.location}: ${it.description} → ${it.suggestion}" }
        } else {
            emptyList()
        }

        assertTrue(feedback.isEmpty())
    }
}
