package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.Cache
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.util.configureSearchFilter
import stan.qodat.util.onItemSelected
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

import javafx.scene.control.ListCell
import javafx.util.Callback

/**
 * Represents a FXML controller for a filterable list of animations.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class AnimationController : Initializable {

    /**
     * A [ListView] containing all [filteredAnimations].
     */
    @FXML lateinit var animationsListView: ListView<Animation>

    /**
     * A [TextField] in which users can filter the [animations][Animation] contained in the [animationsListView].
     * This is used to set the predicate of [filteredAnimations].
     */
    @FXML lateinit var searchTextField: TextField

    /**
     * A [FilteredList] of [animations][Animation]. This list is backed by [animations].
     */
    lateinit var filteredAnimations: FilteredList<Animation>

    /**
     * An [ObservableList] of all the [animations][Animation] present in the loaded [Cache].
     */
    val animations: ObservableList<Animation> = FXCollections.observableArrayList()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        filteredAnimations = FilteredList(animations) { true }
        animationsListView.apply {
            VBox.setVgrow(this, Priority.ALWAYS)
            cellFactory = createCellFactory()
            items = filteredAnimations
            disableProperty().setAndBind(Properties.disableAnimationsView)
            onItemSelected { old, new ->
                if (new == null && old != null) {
                    Properties.selectedAnimationName.set("")
                    SubScene3D.animationPlayer.transformerProperty.set(null)
                } else if(new != null) {
                    Properties.selectedAnimationName.set(new.getName())
                    SubScene3D.animationPlayer.transformerProperty.set(new)
                }
            }
        }
        searchTextField.apply {
            disableProperty().setAndBind(Properties.disableAnimationsView)
            configureSearchFilter(filteredAnimations)
        }
        Properties.cache.addListener { _, _, newValue ->
            Qodat.mainController.executeBackgroundTasks(createLoadAnimationsTask(newValue))
        }
    }

    private fun createCellFactory() = Callback<ListView<Animation>, ListCell<Animation>> {
        object : ListCell<Animation>() {
            override fun updateItem(item: Animation?, empty: Boolean) {
                super.updateItem(item, empty)
                graphic = if (empty || item == null) null else item.getViewNode()
            }
        }
    }

    private fun createLoadAnimationsTask(cache: Cache) = object : Task<Void?>() {
        override fun call(): Void? {
            val animationDefinitions = OldschoolCacheRuneLite.getAnimationDefinitions()
            val animations = ArrayList<Animation>()
            for ((i, definition) in animationDefinitions.withIndex()) {
                try {
                    if (definition.frameHashes.isNotEmpty())
                        animations += Animation("$i", definition, cache)
                    updateProgress((100.0 * i.div(animationDefinitions.size)), 100.0)
                    updateMessage("Loading animation (${i + 1} / ${animationDefinitions.size})")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Platform.runLater {
                this@AnimationController.animations.setAll(animations)
            }
            return null
        }
    }
}