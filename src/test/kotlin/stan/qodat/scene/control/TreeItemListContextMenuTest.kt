package stan.qodat.scene.control

import kotlin.test.Test
import kotlin.test.assertEquals

class TreeItemListContextMenuTest {

    @Test
    fun createActionTypesDistinguishNewAndDuplicateInserts() {
        assertEquals(
            listOf(
                TreeItemListContextMenu.CreateActionType.DUPLICATE,
                TreeItemListContextMenu.CreateActionType.NEW
            ),
            TreeItemListContextMenu.CreateActionType.entries
        )
    }
}
