package stan.qodat.scene.controller

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.control.dialog.CacheChooserDialog
import stan.qodat.scene.control.tree.RootSceneTreeItem
import stan.qodat.scene.layout.AutoScaleSubScenePane
import stan.qodat.util.bind
import stan.qodat.util.createDragSpace
import stan.qodat.util.createSelectTabListener
import stan.qodat.util.setAndBind
import java.awt.Desktop
import java.net.URL
import java.nio.file.Path
import java.util.*


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class MainController : SceneController("main-scene") {

    @FXML
    lateinit var modelsContainer: VBox

    @FXML
    lateinit var sceneTreeView: TreeView<Node>

    @FXML
    lateinit var leftFilesTab: ToggleButton

    @FXML
    lateinit var rightMainTab: ToggleButton

    @FXML
    lateinit var rightPluginsTab: ToggleButton

    @FXML
    lateinit var rightEditorTab: ToggleButton

    @FXML
    lateinit var rightViewerTab: ToggleButton

    @FXML
    lateinit var bottomFramesTab: ToggleButton

    @FXML
    lateinit var bottomEventLogTab: ToggleButton

    @FXML
    lateinit var mainPanes: Accordion

    @FXML
    lateinit var canvasPlaceHolder: Pane

    @FXML
    lateinit var splitPlane: SplitPane

    @FXML
    lateinit var mainPane: BorderPane

    @FXML
    lateinit var searchModelsList: TextField

    @FXML
    lateinit var menuBar: MenuBar

    @FXML
    lateinit var recentPathsMenu: Menu

    @FXML
    lateinit var leftTab: HBox

    @FXML
    lateinit var filesWindow: SplitPane

    @FXML
    lateinit var playControls: HBox

    @FXML
    lateinit var startBtn: Button

    @FXML
    lateinit var endBtn: Button

    @FXML
    lateinit var ffBtn: Button

    @FXML
    lateinit var rwBtn: Button

    @FXML
    lateinit var playBtn: ToggleButton

    @FXML
    lateinit var loopBtn: ToggleButton

    @FXML
    lateinit var bottomBox: VBox

    @FXML
    lateinit var sceneHBox: HBox

    @FXML
    lateinit var sceneLabel: Label

    @FXML
    lateinit var lockSceneContextButton: ToggleButton

    @FXML
    lateinit var sceneContextBox: ComboBox<SceneContext>

    @FXML
    lateinit var progressSpace: HBox

    private val rightTabContents = SimpleObjectProperty<Node?>()
    private val leftTabContents = SimpleObjectProperty<Node?>()
    private val bottomTabContents = SimpleObjectProperty<Node>()

    lateinit var settingsController: SettingsController
    lateinit var viewerController: ViewerController
    lateinit var editorController: EditorController
    lateinit var eventLogController: EventLogController

    private val divider1IndexProperty = SimpleIntegerProperty(0)
    private val divider2IndexProperty = SimpleIntegerProperty(1)
    private var lastLeftDividerPosition = 0.25
    private var lastRightDividerPosition = 0.75

    override fun onSwitch(next: SceneController) {
    }

    override fun getViewNode() = mainPanes

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        SplitSceneDividerDragRegion(filesWindow, sceneLabel, SimpleIntegerProperty(0), Properties.sceneDividerPosition)

        leftTab.minWidth = 1.0
        mainPanes.minWidth = 1.0

        sceneTreeView.root = RootSceneTreeItem()


        VBox.setVgrow(sceneTreeView, Priority.ALWAYS)
        VBox.setVgrow(modelsContainer, Priority.ALWAYS)

        val mainModelsLoader = FXMLLoader(Qodat::class.java.getResource("model.fxml"))
        val mainModelsView = mainModelsLoader.load<VBox>()
        mainModelsLoader.getController<ModelController>().apply {
            bind(sceneContext)
            enableDragAndDrop()
            syncWith(Properties.projectFilesPath)
        }
        modelsContainer.children.add(mainModelsView)
        addTabSelectedListener()

        val settingsLoader = FXMLLoader(Qodat::class.java.getResource("settings.fxml"))
        val settingsPane = settingsLoader.load<TitledPane>()
        settingsController = settingsLoader.getController()
        mainPanes.panes.add(settingsPane)

        val viewerLoader = FXMLLoader(Qodat::class.java.getResource("viewer.fxml"))
        val viewerPane = viewerLoader.load<SplitPane>() // same as viewNode from controller
        viewerController = viewerLoader.getController()
        viewerController.addTabSelectedListener()

        val editorLoader = FXMLLoader(Qodat::class.java.getResource("editor.fxml"))
        val editorPane = editorLoader.load<SplitPane>() // same as viewNode from controller
        editorController = editorLoader.getController()
        editorController.addTabSelectedListener()

        val timeLineBox = FXMLLoader.load<VBox>(Qodat::class.java.getResource("timeline.fxml"))

        val eventLogLoader = FXMLLoader(Qodat::class.java.getResource("eventlog.fxml"))
        val eventLogView = eventLogLoader.load<VBox>()
        eventLogController = eventLogLoader.getController()
        eventLogController.bind(bottomEventLogTab)

        rightEditorTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, editorPane)
        rightViewerTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, viewerPane)
        rightMainTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, mainPanes)

        leftFilesTab.createSelectTabListener(Properties.selectedLeftTab, leftTabContents, leftTab)

        bottomFramesTab.createSelectTabListener(Properties.selectedBottomTab, bottomTabContents, timeLineBox)
        bottomEventLogTab.createSelectTabListener(Properties.selectedBottomTab, bottomTabContents, eventLogView)

        configureScene()

        configureCenterPane()
        configureBottomTab()
        configureLeftPane()
        configureRightPane()

        configurePlayControls()

        val wasLocked = Properties.lockScene.get()
        Properties.lockScene.set(true)
        when (Properties.selectedScene.get()) {
            viewerController.name -> viewerController.selectThisContext()
            editorController.name -> editorController.selectThisContext()
            else -> selectThisContext()
        }
        when (Properties.selectedRightTab.get()) {
            rightEditorTab.id -> rightEditorTab.selectedProperty().set(true)
            rightViewerTab.id -> rightViewerTab.selectedProperty().set(true)
            rightMainTab.id -> rightMainTab.selectedProperty().set(true)
            "null" -> {
                rightEditorTab.isSelected = false
                rightViewerTab.isSelected = false
                rightMainTab.isSelected = false
            }
        }
        when (Properties.selectedLeftTab.get()) {
            leftFilesTab.id -> leftFilesTab.selectedProperty().set(true)
            "null" -> leftFilesTab.isSelected = false
        }
        when (Properties.selectedBottomTab.get()) {
            bottomFramesTab.id -> bottomFramesTab.selectedProperty().set(true)
            bottomEventLogTab.id -> bottomEventLogTab.selectedProperty().set(true)
            "null" -> {
                bottomFramesTab.isSelected = false
                bottomEventLogTab.isSelected = false
            }
        }
        Properties.lockScene.set(wasLocked)
    }

    private fun configureScene() {
        sceneContextBox.apply {
            items.addAll(
                sceneContext,
                viewerController.sceneContext,
                editorController.sceneContext
            )
            tooltip = Tooltip("Select scene context")
            bind(SubScene3D.contextProperty)
        }
        lockSceneContextButton.apply {
            tooltip = Tooltip("Lock scene context")
            selectedProperty().setAndBind(Properties.lockScene, biDirectional = true)
        }
    }

    /**
     * Contains the 3D scene and navigation controller box.
     */
    private fun configureCenterPane() {

        val subScenePane = AutoScaleSubScenePane(parentWidthProperty = splitPlane.widthProperty())
        subScenePane.subSceneProperty.setAndBind(SubScene3D.subSceneProperty)

        mainPane.center = splitPlane
        splitPlane.items.remove(canvasPlaceHolder)
        splitPlane.items.add(1, subScenePane)
        splitPlane.setDividerPositions(lastLeftDividerPosition, lastRightDividerPosition)

        SplitPane.setResizableWithParent(leftTab, false)
        SplitPane.setResizableWithParent(mainPanes, false)

        subScenePane.leftOverlayGroup.children.add(
            splitPlane
                .createDragSpace(Properties.centerDivider1Position, divider1IndexProperty)
        )
        subScenePane.rightOverlayGroup.children.add(
            splitPlane
                .createDragSpace(Properties.centerDivider2Position, divider2IndexProperty)
        )
    }

    private fun configureBottomTab() {
        bottomTabContents.set(null)
        bottomTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue != null)
                bottomBox.children.remove(oldValue)
            if (newValue != null)
                bottomBox.children.add(0, newValue)
        }
    }

    private fun configureLeftPane() {
        leftTabContents.set(leftTab)
        leftTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue !== newValue) {
                if (newValue == null) {
                    if (splitPlane.items.size > 2) {
                        lastLeftDividerPosition = splitPlane.dividerPositions[0]
                        lastRightDividerPosition = splitPlane.dividerPositions[1]
                    } else {
                        lastLeftDividerPosition = splitPlane.dividerPositions[0]
                    }
                    splitPlane.items.remove(oldValue)
                    splitPlane.setDividerPositions(lastRightDividerPosition)
                    divider1IndexProperty.value = -1
                    divider2IndexProperty.value = if (splitPlane.items.isEmpty()) -1 else 0
                } else {
                    splitPlane.items.remove(oldValue)
                    splitPlane.items.add(0, newValue)
                    val pos = if (splitPlane.items.size > 2) {
                        divider1IndexProperty.value = 0
                        divider2IndexProperty.value = 1
                        doubleArrayOf(lastLeftDividerPosition, lastRightDividerPosition)
                    } else {
                        divider1IndexProperty.value = 0
                        divider2IndexProperty.value = -1
                        doubleArrayOf(lastLeftDividerPosition)
                    }
                    splitPlane.setDividerPositions(*pos)
                }
            }
        }
    }

    private fun configureRightPane() {
        rightTabContents.set(mainPanes)
        rightTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue !== newValue) {
                if (newValue == null) {
                    if (splitPlane.items.size > 2) {
                        lastLeftDividerPosition = splitPlane.dividerPositions[0]
                        lastRightDividerPosition = splitPlane.dividerPositions[1]
                    } else {
                        lastRightDividerPosition = splitPlane.dividerPositions[0]
                    }
                    splitPlane.items.remove(oldValue)
                    splitPlane.setDividerPositions(lastLeftDividerPosition)
                    divider1IndexProperty.value = if (splitPlane.items.isEmpty()) -1 else 0
                    divider2IndexProperty.value = -1
                } else {
                    splitPlane.items.remove(oldValue)
                    splitPlane.items.add(newValue)
                    SplitPane.setResizableWithParent(newValue, false)
                    val pos: DoubleArray = if (splitPlane.items.size > 2) {
                        divider1IndexProperty.value = 0
                        divider2IndexProperty.value = 1
                        doubleArrayOf(lastLeftDividerPosition, lastRightDividerPosition)
                    } else {
                        divider1IndexProperty.value = -1
                        divider2IndexProperty.value = 0
                        doubleArrayOf(lastRightDividerPosition)
                    }
                    splitPlane.setDividerPositions(*pos)
                }
            }
        }
    }

    private fun configurePlayControls() {
        playBtn.setOnAction {
            if (playBtn.isSelected)
                SubScene3D.contextProperty.get().animationPlayer.play()
            else
                SubScene3D.contextProperty.get().animationPlayer.pause()
        }
    }


    fun postCacheLoading() {
        bottomFramesTab.selectedProperty().setAndBind(Properties.showFramesTab, biDirectional = true)
        settingsController.root.expandedProperty().setAndBind(Properties.expandSettings, biDirectional = true)
    }



    @FXML
    fun setCachePath() {

        fun resolveAndMake(rootDir: Path, s: String) = rootDir.resolve(s).apply {
            val file = toFile()
            if (!file.exists())
                file.mkdirs()
        }

        val dialog = CacheChooserDialog()
        dialog.showAndWait().ifPresent { (rootDir, cacheDir) ->
            Properties.osrsCachePath.set(cacheDir)
            Properties.rootPath.set(rootDir)
            Properties.downloadsPath.set(resolveAndMake(rootDir, "downloads"))
            Properties.qodatCachePath.set(resolveAndMake(rootDir,"caches/qodat"))
            Properties.legacyCachePath.set(resolveAndMake(rootDir,"cache/667"))
            Properties.projectFilesPath.set(resolveAndMake(rootDir,"data"))
            Properties.defaultExportsPath.set(resolveAndMake(rootDir,"exports"))
            Properties.viewerCache.set(DispleeCache)
        }
    }

    @FXML
    fun openQodatFolder() {
        Properties.rootPath.get().toFile().apply {
            if (exists())
                Desktop.getDesktop().browseFileDirectory(this)
        }
    }

    @FXML
    fun clearModels() {
    }

}
