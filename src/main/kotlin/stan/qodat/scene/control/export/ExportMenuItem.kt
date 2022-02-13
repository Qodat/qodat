package stan.qodat.scene.control.export

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectExpression
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import java.nio.file.Path

class ExportMenuItem<C>(context: C, private val format: ExportFormat<C>, prefixMenuItemName: String = "") :
    Menu(prefixMenuItemName) {
    init {
        items.addAll(
            createSaveToPathMenuItem(context, format.lastSaveDestinationProperty),
            createSaveToPathMenuItem(context, format.defaultSaveDestinationProperty),
            MenuItem("choose...").apply {
                setOnAction {
                    val destination = format.chooseSaveDestination(context)?.toPath()
                    if (destination != null)
                        format.export(context, destination)
                }
            },
        )
    }

    private fun createSaveToPathMenuItem(context: C, objectProperty: ObjectExpression<Path?>): MenuItem {

//        val fileName = format.getFileName(context)
        return MenuItem().apply {
//            val transformedPathProperty =
//                fileName?.let {
//                    Bindings.createObjectBinding({
//                        objectProperty.value?.resolve(it)
//                    }, objectProperty)
//                } ?: objectProperty
            visibleProperty().bind(objectProperty.isNotNull)
            textProperty().bind(
                Bindings.createStringBinding(
                    {
                        val pathString = objectProperty.value?.toString()
                        "save to '$pathString'"
                    },
                    objectProperty
                )
            )
            setOnAction {
                val destination = objectProperty.get()
                if (destination != null)
                    format.export(context, destination)
            }
        }
    }
}