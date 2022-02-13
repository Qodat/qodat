package stan.qodat.scene.controller

import javafx.scene.control.ContextMenu
import stan.qodat.Properties
import stan.qodat.scene.control.export.ExportMenu
import stan.qodat.scene.runescape.entity.Item
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.scene.runescape.entity.Object
import stan.qodat.util.onInvalidation
import java.net.URL
import java.util.ResourceBundle

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
            selectedNpcProperty.onInvalidation {
                bindAnimation(selectedNpcProperty.get().selectedAnimation)
            }
        })
        objectList.contextMenu = ContextMenu(ExportMenu<Object>().apply {
            val selectedObjectProperty = objectList.selectionModel.selectedItemProperty()
            bindExportable(selectedObjectProperty)
            selectedObjectProperty.onInvalidation {
                bindAnimation(selectedObjectProperty.get().selectedAnimation)
            }
        })
    }

    override fun cacheProperty() = Properties.viewerCache
}