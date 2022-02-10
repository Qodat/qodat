package stan.qodat.scene.controller

import javafx.beans.binding.Bindings
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.stage.FileChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.entity.Object
import stan.qodat.task.ModelExportObjTask
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
        itemList.contextMenuBuilder = contextMenuBuilder()
        npcList.contextMenuBuilder = contextMenuBuilder()
        objectList.contextMenuBuilder = contextMenuBuilder()
    }

    private fun<D, E : Entity<D>> contextMenuBuilder() = fun(entity: E): ContextMenu {
        val contextMenu = ContextMenu()
        val editItem = MenuItem().apply {
            textProperty().bind(Bindings.format("Edit \"%s\"", entity.labelProperty))
            setOnAction {
                Qodat.mainController.rightEditorTab.isSelected = true
                val editor = Qodat.mainController.editorController
                if (Properties.lockScene.get()) // force scene switch
                    editor.selectThisContext()
                when (entity) {
                    is Item -> {
                        editor.items.add(entity)
                        editor.itemList.selectionModel.select(entity)
                    }
                    is NPC -> {
                        editor.npcs.add(entity)
                        editor.npcList.selectionModel.select(entity)
                    }
                    is Object -> {
                        editor.objects.add(entity)
                        editor.objectList.selectionModel.select(entity)
                    }
                }
            }
        }
        val exportMenu = Menu("Export", null, MenuItem(".obj/.mtl").apply {
            setOnAction {
                val entity = Properties.selectedEntity.get()
                val fileChooser = FileChooser()
                fileChooser.title = "Export to WaveFront format."
                fileChooser.initialFileName = entity.getName()
                val file = fileChooser.showSaveDialog(null)
                if (file != null) {
                    val model = entity.createMergedModel(file.nameWithoutExtension)
                    Qodat.mainController.executeBackgroundTasks(ModelExportObjTask(file, model))
                }
            }
        })
        contextMenu.items.addAll(
//            editItem,
            exportMenu
        )
        return contextMenu
    }

    override fun cacheProperty() = Properties.viewerCache
}