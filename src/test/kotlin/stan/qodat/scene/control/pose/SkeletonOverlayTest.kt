package stan.qodat.scene.control.pose

import javafx.embed.swing.JFXPanel
import javafx.scene.transform.Rotate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkeletonOverlayTest {

    init {
        JFXPanel()
    }

    @Test
    fun alignedWithPositiveYNeedsNoRotation() {
        val rotate = SkeletonOverlay.rotationFromYAxis(0.0, 1.0, 0.0)
        assertEquals(0.0, rotate.angle, 1e-6)
    }

    @Test
    fun alignedWithNegativeYFlips180AroundX() {
        val rotate = SkeletonOverlay.rotationFromYAxis(0.0, -1.0, 0.0)
        assertEquals(180.0, rotate.angle, 1e-6)
        assertEquals(Rotate.X_AXIS, rotate.axis)
    }

    @Test
    fun alignedWithPositiveXRotatesAroundZ() {
        val rotate = SkeletonOverlay.rotationFromYAxis(1.0, 0.0, 0.0)
        assertEquals(90.0, rotate.angle, 1e-6)
        assertTrue(abs(rotate.axis.z) > 0.99)
    }
}
