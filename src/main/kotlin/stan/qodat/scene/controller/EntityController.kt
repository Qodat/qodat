package stan.qodat.scene.controller

import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
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
import stan.qodat.cache.CacheAssetLoader
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.entity.Object
import stan.qodat.scene.transform.Transformable
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.configureSearchFilter
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
abstract class EntityController(name: String) : SceneController(name) {

    @FXML lateinit var root: SplitPane
    @FXML lateinit var animationModelsSplitPane: SplitPane
    @FXML lateinit var itemList: ViewNodeListView<Item>
    @FXML lateinit var npcList: ViewNodeListView<NPC>
    @FXML lateinit var objectList: ViewNodeListView<Object>
    @FXML lateinit var searchItemField: TextField
    @FXML lateinit var searchNpcField: TextField
    @FXML lateinit var searchObjectField: TextField

    val npcs: ObservableList<NPC> = FXCollections.observableArrayList()
    protected val items: ObservableList<Item> = FXCollections.observableArrayList()
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

        cacheProperty().addListener { _, _, newValue ->
            val loader = CacheAssetLoader(newValue, npcs, items, itemList, npcList, animationController)
            loader.loadAnimations()
            loader.loadNpcs()
            loader.loadItems()
        }
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

        if (node is AnimatedEntity<*>) {
            if (!event.hasNewValue) {
                Properties.selectedNpcName.set("")
                animationController.animationsListView.items = animationController.filteredAnimations
                animationController.filteredAnimations.setPredicate { true }
            }
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
            animationController.animationsListView.items = FXCollections.observableArrayList(*node.getAnimations())
            modelController.models.setAll(*node.getModels())
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
}