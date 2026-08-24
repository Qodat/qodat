package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.text.Text
import qodat.cache.Cache
import qodat.cache.definition.InterfaceDefinition
import stan.qodat.scene.runescape.ui.Sprite

class InterfaceComponentTreeItem(val cache: Cache, val definition: InterfaceDefinition) : TreeItem<Node>() {

    init {
        val textId = Label("${definition.id.and(0xffff)}")
        val typeString = when(definition.type) {
            0 -> "Layer"
            2 -> "Inventory"
            3, 10 -> "Rectangle"
            4 -> "Text"
            5 -> "Graphic"
            6 -> "Model"
            9 -> "Line"
            else -> "?"
        }
        val textType = Text(typeString).apply {
            fill = Color.web("#FFC66D")
        }
        value = textId
        graphic = textType
        if (definition.spriteId >= 0) {
            val spriteDefinition = runCatching { cache.getSprite(definition.spriteId, 0) }.getOrNull()
            if (spriteDefinition != null) {
                val sprite = Sprite(spriteDefinition)
                children.add(TreeItem<Node>(Label("Sprite ${sprite.nameProperty.get()}"), sprite.sceneNode))
            }
        }
    }
}