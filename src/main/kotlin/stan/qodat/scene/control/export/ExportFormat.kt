package stan.qodat.scene.control.export

import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import java.io.File
import java.nio.file.Path

interface ExportFormat<C> {

    val defaultSaveDestinationProperty: ObjectBinding<Path>
    val lastSaveDestinationProperty: ObjectProperty<Path?>

    fun export(context: C, destination: Path)

    fun chooseSaveDestination(context: C) : File?

    fun getFileName(context: C) : String? = null
}