package stan.qodat.scene.controller

import com.sun.javafx.application.PlatformImpl
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.Callback
import stan.qodat.scene.control.tree.ExceptionTreeItem
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
class EventLogController : Initializable {

    @FXML lateinit var exceptionListView: ListView<Throwable>

    val exceptions = FXCollections.observableArrayList<Throwable>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        exceptionListView.items = exceptions
        exceptionListView.cellFactory = Callback {
            val listCell = object : ListCell<Throwable>() {
                override fun updateItem(item: Throwable?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item != null) {
                        graphic = TreeView<TextFlow>().apply {
                            val exceptionTreeItem = ExceptionTreeItem(item)
                            root = exceptionTreeItem
                            val cellHeight = 30.0
                            prefHeight = cellHeight
                            expandedItemCountProperty().addListener { _, _, newValue ->
                                PlatformImpl.runLater {
                                    prefHeight =  (newValue.toInt() * cellHeight)
                                }
                            }
                        }
                    }
                }
            }
            listCell
        }
    }
}
