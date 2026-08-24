package stan.qodat.scene.runescape.widget

import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetLayoutTest {

    @Test
    fun sizeModesMatchClient() {
        assertEquals(40, WidgetLayout.size(200, 100, 40, 10, 0, 0).first)
        assertEquals(160, WidgetLayout.size(200, 100, 40, 10, 1, 0).first)
        assertEquals(100, WidgetLayout.size(200, 100, 8192, 10, 2, 0).first)
        assertEquals(90, WidgetLayout.size(200, 100, 40, 10, 0, 1).second)
        assertEquals(50, WidgetLayout.size(200, 100, 40, 8192, 0, 2).second)
    }

    @Test
    fun aspectSizeUsesTheResolvedOppositeEdge() {
        // height = parent-minus 20; width mode 4 uses that height * 2 / 20
        val wide = WidgetLayout.size(400, 200, 2, 20, 4, 1)
        assertEquals(180, wide.second)
        assertEquals(18, wide.first)

        // width = parent-minus 40; height mode 4 uses that width * 20 / 40
        val tall = WidgetLayout.size(400, 200, 40, 20, 1, 4)
        assertEquals(360, tall.first)
        assertEquals(180, tall.second)
    }

    @Test
    fun positionModesMatchClient() {
        assertEquals(12, WidgetLayout.position(200, 12, 40, 0))
        assertEquals(12 + (200 - 40) / 2, WidgetLayout.position(200, 12, 40, 1))
        assertEquals(200 - 40 - 12, WidgetLayout.position(200, 12, 40, 2))
        assertEquals((8192 * 200) shr 14, WidgetLayout.position(200, 8192, 40, 3))
        assertEquals(((8192 * 200) shr 14) + (200 - 40) / 2, WidgetLayout.position(200, 8192, 40, 4))
        assertEquals(200 - 40 - ((8192 * 200) shr 14), WidgetLayout.position(200, 8192, 40, 5))
    }

    @Test
    fun layoutUsesComputedSizeWhenCentering() {
        val def = InterfaceDefinition().also {
            it.originalX = 10
            it.originalY = 4
            it.originalWidth = 40
            it.originalHeight = 20
            it.xPositionMode = 1
            it.yPositionMode = 1
        }
        val box = WidgetLayout.layout(def, 200, 100)
        assertEquals(10 + (200 - 40) / 2, box.x)
        assertEquals(4 + (100 - 20) / 2, box.y)
        assertEquals(40, box.width)
        assertEquals(20, box.height)
    }

    @Test
    fun parentChildIdsUseTheLow16Bits() {
        assertEquals(3, WidgetLayout.childId(0x00010003))
        assertEquals(-1, WidgetLayout.parentChildId(-1))
        assertEquals(2, WidgetLayout.parentChildId(0x00010002))
    }

    @Test
    fun hierarchyNestsByParentChildIdAndKeepsOrphansAtRoot() {
        val root = iface(0x00010000, parentId = -1)
        val child = iface(0x00010001, parentId = 0x00010000)
        val orphan = iface(0x00010005, parentId = 0x00019999)
        val tree = WidgetLayout.buildHierarchy(listOf(root, child, orphan))
        assertEquals(2, tree.size)
        assertEquals(0, WidgetLayout.childId(tree[0].definition.id))
        assertEquals(1, tree[0].children.size)
        assertEquals(1, WidgetLayout.childId(tree[0].children[0].definition.id))
        assertEquals(5, WidgetLayout.childId(tree[1].definition.id))
    }

    private fun iface(id: Int, parentId: Int) = InterfaceDefinition().also {
        it.id = id
        it.parentId = parentId
    }
}
