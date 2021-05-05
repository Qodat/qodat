package stan.qodat.scene.control

import javafx.event.EventHandler
import javafx.scene.PerspectiveCamera
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ZoomEvent
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import stan.qodat.Properties
import stan.qodat.scene.transform.TransformGroup
import stan.qodat.util.onInvalidation
import kotlin.math.max
import kotlin.math.min

/**
 * Handles mouse, scroll, zoom, and key events which may transform the position and/or rotation of the [camera].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class CameraHandler(
    val camera: PerspectiveCamera = PerspectiveCamera(true).also {
        it.nearClipProperty().bind(Properties.cameraNearClip)
        it.farClipProperty().bind(Properties.cameraFarClip)
    }
) {

    val position = Translate(0.0, -100.0, -1000.0)

    val cameraTransformGroup = TransformGroup()

    private val xViewRotate = Rotate(0.0, 0.0, 0.0, 0.0, Rotate.X_AXIS)
    private val zViewRotate = Rotate(0.0, 0.0, 0.0, 0.0, Rotate.Z_AXIS)

    init {

        camera.transforms.addAll(
            position,
            xViewRotate,
            zViewRotate
        )

        cameraTransformGroup.children.add(camera)

        Properties.cameraMinZoom.onInvalidation {
            updateZValue(position.z)
        }
        Properties.cameraMaxZoom.onInvalidation {
            updateZValue(position.z)
        }
    }

    private var firstEvent = true
    private var mouseX = 0.0
    private var mouseY = 0.0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    val mouseEventHandler = EventHandler<MouseEvent> {

        lastMouseX = if (firstEvent) it.sceneX else mouseX
        lastMouseY = if (firstEvent) it.sceneY else mouseY
        mouseX = it.sceneX
        mouseY = it.sceneY

        val mouseDeltaX = mouseX - lastMouseX
        val mouseDeltaY = mouseY - lastMouseY

        firstEvent = false

        val cameraSpeed = when {
            it.isControlDown -> 0.1
            it.isShiftDown -> 10.0
            else -> 1.0
        } * Properties.cameraSpeed.get()

        when {
            it.isPrimaryButtonDown -> {
                if (it.isControlDown)
                    return@EventHandler
                if (Properties.cameraInvert.get())
                    cameraTransformGroup.yRotate.angle += mouseDeltaX.times(cameraSpeed.times(2))
                else
                    cameraTransformGroup.yRotate.angle -= mouseDeltaX.times(cameraSpeed.times(2))
                cameraTransformGroup.xRotate.angle -= mouseDeltaY.times(cameraSpeed.times(2))
                it.consume()
            }
            it.isSecondaryButtonDown -> {
                if (it.isControlDown) {
                    cameraTransformGroup.translate.x += mouseDeltaX.times(cameraSpeed).times(30)
                    cameraTransformGroup.translate.y += mouseDeltaY.times(cameraSpeed).times(30)
                } else {
                    position.z += (mouseDeltaX + mouseDeltaY).times(cameraSpeed).times(20)
                }
                it.consume()
            }
        }
    }

    val keyEventHandler = EventHandler<KeyEvent> {
        // TODO: implement
    }

    val scrollEventHandler = EventHandler<ScrollEvent> {
        if (it.touchCount > 0){
            cameraTransformGroup.translate.x -= it.deltaX.times(0.01)
            cameraTransformGroup.translate.y += it.deltaY.times(0.01)
        } else {
            val newZ = position.z - it.deltaY.times(0.2)
            updateZValue(newZ)
        }
    }

    val zoomEventHandler = EventHandler<ZoomEvent> {
        if (java.lang.Double.isNaN(it.zoomFactor) && it.zoomFactor in 0.0..1.2) {
            val newZ = position.z.div(it.zoomFactor)
            updateZValue(newZ)
        }
    }

    private fun updateZValue(newZ: Double) {
        var newZ1 = newZ
        newZ1 = max(newZ1, Properties.cameraMaxZoom.get())
        newZ1 = min(newZ1, Properties.cameraMinZoom.get())
        position.z = newZ1
    }
}