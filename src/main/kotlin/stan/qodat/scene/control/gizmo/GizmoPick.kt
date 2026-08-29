package stan.qodat.scene.control.gizmo

import com.sun.javafx.scene.CameraHelper
import javafx.geometry.Point3D
import javafx.scene.Camera
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import org.joml.Vector3f

/**
 * Scene-space pick ray from a JavaFX [Camera], then optionally mapped into a
 * node's local space so gizmo math matches translated / gridded entities.
 */
object GizmoPick {

    fun sceneRay(camera: Camera, event: MouseEvent): Pair<Point3D, Point3D> =
        sceneRay(camera, event.sceneX, event.sceneY)

    fun sceneRay(camera: Camera, sceneX: Double, sceneY: Double): Pair<Point3D, Point3D> {
        val transform = camera.localToSceneTransform
        val origin = Point3D(transform.tx, transform.ty, transform.tz)
        val onPlane = CameraHelper.pickProjectPlane(camera, sceneX, sceneY)
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
