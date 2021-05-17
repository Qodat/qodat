package stan.qodat.scene.controller

import com.sun.javafx.application.PlatformImpl
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.event.SelectedTabChangeEvent
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.LockButton
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.layout.AutoScaleSubScenePane
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.bind
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*
import javax.swing.text.html.ImageView

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class MainController : SceneController("main-scene") {

    @FXML lateinit var filesModelListView: ListView<Model>
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
    @FXML lateinit var removeModelMenuItem: MenuItem
    @FXML lateinit var revertModelMenuItem: MenuItem
    @FXML lateinit var exportModelMenuItem: MenuItem
    @FXML lateinit var openInExplorer: MenuItem
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

    private val rightTabContents = SimpleObjectProperty<Node?>()
    private val leftTabContents = SimpleObjectProperty<Node?>()
    private val bottomTabContents = SimpleObjectProperty<Node>()

    lateinit var settingsController: SettingsController
    lateinit var viewerController: ViewerController

    override fun onSwitch(other: SceneController) {
        if (other is ViewerController) {
            println("do")
        }
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
        VBox.setVgrow(filesModelListView, Priority.ALWAYS)

        val settingsLoader = FXMLLoader(Qodat::class.java.getResource("settings.fxml"))
        val settingsPane = settingsLoader.load<TitledPane>()
        settingsController = settingsLoader.getController()
        mainPanes.panes.add(settingsPane)

        val viewerLoader = FXMLLoader(Qodat::class.java.getResource("viewer.fxml"))
        val viewerPane = viewerLoader.load<SplitPane>() // same as viewNode from controller
        viewerController = viewerLoader.getController()
        viewerController.addTabSelectedListener()

        val navigationBox = FXMLLoader.load<VBox>(Qodat::class.java.getResource("navigation.fxml"))
        val timeLineBox = FXMLLoader.load<VBox>(Qodat::class.java.getResource("timeline.fxml"))

        sceneContextBox.items.addAll(
            sceneContext,
            viewerController.sceneContext)
        sceneContextBox.tooltip = Tooltip("Select scene context")
        sceneContextBox.bind(SubScene3D.contextProperty)

        viewerController.selectThisContext()

        lockSceneContextButton.tooltip = Tooltip("Lock scene context")
        lockSceneContextButton.selectedProperty().setAndBind(SubScene3D.lockContextProperty, biDirectional = true)

        rightViewerTab.selectedProperty().addListener(createSelectTabListener(rightTabContents, viewerPane))
        rightMainTab.selectedProperty().addListener(createSelectTabListener(rightTabContents, mainPanes))
        leftFilesTab.selectedProperty().addListener(createSelectTabListener(leftTabContents, leftTab))
        bottomFramesTab.selectedProperty().addListener(createSelectTabListener(bottomTabContents, timeLineBox))

        configureCenterPane(navigationBox)
        configureBottomTab()
        configureLeftPane()
        configureRightPane()

        configurePlayControls()
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

    private fun createSelectTabListener(tabContents: ObjectProperty<Node?>, node: Node): ChangeListener<Boolean> {
        return ChangeListener { _, oldValue, newValue ->

            val otherSelected = tabContents.value != node && tabContents.value != null

            if (!oldValue && newValue)
                tabContents.set(node)
            else if(!otherSelected)
                tabContents.set(null)

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
        val progressLabel = Label()
        val progressBar = ProgressBar()
        stackPane.children.addAll(progressBar, progressLabel)
        if (originalRightNode is HBox)
            stackPane.prefHeightProperty().bind((originalRightNode as HBox).heightProperty())
        progressBar.prefWidthProperty().bind(mainPane.widthProperty())
        for (task in tasks) {
            Qodat.executor.submit {
                PlatformImpl.runAndWait {
                    mainPane.bottom = stackPane
                    progressLabel.textProperty().bind(task.messageProperty())
                    progressBar.progressProperty().bind(task.progressProperty())
                }
                task.run()
            }
        }
        Qodat.executor.submit {
            PlatformImpl.runAndWait {
                mainPane.bottom = originalRightNode
            }
        }
    }
}