package stan.qodat.scene

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.AmbientLight
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.input.DragEvent
import stan.qodat.Properties
import stan.qodat.scene.control.CameraHandler
import stan.qodat.scene.runescape.animation.AnimationPlayer
import stan.qodat.scene.shape.AxisView
import stan.qodat.scene.transform.AutoScalingGroup
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
object SubScene3D : AbstractSubScene() {

    private val root = Group()

    private val ambientLight = AmbientLight()

    /**
     * Contains nodes to be rendered in the scene.
     */
    private val scalingGroup = AutoScalingGroup()

    private lateinit var axisView: AxisView

    val cameraHandler = CameraHandler()

    val lockContextProperty = SimpleBooleanProperty(true)
    val contextProperty = SimpleObjectProperty<SceneContext>()

    var lastContext : SceneContext? = null

    /**
     * Sequences animations for Rune-Scape models.
     */
    val animationPlayer = AnimationPlayer()

    fun init() {
        ambientLight.colorProperty().setAndBind(Properties.ambientLightColor)

        root.children.addAll(
            cameraHandler.cameraTransformGroup,
            scalingGroup,
            ambientLight
        )

        contextProperty.addListener { _, oldValue, newValue ->
            if (oldValue != null)
                scalingGroup.children.remove(oldValue.getSceneNode())
            if (newValue != null)
                scalingGroup.children.add(newValue.getSceneNode())
        }

        Properties.antialiasing.onInvalidation {
            rebuildSubScene()
        }
        Properties.showAxis.onInvalidation {
            rebuildAxis()
        }

        rebuildSubScene()
        rebuildAxis()

    }

    override val mouseEventHandler = cameraHandler.mouseEventHandler

    override val dragEventHandler = EventHandler<DragEvent> {
    }
    override val keyEventHandler = cameraHandler.keyEventHandler
    override val scrollEventHandler = cameraHandler.scrollEventHandler
    override val zoomEventHandler = cameraHandler.zoomEventHandler

    override fun createSubScene(): SubScene {
        return SubScene(root,
            Properties.subSceneInitialWidth.get(),
            Properties.subSceneInitialHeight.get(),
            Properties.depthBuffer.get(),
            Properties.antialiasing.get())
            .also {
                it.cacheHint = CacheHint.SPEED
                it.camera = cameraHandler.camera
            }
    }

    private fun rebuildAxis() {
        if (Properties.showAxis.get()){
            if (!this::axisView.isInitialized)
                axisView = AxisView()
            scalingGroup.children.add(axisView)
        } else if (this::axisView.isInitialized)
            scalingGroup.children.remove(axisView)
    }

}