package stan.qodat.scene.control.tree

import javafx.animation.TranslateTransition
import javafx.beans.binding.Bindings
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.util.Duration
import qodat.cache.definition.InterfaceDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.javafx.label
import stan.qodat.javafx.text
import stan.qodat.javafx.treeItem
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.scene.runescape.widget.Widget
import stan.qodat.scene.runescape.widget.component.Component
import stan.qodat.scene.runescape.widget.component.Pos
import stan.qodat.scene.runescape.widget.component.Size
import stan.qodat.scene.runescape.widget.component.impl.Graphic
import stan.qodat.scene.runescape.widget.component.impl.Inventory
import stan.qodat.scene.runescape.widget.component.impl.Layer
import stan.qodat.scene.runescape.widget.component.impl.Model
import stan.qodat.util.ModelUtil
import stan.qodat.util.onInvalidation

class InterfaceTreeItem(val group: InterfaceGroup, val selectionModel: MultipleSelectionModel<TreeItem<Node>>) : TreeItem<Node>() {

    val do3d = CheckBox("3D")

    init {
        label(group.nameProperty)
        text(group.javaClass.simpleName, Color.web("#FFC66D"))

        val widget = Widget()

        val componentsById = group.definitions.associateBy { it.id.and(0xffff) }
        componentsById.forEach { (id, comp) ->
            val component = convert(comp, componentsById, id)
            widget.children.add(component)
        }
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
        expandedProperty().set(group.treeItemExpandedProperty().get())
    }

    private fun convert(
        base: InterfaceDefinition,
        componentsById: Map<Int, InterfaceDefinition>,
        id: Int,
    ): Component<*> {
        val component = when (base.type) {
            0 -> Layer().apply {
                scrollX = 0
                scrollY = 0
                scrollHeight = base.scrollHeight
                children.addAll(componentsById.values.filter { it.parentId.and(0xffff) == id }.map { convert(it, componentsById, it.id.and(0xffff))})
            }
            2 -> Inventory().apply {

            }
            3 -> stan.qodat.scene.runescape.widget.component.impl.Rectangle().apply {
            }
            4 -> stan.qodat.scene.runescape.widget.component.impl.Text()
            5 -> Graphic()
            6 -> Model()
            9 -> stan.qodat.scene.runescape.widget.component.impl.Line()
            else -> error("Unsupported component type: $base")
        }.apply {
            name = base.id.toString()
            x = base.originalX
            y = base.originalY
            width = base.originalWidth
            height = base.originalHeight
            hSize = Size.fromId(base.widthMode)
            vSize = Size.fromId(base.heightMode)
            hPos = Pos.fromIdHor(base.xPositionMode)
            vPos = Pos.fromIdVer(base.yPositionMode)
        }
        return component
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
                        val sprite = def.spriteId.takeIf { it > 0 }?.let { OldschoolCacheRuneLite.getSprite(it, 0) }?.let { Sprite(it) }
                        ImageView(sprite?.image)
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
