package stan.qodat.task.export.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import stan.qodat.scene.control.export.wavefront.WaveFrontMaterial
import stan.qodat.scene.control.export.wavefront.WaveFrontWriter
import stan.qodat.scene.control.export.wavefront.getFaceMaterials
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.task.export.ExportTask
import stan.qodat.task.export.ExportTaskResult
import stan.qodat.util.formatName
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an [ExportTask] for WaveFront formatted files.
 */
sealed class ExportWaveFrontTask(val saveDir: Path) : ExportTask() {

    /**
     * Exports the [model] as (multiple) WaveFront file(s), one file per [pose][AnimationFrame].
     */
    class Sequence(
        saveDir: Path,
        val modelGroupName: String,
        val frames: List<AnimationFrame>,
        val model: Model,
        val reColorMap: Map<Short, Short>? = null,
    ) : ExportWaveFrontTask(saveDir) {

        constructor(saveDir: Path, model: Model, frames: List<AnimationFrame>) :
                this(saveDir, model.getName().replace(" ", "_"), frames, model)

        constructor(saveDir: Path, entity: Entity<*>, frames: List<AnimationFrame>) :
                this(
                    saveDir = saveDir,
                    modelGroupName = entity.formatFileName(),
                    frames = frames,
                    model = entity.createMergedModel(entity.formatName()),
                    reColorMap = entity.getRecolorMap()
                )

        override fun call(): ExportTaskResult {

            val materials = model.modelDefinition.getFaceMaterials(reColorMap).toSet()

            val mtlWriter = WaveFrontWriter(saveDir)
            mtlWriter.writeMtlFile(materials, fileNameWithoutExtension = modelGroupName.replace(" ", "_"))

            val modelCount = frames.size
            val modelExportFinishedCount = AtomicInteger(0)

            model.apply {
                modelDefinition.computeNormals()
                modelDefinition.computeTextureUVCoordinates()
            }
            frames.forEach { animationFrame ->
                model.animate(animationFrame)
                val objWriter = WaveFrontWriter(saveDir)

                objWriter.writeObjFile(
                    model = model,
                    materials = materials,
                    computeTextureUVCoordinate = false,
                    mtlFileNameWithoutExtension = modelGroupName,
                    objFileNameWithoutExtension = modelGroupName + "_${animationFrame.getName()}"
                )
                GlobalScope.launch(Dispatchers.JavaFx) {
                    val finishedCount = modelExportFinishedCount.incrementAndGet().toLong()
                    updateMessage("Parsing model $finishedCount in group $modelGroupName")
                    updateProgress(finishedCount, modelCount.toLong())
                }
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
        private val materials: Set<WaveFrontMaterial>? = null,
        val reColorMap: Map<Short, Short>? = null,
    ) : ExportWaveFrontTask(saveDir) {

        constructor(
            saveDir: Path,
            entity: Entity<*>,
            animationFrame: AnimationFrame? = null,
            writeMaterials: Boolean = true,
        ) : this(
            saveDir = saveDir,
            model = entity.createMergedModel(entity.getName()),
            animationFrame = animationFrame,
            writeMaterials = writeMaterials,
            fileName = entity.formatFileName(),
            reColorMap = entity.getRecolorMap()
        )

        override fun call(): ExportTaskResult {

            try {

                val saveFile = saveDir.toFile()
                    .apply {
                        if (!exists())
                            mkdirs()
                    }

                model.modelDefinition.apply {
                    computeTextureUVCoordinates()
                }
                val savePath = saveFile.toPath()
                val writer = WaveFrontWriter(savePath)

                if (animationFrame != null)
                    model.animate(animationFrame)

                val materials = materials ?: model.modelDefinition.getFaceMaterials(reColorMap).toSet()
                writer.writeObjFile(
                    model, materials,
                    mtlFileNameWithoutExtension = fileName,
                    objFileNameWithoutExtension = fileName
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
