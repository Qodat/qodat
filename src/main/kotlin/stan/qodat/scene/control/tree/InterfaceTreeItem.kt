package stan.qodat.scene.control.tree

import javafx.animation.TranslateTransition
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.util.Duration
import stan.qodat.javafx.checkBox
import stan.qodat.javafx.label
import stan.qodat.javafx.text
import stan.qodat.javafx.treeItem
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.util.ModelUtil
import stan.qodat.util.onInvalidation

class InterfaceTreeItem(val group: InterfaceGroup, val selectionModel: MultipleSelectionModel<TreeItem<Node>>) : TreeItem<Node>() {

    val do3d = CheckBox("3D")

    init {
        label(group.nameProperty)
        text(group.javaClass.simpleName, Color.web("#FFC66D"))

        do3d.selectedProperty().onInvalidation {
            if (get()) {

            }
        }
        val sceneNode = group.getSceneNode()
        children.add(TreeItem(do3d))
        treeItem("Components") {
            val components = group.definitions.map { InterfaceComponentTreeItem(group.cache, it) }.toMutableList()
//            children.setAll(components)
            while (components.isNotEmpty()) {
                val iterator = components.listIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.definition.parentId == -1) {
                        children.add(next)
                        iterator.remove()
                    } else {
                        val subId = next.definition.parentId.and(0xffff)
                        val result = search(subId)
                        if (result != null) {
                            result.children.add(next)
                            iterator.remove()
                        }
                    }
                }
            }
            add(sceneNode, 0)
        }
    }

    private fun TreeItem<Node>.add(sceneGroup: Parent, depth: Int) {
        for (child in children) {
            if (child is InterfaceComponentTreeItem) {
                val def = child.definition
                val node = when(def.type) {
                    0 -> Pane().apply {

                        borderProperty().bind(Bindings.createObjectBinding({
                            if (selectionModel.selectedItemProperty().get() == child)
                                Border(
                                    BorderStroke(
                                        Color.ALICEBLUE,
                                        BorderStrokeStyle.SOLID,
                                        CornerRadii.EMPTY,
                                        BorderWidths(2.0)
                                    )
                                )
                            else
                                Border(
                                    BorderStroke(
                                        Color.ALICEBLUE,
                                        BorderStrokeStyle.DASHED,
                                        CornerRadii.EMPTY,
                                        BorderWidths(1.0)
                                    )
                                )
                        }, selectionModel.selectedItemProperty()))
                        setMinSize(def.originalWidth.toDouble(), def.originalHeight.toDouble())
                        setMaxSize(def.originalWidth.toDouble(), def.originalHeight.toDouble())
                        translateX = def.originalX.toDouble()
                        translateY = def.originalY.toDouble()
                        do3d.selectedProperty().onInvalidation {
                            if(get()) {
                                val translateTransition = TranslateTransition(Duration.millis(500.0), this@apply)
                                translateTransition.toZ = depth * -10.0
                                translateTransition.play()
                            } else {
                                val translateTransition = TranslateTransition(Duration.millis(250.0), this@apply)
                                translateTransition.toZ = 0.0
                                translateTransition.play()
                            }
                        }
                    }
                    3 -> Rectangle().apply {
                        width = def.originalWidth.toDouble()
                        height = def.originalHeight.toDouble()
                    }
                    4 -> Text().apply {
                        text = def.text
                        fill = ModelUtil.hsbToColor(def.textColor, def.opacity.toByte())
                    }
                    5 -> {
                        val view = ImageView((child.children.find { it.graphic is ImageView }?.graphic as ImageView).image)
//                        view.x = bounds.x
//                        view.y = bounds.y
                        view
                    }
                    6 -> Group()
                    9 -> Line()
                    else -> Group()
                }
                node.id = child.definition.id.and(0xffff).toString()
                if (sceneGroup is Group)
                    sceneGroup.children.add(node)
                else if (sceneGroup is Pane)
                    sceneGroup.children.add(node)
                if (node is Parent)
                    child.add(node, depth + 1)
            }
        }
    }

    private fun TreeItem<Node>.search(subId: Int): TreeItem<Node>? {
        if (this is InterfaceComponentTreeItem) {
            if (definition.id.and(0xffff) == subId) {
                return this
            }
        }
        for (child in children) {
            val result = child.search(subId)
            if (result != null)
                return result
        }
        return null
    }
}