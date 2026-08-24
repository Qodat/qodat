package stan.qodat.scene.control.gizmo

import org.joml.primitives.Rayf
import us.ihmc.euclid.geometry.Line3D

const val ROTATION_SPEED = 3

fun Line3D.toRay() = Rayf(
    pointX.toFloat(), pointY.toFloat(), pointZ.toFloat(),
    directionX.toFloat(), directionY.toFloat(), directionZ.toFloat()
)