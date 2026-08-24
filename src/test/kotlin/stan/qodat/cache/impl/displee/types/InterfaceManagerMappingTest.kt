package stan.qodat.cache.impl.displee.types

import net.runelite.cache.definitions.InterfaceDefinition
import qodat.cache.definition.InterfaceDefinition as QodatInterfaceDefinition
import stan.qodat.cache.impl.displee.CacheIdPackingTest
import stan.qodat.cache.impl.oldschool.definition.RuneliteInterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterfaceManagerMappingTest {

    @Test
    fun getInterfaceGroupNamesWidgetsByArchiveAndFile() {
        val groupId = 321
        val childId = 4
        val widgetId = CacheIdPackingTest.packWidgetId(groupId, childId)
        assertEquals((groupId shl 16) + childId, widgetId)
        assertEquals(groupId, widgetId shr 16)
        assertEquals(childId, widgetId and 0xFFFF)
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

    @Test
    fun oldschoolRootInterfacesGroupByHighWidgetBits() {
        val a = InterfaceDefinition().apply { id = CacheIdPackingTest.packWidgetId(5, 0) }
        val b = InterfaceDefinition().apply { id = CacheIdPackingTest.packWidgetId(5, 1) }
        val c = InterfaceDefinition().apply { id = CacheIdPackingTest.packWidgetId(9, 0) }
        val raw = arrayOf(
            null,
            arrayOf(a, b),
            arrayOf(c),
        )
        val grouped = mapOldschoolRootInterfaces(raw)
        assertEquals(listOf(5, 9), grouped.keys.toList())
        assertEquals(2, grouped.getValue(5).size)
        assertEquals(1, grouped.getValue(9).size)
    }

    companion object {
        internal fun interfaceGroupName(groupId: Int): String = groupId.toString()

        internal fun getInterfaceGroup(
            interfaces: Array<Array<InterfaceDefinition?>?>,
            groupId: Int,
        ): Array<InterfaceDefinition?>? = interfaces[groupId]

        internal fun mapDispleeInterface(
            group: Array<InterfaceDefinition?>?,
        ): Array<QodatInterfaceDefinition> =
            group
                ?.mapNotNull { it?.let(::RuneliteInterfaceDefinition) }
                ?.toTypedArray()
                ?: emptyArray()

        internal fun mapDispleeRootInterfaces(
            raw: Array<Array<InterfaceDefinition?>?>,
        ): Map<Int, List<QodatInterfaceDefinition>> {
            val groups = LinkedHashMap<Int, List<QodatInterfaceDefinition>>()
            for (groupId in raw.indices) {
                val components = raw[groupId] ?: continue
                if (components.all { it == null }) continue
                groups[groupId] = components.mapNotNull { it?.let(::RuneliteInterfaceDefinition) }
            }
            return groups
        }

        internal fun mapOldschoolRootInterfaces(
            interfaces: Array<Array<InterfaceDefinition>?>,
        ): Map<Int, List<QodatInterfaceDefinition>> =
            interfaces
                .filterNotNull()
                .flatMap { components -> components.map { RuneliteInterfaceDefinition(it) } }
                .groupBy { it.id.shr(16) }
    }
}
