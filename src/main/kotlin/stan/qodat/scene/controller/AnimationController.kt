package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback
import qodat.cache.definition.AnimatedEntityDefinition
import stan.qodat.Properties
import stan.qodat.javafx.onChange
import stan.qodat.scene.control.AnimationRoleLabel
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.state.NamedIdentity
import stan.qodat.scene.state.findByIdentity
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*

/**
 * Represents a FXML controller for a filterable list of animations.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class AnimationController : Initializable, (AnimatedEntityDefinition) -> Array<Animation> {

    /**
     * A [ListView] containing all [filteredAnimations].
     */
    @FXML lateinit var animationsListView: ListView<Animation>

    /**
     * A [TextField] in which users can filter the [animations][AnimationLegacy] contained in the [animationsListView].
     * This is used to set the predicate of [filteredAnimations].
     */
    @FXML lateinit var searchTextField: TextField

    /**
     * A [FilteredList] of [animations][AnimationLegacy]. This list is backed by [animations].
     */
    lateinit var filteredAnimations: FilteredList<Animation>

    /**
     * The animations currently shown in the list (full cache catalog, or the selected entity's matches).
     */
    val animations: ObservableList<Animation> = FXCollections.observableArrayList()

    private val catalog: ObservableList<Animation> = FXCollections.observableArrayList()
    private val animationMap = FXCollections.observableHashMap<String, Animation>()
    private var roleLabels: Map<String, String> = emptyMap()

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        animations.onChange {
            while(next()) {

                if (wasAdded()) {
                    for (i in from until to){
                        val anim = list[i]
                        animationMap[anim.getName()] = anim
                        anim.definition?.id?.let { animationMap[it] = anim }
                    }
                }
            }
        }
        filteredAnimations = FilteredList(animations) { true }
        animationsListView.apply {
            VBox.setVgrow(this, Priority.ALWAYS)
            cellFactory = Callback<ListView<Animation>, ListCell<Animation>> {
                object : ListCell<Animation>() {
                    override fun updateItem(item: Animation?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty || item == null) null
                        else AnimationRoleLabel.wrap(item, roleFor(item))
                    }
                }
            }
            items = filteredAnimations
            disableProperty().setAndBind(Properties.disableAnimationsView)
        }
        searchTextField.apply {
            disableProperty().setAndBind(Properties.disableAnimationsView)
            textProperty().addListener { _, _, query ->
                filteredAnimations.setPredicate { matchesSearch(it, query) }
            }
        }
    }

    fun clearAnimationCache(){
        roleLabels = emptyMap()
        animationMap.clear()
        catalog.clear()
    }

    fun indexCatalog(loaded: List<Animation>) {
        animationMap.clear()
        catalog.setAll(loaded)
        for (anim in loaded) {
            animationMap[anim.getName()] = anim
            anim.definition?.id?.let { animationMap[it] = anim }
            val numericId = anim.idProperty.get()
            if (numericId != 0)
                animationMap[numericId.toString()] = anim
        }
    }

    fun showCatalog() {
        roleLabels = emptyMap()
        if (catalog.isEmpty())
            return
        animations.setAll(catalog)
        animationsListView.refresh()
    }

    fun showForEntity(entity: AnimatedEntity<*>) {
        roleLabels = entity.definition.animationRoleLabels
        val primary = entity.getPrimaryAnimations()
        val extras = entity.getAnimations().filter { it !in primary }
        animations.setAll(primary.toList() + extras)
        animationsListView.refresh()
    }

    fun snapshotSearchText(): String = searchTextField.text.orEmpty()

    fun snapshotSelectedIdentity(): NamedIdentity? {
        val selected = animationsListView.selectionModel.selectedItem ?: return null
        return NamedIdentity(
            id = selected.definition?.id ?: selected.idProperty.get().toString(),
            name = selected.getName()
        )
    }

    fun restoreSearchText(text: String) {
        searchTextField.text = text
    }

    fun restoreSelectedIdentity(identity: NamedIdentity?): Animation? {
        val match = animations.findByIdentity(identity) {
            it.definition?.id ?: it.idProperty.get().toString()
        } ?: return null
        animationsListView.selectionModel.select(match)
        Platform.runLater {
            animationsListView.scrollTo(match)
            Platform.runLater { animationsListView.scrollTo(match) }
        }
        return match
    }

    fun resolve(ids: Array<String>): Array<Animation> {
        if (ids.isEmpty())
            return emptyArray()
        return ids.mapNotNull { id ->
            animationMap[id]
                ?: catalog.find { it.definition?.id == id || it.idProperty.get().toString() == id }
        }.toTypedArray()
    }

    override fun invoke(p1: AnimatedEntityDefinition): Array<Animation> = resolve(p1.animationIds)

    private fun roleFor(animation: Animation): String? {
        val id = animation.definition?.id ?: animation.idProperty.get().toString()
        return roleLabels[id]
    }

    private fun matchesSearch(animation: Animation, query: String?): Boolean {
        if (query.isNullOrEmpty())
            return true
        if (animation.getName().contains(query, ignoreCase = true))
            return true
        val id = animation.definition?.id ?: animation.idProperty.get().toString()
        if (id.contains(query))
            return true
        val role = roleLabels[id]
        return role != null && role.contains(query, ignoreCase = true)
    }
}
