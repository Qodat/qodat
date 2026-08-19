package stan.qodat.scene.control.export.blender

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.stage.FileChooser
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.control.export.ExportFormat
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.Searchable
import stan.qodat.util.formatName
import java.io.File
import java.nio.file.Path

/**
 * Export the current model (bind-pose definition + vertex skins) as a
 * Blender-ready .glb. Import is [Model.fromFile] on .glb / .gltf.
 */
class BlenderFormat(
    override val lastSaveDestinationProperty: ObjectProperty<Path?> =
        Properties.lastBlenderExportPath
) : ExportFormat<Triple<Searchable, Animation?, AnimationFrame?>> {

    override val defaultSaveDestinationProperty: ObjectBinding<Path> =
        Bindings.createObjectBinding(
            { Properties.defaultExportsPath.get().resolve("blender") },
            Properties.defaultExportsPath
        )

    override fun getFileName(context: Triple<Searchable, Animation?, AnimationFrame?>): String =
        context.first.formatName().replace(" ", "_") + ".glb"

    override fun chooseSaveDestination(context: Triple<Searchable, Animation?, AnimationFrame?>): File? {
        val initial = (lastSaveDestinationProperty.value ?: defaultSaveDestinationProperty.get())?.toFile()
        if (initial != null && !initial.exists()) {
            initial.mkdirs()
        }
        val chooser = FileChooser().apply {
            title = "Export ${context.first.getName()} as Blender glTF"
            extensionFilters += FileChooser.ExtensionFilter("glTF binary", "*.glb")
            initialDirectory = when {
                initial == null -> null
                initial.isDirectory -> initial
                else -> initial.parentFile
            }
            initialFileName = getFileName(context)
        }
        return chooser.showSaveDialog(Qodat.stage)?.also {
            lastSaveDestinationProperty.set(it.toPath())
        }
    }

    override fun export(context: Triple<Searchable, Animation?, AnimationFrame?>, destination: Path) {
        try {
            val (exportable, _, _) = context
            val model = when (exportable) {
                is Entity<*> -> exportable.createMergedModel(exportable.formatName())
                is Model -> exportable
                else -> throw IllegalArgumentException(
                    "Cannot export ${exportable.getName()} of type ${exportable::class.java}"
                )
            }
            val file = if (destination.toString().endsWith(".glb", ignoreCase = true))
                destination
            else
                destination.resolve(getFileName(context))
            GltfCodec.write(model.modelDefinition, file, model.getName().replace(" ", "_"))
        } catch (e: Exception) {
            Qodat.logException("Failed to export Blender glTF", e)
        }
    }
}
