package stan.qodat.scene.controller

import javafx.beans.binding.Bindings
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import stan.qodat.Properties
import stan.qodat.Qodat
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ViewerController : EntityViewController("viewer-scene") {

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        super.initialize(location, resources)
        itemList.contextMenuBuilder = { item ->
            val contextMenu = ContextMenu()
            val editItem = MenuItem()
            editItem.textProperty().bind(Bindings.format("Edit \"%s\"", item.labelProperty))
            editItem.setOnAction {
                Qodat.mainController.rightEditorTab.isSelected = true
                if (Properties.lockScene.get()) // force scene switch
                    Qodat.mainController.editorController.selectThisContext()
                Qodat.mainController.editorController.items.add(item)
                Qodat.mainController.editorController.itemList.selectionModel.select(item)
            }
            contextMenu.items.add(editItem)
            contextMenu
        }
    }

    override fun cacheProperty() = Properties.viewerCache
}