package stan.qodat.scene.controller

import javafx.beans.binding.Bindings
import javafx.beans.property.*
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
import qodat.cache.Cache
import stan.qodat.cache.CacheAssetLoader
import stan.qodat.cache.impl.qodat.QodatCache
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator

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

    lateinit var onEntitySelected: (Entity<*>) -> Unit


    override fun initialize(location: URL?, resources: ResourceBundle?) {

        VBox.setVgrow(itemList, Priority.ALWAYS)
        VBox.setVgrow(npcList, Priority.ALWAYS)
        VBox.setVgrow(objectList, Priority.ALWAYS)
        VBox.setVgrow(interfaceList, Priority.ALWAYS)

        HBox.setHgrow(searchNpcField, Priority.ALWAYS)

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

        configureTabPane()
        configureModelAndAnimationController()

        configureSortComboBox()

        npcList.configure(npcs, searchNpcField)
        itemList.configure(items, searchItemField)
        objectList.configure(objects, searchObjectField)
        interfaceList.configure(interfaces, searchInterfaceField)

        cacheProperty().addListener { _, _, newValue ->

            val counter = AtomicInteger(2)

            CacheAssetLoader(newValue, animationController).run {
                loadAnimations {
                    animationController.animations.setAll(it)
                }
                loadNpcs {
                    npcs.setAll(it)
                    if (counter.decrementAndGet() == 0)
                        loadLastSelectedAnimation()
                    handleLastSelectedEntity(it, npcList)
                }
                loadObjects {
                    objects.setAll(it)
                    if (counter.decrementAndGet() == 0)
                        loadLastSelectedAnimation()
                    handleLastSelectedEntity(it, objectList)
                }
                loadItems {
                    items.setAll(it)
                    handleLastSelectedEntity(it, itemList)
                }
                interfaces.setAll(newValue.getRootInterfaces().map {
                    InterfaceGroup(newValue, it.key, it.value)
                })
                interfaceList.selectionModel.select(interfaces.lastSelectedEntity(Properties.selectedInterfaceName))
            }
        }
    }

    private fun loadLastSelectedAnimation() {
        val animationToSelect = animationController.animations.lastSelectedEntity(Properties.selectedAnimationName)
        animationController.animationsListView.selectionModel.select(animationToSelect)
    }

    private inline fun<reified T : Entity<*>> handleLastSelectedEntity(
        it: List<T>,
        view: ViewNodeListView<T>,
    ) {
        val selectedTabNodeProperty = tabPane.selectionModel.selectedItem?.let { getNodeProperty(it) }
        val selectedEntityNameProperty = when (T::class) {
            NPC::class -> Properties.selectedNpcName
            Item::class -> Properties.selectedItemName
            Object::class -> Properties.selectedObjectName
            else -> throw Exception("Unsupported entity type ${T::class}")
        }
        val selectedEntityProperty = when (T::class) {
            NPC::class -> currentSelectedNpcProperty
            Item::class -> currentSelectedItemProperty
            Object::class -> currentSelectedObjectProperty
            else -> throw Exception("Unsupported entity type ${T::class}")
        }
        val entity = it.lastSelectedEntity(selectedEntityNameProperty)
        if (selectedTabNodeProperty == selectedEntityProperty)
            view.selectionModel.select(entity)
        else
            selectedEntityProperty.set(entity)
    }

    private fun<T : Searchable> List<T>.lastSelectedEntity(stringProperty: StringProperty) : T? {
        val lastSelectedName = stringProperty.get()?:""
        return if (lastSelectedName.isBlank())
            null
        else
            find { lastSelectedName == it.getName() }
    }

    private fun configureSortComboBox() {
        sortMethodBox.items.addAll(SortType.values())
        sortMethodBox.selectionModel.selectedItemProperty().onInvalidation {
            (npcList.items as SortedList).comparator = when (get()!!) {
                SortType.NAME -> Comparator.comparing {
                    it.getName()
                }
                SortType.ID -> Comparator.comparing {
                    it.definition.getOptionalId().orElse(0)
                }
            }
        }
    }

    private fun configureModelAndAnimationController() {
        try {

            val animationLoader = FXMLLoader(Qodat::class.java.getResource("animation.fxml"))
            val animationView = animationLoader.load<VBox>()
            animationView.styleClass.add("border-right")
            animationController = animationLoader.getController()
            animationController.animationsListView.onItemSelected { old, new ->
                if (new == null && old != null) {
                    Properties.selectedAnimationName.set("")
                    sceneContext.animationPlayer.transformerProperty.set(null)

                } else if(new != null) {
                    Properties.selectedAnimationName.set(new.getName())
                    sceneContext.animationPlayer.transformerProperty.set(new)
                    (Properties.selectedEntity.get() as? AnimatedEntity<*>)?.selectedAnimation?.set(new)
                }
            }
            Properties.selectedEntity.addListener { _, oldEntity, newEntity ->
                if (newEntity is AnimatedEntity)
                    animationController.animations.setAll(*newEntity.getAnimations())
            }
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
    }

    private fun configureTabPane() {
        val lastSelectedTab = tabPane.tabs.find { it.text == Properties.selectedViewerTab.get() }
        tabPane.selectionModel.select(lastSelectedTab)
        tabPane.selectionModel.selectedItemProperty().addListener { _, previousTab, newTab ->
            Properties.selectedViewerTab.set(newTab?.text)
            val newNode = getNodeProperty(newTab)
            val previousNode = getNodeProperty(previousTab)
            if (previousNode.isNotNull.get())
                onUnselectedEvent.handle(
                    ViewNodeListView.UnselectedEvent(
                        previousNode.get(),
                        hasNewValueOfSameType = false,
                        causedByTabSwitch = true
                    )
                )
            if (newNode.isNotNull.get()) {
                val selectedNode = newNode.get()
//                onSelectedEvent.handle(ViewNodeListView.SelectedEvent(selectedNode))
                when(selectedNode) {
                    is NPC -> npcList.run {
                        selectionModel.clearSelection()
                        selectionModel.select(selectedNode)
                        scrollTo(selectedNode)
                    }
                    is Item -> itemList.run {
                        selectionModel.clearSelection()
                        selectionModel.select(selectedNode)
                        scrollTo(selectedNode)
                    }
                    is Object -> objectList.run {
                        selectionModel.clearSelection()
                        selectionModel.select(selectedNode)
                        scrollTo(selectedNode)
                    }
                    is InterfaceGroup -> interfaceList.run {
                        selectionModel.clearSelection()
                        selectionModel.select(selectedNode)
                        scrollTo(selectedNode)
                    }
                }
            }
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

    override fun onSwitch(next: SceneController) {
        Properties.selectedAnimation.unbindBidirectional(sceneContext.animationPlayer.transformerProperty)
    }

    override fun onSelect(old: SceneController?) {
        Properties.selectedAnimation.setAndBind(sceneContext.animationPlayer.transformerProperty, true)
    }

    override fun getViewNode() : SplitPane = root


    private inline fun<reified T : Searchable> ListView<T>.configure(
        backingList: ObservableList<T>,
        searchField: TextField? = null,
        sortComparator: Comparator<T> = Comparator.comparing { it.getName() },
    ) {
        val filteredList = FilteredList(backingList) { true }
        items = SortedList(filteredList, sortComparator)
        on(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent::handle)
        on(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent::handle)
        if (searchField != null) {
            searchField.configureSearchFilter(filteredList)
            handleEmptySearchField(searchField)
        }
    }

    val onUnselectedEvent = EventHandler<ViewNodeListView.UnselectedEvent> { event ->
        val node = event.viewNodeProvider

        if (node is SceneNodeProvider)
            sceneContext.removeNode(node)

        if (node is Entity<*>) {
            if (!event.hasNewValueOfSameType) {
                modelController.models.clear()
            }
            if (node is AnimatedEntity<*>) {
                if (!event.hasNewValueOfSameType) {
                    animationController.animationsListView.items = animationController.filteredAnimations
                    animationController.filteredAnimations.setPredicate { true }
                }
                sceneContext.animationPlayer.transformerProperty.set(null)
            }
        }

        if (node is Transformable)
            sceneContext.animationPlayer.transformableList.remove(node)

        if (!event.hasNewValueOfSameType && !event.causedByTabSwitch) {
            when (node) {
                is NPC -> currentSelectedNpcProperty.set(null)
                is Item -> currentSelectedItemProperty.set(null)
                is Object -> currentSelectedObjectProperty.set(null)
                is InterfaceGroup -> currentSelectedObjectProperty.set(null)
            }
        }
    }

    private val onSelectedEvent = EventHandler<ViewNodeListView.SelectedEvent> { event ->

        val newNode = event.newValue

        if (newNode is SceneNodeProvider)
            sceneContext.addNode(newNode)

        if (newNode is Entity<*>) {
            newNode.property().set(newNode.getName())
            Properties.selectedEntity.set(newNode)
            modelController.models.setAll(*newNode.getModels())
            if (this::onEntitySelected.isInitialized)
                onEntitySelected(newNode)
        }

        if (newNode is Transformable)
            sceneContext.animationPlayer.transformableList.add(newNode)

        when(newNode) {
            is NPC -> currentSelectedNpcProperty.set(newNode)
            is Item -> currentSelectedItemProperty.set(newNode)
            is Object -> currentSelectedObjectProperty.set(newNode)
            is InterfaceGroup -> currentSelectedInterfaceProperty.set(newNode)
        }
    }

    private fun handleEmptySearchField(searchField: TextField) {
        searchField.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isEmpty() && oldValue != null && oldValue.isNotEmpty()) {
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
