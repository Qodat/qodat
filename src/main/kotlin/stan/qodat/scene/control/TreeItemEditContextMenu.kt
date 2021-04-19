package stan.qodat.scene.control

import javafx.collections.ObservableList
import javafx.scene.control.*
import stan.qodat.Qodat
import stan.qodat.util.Action
import stan.qodat.util.onInvalidation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class TreeItemEditContextMenu<E>(
    private val list: ObservableList<E>,
    private val rootItem: TreeItem<*>,
    private val selectionModel: MultipleSelectionModel<*>,
    private val transformer: (Type, E) -> E
) : ContextMenu() {

    enum class Type {
        COPY,
        NEW
    }

    init {
        val addMenu = Menu("Add")
        selectionModel.selectedItems.onInvalidation {
            addMenu.disableProperty().set(selectionModel.selectedItems.size > 1)
        }
        val addBeforeMenuItem = MenuItem("Before")
        addBeforeMenuItem.setOnAction {
            Qodat.addAction(createInsertAction(Type.NEW, offset = 0))
        }
        val addAfterMenuItem = MenuItem("After")
        addAfterMenuItem.setOnAction {
            Qodat.addAction(createInsertAction(Type.NEW, offset = +1))
        }
        addMenu.items.addAll(addBeforeMenuItem, addAfterMenuItem)

        val duplicateMenu = Menu("Duplicate")
        val duplicateBeforeMenuItem = MenuItem("Before")
        duplicateBeforeMenuItem.setOnAction {
            Qodat.addAction(createInsertAction(Type.COPY, offset = 0))
        }
        val duplicateAfterMenuItem = MenuItem("After")
        duplicateAfterMenuItem.setOnAction {
            Qodat.addAction(createInsertAction(Type.COPY, offset = +1))
        }
        duplicateMenu.items.addAll(duplicateBeforeMenuItem, duplicateAfterMenuItem)

        val deleteMenuItem = MenuItem("Delete")
        deleteMenuItem.setOnAction {
            Qodat.addAction(object : Action {

                lateinit var lastRemovedMap : HashMap<Int, E>

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
            })
        }

        items.addAll(
            addMenu,
            duplicateMenu,
            deleteMenuItem)
    }

    private fun createInsertAction(
        type: Type,
        offset: Int
    ) : Action {
        return object  : Action {

            lateinit var inserted: ArrayList<E>

            override fun action() {
                inserted = ArrayList()
                val localIndices = selectionModel.selectedItems
                    .map { rootItem.children.indexOf(it) }
                    .sortedByDescending { it }
                    .toIntArray()
                for (index in localIndices) {
                    val previous = list[index]
                    val inserted = transformer.invoke(type, previous)
                    list.add(index + offset, inserted)
                    this.inserted.add(inserted)
                }
            }

            override fun undoAction() {
                list.removeAll(inserted)
            }
        }
    }
}