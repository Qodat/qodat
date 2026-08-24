package stan.qodat.util

import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorsTest {

    @Test
    fun rgbUsesHalfOpacity() {
        val color = rgb(10, 20, 30)
        val expected = Color.rgb(10, 20, 30, 0.5)
        assertEquals(expected, color)
        assertEquals(0.5, color.opacity)
    }

    @Test
    fun distinctColorsAreBuiltWithTheSameHelper() {
        assertTrue(DISTINCT_COLORS.isNotEmpty())
        assertEquals(rgb(51, 0, 7), DISTINCT_COLORS.first())
        assertEquals(0.5, DISTINCT_COLORS.first().opacity)
    }
}
