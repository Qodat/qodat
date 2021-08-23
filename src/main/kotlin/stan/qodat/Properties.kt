package stan.qodat

import javafx.beans.property.*
import javafx.scene.SceneAntialiasing
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import stan.qodat.cache.Cache
import stan.qodat.util.PropertiesManager
import java.nio.file.Paths

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
object Properties {

    private val DEFAULT_X_AXIS_MATERIAL = PhongMaterial().also {
        it.diffuseColor = Color.DARKRED
        it.specularColor = Color.RED
    }
    private val DEFAULT_Y_AXIS_MATERIAL = PhongMaterial().also {
        it.diffuseColor = Color.DARKGREEN
        it.specularColor = Color.GREEN
    }
    private val DEFAULT_Z_AXIS_MATERIAL = PhongMaterial().also {
        it.diffuseColor = Color.DARKBLUE
        it.specularColor = Color.BLUE
    }

    val drawModeProperty = SimpleObjectProperty(DrawMode.FILL)

    /**
     * Should MSAA anti-aliasing be used in render engine?
     */
    val antialiasing = SimpleObjectProperty(SceneAntialiasing.BALANCED)
    val ambientLightColor = SimpleObjectProperty(Color.WHITE)

    val disableAnimationsView = SimpleBooleanProperty(false)
    val depthBuffer = SimpleBooleanProperty(true)
    /**
     * Should an [AxisView] object be placed inside the [Scene3D]?
     */
    val showAxis = SimpleBooleanProperty(true)
    val showFPS = SimpleBooleanProperty(true)
    val showPriorityLabels = SimpleBooleanProperty(false)
    val showFramesTab = SimpleBooleanProperty(false)
    val expandSettings = SimpleBooleanProperty(true)

    val xAxisMaterial = SimpleObjectProperty(DEFAULT_X_AXIS_MATERIAL)
    val yAxisMaterial = SimpleObjectProperty(DEFAULT_Y_AXIS_MATERIAL)
    val zAxisMaterial = SimpleObjectProperty(DEFAULT_Z_AXIS_MATERIAL)

    val gridPlaneMaterial = SimpleObjectProperty(PhongMaterial(Color.AQUAMARINE))

    val sceneDividerPosition = SimpleDoubleProperty(0.3)
    val centerDivider1Position = SimpleDoubleProperty(0.3)
    val centerDivider2Position = SimpleDoubleProperty(0.9)

    val subSceneBackgroundColor = SimpleObjectProperty(Color.WHITE)
    val sceneInitialWidth = SimpleDoubleProperty(1080.0)
    val sceneInitialHeight = SimpleDoubleProperty(720.0)

    val subSceneInitialWidth = SimpleDoubleProperty(400.0)
    val subSceneInitialHeight = SimpleDoubleProperty(400.0)

    val cameraInvert = SimpleBooleanProperty()
    val cameraSpeed = SimpleDoubleProperty(0.3)
    val cameraNearClip = SimpleDoubleProperty(1.0)
    val cameraFarClip = SimpleDoubleProperty(10000.0)
    val cameraMaxZoom = SimpleDoubleProperty(-3000.0)
    val cameraMinZoom = SimpleDoubleProperty(0.0)

    val cache = SimpleObjectProperty<Cache>()
    val osrsCachePath = SimpleObjectProperty(Paths.get("caches/LIVE"))

    val selectedNpcName = SimpleStringProperty()
    val selectedItemName = SimpleStringProperty()
    val selectedAnimationName = SimpleStringProperty()

    fun bind(sessionManager: PropertiesManager) {

        sessionManager.bind("anti-aliasing", antialiasing) {
            if (it == SceneAntialiasing.BALANCED.toString())
                SceneAntialiasing.BALANCED
            else
                SceneAntialiasing.DISABLED
        }
        sessionManager.bindString("last-selected-npc-name", selectedNpcName)
        sessionManager.bindString("last-selected-item-name", selectedItemName)
        sessionManager.bindString("last-selected-animation-name", selectedAnimationName)

        sessionManager.bindColor("ambient-light-color", ambientLightColor)
        sessionManager.bindColor("background-color", subSceneBackgroundColor)

        sessionManager.bindBoolean("depth-buffer", depthBuffer)
        sessionManager.bindBoolean("show-axis", showAxis)
        sessionManager.bindBoolean("show-fps", showFPS)
        sessionManager.bindBoolean("show-frames-tab", showFramesTab)
        sessionManager.bindBoolean("expand-settings", expandSettings)

        sessionManager.bindBoolean("camera-inverse", cameraInvert)
        sessionManager.bindDouble("camera-speed", cameraSpeed)
        sessionManager.bindDouble("sub_scene-initial-width", subSceneInitialWidth)
        sessionManager.bindDouble("sub_scene-initial-height", subSceneInitialHeight)
        sessionManager.bindDouble("camera-near-clip", cameraNearClip)
        sessionManager.bindDouble("camera-far-clip", cameraFarClip)
        sessionManager.bindDouble("camera-min-zoom", cameraMinZoom)
        sessionManager.bindDouble("camera-max-zoom", cameraMaxZoom)

        sessionManager.bindDouble("scene-initial-width", sceneInitialWidth)
        sessionManager.bindDouble("scene-initial-height", sceneInitialHeight)

        sessionManager.bindDouble("split-pane-divider-1-position", centerDivider1Position)
        sessionManager.bindDouble("split-pane-divider-2-position", centerDivider2Position)
        sessionManager.bindDouble("split-pane-divider-3-position", sceneDividerPosition)
    }
}