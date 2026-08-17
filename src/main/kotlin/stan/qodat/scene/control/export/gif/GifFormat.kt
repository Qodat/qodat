package stan.qodat.scene.control.export.gif

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.stage.FileChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.export.ExportFormat
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.task.BackgroundTasks
import java.io.File
import java.nio.file.Path

class GifFormat : ExportFormat<Animation> {

    override val defaultSaveDestinationProperty: ObjectBinding<Path> =
        Bindings.createObjectBinding(
            { Properties.defaultExportsPath.get().resolve("GIF") },
            Properties.defaultExportsPath
        )

    override val lastSaveDestinationProperty: ObjectProperty<Path?> =
        Properties.lastGIFExportPath

    override fun getFileName(context: Animation): String = "${context.getName()}.gif"

    override fun export(context: Animation, destination: Path) {
        val scene = SubScene3D.subSceneProperty.get()
            ?: throw IllegalStateException("No 3D scene is available for GIF export")
        val animationPlayer = SubScene3D.contextProperty.get()?.animationPlayer
            ?: throw IllegalStateException("No animation player is available for GIF export")
        BackgroundTasks.submit(
            addProgressIndicator = true,
            AnimationToGifTask(
                exportPath = destination,
                scene = scene,
                animationPlayer = animationPlayer,
                animation = context
            )
        )
    }

    override fun chooseSaveDestination(context: Animation): File? {
        val initial = (lastSaveDestinationProperty.value ?: defaultSaveDestinationProperty.get())?.toFile()
        if (initial != null && !initial.exists()) {
            initial.mkdirs()
        }
        val chooser = FileChooser().apply {
            title = "Export ${context.getName()} as GIF"
            extensionFilters += FileChooser.ExtensionFilter("GIF", "*.gif")
            initialDirectory = when {
                initial == null -> null
                initial.isDirectory -> initial
                else -> initial.parentFile
            }
            initialFileName = getFileName(context)
        }
        return chooser.showSaveDialog(Qodat.stage)?.also {
            lastSaveDestinationProperty.set(it.parentFile.toPath())
        }
    }
}
