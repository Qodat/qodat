package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color
import javafx.scene.text.Text
import stan.qodat.Properties
import stan.qodat.javafx.*
import stan.qodat.scene.control.LockButton
import stan.qodat.scene.control.export.ExportMenu
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.util.setAndBind
import tornadofx.contextmenu
import tornadofx.item
import tornadofx.onChange
import tornadofx.progressindicator

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class EntityTreeItem(
    private val entity: Entity<*>,
    private val treeView: TreeView<Node>
) : TreeItem<Node>() {

    init {
        label(entity.getName()) {
            contextMenu = ContextMenu(
                ExportMenu<Entity<*>>().apply {
                    setExportable(entity)
                    if (entity is AnimatedEntity)
                        bindAnimation(entity.selectedAnimation)
                }
            )
        }
        hBox(isGraphic = true) {
            children += LockButton().apply {
                selectedProperty().setAndBind(entity.locked, true)
            }
            children += Text(entity.javaClass.simpleName).apply {
                fill = Color.web("#FFC66D")
            }
        }

        treeView.selectionModel.onSelected { _, newValue ->
            if (newValue != null) {
                var selected = false
                var parent = newValue
                while (parent != null) {
                    if (parent == this) {
                        if (Properties.selectedEntity.get() != entity) {
                            println("Selected $entity")
                            Properties.selectedEntity.set(entity)
                        }
                        selected = true
                        break
                    }
                    parent = parent.parent
                }
                if (!selected && Properties.selectedEntity.get() == entity) {
                    println("Unselected $entity")
                    Properties.selectedEntity.set(null)
                }
            } else {
                println("Unselected $entity")
                Properties.selectedEntity.set(null)
            }
        }
        treeItem {
            label("Models")
            val treeItemPlaceHolder = addPlaceHolder()
            onExpanded {
                if (!this)
                    Properties.treeItemModelsExpanded.set(false)
                else if (children.remove(treeItemPlaceHolder))
                    for (model in entity.getModels())
                        children.add(model.getTreeItem(treeView))
            }
            expandedProperty().set(Properties.treeItemModelsExpanded.get())
        }

        if (entity is AnimatedEntity<*>) {

//            treeItem("Skeletons") {
//                for ((_, skeleton) in entity.getSkeletons())
//                    children.add(SkeletonTreeItem(skeleton, entity, treeView.selectionModel))
//            }

            treeItem {
                label("Animations") {
                    contextmenu {
                        item("Add empty") {
                            setOnAction {
                                val newAnimation = AnimationLegacy("10000").apply {
                                    idProperty.set(10_000)
                                }
                                val newAnimationTreeItem = AnimationTreeItem(newAnimation, entity, treeView,
                                    onRemoval = children::remove
                                )
                                children.add(0, newAnimationTreeItem)
                            }
                        }
                    }
                }
                val treeItemPlaceHolder = addPlaceHolder()
                onExpanded {
                    if (!this)
                        Properties.treeItemAnimationsExpanded.set(false)
                    else if (children.remove(treeItemPlaceHolder)) {
                        for (animation in entity.getAnimations())
                            children.add(AnimationTreeItem(animation, entity, treeView))
                        entity.selectedAnimation.onChange { animation ->
                            if (animation != null) {
                                children.removeIf { it is AnimationTreeItem && it.animation == animation }
                                children.add(0, AnimationTreeItem(animation, entity, treeView))
                            }
                        }
                    }
                }
                expandedProperty().set(Properties.treeItemAnimationsExpanded.get())
            }
        }
        expandedProperty().set(entity.treeItemExpandedProperty().get())
    }

    private fun TreeItem<Node>.addPlaceHolder(): TreeItem<Node> {
        val treeItemPlaceHolder = treeItem {
            progressindicator { }
        }
        return treeItemPlaceHolder
    }
}
