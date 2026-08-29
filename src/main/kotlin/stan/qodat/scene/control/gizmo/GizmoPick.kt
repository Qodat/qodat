package stan.qodat.scene.control.gizmo

import com.sun.javafx.scene.CameraHelper
import javafx.geometry.Point3D
import javafx.scene.Camera
import javafx.scene.Node
import javafx.scene.SubScene
import javafx.scene.input.MouseEvent
import org.joml.Vector3f
import stan.qodat.scene.SubScene3D

/**
 * Pick ray from a JavaFX [Camera] in the 3D viewport.
 *
 * Qodat renders into a [SubScene], so the click must be converted from the
 * parent Scene into SubScene-local pixels. [CameraHelper.pickProjectPlane]
 * treats those as the camera viewport — passing [MouseEvent.sceneX] aims
 * the ray at the window chrome instead of the model.
 */
object GizmoPick {

    fun sceneRay(camera: Camera, event: MouseEvent): Pair<Point3D, Point3D> {
        val subScene = (event.source as? SubScene) ?: SubScene3D.subSceneProperty.get()
        return if (subScene != null) {
            val local = subScene.sceneToLocal(event.sceneX, event.sceneY)
            sceneRay(camera, local.x, local.y)
        } else {
            sceneRay(camera, event.x, event.y)
        }
    }

    fun sceneRay(camera: Camera, viewX: Double, viewY: Double): Pair<Point3D, Point3D> {
        val origin = camera.localToScene(Point3D.ZERO)
        val onPlane = CameraHelper.pickProjectPlane(camera, viewX, viewY)
        return origin to onPlane.subtract(origin)
    }

    fun toLocal(node: Node, originScene: Point3D, directionScene: Point3D): GizmoMath.Ray {
        val origin = node.sceneToLocal(originScene)
        val tip = node.sceneToLocal(originScene.add(directionScene))
        val direction = tip.subtract(origin)
        return GizmoMath.Ray(
            Vector3f(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat()),
            Vector3f(direction.x.toFloat(), direction.y.toFloat(), direction.z.toFloat())
        )
    }

    fun cameraForwardLocal(camera: Camera, node: Node): Vector3f {
        val originScene = camera.localToScene(Point3D.ZERO)
        val aheadScene = camera.localToScene(Point3D(0.0, 0.0, 1.0))
        val origin = node.sceneToLocal(originScene)
        val ahead = node.sceneToLocal(aheadScene)
        val d = ahead.subtract(origin)
        return Vector3f(d.x.toFloat(), d.y.toFloat(), d.z.toFloat())
    }

    fun cameraDistance(camera: Camera, node: Node, localX: Double, localY: Double, localZ: Double): Double {
        val gizmoScene = node.localToScene(localX, localY, localZ)
        val camScene = camera.localToScene(Point3D.ZERO)
        return gizmoScene.distance(camScene)
    }
}
