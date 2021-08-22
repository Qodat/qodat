package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color
import stan.qodat.cache.definition.save.ObjExporter
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.ModelFaceMesh
import stan.qodat.scene.runescape.model.ModelMeshBuildType
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

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
        label(entity.getName())
        text("NPC", Color.web("#FFC66D"))
        val selectionModel = treeView.selectionModel.apply {
            selectionMode = SelectionMode.MULTIPLE
            onSelected { oldValue, newValue ->
                if (oldValue == this){
                    for (model in entity.getModels()) {
                        model.buildTypeProperty.set(ModelMeshBuildType.ATLAS)
                        for (mesh in model.collectMeshes())
                            if (mesh is ModelFaceMesh)
                                mesh.selectProperty.set(null)
                    }
                }
                if (newValue == this){
                    for (model in entity.getModels())
                        model.buildTypeProperty.set(ModelMeshBuildType.MESH_PER_FACE)
                }
            }
        }
        treeItem {
            button("Export") {
                for ((i, model) in entity.getModels().withIndex()) {
                    val name = "${entity.getName()}_$i"
                    val exporter = ObjExporter(model.modelDefinition)
                    PrintWriter(FileWriter(File("$name.obj"))).use { objWriter ->
                        PrintWriter(FileWriter(File("$name.mtl"))).use { mtlWriter ->
                            exporter.export(
                                name,
                                objWriter,
                                mtlWriter
                            )
                        }
                    }
                }
            }
        }
        treeItem {
            button("Reset Models") {
                for (model in entity.getModels())
                    model.reset()
            }
        }
        treeItem("Models") {
            for (model in entity.getModels())
                children.add(model.getTreeItem(treeView))
        }
        if (entity is AnimatedEntity<*>) {
            treeItem("Skeletons") {
                for ((_, skeleton) in entity.getSkeletons())
                    children.add(SkeletonTreeItem(skeleton, selectionModel, entity))
            }
            treeItem("Animations") {
                for (animation in entity.getAnimations())
                    children.add(AnimationTreeItem(animation, entity, treeView))
            }
        }
    }
}