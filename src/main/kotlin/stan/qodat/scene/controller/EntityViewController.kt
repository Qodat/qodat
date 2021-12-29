package stan.qodat.scene.controller

import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.Cache
import stan.qodat.cache.CacheAssetLoader
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.scene.SubScene2D
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.control.SplitSceneDividerDragRegion.*
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.entity.*
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.*
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class EntityViewController(name: String) : SceneController(name) {

    @FXML lateinit var root: SplitPane
    @FXML lateinit var tabPane: TabPane
    @FXML lateinit var animationModelsSplitPane: SplitPane
    @FXML lateinit var itemList: ViewNodeListView<Item>
    @FXML lateinit var npcList: ViewNodeListView<NPC>
    @FXML lateinit var objectList: ViewNodeListView<Object>
    @FXML lateinit var interfaceList: ViewNodeListView<InterfaceGroup>
    @FXML lateinit var searchItemField: TextField
    @FXML lateinit var searchNpcField: TextField
    @FXML lateinit var searchObjectField: TextField
    @FXML lateinit var searchInterfaceField: TextField
    @FXML lateinit var sortMethodBox: ComboBox<SortType>

    lateinit var animationController: AnimationController
    lateinit var modelController: ModelController

    enum class SortType {
        NAME,
        ID
    }

    val npcs: ObservableList<NPC> = FXCollections.observableArrayList()
    val items: ObservableList<Item> = FXCollections.observableArrayList()
    val objects: ObservableList<Object> = FXCollections.observableArrayList()
    val interfaces: ObservableList<InterfaceGroup> = FXCollections.observableArrayList()

    private val currentSelectedNpcProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedItemProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedObjectProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedInterfaceProperty = SimpleObjectProperty<ViewNodeProvider>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        VBox.setVgrow(itemList, Priority.ALWAYS)
        VBox.setVgrow(npcList, Priority.ALWAYS)
        VBox.setVgrow(objectList, Priority.ALWAYS)
        VBox.setVgrow(interfaceList, Priority.ALWAYS)

        SplitSceneDividerDragRegion(
            splitPane = root,
            node = tabPane,
            dividerIndex = SimpleIntegerProperty(0),
            positionProperty = Properties.viewerDivider1Position,
            relativeBounds = RelativeBounds(Placement.TOP_RIGHT,
                widthProperty = Bindings.subtract(tabPane.widthProperty(), 130.0), // 130 is roughly
                                                                                        // the size of the 3 tabs
                heightProperty = SimpleDoubleProperty(25.0)
            )
        )

        currentSelectedItemProperty.set(itemList.selectionModel.selectedItem)
        currentSelectedNpcProperty.set(npcList.selectionModel.selectedItem)
        currentSelectedObjectProperty.set(objectList.selectionModel.selectedItem)
        currentSelectedInterfaceProperty.set(interfaceList.selectionModel.selectedItem)

        tabPane.selectionModel.selectedItemProperty().addListener { observableValue, previousTab, newTab ->
            val newNode = getNodeProperty(newTab)
            val previousNode = getNodeProperty(previousTab)
            if (previousNode.isNotNull.get())
                onUnselectedEvent.handle(ViewNodeListView.UnselectedEvent(previousNode.get(),
                    hasNewValueOfSameType = false,
                    causedByTabSwitch = true
                ))
            if (newNode.isNotNull.get())
                onSelectedEvent.handle(ViewNodeListView.SelectedEvent(newNode.get()))
        }

        try {

            val animationLoader = FXMLLoader(Qodat::class.java.getResource("animation.fxml"))
            val animationView = animationLoader.load<VBox>()
            animationView.styleClass.add("border-right")
            animationController = animationLoader.getController()

            val modelLoader = FXMLLoader(Qodat::class.java.getResource("model.fxml"))
            val modelView = modelLoader.load<VBox>()
//            modelView.styleClass.add("border-left")
            modelController = modelLoader.getController()
            modelController.modelListView.enableDragAndDrop(
                toFile = {
                    QodatCache.encode(this).file
                },
                imageProvider = {
                    getSceneNode().snapshot(
                        SnapshotParameters().apply { fill = Color.TRANSPARENT },
                        null
                    )
                },
                supportedExtensions = Model.supportedExtensions
            )

            val containerWithDragSpace = HBox()
            HBox.setHgrow(animationView, Priority.ALWAYS)

            containerWithDragSpace.children.add(animationView)
            animationModelsSplitPane.items.add(containerWithDragSpace)
            animationModelsSplitPane.items.add(modelView)
            val dragSpace = animationModelsSplitPane
                .createDragSpace(
                    Properties.viewerDivider2Position,
                    SimpleIntegerProperty(0),
                    size = 1,
                    styleClass = "dark-drag-space"
                )
            containerWithDragSpace.children.add(dragSpace)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        configureNpcList()
        configureObjectList()
        configureItemList()
        configureInterfacesList()

        sortMethodBox.items.addAll(SortType.values())
        sortMethodBox.selectionModel.selectedItemProperty().onInvalidation {
            (npcList.items as SortedList).comparator = when (get()!!){
                SortType.NAME -> Comparator.comparing {
                    it.getName()
                }
                SortType.ID -> Comparator.comparing {
                    it.definition.name
                }
            }
        }

        cacheProperty().addListener { _, _, newValue ->
            val loader = CacheAssetLoader(newValue, npcs, objects, items, itemList, objectList, npcList, animationController)
            loader.loadAnimations()
            loader.loadNpcs()
            loader.loadObjects()
            loader.loadItems()
            interfaces.setAll(newValue.getRootInterfaces().map {
                InterfaceGroup(newValue, it.key, it.value)
            })
        }
    }

    private fun getNodeProperty(previousTab: Tab): ObjectProperty<ViewNodeProvider> = when (previousTab.text) {
        "NPC" -> currentSelectedNpcProperty
        "Object" -> currentSelectedObjectProperty
        "Item" -> currentSelectedItemProperty
        "Interfaces" -> currentSelectedInterfaceProperty
        else -> throw Exception("Unsupported tab name ${previousTab.text}")
    }

    protected abstract fun cacheProperty() : ObjectProperty<Cache>

    protected val cache: Cache
        get() = cacheProperty().get()

    override fun onSwitch(other: SceneController) {
        if (other is MainController) {
//            for (model in sceneContext.getModels())
//                other.sceneContext.addNode(model)
        }
        if (other is EditorController) {
            // TODO
        }
    }

    override fun getViewNode() : SplitPane = root

    private val onUnselectedEvent = EventHandler<ViewNodeListView.UnselectedEvent> { event ->
        val node = event.viewNodeProvider

        if (node is SceneNodeProvider)
            sceneContext.removeNode(node)

        if (node is Entity<*>) {
            if (!event.hasNewValueOfSameType) {
                node.property().set("")
                modelController.models.clear()
            }
            if (node is AnimatedEntity<*>) {
                if (!event.hasNewValueOfSameType) {
                    node.property().set("")
                    animationController.animationsListView.items = animationController.filteredAnimations
                    animationController.filteredAnimations.setPredicate { true }
                }
                SubScene3D.animationPlayer.transformerProperty.set(null)
            }
        }

        if (node is Transformable)
            SubScene3D.animationPlayer.transformableList.remove(node)

        if (!event.hasNewValueOfSameType && !event.causedByTabSwitch) {
            when (node) {
                is NPC -> currentSelectedNpcProperty.set(null)
                is Item -> currentSelectedItemProperty.set(null)
                is Object -> currentSelectedObjectProperty.set(null)
            }
        }
    }

    private val onSelectedEvent = EventHandler<ViewNodeListView.SelectedEvent> { event ->

        val newNode = event.newValue

        if (newNode is SceneNodeProvider)
            sceneContext.addNode(newNode)

        if (newNode is Entity<*>) {
            newNode.property().set(newNode.getName())
            modelController.models.setAll(*newNode.getModels())
            if (newNode is AnimatedEntity<*>)
                animationController.animationsListView.items = FXCollections.observableArrayList(*newNode.getAnimations())
        }

        if (newNode is Transformable)
            SubScene3D.animationPlayer.transformableList.add(newNode)

        when(newNode) {
            is NPC -> currentSelectedNpcProperty.set(newNode)
            is Item -> currentSelectedItemProperty.set(newNode)
            is Object -> currentSelectedObjectProperty.set(newNode)
        }
    }

    private fun configureItemList() {
        val filteredItems = FilteredList(items) { true }
        searchItemField.configureSearchFilter(filteredItems)
        val sortedITEMs = SortedList(filteredItems)
        sortedITEMs.comparator = Comparator.comparing { it.getName() }
        itemList.items = sortedITEMs
        itemList.addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        itemList.addEventHandler(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent)
        handleEmptySearchField(searchItemField)
    }
    private fun configureObjectList() {
        val filteredObjects = FilteredList(objects) { true }
        searchObjectField.configureSearchFilter(filteredObjects)
        val sortedObjects = SortedList(filteredObjects)
        sortedObjects.comparator = Comparator.comparing { it.getName() }
        objectList.items = sortedObjects
        objectList.addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        objectList.addEventHandler(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent)
        handleEmptySearchField(searchObjectField)
    }
    private fun configureNpcList() {
        val filteredNPCs = FilteredList(npcs) { true }
        searchNpcField.configureSearchFilter(filteredNPCs)
        val sortedNPCs = SortedList(filteredNPCs)
        sortedNPCs.comparator = Comparator.comparing { it.getName() }
        npcList.items = sortedNPCs
        npcList.addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        npcList.on(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent::handle)
        handleEmptySearchField(searchNpcField)
    }
    private fun configureInterfacesList() {
        val filteredInterfaces = FilteredList(interfaces) { true }
        searchInterfaceField.configureSearchFilter(filteredInterfaces)
        val sortedInterfaces = SortedList(filteredInterfaces, Comparator.comparingInt { it.idProperty.get() })
        interfaceList.items = sortedInterfaces
        interfaceList.addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        interfaceList.on(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent::handle)
    }
    fun handleEmptySearchField(searchField: TextField) {
        searchField.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isEmpty() && oldValue != null && !oldValue.isEmpty()) {
                modelController.filteredModels.setPredicate { true }
                animationController.filteredAnimations.setPredicate { true }
            }
        }
    }
}

private fun <N : Event> Node.on(
    unselectedEventType: EventType<N>,
    action: suspend (N) -> Unit
) {
    val eventActor = GlobalScope.actor<N>(Dispatchers.Main) {
        for (event in channel) action(event) // pass event to action
    }
    // install a listener to offer events to this actor
   addEventHandler(unselectedEventType, eventActor::trySend)
}
