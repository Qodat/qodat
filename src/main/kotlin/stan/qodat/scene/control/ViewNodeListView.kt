package stan.qodat.scene.control

import javafx.event.Event
import javafx.event.EventType
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import stan.qodat.util.ViewNodeProvider

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
                    graphic = item?.getViewNode()
                }
            }
            listCell.addEventFilter(MouseEvent.MOUSE_PRESSED) { event: MouseEvent ->
                if (event.isPrimaryButtonDown && !event.isSecondaryButtonDown && !listCell.isEmpty) {
                    requestFocus()
                    val index = listCell.index
                    if (selectionModel.selectedIndices.contains(index)) {
                        selectionModel.clearSelection(index)
                        fireEvent(UnselectedEvent(listCell.item))
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
                fireEvent(UnselectedEvent(oldValue))
            if (newValue != null)
                fireEvent(SelectedEvent(newValue))
        }
    }

    class UnselectedEvent(val viewNodeProvider: ViewNodeProvider) : Event(UNSELECTED_EVENT_TYPE)
    class SelectedEvent(val viewNodeProvider: ViewNodeProvider) : Event(SELECTED_EVENT_TYPE)

    companion object {

        @JvmStatic
        val UNSELECTED_EVENT_TYPE = EventType<UnselectedEvent>("Unselected Renderable")
        @JvmStatic
        val SELECTED_EVENT_TYPE = EventType<SelectedEvent>("Selected Renderable")
    }
}