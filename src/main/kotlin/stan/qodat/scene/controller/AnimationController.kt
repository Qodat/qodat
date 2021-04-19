package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.collections.FXCollections
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
import stan.qodat.cache.impl.oldschool.OldschoolCache
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.util.configureSearchFilter
import stan.qodat.util.onItemSelected
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import javafx.beans.property.SimpleStringProperty

import javafx.beans.value.ObservableValue
import javafx.scene.control.ListCell
import javafx.util.Callback


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class AnimationController : Initializable {

    @FXML lateinit var animationsListView: ListView<Animation>
    @FXML lateinit var searchAnimationsList: TextField

    val animations = FXCollections.observableArrayList<Animation>()
    lateinit var filteredAnimations: FilteredList<Animation>

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        VBox.setVgrow(animationsListView, Priority.ALWAYS)

        filteredAnimations = FilteredList(animations) { true }

        animationsListView.cellFactory = Callback<ListView<Animation>, ListCell<Animation>> {
            object : ListCell<Animation>() {
                override fun updateItem(item: Animation?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty || item == null)
                        null
                    else
                        item.getViewNode()
                }
            }
        }
        animationsListView.disableProperty().setAndBind(Properties.disableAnimationsView)
        animationsListView.items = filteredAnimations
        animationsListView.onItemSelected { old, new ->
            if (new == null && old != null)
                SubScene3D.animationPlayer.transformerProperty.set(null)
            else if(new != null)
                SubScene3D.animationPlayer.transformerProperty.set(new)
        }

        searchAnimationsList.disableProperty().setAndBind(Properties.disableAnimationsView)
        searchAnimationsList.configureSearchFilter(filteredAnimations)

        Properties.cache.addListener { _, _, newValue ->
            Qodat.mainController.executeBackgroundTasks(createSequenceLoadTask(newValue))
        }
    }

    private fun createSequenceLoadTask(cache: Cache) = object : Task<Void?>() {
        override fun call(): Void? {
            val animationDefinitions = OldschoolCache.getAnimationDefinitions()
            val animations = ArrayList<Animation>()
            for ((i, definition) in animationDefinitions.withIndex()) {
                try {
                    if (definition.frameHashes.isNotEmpty()) {
                        val animation = Animation("$i", definition, cache)
                        animations.add(animation)
                    }
                    val progress = (100.0 * i.div(animationDefinitions.size))
                    updateProgress(progress, 100.0)
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