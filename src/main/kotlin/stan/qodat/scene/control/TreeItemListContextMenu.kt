package stan.qodat.scene.control

import javafx.collections.ObservableList
import javafx.scene.control.*
import stan.qodat.Qodat
import stan.qodat.scene.control.TreeItemListContextMenu.CreateActionType.*
import stan.qodat.util.Action
import stan.qodat.util.onInvalidation

/**
 * Represents a [ContextMenu] for a [TreeItem] that contains a [ObservableList] of [elements][E].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 *
 * @param list           the [ObservableList] to be targeted by context actions.
 * @param rootItem       the root [TreeItem] whose children represent the [list] elements (same indices).
 * @param selectionModel the [TreeView.selectionModel].
 * @param itemCreator
 */
class TreeItemListContextMenu<E>(
    private val list: ObservableList<E>,
    private val rootItem: TreeItem<*>,
    private val selectionModel: MultipleSelectionModel<*>,
    private val itemCreator: (CreateActionType, E) -> E
) : ContextMenu() {

    /**
     * An enumerated type representing how an insertion should be handled.
     */
    enum class CreateActionType {
        /**
         * Duplicates the currently selected item.
         */
        DUPLICATE,
        /**
         * Creates a new item.
         */
        NEW
    }

    init {
        items.addAll(
            Menu("Add").apply {
                items.addAll(
                    MenuItem("Before").apply {
                        setOnAction { Qodat.addAction(createInsertAction(NEW, offset = 0)) }
                    },
                    MenuItem("After").apply {
                        setOnAction { Qodat.addAction(createInsertAction(NEW, offset = +1)) }
                    }
                )
                selectionModel.selectedItems.onInvalidation {
                    this@apply.disableProperty().set(selectionModel.selectedItems.size > 1)
                }
            },
            Menu("Duplicate").apply {
                items.addAll(
                    MenuItem("Before").apply {
                        setOnAction { Qodat.addAction(createInsertAction(DUPLICATE, offset = 0)) }
                    },
                    MenuItem("After").apply {
                        setOnAction { Qodat.addAction(createInsertAction(DUPLICATE, offset = +1)) }
                    }
                )
            },
            MenuItem("Delete").apply {
                setOnAction { Qodat.addAction(createDeleteAction()) }
            }
        )
    }

    private fun createInsertAction(type: CreateActionType, offset: Int) = object  : Action {
        lateinit var inserted: ArrayList<E>
        override fun action() {
            inserted = ArrayList()
            val localIndices = selectionModel.selectedItems
                .map { rootItem.children.indexOf(it) }
                .sortedByDescending { it }
                .toIntArray()
            for (index in localIndices) {
                val previous = list[index]
                val inserted = itemCreator.invoke(type, previous)
                list.add(index + offset, inserted)
                this.inserted.add(inserted)
            }
        }
        override fun undoAction() {
            list.removeAll(inserted)
        }
    }

    private fun createDeleteAction() = object : Action {
        private lateinit var lastRemovedMap: HashMap<Int, E>
        override fun action() {
            lastRemovedMap = HashMap()
            val selectedItems = selectionModel.selectedItems
                .sortedByDescending { rootItem.children.indexOf(it) }
            for (item in selectedItems) {
                val index = rootItem.children.indexOf(item)
                val element = list[index]
                lastRemovedMap[index] = element
            }
            list.removeAll(lastRemovedMap.values)
        }

        override fun undoAction() {
            val sortedIndices = lastRemovedMap.keys.sortedBy { it }
            for (index in sortedIndices) {
                val element = lastRemovedMap[index]
                list.add(index, element)
            }
        }
    }
}