package stan.qodat.scene.control.tree

import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color
import stan.qodat.Qodat
import stan.qodat.javafx.*
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.util.getAnimationsView

/**
 * Represents a [TreeItem] for the provided [animation].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class AnimationTreeItem(
    private val animation: Animation,
    private val entity: Entity<*>,
    private val treeView: TreeView<Node>
) : TreeItem<Node>() {

    private val framesMap = HashMap<TreeItem<Node>, AnimationFrame>()

    init {
        text("ANIMATION", Color.web("#FFC66D"))
        label(animation.labelProperty) {
            tooltip = Tooltip("Click to play animation")
            setOnMouseClicked {
                Qodat.getAnimationsView().apply {
                    scrollTo(animation)
                    selectionModel.select(animation)
                }
            }
        }
        onExpanded {
            if (this) {
                if (framesMap.isEmpty())
                    loadFrames()
                transformsContextMenuMap.clear()
                for (frameItem in children) {
                    val frame = framesMap[frameItem]?:continue
                    val transforms = frame.transformationList?:continue
                    transforms.onChange { resetTransformTreeItems(transforms, frameItem, entity, frame, treeView) }
                    resetTransformTreeItems(transforms, frameItem, entity, frame, treeView)
                }
            }
        }
    }

    override fun isLeaf() = if (framesMap.isNotEmpty()) children.isEmpty() else false

    private fun loadFrames(){
        val frames = animation.getFrameList()
        for (frame in frames) {
            treeItem {
                framesMap[this] = frame
                hBox {
                    checkBox(frame.enabledProperty, biDirectional = true)
                    label(frame.getName())
                }
            }
        }
    }

    private fun resetTransformTreeItems(
        list: ObservableList<Transformation>,
        frameItem: TreeItem<Node>,
        entity: Entity<*>,
        frame: AnimationFrame,
        treeView: TreeView<Node>
    ) {
        frameItem.children.clear()

        val flat = false
        if (!flat) {
            val groupedTransformations = mutableMapOf<Transformation, List<Transformation>>()
            var children: MutableList<Transformation>? = null
            val transformationsIterator = list.iterator()
            while (transformationsIterator.hasNext()) {
                val next = transformationsIterator.next()
                if (next.getType() == TransformationType.SET_OFFSET) {
                    children = mutableListOf()
                    groupedTransformations[next] = children
                } else {
                    requireNotNull(children) { "First transform should be of type ${TransformationType.SET_OFFSET}" }
                        .add(next)
                }
            }
            for ((rootTransformation, childTransformations) in groupedTransformations) {
                frameItem.children.add(TransformGroupTreeItem(
                    entity as AnimatedEntity<*>, frame, frameItem, treeView,
                    rootTransformation, childTransformations))
            }
        } else {
            for ((index, transform) in list.withIndex())
                frameItem.children.add(
                    index,
                    TransformTreeItem(entity, frame, transform, frameItem, treeView.selectionModel))
        }

    }

    companion object {
        val transformsContextMenuMap = HashMap<AnimationFrame, TreeItemListContextMenu<Transformation>>()
    }
}