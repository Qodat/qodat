package stan.qodat.scene.controller

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.SplitPane
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.EntityDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.entity.*
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.Searchable
import stan.qodat.util.configureSearchFilter
import stan.qodat.util.createNpcAnimsJsonDir
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ViewerController : SceneController("viewer-scene") {

    @FXML lateinit var root: SplitPane
    @FXML lateinit var animationModelsSplitPane: SplitPane
    @FXML lateinit var itemList: ViewNodeListView<Item>
    @FXML lateinit var npcList: ViewNodeListView<NPC>
    @FXML lateinit var objectList: ViewNodeListView<Object>
    @FXML lateinit var searchItemField: TextField
    @FXML lateinit var searchNpcField: TextField
    @FXML lateinit var searchObjectField: TextField

    private val npcs: ObservableList<NPC> = FXCollections.observableArrayList()
    private val items: ObservableList<Item> = FXCollections.observableArrayList()
    private val objectWrappers: ObservableList<Object> = FXCollections.observableArrayList()

    lateinit var animationController: AnimationController
    private lateinit var modelController: ModelController

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        VBox.setVgrow(itemList, Priority.ALWAYS)
        VBox.setVgrow(npcList, Priority.ALWAYS)
        VBox.setVgrow(objectList, Priority.ALWAYS)

        try {
            val animationLoader = FXMLLoader(Qodat::class.java.getResource("animation.fxml"))
            animationModelsSplitPane.items.add(animationLoader.load())
            animationController = animationLoader.getController()

            val modelLoader = FXMLLoader(Qodat::class.java.getResource("model.fxml"))
            animationModelsSplitPane.items.add(modelLoader.load())
            modelController = modelLoader.getController()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        configureNpcList()
        configureItemList()

        Properties.cache.addListener { _, _, newValue ->
            loadNpcs(newValue)
            loadItems(newValue)
        }
    }

    private fun loadItems(newValue: Cache) {
        Qodat.mainController.executeBackgroundTasks(createItemsLoadTask(newValue))
    }

    private fun loadNpcs(newValue: Cache) {
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists()) {
            println("Did not find npc_anims dir, creating...")
            npcAnimsDir.mkdir()
            val task = createNpcAnimsJsonDir(OldschoolCacheRuneLite.store, OldschoolCacheRuneLite.npcManager)
            task.setOnSucceeded {
                Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(newValue))
            }
            Qodat.mainController.executeBackgroundTasks(task)
        } else
            Qodat.mainController.executeBackgroundTasks(createNPCLoadTask(newValue))
    }

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

        if (node is AnimatedEntity<*>) {
            Properties.selectedNpcName.set("")
            animationController.filteredAnimations.setPredicate { true }
            SubScene3D.animationPlayer.transformerProperty.set(null)
        }

        if (node is Transformable)
            SubScene3D.animationPlayer.transformableList.remove(node)
    }

    private val onSelectedEvent = EventHandler<ViewNodeListView.SelectedEvent> { event ->

        val node = event.viewNodeProvider

        if (node is SceneNodeProvider)
            sceneContext.addNode(node)

        if (node is AnimatedEntity<*>) {
            Properties.selectedNpcName.set(node.getName())
            val animationNames = node.getAnimations().map { it.getName() }
            animationController.filteredAnimations.setPredicate { animationNames.contains(it.getName()) }
        }

        if (node is Transformable)
            SubScene3D.animationPlayer.transformableList.add(node)
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

    private fun configureNpcList() {
        val filteredNPCs = FilteredList(npcs) { true }
        searchNpcField.configureSearchFilter(filteredNPCs)
        val sortedNPCs = SortedList(filteredNPCs)
        sortedNPCs.comparator = Comparator.comparing { it.getName() }
        npcList.items = sortedNPCs
        npcList.addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        npcList.addEventHandler(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent)
        handleEmptySearchField(searchNpcField)
    }

    private fun handleEmptySearchField(searchField: TextField) {
        searchField.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isEmpty() && oldValue != null && !oldValue.isEmpty()) {
                modelController.filteredModels.setPredicate { true }
                animationController.filteredAnimations.setPredicate { true }
            }
        }
    }

    private fun createNPCLoadTask(cache: Cache) = createLoadTask(cache.getNPCs(), mapper = {NPC(cache, this)}) {
        val npcToSelect = lastSelectedEntity(Properties.selectedNpcName)
        val animationToSelect = animationController.animations.lastSelectedEntity(Properties.selectedNpcName)
        Platform.runLater {
            this@ViewerController.npcs.setAll(this)
            Qodat.mainController.postCacheLoading()
            if (npcToSelect != null)
                npcList.selectionModel.select(npcToSelect)
            if (animationToSelect != null)
                animationController.animationsListView.selectionModel.select(animationToSelect)
        }
    }

    private fun createItemsLoadTask(cache: Cache) = createLoadTask(
        definitions = cache.getItems(),
        mapper = { Item(cache, this) })
    {
        val itemToSelect = lastSelectedEntity(Properties.selectedItemName)
        Platform.runLater {
            this@ViewerController.items.setAll(this)
            Qodat.mainController.postCacheLoading()
            if (itemToSelect != null)
                itemList.selectionModel.select(itemToSelect)
        }
    }

    private fun<T : Searchable> List<T>.lastSelectedEntity(stringProperty: StringProperty) : T? {
        val lastSelectedName = stringProperty.get()?:""
        return if (lastSelectedName.isBlank())
            null
        else
            find { lastSelectedName == it.getName() }
    }

    private inline fun<D : EntityDefinition, reified T : Entity<D>> createLoadTask(
        definitions: Array<D>,
        crossinline mapper: D.() -> T,
        crossinline onLoaded: List<T>.() -> Unit
    ) : Task<Unit> {
        val progressCounter = AtomicInteger()
        val total = definitions.size
        val updateFrequency = total / 500
        val name = T::class.simpleName
        return object : Task<Unit>() {
            override fun call() {
                val values = Stream.of(*definitions).parallel().map {
                    val count = progressCounter.incrementAndGet()
                    if (count % updateFrequency == 0) {
                        PlatformImpl.runLater {
                            updateProgress(count.toLong(), total.toLong())
                            updateMessage("Loading $name ($count / $total)")
                        }
                    }
                    if (it.modelIds.isNotEmpty())
                        mapper(it)
                    else
                        null
                }.toArray { arrayOfNulls<T>(it) }.filterNotNull()
                onLoaded(values)
            }
        }
    }
}