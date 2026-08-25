package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.withContext
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.WindowEvent
import qodat.cache.event.CacheReloadEvent
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.MacAppMenu
import stan.qodat.scene.control.MainMenuBar
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.control.dialog.AboutDialog
import stan.qodat.scene.control.dialog.CacheChooserDialog
import stan.qodat.scene.control.dialog.FxDialogs
import stan.qodat.scene.control.dialog.SettingsDialog
import stan.qodat.scene.control.tree.RootSceneTreeItem
import stan.qodat.scene.layout.AutoScaleSubScenePane
import stan.qodat.scene.presentation.PlanarView
import stan.qodat.scene.state.AppViewState
import stan.qodat.scene.state.ViewStateRestorable
import stan.qodat.task.BackgroundTasks
import stan.qodat.util.ActionCache
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
class MainController : SceneController("main-scene"), ViewStateRestorable<AppViewState> {

    @FXML
    lateinit var modelsContainer: VBox

    @FXML
    lateinit var sceneTreeView: TreeView<Node>

    @FXML
    lateinit var leftFilesTab: ToggleButton

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
    lateinit var menuBar: MenuBar

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

    lateinit var viewerController: ViewerController
    lateinit var editorController: EditorController
    lateinit var eventLogController: EventLogController

    private val divider1IndexProperty = SimpleIntegerProperty(0)
    private val divider2IndexProperty = SimpleIntegerProperty(1)
    private var lastLeftDividerPosition = 0.25
    private var lastRightDividerPosition = 0.75
    private var reloadingCache = false
    private var rescanningAnimations = false
    private val settingsDialog by lazy { SettingsDialog() }
    private val aboutDialog by lazy { AboutDialog() }

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
        configureMenuBar()

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

        leftFilesTab.createSelectTabListener(Properties.selectedLeftTab, leftTabContents, leftTab)

        bottomFramesTab.createSelectTabListener(Properties.selectedBottomTab, bottomTabContents, timeLineBox)
        bottomEventLogTab.createSelectTabListener(Properties.selectedBottomTab, bottomTabContents, eventLogView)

        configureScene()

        configureCenterPane()
        configureBottomTab()
        configureLeftPane()
        configureRightPane(viewerPane)

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
            "null" -> {
                rightEditorTab.isSelected = false
                rightViewerTab.isSelected = false
            }
            else -> rightViewerTab.selectedProperty().set(true)
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
        val viewToggle = createPlanarViewToggle()
        val sceneStack = StackPane(subScenePane, viewToggle).apply {
            StackPane.setAlignment(viewToggle, Pos.TOP_CENTER)
            StackPane.setMargin(viewToggle, Insets(10.0, 0.0, 0.0, 0.0))
        }

        mainPane.center = splitPlane
        splitPlane.items.remove(canvasPlaceHolder)
        splitPlane.items.add(1, sceneStack)
        splitPlane.setDividerPositions(lastLeftDividerPosition, lastRightDividerPosition)

        SplitPane.setResizableWithParent(leftTab, false)

        subScenePane.leftOverlayGroup.children.add(
            splitPlane
                .createDragSpace(Properties.centerDivider1Position, divider1IndexProperty)
        )
        subScenePane.rightOverlayGroup.children.add(
            splitPlane
                .createDragSpace(Properties.centerDivider2Position, divider2IndexProperty)
        )
    }

    private fun createPlanarViewToggle(): HBox {
        val group = ToggleGroup()
        val twoD = ToggleButton("2D").apply {
            toggleGroup = group
            isSelected = !PlanarView.exploded.get()
            styleClass += "planar-view-toggle"
        }
        val threeD = ToggleButton("3D").apply {
            toggleGroup = group
            isSelected = PlanarView.exploded.get()
            styleClass += "planar-view-toggle"
        }
        threeD.selectedProperty().addListener { _, _, selected ->
            PlanarView.exploded.set(selected)
        }
        PlanarView.exploded.addListener { _, _, exploded ->
            if (exploded) threeD.isSelected = true else twoD.isSelected = true
        }
        return HBox(twoD, threeD).apply {
            styleClass += "planar-view-toggle-bar"
            visibleProperty().bind(PlanarView.active)
            managedProperty().bind(visibleProperty())
            isPickOnBounds = false
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
        }
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

    private fun configureMenuBar() {
        val actions = MainMenuBar.Actions(
            changeCache = ::setCachePath,
            reloadCache = ::reloadCache,
            rescanAnimations = ::rescanAnimations,
            openQodatFolder = ::openQodatFolder,
            openSettings = ::openSettings,
            quit = ::quit,
            undo = ActionCache::undoLast,
            redo = ActionCache::redoLast,
            clearScene = ::clearModels,
            showAbout = ::showAbout
        )
        MainMenuBar.install(menuBar, actions, MainMenuBar.ViewToggles.fromProperties())
        MacAppMenu.install(actions)
    }

    private fun configureRightPane(initialContents: Node) {
        val mainIndex = splitPlane.items.indexOf(mainPanes)
        if (mainIndex >= 0) {
            splitPlane.items[mainIndex] = initialContents
        }
        SplitPane.setResizableWithParent(initialContents, false)
        rightTabContents.set(initialContents)
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
    }



    override fun snapshotViewState() = AppViewState(
        camera = SubScene3D.cameraHandler.snapshotViewState(),
        viewer = viewerController.snapshotViewState(),
        editor = editorController.snapshotViewState(),
        selectedScene = Properties.selectedScene.get(),
        selectedRightTab = Properties.selectedRightTab.get(),
        selectedLeftTab = Properties.selectedLeftTab.get(),
        selectedBottomTab = Properties.selectedBottomTab.get(),
        playButtonSelected = playBtn.isSelected
    )

    override fun restoreViewState(state: AppViewState) {
        viewerController.restoreViewState(state.viewer)
        editorController.restoreViewState(state.editor)
        SubScene3D.cameraHandler.restoreViewState(state.camera)
        restoreSelectedTabs(state)
        restoreSelectedScene(state.selectedScene)
        playBtn.isSelected = state.playButtonSelected
    }

    private fun restoreSelectedTabs(state: AppViewState) {
        when (state.selectedRightTab) {
            rightEditorTab.id -> rightEditorTab.selectedProperty().set(true)
            else -> rightViewerTab.selectedProperty().set(true)
        }
        when (state.selectedLeftTab) {
            leftFilesTab.id -> leftFilesTab.selectedProperty().set(true)
        }
        when (state.selectedBottomTab) {
            bottomFramesTab.id -> bottomFramesTab.selectedProperty().set(true)
            bottomEventLogTab.id -> bottomEventLogTab.selectedProperty().set(true)
        }
    }

    private fun restoreSelectedScene(selectedScene: String?) {
        when (selectedScene) {
            viewerController.name -> viewerController.selectThisContext()
            editorController.name -> editorController.selectThisContext()
            name -> selectThisContext()
        }
    }

    @FXML
    fun reloadCache() {
        if (reloadingCache)
            return
        reloadingCache = true
        viewerController.cancelAssetLoad()
        editorController.cancelAssetLoad()
        val state = snapshotViewState()
        BackgroundTasks.launch(addProgressIndicator = true, title = "Reloading cache") {
            try {
                withContext(Dispatchers.IO) {
                    Properties.viewerCache.get()?.reloadFromSource()
                    Properties.editorCache.get()?.reloadFromSource()
                }
            } catch (e: Exception) {
                Qodat.logException("Failed to reload cache", e)
            }
            withContext(Dispatchers.JavaFx) {
                try {
                    viewerController.restoreViewState(state.viewer)
                    editorController.restoreViewState(state.editor)
                    Properties.viewerCache.get()?.let { it.fire(CacheReloadEvent(it)) }
                    Properties.editorCache.get()?.let { it.fire(CacheReloadEvent(it)) }
                    SubScene3D.cameraHandler.restoreViewState(state.camera)
                    restoreSelectedTabs(state)
                    restoreSelectedScene(state.selectedScene)
                } catch (e: Exception) {
                    Qodat.logException("Failed to restore view after cache reload", e)
                } finally {
                    reloadingCache = false
                }
            }
        }
    }

    @FXML
    fun rescanAnimations() {
        if (rescanningAnimations) return
        val cache = sequenceOf(Properties.viewerCache.get(), Properties.editorCache.get())
            .filterIsInstance<DispleeCache>()
            .firstOrNull()
        if (cache == null) {
            FxDialogs.runAfterCurrentEvent {
                Alert(
                    Alert.AlertType.INFORMATION,
                    "Animation rescan is only available when the Displee OSRS cache is loaded."
                ).showAndWait()
            }
            return
        }
        rescanningAnimations = true
        BackgroundTasks.submit(
            addProgressIndicator = true,
            cache.createAnimationRescanTask { rescanningAnimations = false }
        )
    }

    @FXML
    fun setCachePath() {
        FxDialogs.runAfterCurrentEvent { showCacheChooserAndApply() }
    }

    private fun showCacheChooserAndApply() {
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

    fun openSettings() {
        FxDialogs.runAfterCurrentEvent {
            if (Qodat.isStageInitialized()) {
                settingsDialog.attachTo(Qodat.stage)
            }
            settingsDialog.showAndWait()
        }
    }

    fun showAbout() {
        FxDialogs.runAfterCurrentEvent {
            if (Qodat.isStageInitialized()) {
                aboutDialog.attachTo(Qodat.stage)
            }
            aboutDialog.present()
        }
    }

    fun quit() {
        if (Qodat.isStageInitialized()) {
            Qodat.stage.fireEvent(WindowEvent(Qodat.stage, WindowEvent.WINDOW_CLOSE_REQUEST))
        } else {
            Platform.exit()
        }
    }

    @FXML
    fun clearModels() {
        SubScene3D.contextProperty.get()?.clear()
    }

}
