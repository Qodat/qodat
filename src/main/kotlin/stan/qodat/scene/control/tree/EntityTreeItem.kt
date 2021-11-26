package stan.qodat.scene.control.tree

import com.sun.javafx.application.PlatformImpl
import javafx.concurrent.Task
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.definition.save.ObjExporter
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.entity.Entity
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
        text(entity.javaClass.simpleName, Color.web("#FFC66D"))
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
           this@EntityTreeItem.onExpanded {
                for (model in entity.getModels())
                    children.add(model.getTreeItem(treeView))
            }
        }
        treeItem {
            this@EntityTreeItem.onExpanded {
                vBox {
                    children.add(Button("Export as .obj/.mtl").apply {
                        setOnAction { Qodat.mainController.executeBackgroundTasks(createModelsExportTask(entity)) }
                    })
                    children.add(Button("Reset Models").apply {
                        setOnAction { for (model in entity.getModels()) model.reset() }
                    })
                }
            }
        }

//        if (entity is AnimatedEntity<*>) {
//            treeItem("Skeletons") {
//                for ((_, skeleton) in entity.getSkeletons())
//                    children.add(SkeletonTreeItem(skeleton, selectionModel, entity))
//            }
//            treeItem("Animations") {
//                for (animation in entity.getAnimations())
//                    children.add(AnimationTreeItem(animation, entity, treeView))
//            }
//        }
    }

    private fun createModelsExportTask(entity: Entity<*>) = object : Task<Unit>() {
        override fun call() {

            val savePath = Properties.exportsPath.get().resolve("obj").toFile().apply {
                if (!parentFile.exists())
                    parentFile.mkdir()
                if (!exists())
                    mkdir()
            }

            val models = entity.getModels()
            val count = models.size

            val entityName = entity.getName()

            for ((i, model) in models.withIndex()) {
                PlatformImpl.runAndWait {
                    updateMessage("Parsing model $i of $entityName")
                    updateProgress(i + 1L, count.toLong())
                }
                val exporter = ObjExporter(model.modelDefinition)

                val saveDir = savePath.resolve(entityName).apply {
                    if (!exists())
                        mkdir()
                }
                PrintWriter(FileWriter("$saveDir/$i.obj")).use { objWriter ->
                    PrintWriter(FileWriter("$saveDir/$i.mtl")).use { mtlWriter ->
                        exporter.export(
                            i.toString(),
                            objWriter,
                            mtlWriter
                        )
                    }
                }
            }
            PlatformImpl.runAndWait {
                updateMessage("Saved models to $savePath")
            }
            Thread.sleep(1500L)
        }
    }
}