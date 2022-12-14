package stan.qodat.scene

import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.AmbientLight
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.input.DragEvent
import javafx.scene.input.MouseEvent
import stan.qodat.Properties
import stan.qodat.scene.control.CameraHandler
import stan.qodat.scene.runescape.animation.AnimationPlayer
import stan.qodat.scene.shape.AxisView
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
    val scalingGroup = AutoScalingGroup()

    private lateinit var axisView: AxisView

    val cameraHandler = CameraHandler()

    val contextProperty = SimpleObjectProperty<SceneContext>()

    /**
     * Sequences animations for Rune-Scape models.
     */
    val animationPlayer = AnimationPlayer()

    val mouseListener = SimpleObjectProperty<EventHandler<MouseEvent>>()

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
            if (newValue != null) {
                val sceneContents = newValue.getSceneNode()
                scalingGroup.children.add(sceneContents)
            }
            Properties.selectedScene.set(newValue?.name)
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

    override val mouseEventHandler = EventHandler<MouseEvent> {
        if (mouseListener.get() != null){
            mouseListener.get().handle(it)
            if (it.isConsumed)
                return@EventHandler
        }
        cameraHandler.mouseEventHandler.handle(it)
    }

    override val dragEventHandler = EventHandler<DragEvent> {
    }
    override val keyEventHandler = cameraHandler.keyEventHandler
    override val scrollEventHandler = cameraHandler.scrollEventHandler
    override val zoomEventHandler = cameraHandler.zoomEventHandler

    override fun createSubScene(copyNodes: Boolean): SubScene {
        return SubScene(
            if (copyNodes)
                root
            else
                Group().apply {
                    children.addAll(
                        cameraHandler.cameraTransformGroup,
                        ambientLight
                    )
                },
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