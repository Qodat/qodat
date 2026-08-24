package stan.qodat.scene.control.export.blender

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GltfRigidTest {

    @Test
    fun identityCovarianceIsUnitQuaternion() {
        val q = GltfRigid.quaternionFromCovariance(
            2f, 0f, 0f,
            0f, 2f, 0f,
            0f, 0f, 2f,
        )
        assertQuaternion(floatArrayOf(0f, 0f, 0f, 1f), q)
    }

    @Test
    fun ninetyDegreesAroundY() {
        // +90° about Y: (x, y, z) → (z, y, -x). Three non-collinear points.
        val bind = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val posed = floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f, 1f, 0f, 0f)
        val skins = intArrayOf(7, 7, 7)
        val pose = GltfRigid.pose(bind, posed, skins, 7, 3)
        assertEquals(1f / 3f, pose.translation[0], 1e-5f)
        assertEquals(1f / 3f, pose.translation[1], 1e-5f)
        assertEquals(-1f / 3f, pose.translation[2], 1e-5f)
        val h = 0.70710678f
        assertQuaternion(floatArrayOf(0f, h, 0f, h), pose.rotation)
    }

    @Test
    fun translationOnlyKeepsIdentityRotation() {
        val bind = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f)
        val posed = floatArrayOf(2f, 0f, 0f, 3f, 0f, 0f)
        val pose = GltfRigid.pose(bind, posed, intArrayOf(0, 0), 0, 2)
        assertEquals(2.5f, pose.translation[0], 1e-5f)
        assertQuaternion(floatArrayOf(0f, 0f, 0f, 1f), pose.rotation)
    }

    private fun assertQuaternion(expected: FloatArray, actual: FloatArray) {
        var sign = 1f
        if (expected[0] * actual[0] + expected[1] * actual[1] + expected[2] * actual[2] + expected[3] * actual[3] < 0f)
            sign = -1f
        for (i in 0..3) {
            assertTrue(abs(expected[i] - actual[i] * sign) < 1e-4f, "q[$i] ${actual[i]} != ${expected[i]}")
        }
    }
}
