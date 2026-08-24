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

    var translateAxes = axes()
    var rotateAxes = axes()

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
        val slider = translateSlider(axis.type)
        slider.min = -1000.0
        slider.max = 1000.0
        var value = if (delta > 0) ceil(delta) else floor(delta)
        value = if (axis.type == GizmoAxisType.X) -value else value
        slider.applyCyclic(slider.value + value)
    }

    fun manipulateRotateGizmo(ray: Rayf) {

        val axis = when(gizmo.selectedAxis.get()) {
            GizmoStackoverflow.Axis.X -> rotateAxes[2]
            GizmoStackoverflow.Axis.Y -> rotateAxes[0]
            GizmoStackoverflow.Axis.Z -> rotateAxes[1]
            else -> return
        }

        val intersection = getCircleIntersection(ray)
        if (axis.previousIntersection != Vector3f()) {
            val cross = Vector3f(intersection).cross(axis.previousIntersection)
            val sin = cross.length()
            val theta = asin(sin)
            val delta = Math.toDegrees(theta.toDouble())
            transform(axis, delta, cross[axis.type.ordinal] > 0)
        }
        axis.previousIntersection = intersection
    }

    private fun transform(axis: GizmoAxis, delta: Double, negative: Boolean) {
        var value = ceil(delta).toInt()
        value = if (negative) -value else value
        value = if (axis.type == GizmoAxisType.Y) -value else value
        value = value.coerceIn(-1, 1) * ROTATION_SPEED
        val slider = rotateSlider(axis.type)
        slider.applyCyclic(slider.value + value)
    }

    private fun translateSlider(type: GizmoAxisType) = when (type) {
        GizmoAxisType.X -> translateSliderX
        GizmoAxisType.Y -> translateSliderY
        GizmoAxisType.Z -> translateSliderZ
    }

    private fun rotateSlider(type: GizmoAxisType) = when (type) {
        GizmoAxisType.X -> rotateSliderX
        GizmoAxisType.Y -> rotateSliderY
        GizmoAxisType.Z -> rotateSliderZ
    }

    private fun Slider.applyCyclic(newValue: Double) {
        adjustValue(
            when {
                newValue > max -> min
                newValue < min -> max
                else -> newValue
            }
        )
    }

    private fun getCircleIntersection(ray: Rayf): Vector3f {
        val onPlane = getPlaneIntersection(ray)
        val p = Vector3f(onPlane).sub(position).normalize()
        val point = Vector3f(position).add(p)
        return Vector3f(onPlane).sub(point).normalize()
    }

    private fun getPlaneIntersection(ray: Rayf): Vector3f {
        val plane = Planef(Vector3f(position), Vector3f(-ray.dX, -ray.dY, -ray.dZ))
        val epsilon = Intersectionf.intersectRayPlane(ray, plane, 0f)
        return Vector3f(ray.oX, ray.oY, ray.oZ).add(Vector3f(ray.dX, ray.dY, ray.dZ).mul(epsilon))
    }

    private fun axes() = arrayOf(
        GizmoAxis(GizmoAxisType.X),
        GizmoAxis(GizmoAxisType.Y),
        GizmoAxis(GizmoAxisType.Z)
    )
}
