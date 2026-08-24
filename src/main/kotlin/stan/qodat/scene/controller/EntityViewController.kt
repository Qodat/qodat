package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.withContext
import stan.qodat.scene.EntitySelectionCoordinator
import qodat.cache.Cache
import qodat.cache.CacheEventListener
import qodat.cache.definition.EntityDefinition
import qodat.cache.event.CacheReloadEvent
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.CacheAssetLoader
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.task.BackgroundTasks
import stan.qodat.scene.SubScene3D
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.scene.control.SplitSceneDividerDragRegion
import stan.qodat.scene.control.SplitSceneDividerDragRegion.Placement
import stan.qodat.scene.control.SplitSceneDividerDragRegion.RelativeBounds
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.runescape.entity.*
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.presentation.PlanarView
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.scene.state.EntityViewState
import stan.qodat.scene.state.NamedIdentity
import stan.qodat.scene.state.ViewStateRestorable
import stan.qodat.scene.state.findByIdentity
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
abstract class EntityViewController(name: String) : SceneController(name), ViewStateRestorable<EntityViewState> {

    @FXML
    lateinit var root: SplitPane

    @FXML
    lateinit var tabPane: TabPane

    @FXML
    lateinit var animationModelsSplitPane: SplitPane

    @FXML
    lateinit var npcList: ViewNodeListView<NPC>

    @FXML
    lateinit var itemList: ViewNodeListView<Item>

    @FXML
    lateinit var objectList: ViewNodeListView<Object>

    @FXML
    lateinit var spritesList: ViewNodeListView<Sprite>

    @FXML
    lateinit var spotAnimList: ViewNodeListView<SpotAnimation>

    @FXML
    lateinit var interfaceList: ViewNodeListView<InterfaceGroup>

    @FXML
    lateinit var sortNpcBox: ComboBox<SortType>

    @FXML
    lateinit var sortItemBox: ComboBox<SortType>

    @FXML
    lateinit var sortObjectBox: ComboBox<SortType>

    @FXML
    lateinit var sortSpotAnimBox: ComboBox<SortType>

    @FXML
    lateinit var sortSpritesBox: ComboBox<SortType>

    enum class SortType {
        NAME,
        ID
    }

    @FXML
    lateinit var searchNpcField: TextField

    @FXML
    lateinit var searchItemField: TextField

    @FXML
    lateinit var searchObjectField: TextField

    @FXML
    lateinit var searchSpritesField: TextField

    @FXML
    lateinit var searchSpotAnimField: TextField

    @FXML
    lateinit var searchInterfaceField: TextField

    lateinit var animationController: AnimationController
    lateinit var modelController: ModelController
    lateinit var materialController: MaterialController

    val npcs: ObservableList<NPC> = FXCollections.observableArrayList()
    val items: ObservableList<Item> = FXCollections.observableArrayList()
    val objects: ObservableList<Object> = FXCollections.observableArrayList()
    val sprites: ObservableList<Sprite> = FXCollections.observableArrayList()
    val spotAnims: ObservableList<SpotAnimation> = FXCollections.observableArrayList()
    val interfaces: ObservableList<InterfaceGroup> = FXCollections.observableArrayList()

    private val currentSelectedNpcProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedItemProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedObjectProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedSpriteProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedSpotAnimProperty = SimpleObjectProperty<ViewNodeProvider>()
    private val currentSelectedInterfaceProperty = SimpleObjectProperty<ViewNodeProvider>()

    lateinit var onEntitySelected: (Entity<*>) -> Unit

    private var pendingViewState: EntityViewState? = null
    private var loadJob: Job? = null
    private val selectionCoordinator = EntitySelectionCoordinator(sceneContext).apply {
        onPreview = { node, gen -> previewSelection(node, gen) }
        onSettled = { node, gen -> settleSelection(node, gen) }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {


        VBox.setVgrow(npcList, Priority.ALWAYS)
        VBox.setVgrow(itemList, Priority.ALWAYS)
        VBox.setVgrow(objectList, Priority.ALWAYS)
        VBox.setVgrow(spritesList, Priority.ALWAYS)
        VBox.setVgrow(spotAnimList, Priority.ALWAYS)
        VBox.setVgrow(interfaceList, Priority.ALWAYS)

        HBox.setHgrow(searchNpcField, Priority.ALWAYS)
        HBox.setHgrow(searchItemField, Priority.ALWAYS)
        HBox.setHgrow(searchObjectField, Priority.ALWAYS)
        HBox.setHgrow(searchSpritesField, Priority.ALWAYS)
        HBox.setHgrow(searchSpotAnimField, Priority.ALWAYS)
        HBox.setHgrow(searchInterfaceField, Priority.ALWAYS)

        SplitSceneDividerDragRegion(
            splitPane = root,
            node = tabPane,
            dividerIndex = SimpleIntegerProperty(0),
            positionProperty = Properties.viewerDivider1Position,
            relativeBounds = RelativeBounds(
                Placement.TOP_RIGHT,
                widthProperty = Bindings.subtract(tabPane.widthProperty(), 130.0), // 130 is roughly
                // the size of the 3 tabs
                heightProperty = SimpleDoubleProperty(25.0)
            )
        )

        configureTabPane()
        configureTopSplitPane()

        npcList.configure(npcs, searchNpcField)
        itemList.configure(items, searchItemField)
        objectList.configure(objects, searchObjectField)
        spritesList.configure(sprites, searchSpritesField)
        spotAnimList.configure(spotAnims, searchSpotAnimField)
        interfaceList.configure(interfaces, searchInterfaceField, Comparator.comparingInt { it.idProperty.get() })

        configureEntitySortComboBox(sortNpcBox, npcList, Properties.selectedNpcSortType)
        configureEntitySortComboBox(sortItemBox, itemList, Properties.selectedItemSortType)
        configureEntitySortComboBox(sortObjectBox, objectList, Properties.selectedObjectSortType)
        configureSpriteSortComboBox(sortSpritesBox, spritesList, Properties.selectedSpriteSortType)
        configureEntitySortComboBox(sortSpotAnimBox, spotAnimList, Properties.selectedSpotAnimSortType)

        val cacheListener = CacheEventListener {
            if (it is CacheReloadEvent) {
                val cache = it.cache
                val properties = arrayOf(
                    currentSelectedNpcProperty,
                    currentSelectedItemProperty,
                    currentSelectedObjectProperty,
                    currentSelectedSpriteProperty,
                    currentSelectedSpotAnimProperty,
                    currentSelectedInterfaceProperty
                )
                for (property in properties) {
                    val value = property.get()
                    if (value != null)
                        onUnselectedEvent.handle(ViewNodeListView.UnselectedEvent(value, false, false))
                }
                loadAssets(cache)
            }
        }
        cacheProperty().addListener { _, oldValue, newValue ->
            loadAssets(newValue)
            oldValue?.removeListener(cacheListener)
            newValue.addListener(cacheListener)
        }
    }

    fun cancelAssetLoad() {
        loadJob?.cancel()
        loadJob = null
    }

    private fun loadAssets(cache: Cache) {
        loadJob?.cancel()
        loadJob = BackgroundTasks.launch(
            addProgressIndicator = true,
            title = "Loading ${cache.name} cache lists"
        ) { progress ->
            if (cache is DispleeCache) {
                withContext(Dispatchers.IO) { cache.ensureReady() }
            }
            CacheAssetLoader(cache, animationController::resolve).loadAll(
                selectedFirst = Properties.selectedViewerTab.get(),
                onProgress = { progress.message(it) },
                onNpcs = {
                    npcs.setAll(it)
                    handleLastSelectedEntity(it, npcList)
                },
                onObjects = {
                    objects.setAll(it)
                    handleLastSelectedEntity(it, objectList)
                },
                onItems = {
                    items.setAll(it)
                    handleLastSelectedEntity(it, itemList)
                },
                onSpotAnims = {
                    spotAnims.setAll(it)
                    handleLastSelectedEntity(it, spotAnimList)
                },
                onSprites = {
                    try {
                        sprites.setAll(it)
                        restoreSpriteSelection(it)
                    } catch (e: Exception) {
                        Qodat.logException("Failed to load sprites", e)
                    }
                },
                onInterfaces = {
                    try {
                        interfaces.setAll(it)
                        restoreInterfaceSelection(it)
                    } catch (e: Exception) {
                        Qodat.logException("Failed to load interfaces", e)
                    }
                },
                onAnimations = { animationList ->
                    animationController.clearAnimationCache()
                    animationController.animationsListView.selectionModel.clearSelection()
                    animationController.indexCatalog(animationList)
                    applySelectedEntityAnimations()
                },
            )
            withContext(Dispatchers.JavaFx) {
                restoreAfterListsLoaded()
            }
        }
    }

    private fun restoreAfterListsLoaded() {
        applySelectedEntityAnimations()
        val pending = pendingViewState
        val identity = pending?.selectedAnimation
            ?: NamedIdentity(
                id = Properties.selectedAnimationId.get(),
                name = Properties.selectedAnimationName.get()
            )
        animationController.restoreSelectedIdentity(identity)
        scrollActiveEntityIntoView()
        if (pending != null) {
            sceneContext.animationPlayer.restorePlayback(
                pending.animationPlaying,
                pending.animationFrameIndex
            )
            if (SubScene3D.contextProperty.get() == sceneContext)
                Qodat.mainController.playBtn.isSelected = pending.animationPlaying
            pendingViewState = null
        }
    }

    private fun applySelectedEntityAnimations() {
        if (!::animationController.isInitialized)
            return
        val entity = tabPane.selectionModel.selectedItem
            ?.let { getNodeProperty(it).get() } as? AnimatedEntity<*>
        if (entity != null)
            animationController.showForEntity(entity)
        else
            animationController.showCatalog()
    }

    private fun scrollActiveEntityIntoView() {
        when (val selected = tabPane.selectionModel.selectedItem?.let { getNodeProperty(it).get() }) {
            is NPC -> npcList.scrollToAfterLayout(selected)
            is Item -> itemList.scrollToAfterLayout(selected)
            is Object -> objectList.scrollToAfterLayout(selected)
            is SpotAnimation -> spotAnimList.scrollToAfterLayout(selected)
            is Sprite -> spritesList.scrollToAfterLayout(selected)
            is InterfaceGroup -> interfaceList.scrollToAfterLayout(selected)
        }
    }

    override fun snapshotViewState(): EntityViewState {
        val player = sceneContext.animationPlayer
        return EntityViewState(
            selectedTab = tabPane.selectionModel.selectedItem?.text,
            searches = mapOf(
                "npc" to searchNpcField.text.orEmpty(),
                "item" to searchItemField.text.orEmpty(),
                "object" to searchObjectField.text.orEmpty(),
                "sprite" to searchSpritesField.text.orEmpty(),
                "spotAnim" to searchSpotAnimField.text.orEmpty(),
                "interface" to searchInterfaceField.text.orEmpty()
            ),
            selections = mapOf(
                "npc" to identityOf(npcList.selectionModel.selectedItem ?: currentSelectedNpcProperty.get()),
                "item" to identityOf(itemList.selectionModel.selectedItem ?: currentSelectedItemProperty.get()),
                "object" to identityOf(objectList.selectionModel.selectedItem ?: currentSelectedObjectProperty.get()),
                "sprite" to identityOf(spritesList.selectionModel.selectedItem ?: currentSelectedSpriteProperty.get()),
                "spotAnim" to identityOf(spotAnimList.selectionModel.selectedItem ?: currentSelectedSpotAnimProperty.get()),
                "interface" to identityOf(interfaceList.selectionModel.selectedItem ?: currentSelectedInterfaceProperty.get())
            ),
            animationSearch = if (::animationController.isInitialized) animationController.snapshotSearchText() else "",
            selectedAnimation = if (::animationController.isInitialized) animationController.snapshotSelectedIdentity() else null,
            modelSearch = if (::modelController.isInitialized) modelController.snapshotSearchText() else "",
            selectedModelName = if (::modelController.isInitialized) modelController.snapshotSelectedName() else null,
            materialSearch = if (::materialController.isInitialized) materialController.snapshotSearchText() else "",
            animationPlaying = player.isPlaying(),
            animationFrameIndex = player.frameIndexProperty.get()
        )
    }

    override fun restoreViewState(state: EntityViewState) {
        pendingViewState = state
        searchNpcField.text = state.searches["npc"].orEmpty()
        searchItemField.text = state.searches["item"].orEmpty()
        searchObjectField.text = state.searches["object"].orEmpty()
        searchSpritesField.text = state.searches["sprite"].orEmpty()
        searchSpotAnimField.text = state.searches["spotAnim"].orEmpty()
        searchInterfaceField.text = state.searches["interface"].orEmpty()
        animationController.restoreSearchText(state.animationSearch)
        if (::modelController.isInitialized)
            modelController.restoreSearchText(state.modelSearch)
        if (::materialController.isInitialized)
            materialController.restoreSearchText(state.materialSearch)
        state.selectedTab?.let { tabName ->
            tabPane.tabs.find { it.text == tabName }?.let { tabPane.selectionModel.select(it) }
        }
        applyPersistedIdentity(state.selections["npc"], Properties.selectedNpcName, Properties.selectedNpcId)
        applyPersistedIdentity(state.selections["item"], Properties.selectedItemName, Properties.selectedItemId)
        applyPersistedIdentity(state.selections["object"], Properties.selectedObjectName, Properties.selectedObjectId)
        applyPersistedIdentity(state.selections["sprite"], Properties.selectedSpriteName, Properties.selectedSpriteId)
        applyPersistedIdentity(state.selections["spotAnim"], Properties.selectedSpotAnimName, Properties.selectedSpotAnimId)
        applyPersistedIdentity(state.selections["interface"], Properties.selectedInterfaceName, Properties.selectedInterfaceId)
        applyPersistedIdentity(state.selectedAnimation, Properties.selectedAnimationName, Properties.selectedAnimationId)
    }

    private fun persistEntityIdentity(node: ViewNodeProvider) {
        when (node) {
            is NPC -> {
                Properties.selectedNpcName.set(node.getName())
                Properties.selectedNpcId.set(entityId(node))
            }
            is Item -> {
                Properties.selectedItemName.set(node.getName())
                Properties.selectedItemId.set(entityId(node))
            }
            is Object -> {
                Properties.selectedObjectName.set(node.getName())
                Properties.selectedObjectId.set(entityId(node))
            }
            is SpotAnimation -> {
                Properties.selectedSpotAnimName.set(node.getName())
                Properties.selectedSpotAnimId.set(entityId(node))
            }
            is Sprite -> {
                Properties.selectedSpriteName.set(node.getName())
                Properties.selectedSpriteId.set("${node.definition.id}:${node.definition.frame}")
            }
            is InterfaceGroup -> {
                Properties.selectedInterfaceName.set(node.getName())
                Properties.selectedInterfaceId.set(node.idProperty.get().toString())
            }
            is Entity<*> -> node.property().set(node.getName())
        }
    }

    private fun entityId(entity: Entity<*>): String =
        entity.definition.getOptionalId().orElse(-1).takeIf { it >= 0 }?.toString().orEmpty()

    private fun identityOf(node: ViewNodeProvider?): NamedIdentity? = when (node) {
        is Entity<*> -> NamedIdentity(
            id = node.definition.getOptionalId().orElse(-1).takeIf { it >= 0 }?.toString(),
            name = node.getName()
        )
        is Sprite -> NamedIdentity(
            id = "${node.definition.id}:${node.definition.frame}",
            name = node.getName()
        )
        is InterfaceGroup -> NamedIdentity(
            id = node.idProperty.get().toString(),
            name = node.getName()
        )
        else -> null
    }

    private fun applyPersistedIdentity(
        identity: NamedIdentity?,
        name: StringProperty,
        id: StringProperty,
    ) {
        identity ?: return
        identity.name?.let { name.set(it) }
        identity.id?.let { id.set(it) }
    }

    private inline fun <reified T : Entity<*>> handleLastSelectedEntity(
        it: List<T>,
        view: ViewNodeListView<T>,
    ) {
        val selectedTabNodeProperty = tabPane.selectionModel.selectedItem?.let { getNodeProperty(it) }
        val selectedEntityNameProperty = when (T::class) {
            NPC::class -> Properties.selectedNpcName
            Item::class -> Properties.selectedItemName
            Object::class -> Properties.selectedObjectName
            Sprite::class -> Properties.selectedSpriteName
            SpotAnimation::class -> Properties.selectedSpotAnimName
            else -> throw Exception("Unsupported entity type ${T::class}")
        }
        val selectedEntityIdProperty = when (T::class) {
            NPC::class -> Properties.selectedNpcId
            Item::class -> Properties.selectedItemId
            Object::class -> Properties.selectedObjectId
            Sprite::class -> Properties.selectedSpriteId
            SpotAnimation::class -> Properties.selectedSpotAnimId
            else -> throw Exception("Unsupported entity type ${T::class}")
        }
        val selectedEntityProperty = when (T::class) {
            NPC::class -> currentSelectedNpcProperty
            Item::class -> currentSelectedItemProperty
            Object::class -> currentSelectedObjectProperty
            Sprite::class -> currentSelectedSpriteProperty
            SpotAnimation::class -> currentSelectedSpotAnimProperty
            else -> throw Exception("Unsupported entity type ${T::class}")
        }
        val selectionKey = when (T::class) {
            NPC::class -> "npc"
            Item::class -> "item"
            Object::class -> "object"
            SpotAnimation::class -> "spotAnim"
            else -> null
        }
        val identity = selectionKey?.let { pendingViewState?.selections?.get(it) }
            ?: NamedIdentity(
                id = selectedEntityIdProperty.get(),
                name = selectedEntityNameProperty.get()
            )
        val entity = it.findByIdentity(identity) { entity ->
            entity.definition.getOptionalId().orElse(-1).takeIf { id -> id >= 0 }?.toString()
        }
        if (selectedTabNodeProperty == selectedEntityProperty)
            view.selectAndScrollTo(entity)
        else
            selectedEntityProperty.set(entity)
    }

    private fun restoreSpriteSelection(loaded: List<Sprite>) {
        val identity = pendingViewState?.selections?.get("sprite")
            ?: NamedIdentity(
                id = Properties.selectedSpriteId.get(),
                name = Properties.selectedSpriteName.get()
            )
        val match = loaded.findByIdentity(identity) { "${it.definition.id}:${it.definition.frame}" }
        val selectedTabNodeProperty = tabPane.selectionModel.selectedItem?.let { getNodeProperty(it) }
        if (selectedTabNodeProperty == currentSelectedSpriteProperty)
            spritesList.selectAndScrollTo(match)
        else
            currentSelectedSpriteProperty.set(match)
    }

    private fun restoreInterfaceSelection(loaded: List<InterfaceGroup>) {
        val identity = pendingViewState?.selections?.get("interface")
            ?: NamedIdentity(
                id = Properties.selectedInterfaceId.get(),
                name = Properties.selectedInterfaceName.get()
            )
        val match = loaded.findByIdentity(identity) { it.idProperty.get().toString() }
        val selectedTabNodeProperty = tabPane.selectionModel.selectedItem?.let { getNodeProperty(it) }
        if (selectedTabNodeProperty == currentSelectedInterfaceProperty)
            interfaceList.selectAndScrollTo(match)
        else
            currentSelectedInterfaceProperty.set(match)
    }

    private fun <D : EntityDefinition, N : Entity<D>> configureEntitySortComboBox(
        box: ComboBox<SortType>,
        list: ViewNodeListView<N>,
        property: ObjectProperty<SortType>,
    ) = configureSortComboBox(box, list, property) {
        when (it) {
            SortType.NAME -> Comparator.comparing(Entity<*>::getName)
            SortType.ID -> Comparator.comparing { it.definition.getOptionalId().orElse(0) }
        }
    }


    private fun configureSpriteSortComboBox(
        box: ComboBox<SortType>,
        list: ViewNodeListView<Sprite>,
        property: ObjectProperty<SortType>,
    ) = configureSortComboBox(box, list, property) {
        when (it) {
            SortType.NAME -> Comparator.comparing(Sprite::getName)
            SortType.ID -> Comparator.comparing { it.definition.id }
        }
    }

    private fun <T : ViewNodeProvider> configureSortComboBox(
        box: ComboBox<SortType>,
        list: ViewNodeListView<T>,
        property: ObjectProperty<SortType>,
        comparator: (SortType) -> Comparator<T>
    ) {
        box.items.addAll(SortType.values())
        box.selectionModel.selectedItemProperty().onInvalidation {
            property.set(get())
            (list.items as SortedList).comparator = comparator(property.get()!!)
        }
        box.selectionModel.select(property.get())
    }


    private fun configureTopSplitPane() {
        try {

            val animationView = loadTopAnimationsView()
            val modelView = loadTopModelsView()
            val materialView = loadTopMaterialsView()

            val containerWithDragSpace1 = HBox()
            val containerWithDragSpace2 = HBox()
            HBox.setHgrow(animationView, Priority.ALWAYS)
            HBox.setHgrow(modelView, Priority.ALWAYS)

            containerWithDragSpace1.children.add(animationView)
            containerWithDragSpace2.children.add(modelView)

            animationModelsSplitPane.items.add(containerWithDragSpace1)
            animationModelsSplitPane.items.add(containerWithDragSpace2)
            animationModelsSplitPane.items.add(materialView)

            containerWithDragSpace1.children.add(
                animationModelsSplitPane
                    .createDragSpace(
                        Properties.viewerDivider2Position,
                        SimpleIntegerProperty(0),
                        size = 1,
                        styleClass = "dark-drag-space"
                    )
            )

            containerWithDragSpace2.children.add(
                animationModelsSplitPane
                    .createDragSpace(
                        Properties.viewerDivider3Position,
                        SimpleIntegerProperty(1),
                        size = 1,
                        styleClass = "dark-drag-space"
                    )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTopModelsView(): VBox? {
        val modelLoader = FXMLLoader(Qodat::class.java.getResource("model.fxml"))
        val modelView = modelLoader.load<VBox>().apply {
            styleClass.add("border-right")
        }
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
        return modelView
    }

    private fun loadTopMaterialsView(): VBox? {
        val materialListViewLoader = FXMLLoader(Qodat::class.java.getResource("materials.fxml"))
        val materialListView = materialListViewLoader.load<VBox>()
        materialController = materialListViewLoader.getController()
        return materialListView
    }

    private fun loadTopAnimationsView(): VBox? {
        val animationLoader = FXMLLoader(Qodat::class.java.getResource("animation.fxml"))
        val animationView = animationLoader.load<VBox>().apply {
            styleClass.add("border-right")
        }
        animationController = animationLoader.getController()
        animationController.animationsListView.onItemSelected { old, new ->
            if (new == null && old != null) {
                Properties.selectedAnimationName.set("")
                Properties.selectedAnimationId.set("")
                sceneContext.animationPlayer.transformerProperty.set(null)

            } else if (new != null) {
                Properties.selectedAnimationName.set(new.getName())
                Properties.selectedAnimationId.set(new.catalogId())
                sceneContext.animationPlayer.transformerProperty.set(new)
                (Properties.selectedEntity.get() as? AnimatedEntity<*>)?.selectedAnimation?.set(new)
            }
        }
        return animationView
    }

    private fun configureTabPane() {

        val lastSelectedTab = tabPane.tabs.find { it.text == Properties.selectedViewerTab.get() }
        tabPane.selectionModel.select(lastSelectedTab)
        tabPane.selectionModel.selectedItemProperty().addListener { _, previousTab, newTab ->

            Properties.selectedViewerTab.set(newTab?.text)
            val newNode = getNodeProperty(newTab)
            val previousNode = getNodeProperty(previousTab)

            if (previousNode.isNotNull.get()) {
                onUnselectedEvent.handle(
                    ViewNodeListView.UnselectedEvent(
                        previousNode.get(),
                        hasNewValueOfSameType = false,
                        causedByTabSwitch = true
                    )
                )
            }

            if (newNode.isNotNull.get()) {
                when (val selectedNode = newNode.get()) {
                    is NPC -> npcList.clearAndScrollToSelect(selectedNode)
                    is Item -> itemList.clearAndScrollToSelect(selectedNode)
                    is Object -> objectList.clearAndScrollToSelect(selectedNode)
                    is SpotAnimation -> spotAnimList.clearAndScrollToSelect(selectedNode)
                    is InterfaceGroup -> interfaceList.clearAndScrollToSelect(selectedNode)
                }
            }
        }
    }

    private fun <T : ViewNodeProvider> ViewNodeListView<T>.clearAndScrollToSelect(selectedNode: T?) {
        selectionModel.clearSelection()
        selectAndScrollTo(selectedNode)
    }

    private fun <T> ListView<T>.selectAndScrollTo(item: T?) {
        if (item == null)
            return
        selectionModel.select(item)
        scrollToAfterLayout(item)
    }

    private fun <T> ListView<T>.scrollToAfterLayout(item: T) {
        Platform.runLater {
            scrollTo(item)
            Platform.runLater { scrollTo(item) }
        }
    }

    private fun getNodeProperty(previousTab: Tab): ObjectProperty<ViewNodeProvider> = when (previousTab.text) {
        "NPC" -> currentSelectedNpcProperty
        "Item" -> currentSelectedItemProperty
        "Object" -> currentSelectedObjectProperty
        "Sprites" -> currentSelectedSpriteProperty
        "SpotAnim" -> currentSelectedSpotAnimProperty
        "Interfaces" -> currentSelectedInterfaceProperty
        else -> throw Exception("Unsupported tab name ${previousTab.text}")
    }

    protected abstract fun cacheProperty(): ObjectProperty<Cache>

    protected val cache: Cache
        get() = cacheProperty().get()

    override fun onSwitch(next: SceneController) {
        Properties.selectedAnimation.unbindBidirectional(sceneContext.animationPlayer.transformerProperty)
    }

    override fun onSelect(old: SceneController?) {
        Properties.selectedAnimation.setAndBind(sceneContext.animationPlayer.transformerProperty, true)
        animationController.animationsListView.refresh()
    }

    override fun getViewNode(): SplitPane = root


    private inline fun <reified T : Searchable> ListView<T>.configure(
        backingList: ObservableList<T>,
        searchField: TextField? = null,
        sortComparator: Comparator<T> = Comparator.comparing { it.getName() },
    ) {
        val filteredList = FilteredList(backingList) { true }
        items = SortedList(filteredList, sortComparator)
        addEventHandler(ViewNodeListView.UNSELECTED_EVENT_TYPE, onUnselectedEvent)
        addEventHandler(ViewNodeListView.SELECTED_EVENT_TYPE, onSelectedEvent)
        if (searchField != null) {
            searchField.configureSearchFilter(filteredList)
            handleEmptySearchField(searchField)
        }
    }

    val onUnselectedEvent = EventHandler<ViewNodeListView.UnselectedEvent> { event ->
        val node = event.viewNodeProvider
        selectionCoordinator.unselect(node)

        if (node is Entity<*>) {
            if (!event.hasNewValueOfSameType) {
                modelController.models.clear()
                materialController.materials.clear()
            }
            if (node is AnimatedEntity<*>) {
                if (!event.hasNewValueOfSameType) {
                    animationController.showCatalog()
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
                is Sprite -> {
                    currentSelectedSpriteProperty.set(null)
                    PlanarView.active.set(false)
                }
                is SpotAnimation -> currentSelectedSpotAnimProperty.set(null)
                is InterfaceGroup -> {
                    currentSelectedInterfaceProperty.set(null)
                    PlanarView.active.set(false)
                }
            }
        }
    }

    private val onSelectedEvent = EventHandler<ViewNodeListView.SelectedEvent> { event ->
        val newNode = event.newValue
        val start = stan.qodat.util.PerfTrace.begin()
        selectionCoordinator.select(newNode)

        persistEntityIdentity(newNode)

        if (newNode is Transformable)
            sceneContext.animationPlayer.transformableList.add(newNode)

        when (newNode) {
            is NPC -> currentSelectedNpcProperty.set(newNode)
            is Item -> currentSelectedItemProperty.set(newNode)
            is Object -> currentSelectedObjectProperty.set(newNode)
            is Sprite -> currentSelectedSpriteProperty.set(newNode)
            is SpotAnimation -> currentSelectedSpotAnimProperty.set(newNode)
            is InterfaceGroup -> currentSelectedInterfaceProperty.set(newNode)
        }
        PlanarView.active.set(newNode is InterfaceGroup || newNode is Sprite)
        stan.qodat.util.PerfTrace.end("select.immediate", start)
    }

    private fun previewSelection(node: ViewNodeProvider, generation: Long) {
        if (!selectionCoordinator.isCurrent(generation))
            return
        if (node !is Entity<*>)
            return
        stan.qodat.util.PerfTrace.span("select.previewLists ${node.getName()}") {
            Properties.selectedEntity.set(node)
            modelController.models.setAll(*node.getModels())
            pendingViewState?.selectedModelName?.let { modelController.restoreSelectedName(it) }
        }
    }

    private fun settleSelection(node: ViewNodeProvider, generation: Long) {
        if (!selectionCoordinator.isCurrent(generation))
            return
        if (node !is Entity<*>)
            return
        stan.qodat.util.PerfTrace.span("select.settle ${node.getName()}") {
            materialController.materials.setAll(*node.getMaterials())
            if (node is AnimatedEntity<*>)
                animationController.showForEntity(node)
            if (this::onEntitySelected.isInitialized)
                onEntitySelected(node)
        }
    }

    private fun handleEmptySearchField(searchField: TextField) {
        searchField.textProperty()
            .addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
                if (newValue != null && newValue.isEmpty() && oldValue != null && oldValue.isNotEmpty()) {
                    modelController.filteredModels.setPredicate { true }
                    animationController.filteredAnimations.setPredicate { true }
                }
            }
    }
}
