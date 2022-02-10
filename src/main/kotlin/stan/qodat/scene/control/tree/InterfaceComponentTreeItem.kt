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
            3 -> "Rectangle"
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
        if (definition.spriteId != -1) {
            val spriteDefinition = cache.getSprite(definition.spriteId, 0)
            val sprite = Sprite(spriteDefinition)
            val tree = TreeItem<Node>(Label("Sprite ${sprite.nameProperty.get()}"), sprite.sceneNode)
            children.add(tree)
        }
    }
}