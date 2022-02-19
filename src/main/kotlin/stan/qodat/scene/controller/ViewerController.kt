package stan.qodat.scene.controller

import javafx.scene.control.ContextMenu
import javafx.scene.control.ListView
import stan.qodat.Properties
import stan.qodat.scene.control.export.ExportMenu
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import java.net.URL
import java.util.*

/**
 * Represents an [EntityViewController] in which entities can be viewed but not edited.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ViewerController : EntityViewController("viewer-scene") {

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        super.initialize(location, resources)

        itemList.contextMenu = ContextMenu(ExportMenu<Item>().apply {
            bindExportable(itemList.selectionModel.selectedItemProperty())
        })
        npcList.contextMenu = ContextMenu(ExportMenu<NPC>().apply {
            val selectedNpcProperty = npcList.selectionModel.selectedItemProperty()
            bindExportable(selectedNpcProperty)
            selectedNpcProperty.addListener { _, _, newValue ->
                if (newValue != null)
                    bindAnimation(newValue.selectedAnimation)
            }
        })
        npcList.createContextMenu()
        objectList.createContextMenu()
        spotAnimList.createContextMenu()
    }

    private fun <E : AnimatedEntity<*>> ListView<E>.createContextMenu() {
        contextMenu = ContextMenu(ExportMenu<E>().apply {
            val selectedObjectProperty = selectionModel.selectedItemProperty()
            bindExportable(selectedObjectProperty)
            selectedObjectProperty.addListener { _, _, newValue ->
                if (newValue != null)
                    bindAnimation(newValue.selectedAnimation)
            }
        })
    }

    override fun cacheProperty() = Properties.viewerCache
}