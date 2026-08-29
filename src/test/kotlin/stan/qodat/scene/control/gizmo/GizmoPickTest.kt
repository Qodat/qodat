package stan.qodat.scene.control.gizmo

import kotlin.math.abs
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GizmoPickTest {

    @Test
    fun centerClickSitsOnTheCameraZAxis() {
        val point = GizmoPick.projectionPlaneLocal(50.0, 50.0, 100.0, 100.0, 90.0, true)
        assertEquals(0.0, point.x, 1e-6)
        assertEquals(0.0, point.y, 1e-6)
        assertEquals(50.0, point.z, 1e-6)
    }

    @Test
    fun rightOfCenterIncreasesLocalX() {
        val point = GizmoPick.projectionPlaneLocal(100.0, 50.0, 100.0, 100.0, 90.0, true)
        assertTrue(point.x > 0.0)
        assertEquals(0.0, point.y, 1e-6)
    }

    @Test
    fun verticalFovDistanceMatchesHalfHeightOverTan() {
        val fov = 30.0
        val expected = 50.0 / tan(Math.toRadians(fov) / 2.0)
        val point = GizmoPick.projectionPlaneLocal(50.0, 50.0, 100.0, 100.0, fov, true)
        assertTrue(abs(point.z - expected) < 1e-6)
    }
}
