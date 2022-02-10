package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.javafx.*
import stan.qodat.scene.control.LockButton
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.task.EntityExportObjTask
import stan.qodat.util.setAndBind

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
        label(entity.getName())
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
//        val selectionModel = treeView.selectionModel.apply {
//            selectionMode = SelectionMode.MULTIPLE
//            onSelected { oldValue, newValue ->
//                if (oldValue == this){
//                    for (model in entity.getModels()) {
//                        model.buildTypeProperty.set(ModelMeshBuildType.ATLAS)
//                        for (mesh in model.collectMeshes())
//                            if (mesh is ModelFaceMesh)
//                                mesh.selectProperty.set(null)
//                    }
//                }
//                if (newValue == this){
//                    for (model in entity.getModels())
//                        model.buildTypeProperty.set(ModelMeshBuildType.MESH_PER_FACE)
//                }
//            }
//        }
        treeItem("Models") {
             treeItem("Util") {
                 treeItem {
                     vBox {
                         children += Button("Export as .obj/.mtl")
                             .apply { setOnAction { Qodat.mainController.executeBackgroundTasks(EntityExportObjTask(entity)) } }
                         children += Button("Reset Models")
                             .apply { setOnAction { for (model in entity.getModels()) model.reset() } }
                     }
                 }
             }
            onExpanded {
                if (children.isEmpty()) {
                    for (model in entity.getModels())
                        children.add(model.getTreeItem(treeView))
                }
            }
        }

        if (entity is AnimatedEntity<*>) {

//            treeItem("Skeletons") {
//                for ((_, skeleton) in entity.getSkeletons())
//                    children.add(SkeletonTreeItem(skeleton, entity, treeView.selectionModel))
//            }

            treeItem {

                label("Animations") {
                    contextMenu = ContextMenu(
                        MenuItem("Add empty").apply {
                            setOnAction {
                                val newAnimation = Animation("10000").apply {
                                    idProperty.set(10_000)
                                }
                                val newAnimationTreeItem = AnimationTreeItem(newAnimation, entity, treeView,
                                    onRemoval = children::remove
                                )
                                children.add(0, newAnimationTreeItem)
                            }
                        }
                    )
                }

                val showAllTreeItem = TreeItem<Node>().apply {
                    label("Show all")

                }
                children += showAllTreeItem

                onExpanded {
                    if (showAllTreeItem.children.isEmpty())
                        for (animation in entity.getAnimations())
                            showAllTreeItem.children.add(AnimationTreeItem(animation, entity, treeView))
                }
                entity.selectedAnimation.addListener { _, oldAnimation, newAnimation ->
                    if (oldAnimation != null)
                        showAllTreeItem.move(newAnimation, this, false)
                    if (newAnimation != null)
                        move(newAnimation, showAllTreeItem, true)
                }

                // must be below adding of animations
                val selected = entity.selectedAnimation.get()
                if (selected != null)
                    move(selected, showAllTreeItem, true)
            }
        }
    }

    private fun TreeItem<Node>.move(
        selected: Animation,
        showAllTreeItem: TreeItem<Node>,
        createIfNoTreeItem: Boolean
    ) {

        var animationTreeItem = selected.treeItemProperty.get()
        if (animationTreeItem != null)
            showAllTreeItem.children.remove(animationTreeItem)
        else if (createIfNoTreeItem) {
            animationTreeItem = AnimationTreeItem(selected, entity as AnimatedEntity<*>, treeView)
            selected.treeItemProperty.set(animationTreeItem)
        }

        if (animationTreeItem != null && children.filterIsInstance<AnimationTreeItem>().none { it.animation == selected})
            children.add(0, animationTreeItem)
    }
}