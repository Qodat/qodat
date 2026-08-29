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
    fun pointToSegment2DIsZeroOnTheSegment() {
        assertEquals(0.0, GizmoMath.pointToSegment2D(5.0, 0.0, 0.0, 0.0, 10.0, 0.0), 1e-6)
        assertEquals(3.0, GizmoMath.pointToSegment2D(5.0, 3.0, 0.0, 0.0, 10.0, 0.0), 1e-6)
    }

    @Test
    fun screenAxisDeltaIsOneWhenMouseTravelsTheProjectedAxis() {
        val delta = GizmoMath.screenAxisDelta(0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 10.0, 0.0)
        assertEquals(1.0, delta, 1e-6)
    }

    @Test
    fun foreshortenedScreenAxisDeltaExplodes() {
        val delta = GizmoMath.screenAxisDelta(0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.5, 0.0)
        assertTrue(delta > 10.0, "delta=$delta")
    }

    @Test
    fun segmentParamIsHalfAtTheMidpoint() {
        assertEquals(0.5, GizmoMath.segmentParam2D(5.0, 0.0, 0.0, 0.0, 10.0, 0.0), 1e-6)
        assertTrue(GizmoMath.segmentParam2D(1.0, 0.0, 0.0, 0.0, 10.0, 0.0) < 0.2)
    }

    @Test
    fun clampStepMatchesTheProvenRotateCap() {
        assertEquals(3f, GizmoMath.clampStep(40f, ROTATION_SPEED.toFloat()))
        assertEquals(-3f, GizmoMath.clampStep(-40f, ROTATION_SPEED.toFloat()))
        assertEquals(1.5f, GizmoMath.clampStep(1.5f, ROTATION_SPEED.toFloat()))
    }

    @Test
    fun worldTranslateDeltaIsOneToOneOnTheAxis() {
        val previous = Vector3f(0f, 0f, 0f)
        val current = Vector3f(8f, 3f, -1f)
        assertEquals(8f, GizmoMath.translateDelta(previous, current, Vector3f(1f, 0f, 0f)))
    }

    @Test
    fun screenRotateDeltaIsNinetyClockwise() {
        val degrees = GizmoMath.screenRotateDeltaDegrees(10.0, 0.0, 0.0, 10.0, 0.0, 0.0)
        assertEquals(90.0, degrees, 1e-6)
    }

    @Test
    fun raySegmentDistanceIsZeroOnTheSegment() {
        val ray = GizmoMath.Ray(Vector3f(5f, 10f, 0f), Vector3f(0f, -1f, 0f))
        val dist = GizmoMath.raySegmentDistance(ray, Vector3f(0f, 0f, 0f), Vector3f(10f, 0f, 0f))
        assertTrue(dist < 1e-4f, "dist=$dist")
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
