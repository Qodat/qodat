package stan.qodat.scene.controller

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.event.SelectedTabChangeEvent
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.layout.AutoScaleSubScenePane
import stan.qodat.util.bind
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class MainController : SceneController("main-scene") {

    @FXML lateinit var modelsContainer: VBox
    @FXML lateinit var sceneTreeView: TreeView<Node>
    @FXML lateinit var leftFilesTab: ToggleButton
    @FXML lateinit var rightMainTab: ToggleButton
    @FXML lateinit var rightPluginsTab: ToggleButton
    @FXML lateinit var rightEditorTab: ToggleButton
    @FXML lateinit var rightViewerTab: ToggleButton
    @FXML lateinit var bottomFramesTab: ToggleButton
    @FXML lateinit var mainPanes: Accordion
    @FXML lateinit var canvasPlaceHolder: Pane
    @FXML lateinit var splitPlane: SplitPane
    @FXML lateinit var mainPane: BorderPane
    @FXML lateinit var infoLabel: Label
    @FXML lateinit var searchModelsList: TextField
    @FXML lateinit var menuBar: MenuBar
    @FXML lateinit var recentPathsMenu: Menu
    @FXML lateinit var leftTab: HBox
    @FXML lateinit var filesWindow: SplitPane
    @FXML lateinit var playControls: HBox
    @FXML lateinit var startBtn: Button
    @FXML lateinit var endBtn: Button
    @FXML lateinit var ffBtn: Button
    @FXML lateinit var rwBtn: Button
    @FXML lateinit var playBtn: ToggleButton
    @FXML lateinit var loopBtn: ToggleButton
    @FXML lateinit var bottomBox: VBox
    @FXML lateinit var sceneHBox: HBox
    @FXML lateinit var sceneLabel: Label
    @FXML lateinit var lockSceneContextButton: ToggleButton
    @FXML lateinit var sceneContextBox: ComboBox<SceneContext>
    @FXML lateinit var progressSpace: HBox

    private val rightTabContents = SimpleObjectProperty<Node?>()
    private val leftTabContents = SimpleObjectProperty<Node?>()
    private val bottomTabContents = SimpleObjectProperty<Node>()

    lateinit var settingsController: SettingsController
    lateinit var viewerController: ViewerController
    lateinit var editorController: EditorController

    override fun onSwitch(other: SceneController) {

    }

    override fun getViewNode() = mainPanes

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        SplitSceneDividerDragRegion(filesWindow, sceneLabel, 0, Properties.sceneDividerPosition)

        leftTab.minWidth = 1.0
        mainPanes.minWidth = 1.0
        originalRightNode = mainPane.bottom

        sceneTreeView.root = TreeItem(Label("Nodes"))
        sceneTreeView.root.isExpanded = true

        VBox.setVgrow(sceneTreeView, Priority.ALWAYS)
        VBox.setVgrow(modelsContainer, Priority.ALWAYS)

        val mainModelsLoader = FXMLLoader(Qodat::class.java.getResource("model.fxml"))
        val mainModelsView = mainModelsLoader.load<SplitPane>()
        mainModelsLoader.getController<ModelController>().apply {
            bind(sceneContext)
            enableDragAndDrop()
            syncWith(Properties.mainModelFilesPath)
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

        val navigationBox = FXMLLoader.load<VBox>(Qodat::class.java.getResource("navigation.fxml"))
        val timeLineBox = FXMLLoader.load<VBox>(Qodat::class.java.getResource("timeline.fxml"))

        rightEditorTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, editorPane)
        rightViewerTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, viewerPane)
        rightMainTab.createSelectTabListener(Properties.selectedRightTab, rightTabContents, mainPanes)

        leftFilesTab.createSelectTabListener(Properties.selectedLeftTab, leftTabContents, leftTab)
        bottomFramesTab.createSelectTabListener(Properties.selectedBottomTab, bottomTabContents, timeLineBox)

        configureScene()

        configureCenterPane(navigationBox)
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
        }
        Properties.lockScene.set(wasLocked)
    }

    private fun configureScene() {

        sceneContextBox.items.addAll(
            sceneContext,
            viewerController.sceneContext,
            editorController.sceneContext
        )

        sceneContextBox.tooltip = Tooltip("Select scene context")
        sceneContextBox.bind(SubScene3D.contextProperty)

        lockSceneContextButton.tooltip = Tooltip("Lock scene context")
        lockSceneContextButton.selectedProperty().setAndBind(Properties.lockScene, biDirectional = true)
    }

    fun postCacheLoading() {
        bottomFramesTab.selectedProperty().setAndBind(Properties.showFramesTab, biDirectional = true)
        settingsController.root.expandedProperty().setAndBind(Properties.expandSettings, biDirectional = true)
    }


    /**
     * Contains the 3D scene and navigation controller box.
     */
    private fun configureCenterPane(navigationBox: VBox) {
        SubScene3D.overlayGroup.children.add(navigationBox)
        val subScenePane = AutoScaleSubScenePane(parentWidthProperty = splitPlane.widthProperty())
        subScenePane.overlayMiscGroup.children.add(SubScene3D.overlayGroup)
        subScenePane.subSceneProperty.setAndBind(SubScene3D.subSceneProperty)
        splitPlane.items.remove(canvasPlaceHolder)
        splitPlane.items.add(1, subScenePane)
        splitPlane.setDividerPositions(lastLeftDividerPosition, lastRightDividerPosition)
        SplitPane.setResizableWithParent(leftTab, false)
        SplitPane.setResizableWithParent(mainPanes, false)
        splitPlane.dividers[0].positionProperty().setAndBind(Properties.centerDivider1Position, true)
        splitPlane.dividers[1].positionProperty().setAndBind(Properties.centerDivider2Position, true)
        mainPane.center = splitPlane
    }

    private fun configureBottomTab() {
        bottomTabContents.value = null
        bottomTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue != null)
                bottomBox.children.remove(oldValue)
            if (newValue != null)
                bottomBox.children.add(0, newValue)
        }
    }

    private var lastLeftDividerPosition = 0.25
    private var lastRightDividerPosition = 0.75

    private fun configureLeftPane() {
        leftTabContents.value = leftTab
        leftTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue !== newValue) {
                if (newValue == null) {
                    lastLeftDividerPosition = splitPlane.dividerPositions[0]
                    lastRightDividerPosition =
                        splitPlane.dividerPositions[splitPlane.dividerPositions.size - 1]
                    splitPlane.items.remove(oldValue)
                    splitPlane.setDividerPositions(lastRightDividerPosition)
                } else {
                    splitPlane.items.remove(oldValue)
                    splitPlane.items.add(0, newValue)
                    val pos = if (splitPlane.items.size > 2)
                        doubleArrayOf(lastLeftDividerPosition, lastRightDividerPosition)
                    else
                        doubleArrayOf(lastLeftDividerPosition)
                    splitPlane.setDividerPositions(*pos)
                }
            }
        }
    }

    private var originalRightNode: Node? = null

    private fun configureRightPane() {

        rightTabContents.value = mainPanes
        rightTabContents.addListener { _, oldValue: Node?, newValue: Node? ->
            if (oldValue !== newValue) {
                if (newValue == null) {
                    lastLeftDividerPosition = splitPlane.dividerPositions[0]
                    lastRightDividerPosition =
                        splitPlane.dividerPositions[splitPlane.dividerPositions.size - 1]
                    splitPlane.items.remove(oldValue)
                    splitPlane.setDividerPositions(lastLeftDividerPosition)
                } else {
                    splitPlane.items.remove(oldValue)
                    splitPlane.items.add(newValue)
                    SplitPane.setResizableWithParent(newValue, false)
                    val pos: DoubleArray = if (splitPlane.items.size > 2)
                        doubleArrayOf(lastLeftDividerPosition, lastRightDividerPosition)
                    else
                        doubleArrayOf(lastRightDividerPosition)
                    splitPlane.setDividerPositions(*pos)
                }
            }
        }
    }

    private fun ToggleButton.createSelectTabListener(selectProperty: SimpleStringProperty, tabContents: ObjectProperty<Node?>, node: Node) {
        selectedProperty().addListener(createSelectTabListener(id, selectProperty, tabContents, node))
    }

    private fun createSelectTabListener(id: String, selectProperty: SimpleStringProperty, tabContents: ObjectProperty<Node?>, node: Node): ChangeListener<Boolean> {
        return ChangeListener { _, oldValue, newValue ->

            val otherSelected = tabContents.value != node && tabContents.value != null

            if (!oldValue && newValue)
                tabContents.set(node)
            else if(!otherSelected)
                tabContents.set(null)

            selectProperty.set(id)

            node.fireEvent(
                SelectedTabChangeEvent(
                    selected = newValue,
                    otherSelected = otherSelected))
        }
    }

    private fun configurePlayControls() {
        playBtn.setOnAction {
            if (playBtn.isSelected)
                SubScene3D.animationPlayer.play()
            else
                SubScene3D.animationPlayer.pause()
        }
    }

    @FXML
    fun loadModels() {
    }

    @FXML
    fun setLoadPath() {
    }

    @FXML
    fun setSavePath() {
    }

    @FXML
    fun setCachePath() {
        val chooser = DirectoryChooser()

        val currentCachePath = Properties.osrsCachePath.get()
        if (currentCachePath != null)
            chooser.initialDirectory = currentCachePath.toFile()

        chooser.title = "Select a path to load files from"
        try {
            val path = chooser.showDialog(menuBar.scene.window).toPath()
            Properties.osrsCachePath.set(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @FXML
    fun clearModels() {
    }

    @FXML
    fun openModelInExplorer() {
    }

    fun executeBackgroundTasks(vararg tasks: Task<*>) {
        val stackPane = StackPane()
        val progressLabel = Text()
        progressLabel.fill = Color.rgb(100, 100, 100)
        val progressBar = ProgressBar()
        stackPane.children.addAll(progressBar, progressLabel)
        val original = originalRightNode
        if (original is HBox) {
            stackPane.prefHeightProperty().bind(original.heightProperty())
            stackPane.maxHeight = original.height
            progressLabel.minWidth(original.width)
            progressLabel.maxHeight(original.height)
        }

        progressBar.prefWidthProperty().bind(mainPane.widthProperty())

        for (task in tasks) {
            println("Running task ${task.title}")
            task.setOnFailed {
                Platform.runLater {
                    task.exception.printStackTrace()
                    val dialog = Alert(AlertType.ERROR, "Error", ButtonType.OK)
                    dialog.show()
                }
            }
            Qodat.executor.submit {
                PlatformImpl.runAndWait {
                    progressSpace.children.setAll(stackPane)
                    progressLabel.textProperty().unbind()
                    progressLabel.textProperty().bind(task.messageProperty())
                    progressBar.progressProperty().unbind()
                    progressBar.progressProperty().bind(task.progressProperty())
                }
                task.run()

            }

        }
        Qodat.executor.submit {
            PlatformImpl.runAndWait {
                progressSpace.children.clear()
            }
        }
    }
}