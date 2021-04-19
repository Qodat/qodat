package stan.qodat.scene.controller

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import stan.qodat.scene.runescape.model.Model
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ModelController : Initializable{

    @FXML private lateinit var modelListView: ListView<Model>
    @FXML private lateinit var searchModelField: TextField
    @FXML private lateinit var setLabel: MenuItem

    private val models: ObservableList<Model> = FXCollections.observableArrayList()
    lateinit var filteredModels: FilteredList<Model>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        VBox.setVgrow(modelListView, Priority.ALWAYS)
        filteredModels = FilteredList(models) { true }
        modelListView.items = filteredModels
    }
}