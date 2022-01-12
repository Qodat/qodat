package stan

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.transform.Rotate
import org.joml.Vector3f
import org.joml.primitives.Intersectionf
import org.joml.primitives.Planef
import org.joml.primitives.Rayf
import stan.qodat.util.onInvalidation
import us.ihmc.euclid.geometry.Line3D
import kotlin.math.asin
import kotlin.math.ceil

const val ROTATION_SPEED = 3
const val MOVE_THRESHOLD = 0.01f

class GizmoController(val gizmo: GizmoStackoverflow.Gizmo) {

    val rotateX = Rotate(0.0, Rotate.X_AXIS)
    val rotateY = Rotate(0.0, Rotate.Y_AXIS)
    val rotateZ = Rotate(0.0, Rotate.Z_AXIS)

    var axes = arrayOf(
        GizmoAxis(AxisType.X, Vector3f(0f, 0f, 90f)),
        GizmoAxis(AxisType.Y, Vector3f(0f, 0f, 0f)),
        GizmoAxis(AxisType.Z, Vector3f(0f, 90f, 90f))
    )
    internal var selectedAxis: GizmoAxis = axes.first()

    var nodeProperty = SimpleObjectProperty<Node>().apply {
        onInvalidation {
            get().transforms.addAll(rotateX, rotateY, rotateZ)
        }
    }
    var position = Vector3f()

    fun Line3D.toRay() = Rayf(
        pointX.toFloat(), pointY.toFloat(), pointZ.toFloat(),
        directionX.toFloat(), directionY.toFloat(), directionZ.toFloat()
    )

    fun manipulate(ray: Rayf) {

        val axis = when(gizmo.selectedAxis.get()) {
            GizmoStackoverflow.Axis.X -> axes[0]
            GizmoStackoverflow.Axis.Y -> axes[1]
            GizmoStackoverflow.Axis.Z -> axes[2]
            else -> return
        }

        val intersection = getCircleIntersection(ray)
        if (axis.previousIntersection != Vector3f()
//                && !intersection.equals(it.previousIntersection, MOVE_THRESHOLD)
        ) {
            val cross = Vector3f(intersection).cross(axis.previousIntersection)
            val sin = cross.length()
            val theta = asin(sin)
            val delta = Math.toDegrees(theta.toDouble())
            println("manipulate($ray) -> $intersection\t$cross\t$sin\t$theta\t$delta")
            transform(axis, delta, cross[axis.type.ordinal] > 0)
        } else {
            println("intersection($ray) -> $intersection")
        }
        axis.previousIntersection = intersection
    }

    private fun transform(axis: GizmoAxis, delta: Double, negative: Boolean) {
        var value = ceil(delta).toInt()
        value = if (negative) -value else value
        value = if (axis.type == AxisType.Y) -value else value
        value = value.coerceIn(-1, 1) * ROTATION_SPEED
        println(value)

        val rotate = when (axis.type) {
            AxisType.X -> rotateX
            AxisType.Y -> rotateY
            AxisType.Z -> rotateZ
        }
        println(axis.type)
        rotate.angle += value.toDouble()
//        context.gui.editorPanel.sliders[axis.type.ordinal].adjust(value, true)
    }

    private fun getCircleIntersection(ray: Rayf): Vector3f {
        val onPlane = getPlaneIntersection(ray)
        // Project point onto circle's circumference
        val p = Vector3f(onPlane).sub(position).normalize()
        val point = Vector3f(position).add(p)
        return Vector3f(onPlane).sub(point).normalize()
    }

    internal fun getPlaneIntersection(ray: Rayf): Vector3f {
        // Allow for transforming without need to hover over axis
        val plane = Planef(Vector3f(position), Vector3f(-ray.dX, -ray.dY, -ray.dZ))
        val epsilon = Intersectionf.intersectRayPlane(ray, plane, 0f)

        // Origin + direction * epsilon
        return Vector3f(ray.oX, ray.oY, ray.oZ).add(Vector3f(ray.dX, ray.dY, ray.dZ).mul(epsilon))
    }

}