package stan.qodat.scene.control.gizmo

import javafx.geometry.Point3D
import javafx.scene.Camera
import javafx.scene.Node
import javafx.scene.PerspectiveCamera
import javafx.scene.SubScene
import javafx.scene.input.MouseEvent
import org.joml.Vector3f
import stan.qodat.scene.SubScene3D
import kotlin.math.tan

/**
 * Pick ray from a JavaFX [Camera] in the 3D viewport.
 *
 * Qodat renders into a [SubScene], so the click must be converted from the
 * parent Scene into SubScene-local pixels. The ray is the public-API
 * equivalent of OpenJFX `PickRay.computePerspectivePickRay` for a
 * fixed-eye [PerspectiveCamera] — `CameraHelper` is not exported.
 */
object GizmoPick {

    fun sceneRay(camera: Camera, event: MouseEvent): Pair<Point3D, Point3D> {
        val subScene = (event.source as? SubScene) ?: SubScene3D.subSceneProperty.get()
        return if (subScene != null) {
            val local = subScene.sceneToLocal(event.sceneX, event.sceneY)
            sceneRay(camera, local.x, local.y, subScene.width, subScene.height)
        } else {
            val scene = camera.scene
            sceneRay(camera, event.x, event.y, scene?.width ?: 1.0, scene?.height ?: 1.0)
        }
    }

    fun sceneRay(
        camera: Camera,
        viewX: Double,
        viewY: Double,
        viewWidth: Double,
        viewHeight: Double
    ): Pair<Point3D, Point3D> {
        val perspective = camera as? PerspectiveCamera
        val onPlaneLocal = if (perspective != null)
            projectionPlaneLocal(
                viewX,
                viewY,
                viewWidth,
                viewHeight,
                perspective.fieldOfView,
                perspective.isVerticalFieldOfView
            )
        else
            Point3D(viewX - viewWidth / 2.0, viewY - viewHeight / 2.0, 1.0)
        val origin = camera.localToScene(Point3D.ZERO)
        val onPlane = camera.localToScene(onPlaneLocal)
        return origin to onPlane.subtract(origin)
    }

    fun sceneRay(camera: Camera, viewX: Double, viewY: Double): Pair<Point3D, Point3D> {
        val subScene = SubScene3D.subSceneProperty.get()
        val width = subScene?.width ?: camera.scene?.width ?: 1.0
        val height = subScene?.height ?: camera.scene?.height ?: 1.0
        return sceneRay(camera, viewX, viewY, width, height)
    }

    /**
     * Camera-local point on the projection plane. Matches OpenJFX
     * `PickRay.computePerspectivePickRay` for `fixedEye = true`.
     */
    fun projectionPlaneLocal(
        viewX: Double,
        viewY: Double,
        viewWidth: Double,
        viewHeight: Double,
        fieldOfViewDegrees: Double,
        verticalFieldOfView: Boolean
    ): Point3D {
        val width = viewWidth.coerceAtLeast(1.0)
        val height = viewHeight.coerceAtLeast(1.0)
        val halfWidth = width / 2.0
        val halfHeight = height / 2.0
        val halfDim = if (verticalFieldOfView) halfHeight else halfWidth
        val distanceZ = halfDim / tan(Math.toRadians(fieldOfViewDegrees) / 2.0)
        return Point3D(viewX - halfWidth, viewY - halfHeight, distanceZ)
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
