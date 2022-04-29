package stan.qodat

import javafx.beans.property.*
import javafx.scene.SceneAntialiasing
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import qodat.cache.Cache
import stan.qodat.scene.controller.EntityViewController.SortType
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.util.PropertiesManager
import java.nio.file.Path
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

    val viewerDivider1Position = SimpleDoubleProperty(0.5)
    val viewerDivider2Position = SimpleDoubleProperty(0.3)
    val viewerDivider3Position = SimpleDoubleProperty(0.6)

    val centerDivider1Position = SimpleDoubleProperty(0.3)
    val centerDivider2Position = SimpleDoubleProperty(0.9)

    val subSceneBackgroundColor = SimpleObjectProperty(Color.web("0x33333300"))
    val sceneInitialWidth = SimpleDoubleProperty(1080.0)
    val sceneInitialHeight = SimpleDoubleProperty(720.0)

    val subSceneInitialWidth = SimpleDoubleProperty(400.0)
    val subSceneInitialHeight = SimpleDoubleProperty(400.0)

    val alwaysRenderUsingAtlas = SimpleBooleanProperty(false)

    val cameraInvert = SimpleBooleanProperty(true)
    val cameraSpeed = SimpleDoubleProperty(0.3)
    val cameraNearClip = SimpleDoubleProperty(1.0)
    val cameraFarClip = SimpleDoubleProperty(10000.0)
    val cameraMaxZoom = SimpleDoubleProperty(-3000.0)
    val cameraMinZoom = SimpleDoubleProperty(0.0)

    val viewerCache = SimpleObjectProperty<Cache>()
    val editorCache = SimpleObjectProperty<Cache>()

    val rootPath = SimpleObjectProperty(Paths.get(System.getProperty("user.home"), ".qodat"))
    val osrsCachePath = SimpleObjectProperty(rootPath.get().resolve("caches/OS/rev203"))
    val qodatCachePath = SimpleObjectProperty(rootPath.get().resolve("caches/qodat"))
    val legacyCachePath = SimpleObjectProperty(rootPath.get().resolve("caches/667"))

    val downloadsPath = SimpleObjectProperty(rootPath.get().resolve("downloads"))

    val projectFilesPath = SimpleObjectProperty(rootPath.get().resolve("data"))
    val defaultExportsPath = SimpleObjectProperty(rootPath.get().resolve("exports"))
    val lastWaveFrontSingleExportPath = SimpleObjectProperty<Path?>(null)
    val lastWaveFrontSequenceExportPath = SimpleObjectProperty<Path?>(null)
    val lastGIFExportPath = SimpleObjectProperty<Path?>(null)


    val selectedNpcName = SimpleStringProperty()
    val selectedItemName = SimpleStringProperty()
    val selectedObjectName = SimpleStringProperty()
    val selectedSpriteName = SimpleStringProperty()
    val selectedSpotAnimName = SimpleStringProperty()
    val selectedAnimationName = SimpleStringProperty()
    val selectedInterfaceName = SimpleStringProperty()

    val selectedNpcSortType = SimpleObjectProperty(SortType.NAME)
    val selectedItemSortType = SimpleObjectProperty(SortType.NAME)
    val selectedObjectSortType = SimpleObjectProperty(SortType.NAME)
    val selectedSpotAnimSortType = SimpleObjectProperty(SortType.ID)

    val selectedEntity = SimpleObjectProperty<Entity<*>>()
    val selectedAnimation = SimpleObjectProperty<Animation>()

    val selectedViewerTab = SimpleStringProperty()
    val selectedRightTab = SimpleStringProperty()
    val selectedLeftTab = SimpleStringProperty()
    val selectedBottomTab = SimpleStringProperty()
    val selectedScene = SimpleStringProperty()
    val lockScene = SimpleBooleanProperty(false)

    val treeItemAnimationFrameSelectedColor = SimpleObjectProperty(Color.web("#7e76aa"))
    val treeItemAnimationFrameColor = SimpleObjectProperty(Color.web("#bba5c7"))

    val treeItemAnimationsExpanded = SimpleBooleanProperty(true)
    val treeItemModelsExpanded = SimpleBooleanProperty(true)
    val treeItemEntityExpanded = SimpleBooleanProperty(true)
    val treeItemInterfaceExpanded = SimpleBooleanProperty(true)

    val copyModelsFromNpc = SimpleBooleanProperty(true)
    val copyAnimationsFromNpc = SimpleBooleanProperty(true)

    fun bind(sessionManager: PropertiesManager) {

        sessionManager.bind("anti-aliasing", antialiasing) {
            if (it == SceneAntialiasing.BALANCED.toString())
                SceneAntialiasing.BALANCED
            else
                SceneAntialiasing.DISABLED
        }
        sessionManager.bindString("last-selected-npc-name", selectedNpcName)
        sessionManager.bindString("last-selected-item-name", selectedItemName)
        sessionManager.bindString("last-selected-object-name", selectedObjectName)
        sessionManager.bindString("last-selected-spot-anim-name", selectedSpotAnimName)
        sessionManager.bindString("last-selected-animation-name", selectedAnimationName)
        sessionManager.bindString("last-selected-viewer-tab", selectedViewerTab)
        sessionManager.bindString("last-selected-right-tab", selectedRightTab)
        sessionManager.bindString("last-selected-left-tab", selectedLeftTab)
        sessionManager.bindString("last-selected-bottom-tab", selectedBottomTab)
        sessionManager.bindString("last-selected-scene", selectedScene)

        sessionManager.bindBoolean("tree-item-animations-expanded", treeItemAnimationsExpanded)
        sessionManager.bindBoolean("tree-item-models-expanded", treeItemModelsExpanded)
        sessionManager.bindBoolean("tree-item-entity-expanded", treeItemEntityExpanded)
        sessionManager.bindBoolean("tree-item-interface-expanded", treeItemInterfaceExpanded)

        sessionManager.bindColor("ambient-light-color", ambientLightColor)
        sessionManager.bindColor("background-color", subSceneBackgroundColor)

        sessionManager.bindBoolean("always-render-using-atlas", alwaysRenderUsingAtlas)

        sessionManager.bindBoolean("depth-buffer", depthBuffer)
        sessionManager.bindBoolean("show-axis", showAxis)
        sessionManager.bindBoolean("show-fps", showFPS)
        sessionManager.bindBoolean("show-frames-tab", showFramesTab)
        sessionManager.bindBoolean("expand-settings", expandSettings)
        sessionManager.bindBoolean("lock-scene", lockScene)
        sessionManager.bindBoolean("copy-models-from-npc", copyModelsFromNpc)
        sessionManager.bindBoolean("copy-animations-from-npc", copyAnimationsFromNpc)

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
        sessionManager.bindDouble("split-pane-divider-4-position", viewerDivider1Position)
        sessionManager.bindDouble("split-pane-divider-5-position", viewerDivider2Position)
        sessionManager.bindDouble("split-pane-divider-6-position", viewerDivider3Position)

        sessionManager.bindEnum("selected-npc-sort-type", selectedNpcSortType)
        sessionManager.bindEnum("selected-item-sort-type", selectedItemSortType)
        sessionManager.bindEnum("selected-object-sort-type", selectedObjectSortType)

        sessionManager.bindPath("osrs-cache-path", osrsCachePath)
        sessionManager.bindPath("qodat-cache-path", qodatCachePath)
        sessionManager.bindPath("legacy-cache-path", legacyCachePath)
        sessionManager.bindPath("main-data-path", projectFilesPath)
        sessionManager.bindPath("exports-path", defaultExportsPath)
    }
}