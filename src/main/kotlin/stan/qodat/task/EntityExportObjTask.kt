package stan.qodat.task

import javafx.concurrent.Task
import stan.qodat.Properties
import stan.qodat.cache.definition.save.ModelDefinitionObjExporter
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.runescape.entity.Entity
import java.io.FileWriter
import java.io.PrintWriter

class EntityExportObjTask(private val entity: Entity<*>) : Task<Unit>() {

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
            JavaFXExecutor.execute {
                updateMessage("Parsing model $i of $entityName")
                updateProgress(i + 1L, count.toLong())
            }
            val exporter = ModelDefinitionObjExporter(model.modelDefinition)

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
        JavaFXExecutor.execute {
            updateMessage("Saved models to $savePath")
        }
        Thread.sleep(1500L)
    }
}