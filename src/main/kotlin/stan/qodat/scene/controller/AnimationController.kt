package stan.qodat.scene.controller

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import qodat.cache.Cache
import qodat.cache.definition.AnimatedEntityDefinition
import stan.qodat.Properties
import stan.qodat.javafx.onChange
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.util.configureSearchFilter
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
     * An [ObservableList] of all the [animations][AnimationLegacy] present in the loaded [Cache].
     */
    val animations: ObservableList<Animation> = FXCollections.observableArrayList()

    private val animationMap = FXCollections.observableHashMap<String, Animation>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        animations.onChange {
            while(next()) {

                if (wasAdded()) {
                    for (i in from until to){
                        val anim = list[i]
                        animationMap[anim.getName()] = anim
                    }
                }
            }
        }
        filteredAnimations = FilteredList(animations) { true }
        animationsListView.apply {
            VBox.setVgrow(this, Priority.ALWAYS)
            cellFactory = Animation.createCellFactory()
            items = filteredAnimations
            disableProperty().setAndBind(Properties.disableAnimationsView)
        }
        searchTextField.apply {
            disableProperty().setAndBind(Properties.disableAnimationsView)
            configureSearchFilter(filteredAnimations)
        }
    }

    fun clearAnimationCache(){
        animationMap.clear()
    }

    override fun invoke(p1: AnimatedEntityDefinition): Array<Animation> {
        return p1.animationIds
            .mapNotNull { animationMap[it] }
            .toTypedArray()
    }
}
