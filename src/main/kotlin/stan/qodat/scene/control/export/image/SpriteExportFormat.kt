package stan.qodat.scene.control.export.image

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.stage.FileChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.control.export.ExportFormat
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.task.BackgroundTasks
import stan.qodat.task.export.ExportTaskResult
import tornadofx.FileChooserMode
import tornadofx.chooseFile
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.isDirectory

sealed class SpriteExportFormat : ExportFormat<Sprite> {

    companion object {
        val all by lazy { listOf(PNG) }
    }

    abstract val formatName: String
    abstract val extensions: List<String>

    override val defaultSaveDestinationProperty: ObjectBinding<Path> =
        Bindings.createObjectBinding(
            { Properties.defaultExportsPath.get().resolve("sprites") },
            Properties.defaultExportsPath
        )

    override val lastSaveDestinationProperty: ObjectProperty<Path?> =
        Properties.lastSpriteExportPath

    private fun getFileChooserInitialDirectory(): File? =
        (lastSaveDestinationProperty.value ?: defaultSaveDestinationProperty.get())?.toFile()

    override fun chooseSaveDestination(context: Sprite): File? {
        val initialDirectory = getFileChooserInitialDirectory()
        if (initialDirectory != null && !initialDirectory.exists())
            initialDirectory.mkdirs()
        return chooseFile(
            "Sprite Export",
            initialDirectory = initialDirectory,
            filters = arrayOf(FileChooser.ExtensionFilter("Only $formatName files", extensions)),
            mode = FileChooserMode.Save,
            owner = Qodat.stage
        ) {
            initialFileName = context.getName()+".${extensions.first()}"
        }.first().apply { lastSaveDestinationProperty.set(parentFile.toPath()) }
    }

    override fun export(context: Sprite, destination: Path) {
        BackgroundTasks.submit(true, object : Task<ExportTaskResult>() {
            override fun call(): ExportTaskResult {
                val savePath = if (destination.isDirectory())
                    destination.resolve(context.getName()+".${extensions.first()}")
                else
                    destination
                try {
                    val bufferedImage = SwingFXUtils.fromFXImage(context.image, null)
                    ImageIO.write(bufferedImage, formatName, savePath.toFile())
                } catch (e: Exception) {
                    return ExportTaskResult.Failed(e)
                }
                return ExportTaskResult.Success(savePath)
            }
        })
    }

    object PNG : SpriteExportFormat() {
        override val formatName: String = "PNG"
        override val extensions: List<String> = listOf("png")
    }

    object JPEG : SpriteExportFormat() {
        override val formatName: String = "JPEG"
        override val extensions: List<String> = listOf("jpg", "jpeg")
    }

    object BMP : SpriteExportFormat() {
        override val formatName: String = "BMP"
        override val extensions: List<String> = listOf("bmp")
    }

    object WBMP : SpriteExportFormat() {
        override val formatName: String = "WBMP"
        override val extensions: List<String> = listOf("wbmp")
    }

    object GIF : SpriteExportFormat() {
        override val formatName: String = "GIF"
        override val extensions: List<String> = listOf("gif")
    }
}
