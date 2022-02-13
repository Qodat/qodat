package stan.qodat.task

import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import stan.qodat.scene.control.export.wavefront.WaveFrontMaterial
import stan.qodat.scene.control.export.wavefront.WaveFrontWriter
import stan.qodat.scene.control.export.wavefront.getFaceMaterials
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.task.ExportWaveFrontTask.*
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Exception

/**
 * Exports a single or multiple [models] as WaveFront (.obj/.mtl) file(s).
 */
sealed class ExportWaveFrontTask(val saveDir: Path) : ExportTask() {

    /**
     * Exports multiple [models] as WaveFront (.obj/.mtl) file(s) for each pose.
     */
    class Sequence(
        saveDir: Path,
        val modelGroupName: String,
        val frames: List<AnimationFrame>,
        val model: Model,
    ) : ExportWaveFrontTask(saveDir) {

        constructor(saveDir: Path, model: Model, frames: List<AnimationFrame>) :
                this(saveDir, model.getName(), frames, model)

        constructor(saveDir: Path, entity: Entity<*>, frames: List<AnimationFrame>) :
                this(saveDir, entity.getName(), frames, entity.createMergedModel(entity.getName()))

        override fun call(): ExportTaskResult {
            val materials = model.modelDefinition.getFaceMaterials().toSet()

            val mtlWriter = WaveFrontWriter(saveDir)
            mtlWriter.writeMtlFile(materials, fileNameWithoutExtension = modelGroupName)

            val modelCount = frames.size
            val modelExportFinishedCount = AtomicInteger(0)

            model.apply {
                modelDefinition.computeNormals()
                modelDefinition.computeTextureUVCoordinates()
            }

            runBlocking {
                frames.map { animationFrame ->
                    GlobalScope.async(Dispatchers.IO) {
                        val objWriter = WaveFrontWriter(saveDir)
                        objWriter.writeObjFile(
                            model = model,
                            materials = materials,
                            computeNormals = false,
                            computeTextureUVCoordinate = false,
                            objFileNameWithoutExtension = modelGroupName + "_${animationFrame.getName()}",
                            mtlFileNameWithoutExtension = modelGroupName
                        )
                        GlobalScope.launch(Dispatchers.JavaFx) {
                            val finishedCount = modelExportFinishedCount.incrementAndGet().toLong()
                            updateMessage("Parsing model $finishedCount in group $modelGroupName")
                            updateProgress(finishedCount, modelCount.toLong())
                        }
                    }
                }.awaitAll()
            }
            return ExportTaskResult.Success(saveDir)
        }
    }

    class Single(
        saveDir: Path,
        private val model: Model,
        private val animationFrame: AnimationFrame? = null,
        private val writeMaterials: Boolean = true,
        private val fileName: String = model.getName(),
        private val materials: Set<WaveFrontMaterial> = model.modelDefinition.getFaceMaterials().toSet(),
    ) : ExportWaveFrontTask(saveDir) {

        constructor(
            saveDir: Path,
            entity: Entity<*>,
            animationFrame: AnimationFrame? = null,
            writeMaterials: Boolean = true
        ) : this(
            saveDir = saveDir,
            model = entity.createMergedModel(entity.getName()),
            animationFrame = animationFrame,
            writeMaterials = writeMaterials,
            fileName = entity.getName()
        )

        override fun call(): ExportTaskResult {

            try {

                val saveFile = saveDir.toFile()
                    .apply {
                        if (!exists())
                            mkdirs()
                    }


                val savePath = saveFile.toPath()
                val writer = WaveFrontWriter(savePath)

                if (animationFrame != null)
                    model.animate(animationFrame)

                writer.writeObjFile(
                    model, materials,
                    objFileNameWithoutExtension = fileName,
                    mtlFileNameWithoutExtension = fileName
                )
                if (writeMaterials) {
                    if (materials.isEmpty())
                        return ExportTaskResult.Failed(Exception("Attempted to write materials, but none were provided, skipping."))
                    writer.writeMtlFile(materials, fileNameWithoutExtension = fileName)
                }
            } catch (e: Exception) {
                return ExportTaskResult.Failed(e)
            }

            return ExportTaskResult.Success(saveDir)
        }
    }
}