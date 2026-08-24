package stan.qodat.scene.control.gizmo

import us.ihmc.euclid.geometry.Line3D
import us.ihmc.euclid.tuple3D.Point3D
import us.ihmc.euclid.tuple3D.Vector3D
import kotlin.test.Test
import kotlin.test.assertEquals

class GizmoUtilTest {

    @Test
    fun lineToRayCopiesPointAndDirection() {
        val line = Line3D(Point3D(1.5, -2.0, 3.25), Vector3D(0.0, 1.0, 0.0))
        val ray = line.toRay()
        assertEquals(1.5f, ray.oX)
        assertEquals(-2.0f, ray.oY)
        assertEquals(3.25f, ray.oZ)
        assertEquals(0.0f, ray.dX)
        assertEquals(1.0f, ray.dY)
        assertEquals(0.0f, ray.dZ)
    }

    @Test
    fun rotationSpeedStaysSmall() {
        assertEquals(3, ROTATION_SPEED)
    }
}
