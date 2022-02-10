package stan.qodat.task

import javafx.concurrent.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import stan.qodat.cache.definition.save.ModelObjExporter
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.model.Model
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

class ModelExportObjTask(
    private val saveFile: File,
    private val model: Model,
    private val animationFrame: AnimationFrame? = null,
) : Task<Unit>() {

    override fun call() {

        val exporter = ModelObjExporter(model, animationFrame)

        val savePath = saveFile.parentFile
        val saveName = saveFile.nameWithoutExtension

        PrintWriter(FileWriter("$savePath/$saveName.obj")).use { objWriter ->
            PrintWriter(FileWriter("$savePath/$saveName.mtl")).use { mtlWriter ->
                exporter.export(
                    saveName,
                    objWriter,
                    mtlWriter
                )
            }
        }

        JavaFXExecutor.execute {
            updateMessage("Saved model to $savePath")
        }
    }
}