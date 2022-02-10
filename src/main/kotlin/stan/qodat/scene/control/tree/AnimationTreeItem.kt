package stan.qodat.scene.control.tree

import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import stan.qodat.Properties
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.javafx.*
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.util.setAndBind

/**
 * Represents a [TreeItem] for the provided [animation].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class AnimationTreeItem(
    val animation: Animation,
    private val entity: AnimatedEntity<*>,
    private val treeView: TreeView<Node>,
    private val onRemoval: (AnimationTreeItem) -> Unit = {},
) : TreeItem<Node>() {

    private val framesMap = HashMap<AnimationFrameTreeItem, AnimationFrame>()
    private var loadedFrames = false

    init {

        assert(animation.treeItemProperty.get() == null)
        { "Tree item property for animation $animation is already set!" }

        hBox(isGraphic = false) {
            children += Text("ANIMATION").apply {
                fill = Color.web("#FFC66D")
            }
            children += Label().apply {
                textProperty().setAndBind(animation.labelProperty)
                contextMenu = createContextMenu()
            }
            children += Button("Pack").apply {
                setOnAction {
                    if (animation.exportFrameArchiveId.get() == 0){
                        val dialog = TextInputDialog()
                        dialog.showAndWait().ifPresent {
                            animation.exportFrameArchiveId.set(it.toIntOrNull()?:0)
                        }
                    }
                    if (animation.exportFrameArchiveId.get() != 0)
                        QodatCache.encode(animation)
                }
            }
            children += Button("Load").apply {
                tooltip = Tooltip("Loads this animation into the timeline.")
                disableProperty().setAndBind(Properties.selectedAnimation.isEqualTo(animation))
                setOnAction {
                    entity.selectedAnimation.set(animation)
                    Properties.selectedAnimation.set(animation)
                }
            }
        }
        onExpanded {
            if (this) {
                if (!loadedFrames) {
                    loadFrameTreeItems()
                    transformsContextMenuMap.clear()
                    updateFrameTreeItems()
                }
            }
        }
    }

    private fun createContextMenu() = ContextMenu(
        MenuItem("Delete").apply {
            setOnAction {
                Alert(Alert.AlertType.CONFIRMATION, "Are you sure you wish to delete this animation?", ButtonType.YES, ButtonType.CANCEL)
                    .showAndWait()
                    .ifPresent {
                        if (it == ButtonType.YES) {
                            onRemoval(this@AnimationTreeItem)
                        }
                    }
            }
        }
    )

    private fun updateFrameTreeItems() {
        for (treeItem in children) {
            if (treeItem is AnimationFrameTreeItem) {
                val frame = treeItem.frame
                val transforms = frame.transformationList ?: continue
                transforms.onChange { treeItem.resetTransformTreeItems(transforms, entity, frame, treeView) }
                treeItem.resetTransformTreeItems(transforms, entity, frame, treeView)
            }
        }
    }

    override fun isLeaf() = if (framesMap.isNotEmpty()) children.isEmpty() else false

    private fun loadFrameTreeItems(){
        val frames = animation.getFrameList()
        animation.getFrameList().addListener(ListChangeListener {
            while(it.next()){
                if (it.wasAdded()) {
                    for (index in it.from until it.to){
                        val frame = it.list[index]
                        val frameTreeItem =  AnimationFrameTreeItem(entity, animation, frame, treeView)
                        children.add(index, frameTreeItem)
                    }
                    updateFrameTreeItems()
                } else if (it.wasRemoved()) {
                    val removed = it.removed
                    children.removeIf { treeItem ->
                        if (treeItem is AnimationFrameTreeItem) {
                            removed.contains(treeItem.frame)
                        } else
                            true
                    }
                }
            }
        })
        for (frame in frames) {
            val frameTreeItem = AnimationFrameTreeItem(entity, animation, frame, treeView)
            children += frameTreeItem
        }
        loadedFrames = true
    }


    companion object {
        val transformsContextMenuMap = HashMap<AnimationFrame, TreeItemListContextMenu<Transformation>>()
    }
}