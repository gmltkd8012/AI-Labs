import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ucceditor.model.EditPlan
import org.ucceditor.model.EditStep
import org.ucceditor.tools.FFmpegTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditPlanTest {

    @Test
    fun `EditPlan 직렬화 라운드트립`() {
        val plan = EditPlan(
            source = "input.mp4",
            output = "out.mp4",
            pattern = "유튜브 숏폼 훅",
            steps = listOf(
                EditStep("trim", mapOf("start" to "0", "end" to "5"), "앞 5초로 훅 구성"),
                EditStep("overlayText", mapOf("text" to "이거 보세요"), "강조 자막")
            )
        )
        val json = Json.encodeToString(EditPlan.serializer(), plan)
        val restored = Json.decodeFromString(EditPlan.serializer(), json)
        assertEquals(plan, restored)
        assertEquals(2, restored.steps.size)
    }

    @Test
    fun `없는 파일은 probe에서 안내 메시지를 반환한다`() = runTest {
        val result = FFmpegTools().probe("does-not-exist.mp4")
        assertTrue(result.contains("찾을 수 없습니다"), "실제 결과: $result")
    }
}
