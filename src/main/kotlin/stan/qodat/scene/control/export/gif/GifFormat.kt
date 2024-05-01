package stan.qodat.scene.control.export.gif

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import stan.qodat.Properties
import stan.qodat.scene.control.export.ExportFormat
import stan.qodat.scene.runescape.animation.AnimationLegacy
import java.io.File
import java.nio.file.Path

class GifFormat : ExportFormat<AnimationLegacy> {

    override val defaultSaveDestinationProperty: ObjectBinding<Path> =
        Bindings.createObjectBinding(
            { Properties.defaultExportsPath.get().resolve("GIF") },
            Properties.defaultExportsPath
        )

    override val lastSaveDestinationProperty: ObjectProperty<Path?> =
        Properties.lastGIFExportPath

    override fun export(context: AnimationLegacy, destination: Path) {
        TODO("Not yet implemented")
    }

    override fun chooseSaveDestination(context: AnimationLegacy): File? {
        TODO("Not yet implemented")
    }
}
