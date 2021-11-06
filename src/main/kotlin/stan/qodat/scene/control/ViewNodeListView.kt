package stan.qodat.scene.control

import javafx.event.Event
import javafx.event.EventType
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.util.Callback
import stan.qodat.util.ViewNodeProvider
import java.io.File

/**
 * Represents a [ListView] containing [nodes][N].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
open class ViewNodeListView<N : ViewNodeProvider> : ListView<N>() {

    init {
        cellFactory = Callback {
            val listCell = object : ListCell<N>() {
                override fun updateItem(item: N?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item != null)
                        graphic = item.getViewNode()
                }
            }
            listCell.addEventFilter(MouseEvent.MOUSE_PRESSED) { event: MouseEvent ->
                if (event.isPrimaryButtonDown && !event.isSecondaryButtonDown && !listCell.isEmpty) {
                    requestFocus()
                    val index = listCell.index
                    if (selectionModel.selectedIndices.contains(index)) {
                        selectionModel.clearSelection(index)
                        fireEvent(UnselectedEvent(listCell.item, false))
                    } else
                        selectionModel.select(index)
                    event.consume()
                }
            }
            return@Callback listCell
        }
        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            /*
             * TODO: add multi-select support?
             */
            if (oldValue != null)
                fireEvent(UnselectedEvent(oldValue, newValue != null))
            if (newValue != null)
                fireEvent(SelectedEvent(newValue))
        }
    }
    fun enableDragAndDrop(
        toFile:(N.() -> File)? = null,
        fromFile: (File.() -> N)? = null,
        onDropFrom: (List<Pair<File, N>>) -> Unit,
        imageProvider: (N.() -> Image)? = null,
        vararg supportedExtensions: String
    ){
        if (fromFile != null) {
            setOnDragOver {

                val db = it.dragboard
                if (db.hasFiles())
                    it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                it.consume()
            }
            setOnDragDropped {
                val db = it.dragboard
                if (db.hasFiles()) {
                    val newEntries = db.files
                        .filter { file -> supportedExtensions.contains(file.extension) }
                        .mapNotNull { file -> file to fromFile(file) }
                    onDropFrom(newEntries)
                    it.isDropCompleted = true
                }
                it.consume()
            }
        }
        if (toFile != null) {
            setOnDragDetected {
                val db = startDragAndDrop(*TransferMode.COPY_OR_MOVE)
                val selected = selectionModel.selectedItems
                val files = selected.map(toFile)
                val image = if (selected.isNotEmpty())
                    imageProvider?.invoke(selected.first())
                else
                    null
                if (image != null)
                    filesToCopyClipboard.putImage(image)
                filesToCopyClipboard.putFiles(files)
                db.setContent(filesToCopyClipboard)
                it.consume()
            }
            setOnDragDone {
                it.consume()
            }
        }
    }
    class UnselectedEvent(val viewNodeProvider: ViewNodeProvider, val hasNewValue: Boolean) : Event(UNSELECTED_EVENT_TYPE)
    class SelectedEvent(val viewNodeProvider: ViewNodeProvider) : Event(SELECTED_EVENT_TYPE)

    companion object {

        val filesToCopyClipboard = ClipboardContent()

        @JvmStatic
        val UNSELECTED_EVENT_TYPE = EventType<UnselectedEvent>("Unselected Renderable")
        @JvmStatic
        val SELECTED_EVENT_TYPE = EventType<SelectedEvent>("Selected Renderable")
    }
}