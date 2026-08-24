package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.displee.CacheIdPackingTest
import stan.qodat.cache.impl.displee.types.InterfaceManager.Companion.getInterfaceGroup
import stan.qodat.cache.impl.displee.types.InterfaceManager.Companion.interfaceGroupName
import stan.qodat.cache.impl.displee.types.InterfaceManager.Companion.mapDispleeInterface
import stan.qodat.cache.impl.displee.types.InterfaceManager.Companion.mapDispleeRootInterfaces
import stan.qodat.cache.impl.displee.types.InterfaceManager.Companion.widgetId
import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterfaceManagerMappingTest {

    @Test
    fun getInterfaceGroupNamesWidgetsByArchiveAndFile() {
        val groupId = 321
        val childId = 4
        val packed = widgetId(groupId, childId)
        assertEquals(CacheIdPackingTest.packWidgetId(groupId, childId), packed)
        assertEquals((groupId shl 16) + childId, packed)
        assertEquals(groupId, packed shr 16)
        assertEquals(childId, packed and 0xFFFF)
        assertEquals(groupId.toString(), interfaceGroupName(groupId))
    }

    @Test
    fun getInterfaceGroupReturnsTheArchiveSlot() {
        val groups = arrayOfNulls<Array<InterfaceDefinition?>?>(3)
        val child = InterfaceDefinition().apply { id = CacheIdPackingTest.packWidgetId(1, 0) }
        groups[1] = arrayOf(child)
        assertEquals(child, getInterfaceGroup(groups, 1)!![0])
        assertEquals(null, getInterfaceGroup(groups, 0))
        assertEquals(null, getInterfaceGroup(groups, 2))
    }

    @Test
    fun displeeGetInterfaceMapsNonNullWidgetsAndEmptyMissingGroups() {
        val child = InterfaceDefinition().apply {
            id = CacheIdPackingTest.packWidgetId(12, 3)
            name = "icon"
        }
        val group = arrayOf<InterfaceDefinition?>(null, child)
        val mapped = mapDispleeInterface(group)
        assertEquals(1, mapped.size)
        assertEquals(child.id, mapped[0].id)
        assertEquals("icon", mapped[0].name)
        assertTrue(mapDispleeInterface(null).isEmpty())
    }

    @Test
    fun rootInterfaceGroupsSkipNullAndAllNullArchives() {
        val keep = InterfaceDefinition().apply { id = CacheIdPackingTest.packWidgetId(2, 1) }
        val raw = arrayOf(
            null,
            arrayOf<InterfaceDefinition?>(null, null),
            arrayOf<InterfaceDefinition?>(keep, null),
        )
        val groups = mapDispleeRootInterfaces(raw)
        assertEquals(setOf(2), groups.keys)
        assertEquals(1, groups.getValue(2).size)
        assertEquals(keep.id, groups.getValue(2)[0].id)
    }

    @Test
    fun emptyRootInterfaceMapMatchesDispleeErrorCondition() {
        assertTrue(mapDispleeRootInterfaces(emptyArray()).isEmpty())
        assertTrue(mapDispleeRootInterfaces(arrayOf(null, arrayOf(null))).isEmpty())
    }
}
