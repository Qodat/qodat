package stan.qodat.scene.control.gizmo

import javafx.scene.control.Slider
import org.joml.Vector3f
import org.joml.primitives.Intersectionf
import org.joml.primitives.Planef
import org.joml.primitives.Rayf
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.floor

class GizmoController(val gizmo: GizmoStackoverflow.Gizmo) {

    var translateAxes = arrayOf(
        GizmoAxis(GizmoAxisType.X, Vector3f(0f, 0f, 90f)),
        GizmoAxis(GizmoAxisType.Y, Vector3f(0f, 0f, 0f)),
        GizmoAxis(GizmoAxisType.Z, Vector3f(0f, 90f, 90f))
    )

    var rotateAxes = arrayOf(
        GizmoAxis(GizmoAxisType.X, Vector3f(0f, 0f, 90f)),
        GizmoAxis(GizmoAxisType.Y, Vector3f(0f, 0f, 0f)),
        GizmoAxis(GizmoAxisType.Z, Vector3f(0f, 90f, 90f))
    )

    val rotateSliderX = Slider(-255.0, 255.0, 0.0)
    val rotateSliderY = Slider(-255.0, 255.0, 0.0)
    val rotateSliderZ = Slider(-255.0, 255.0, 0.0)

    val translateSliderX = Slider(-1000.0, 1000.0, 0.0)
    val translateSliderY = Slider(-1000.0, 1000.0, 0.0)
    val translateSliderZ = Slider(-1000.0, 1000.0, 0.0)

    var position = Vector3f()

    fun manipulateTranslateGizmo(ray: Rayf) {
        val axis = when(gizmo.selectedAxis.get()) {
            GizmoStackoverflow.Axis.X -> translateAxes[0]
            GizmoStackoverflow.Axis.Y -> translateAxes[1]
            GizmoStackoverflow.Axis.Z -> translateAxes[2]
            else -> return
        }
        val intersection = getPlaneIntersection(ray)
        if (axis.previousIntersection != Vector3f()
            && intersection != axis.previousIntersection
        ) {
            val delta = Vector3f(intersection).sub(axis.previousIntersection)[axis.type.ordinal]
            transform(axis, delta)
        }
        axis.previousIntersection = intersection
    }
    private fun transform(axis: GizmoAxis, delta: Float) {
        val slider = when (axis.type) {
            GizmoAxisType.X -> translateSliderX
            GizmoAxisType.Y -> translateSliderY
            GizmoAxisType.Z -> translateSliderZ
        }
        slider.min = -1000.0
        slider.max = 1000.0
        var value = if (delta > 0) ceil(delta) else floor(delta)
        value = if (axis.type == GizmoAxisType.X) -value else value
        val newValue = slider.value + value
        val limited = slider.limit(newValue, true)
        println("$newValue\t$limited")
        slider.adjustValue(limited)
    }

    fun manipulateRotateGizmo(ray: Rayf) {

        val axis = when(gizmo.selectedAxis.get()) {
            GizmoStackoverflow.Axis.X -> rotateAxes[2]
            GizmoStackoverflow.Axis.Y -> rotateAxes[0]
            GizmoStackoverflow.Axis.Z -> rotateAxes[1]
            else -> return
        }

        val intersection = getCircleIntersection(ray)
        if (axis.previousIntersection != Vector3f()
//                && !intersection.equals(axis.previousIntersection, MOVE_THRESHOLD)
        ) {
            val cross = Vector3f(intersection).cross(axis.previousIntersection)
            val sin = cross.length()
            val theta = asin(sin)
            val delta = Math.toDegrees(theta.toDouble())
//            println("manipulate($ray) -> $intersection\t$cross\t$sin\t$theta\t$delta")
            transform(axis, delta, cross[axis.type.ordinal] > 0)
        } else {
//            println("intersection($ray) -> $intersection")
        }
        axis.previousIntersection = intersection
    }

    private fun transform(axis: GizmoAxis, delta: Double, negative: Boolean) {
        var value = ceil(delta).toInt()
        value = if (negative) -value else value
        value = if (axis.type == GizmoAxisType.Y) -value else value
        value = value.coerceIn(-1, 1) * ROTATION_SPEED
//        println(value)

        val slider = when (axis.type) {
            GizmoAxisType.X -> rotateSliderX
            GizmoAxisType.Y -> rotateSliderY
            GizmoAxisType.Z -> rotateSliderZ
        }
//        println(axis.type)
        val newValue = slider.value + value
        val limited = slider.limit(newValue, true)
        println("$newValue\t$limited")
        slider.adjustValue(limited)
//        context.gui.editorPanel.sliders[axis.type.ordinal].adjust(value, true)
    }

    private fun Slider.limit(value: Double, cyclic: Boolean = false): Double {
        return if (!cyclic) {
            value.coerceIn(min, max)
        } else {
            when {
                value > max -> min
                value < min -> max
                else -> value
            }
        }
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