package stan.qodat.scene.control.tree

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import stan.qodat.javafx.*
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.entity.Entity

/**
 * Represents a [TreeItem] for the provided [animation].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class AnimationTreeItem(
    private val animation: Animation,
    entity: Entity<*>,
    treeView: TreeView<Node>
) : TreeItem<Node>() {

    private val framesMap = HashMap<TreeItem<Node>, AnimationFrame>()

    init {
        setGraphic("ANIMATION", Color.web("#FFC66D"))
        setValue(animation.labelProperty)
        onExpanded {
            if (this) {
                transformsContextMenuMap.clear()
                for (frameItem in children) {
                    val frame = framesMap[frameItem]?:continue
                    val transforms = frame.transformationList?.apply {
                        onChange {
                            frameItem.children.clear()
                            for ((index, transform) in withIndex()) {
                                val transformControlItem =
                                    TransformTreeItem(entity, frame, transform, frameItem, treeView.selectionModel)
                                frameItem.children.add(index, transformControlItem)
                            }
                        }
                    }?:continue
                    for ((index, transform) in transforms.withIndex())
                        frameItem.children.add(index, TransformTreeItem(entity, frame, transform, frameItem, treeView.selectionModel))
                }
            }
        }
    }

    override fun isLeaf() = if (framesMap.isNotEmpty()) children.isEmpty() else false

    override fun getChildren(): ObservableList<TreeItem<Node>> {
        if (framesMap.isEmpty())
            loadFrames()
        return super.getChildren()
    }

    private fun loadFrames(){
        val frames = animation.getFrameList()
        for (frame in frames) {
            val frameItem = TreeItem<Node>(HBox(5.0).apply {
                alignment = Pos.CENTER_LEFT
                checkBox(frame.enabledProperty, biDirectional = true)
                label(frame.getName())
            })
            framesMap[frameItem] = frame
            children.add(frameItem)
        }
    }

    companion object {
        val transformsContextMenuMap = HashMap<AnimationFrame, TreeItemListContextMenu<Transformation>>()
    }
}