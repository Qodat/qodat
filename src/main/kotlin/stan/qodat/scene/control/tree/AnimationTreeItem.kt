package stan.qodat.scene.control.tree

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

/**
 * TODO: add documentation
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
        val text = Text("ANIMATION").also {
            it.fill = Color.web("#FFC66D")
        }
        val label = Label().also {
            it.textProperty().setAndBind(animation.labelProperty)
        }

        value = label
        graphic = text

        expandedProperty().onInvalidation {
            if (value) {

                transformsContextMenuMap.clear()

                for (frameItem in children) {

                    val frame = framesMap[frameItem]!!
                    val transforms = frame.transformationList

                    transforms.addListener(ListChangeListener {
                        frameItem.children.clear()
                        for ((index, transform) in transforms.withIndex()) {
                            val transformControlItem =
                                TransformTreeItem(entity, frame, transform, frameItem, treeView.selectionModel)
                            frameItem.children.add(index, transformControlItem)
                        }
                    })
                    for ((index, transform) in transforms.withIndex()) {
                        val transformControlItem =
                            TransformTreeItem(entity, frame, transform, frameItem, treeView.selectionModel)
                        frameItem.children.add(index, transformControlItem)
                    }
                }
            }
        }
    }

    override fun isLeaf(): Boolean {
        if (framesMap.isNotEmpty())
            return children.isEmpty()
        return false
    }

    override fun getChildren(): ObservableList<TreeItem<Node>> {
        if (framesMap.isNotEmpty())
            return super.getChildren()
        loadFrames()
        return super.getChildren()
    }

    private fun loadFrames(){
        val frames = animation.getFrameList()
        for (frame in frames) {

            val controlBox = HBox(5.0)
            controlBox.alignment = Pos.CENTER_LEFT

            val disableBox = CheckBox()
            disableBox.selectedProperty()
                .setAndBind(frame.enabledProperty, biDirectional = true)
            controlBox.children.add(disableBox)

            val label = Label(frame.getName())
            controlBox.children.add(label)

            val frameItem = TreeItem<Node>(controlBox)
            framesMap[frameItem] = frame
            children.add(frameItem)
        }
    }

    companion object {
        val transformsContextMenuMap = HashMap<AnimationFrame, TreeItemListContextMenu<Transformation>>()
    }

}