package stan.qodat.scene.control.gizmo

import org.joml.Vector3f
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GizmoMathTest {

    @Test
    fun rayHitsXYPlaneAtExpectedPoint() {
        val ray = GizmoMath.Ray(Vector3f(0f, 0f, -10f), Vector3f(0f, 0f, 1f))
        val hit = GizmoMath.intersectRayPlane(ray, Vector3f(0f, 0f, 0f), Vector3f(0f, 0f, 1f))
        assertNotNull(hit)
        assertVec(Vector3f(0f, 0f, 0f), hit)
    }

    @Test
    fun translateKeepsOnlyTheDraggedAxis() {
        val previous = Vector3f(0f, 0f, 0f)
        val current = Vector3f(10f, 4f, -3f)
        assertEquals(10f, GizmoMath.translateDelta(previous, current, Vector3f(1f, 0f, 0f)))
        assertEquals(4f, GizmoMath.translateDelta(previous, current, Vector3f(0f, 1f, 0f)))
        assertEquals(-3f, GizmoMath.translateDelta(previous, current, Vector3f(0f, 0f, 1f)))
    }

    @Test
    fun translatePlaneNormalIsPerpendicularToTheAxis() {
        val axis = Vector3f(1f, 0f, 0f)
        val normal = GizmoMath.translatePlaneNormal(axis, Vector3f(0f, 0f, 1f))
        assertTrue(abs(normal.dot(axis)) < 1e-5f)
        assertTrue(normal.length() > 0.99f)
    }

    @Test
    fun rotateNinetyDegreesAroundY() {
        val origin = Vector3f(0f, 0f, 0f)
        val previous = Vector3f(10f, 0f, 0f)
        val current = Vector3f(0f, 0f, -10f)
        val degrees = GizmoMath.rotateDeltaDegrees(previous, current, origin, Vector3f(0f, 1f, 0f))
        assertTrue(abs(degrees - 90f) < 0.01f, "degrees=$degrees")
    }

    @Test
    fun rsUnitsMapToFullTurns() {
        assertEquals(64, GizmoMath.degreesToRs(90f))
        assertEquals(256, GizmoMath.degreesToRs(360f))
        assertEquals(90f, GizmoMath.rsToDegrees(64), 0.01f)
    }

    private fun assertVec(expected: Vector3f, actual: Vector3f) {
        assertEquals(expected.x, actual.x, 1e-4f)
        assertEquals(expected.y, actual.y, 1e-4f)
        assertEquals(expected.z, actual.z, 1e-4f)
    }
}
