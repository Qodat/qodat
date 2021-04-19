package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.ModelFaceMesh
import stan.qodat.scene.runescape.model.ModelMeshBuildType
import java.util.function.Consumer

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class EntityTreeItem(
    entity: Entity<*>,
    treeView: TreeView<Node>
) : TreeItem<Node>() {

    init {

        val selectionModel = treeView.selectionModel
        selectionModel.selectionMode = SelectionMode.MULTIPLE

        val mouseHandler = Consumer<ModelFaceMesh> {

        }

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            if (oldValue == this){
                entity.getModels().forEach {
                    it.buildTypeProperty.set(ModelMeshBuildType.ATLAS)
                    it.collectMeshes().forEach {
                        if (it is ModelFaceMesh) {
                            it.selectProperty.set(null)
                        }
                    }
                }
            }
            if (newValue == this){
                entity.getModels().forEach {
                    it.buildTypeProperty.set(ModelMeshBuildType.MESH_PER_FACE)
                    it.collectMeshes().forEach {
                        if (it is ModelFaceMesh) {
                            it.selectProperty.set(mouseHandler)
                        }
                    }
                }
            }
        }
        value = Label(entity.getName())
        graphic = Text("NPC").also {
            it.fill = Color.web("#FFC66D")
        }

        val resetModelBox = createResetTreeItem(entity)
        children.add(resetModelBox)

        val modelsItem = TreeItem<Node>(Label("Models"))
        for (model in entity.getModels())
            modelsItem.children.add(model.getTreeItem(treeView))
        children.add(modelsItem)

        if (entity is AnimatedEntity<*>) {

            val skeletonsItem = TreeItem<Node>(Label("Skeletons"))
            for ((_, skeleton) in entity.getSkeletons())
                skeletonsItem.children.add(SkeletonTreeItem(skeleton, selectionModel, entity))
            children.add(skeletonsItem)

            val animationsItem = TreeItem<Node>(Label("Animations"))
            for (animation in entity.getAnimations()) {
                animationsItem.children.add(AnimationTreeItem(animation, entity, treeView))
            }
            children.add(animationsItem)
        }
    }

    private fun createResetTreeItem(entity: Entity<*>) =
        TreeItem<Node>(Button("Reset Models").also {
            it.setOnAction {
                entity.getModels().forEach {
                    it.reset()
                }
            }
        })
}