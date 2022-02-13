package stan.qodat.scene.control.gizmo

import org.joml.Vector3f

class GizmoAxis(val type: GizmoAxisType, var rotation: Vector3f) {

    var previousIntersection = Vector3f()
    private val defaultRotation = Vector3f(rotation)

    fun reset() {
        rotation = Vector3f(defaultRotation)
    }
}

