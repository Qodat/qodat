package stan.qodat.scene.controller

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import stan.qodat.scene.SceneContext
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.paint.Material
import stan.qodat.util.onItemSelected
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class MaterialController : Initializable {

    @FXML lateinit var searchMaterialField: TextField
    @FXML lateinit var materialListView: ViewNodeListView<Material>

    val materials: ObservableList<Material> = FXCollections.observableArrayList()

    private var sceneContextProperty = SimpleObjectProperty<SceneContext>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        VBox.setVgrow(materialListView, Priority.ALWAYS)
        materialListView.items = materials
        materialListView.apply {
            onItemSelected { old, new ->

            }
        }
    }

    fun bind(sceneContext: SceneContext) {
        sceneContextProperty.set(sceneContext)
    }
}