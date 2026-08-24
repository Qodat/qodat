package stan.qodat.cache.impl.displee.types

import qodat.cache.definition.ItemDefinition
import stan.qodat.cache.impl.oldschool.definition.ItemDefinition226
import java.util.OptionalInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemManagerMappingTest {

    @Test
    fun itemDefinition226ExposesIdNameAndInventoryModel() {
        val def = ItemDefinition226(4151).apply {
            name = "Abyssal whip"
            inventoryModel = 321
            recolorToFind = shortArrayOf(0x1111)
            recolorToReplace = shortArrayOf(0x2222)
        }

        assertEquals(OptionalInt.of(4151), def.getOptionalId())
        assertEquals("Abyssal whip", def.name)
        assertTrue(def.modelIds.contentEquals(arrayOf("321")))
    }

    @Test
    fun defaultNameIsTheNumericId() {
        val def = ItemDefinition226(99)
        assertEquals("99", def.name)
        assertTrue(def.modelIds.contentEquals(arrayOf("-1")))
    }

    @Test
    fun getItemsReturnsMappedDefinitions() {
        val whip = ItemDefinition226(4151).apply { name = "Abyssal whip" }
        val mapped = getItems(mapOf(4151 to whip))
        assertEquals(1, mapped.size)
        assertEquals(whip, mapped.single())
    }

    @Test
    fun getItemsReturnsEmptyWhenNothingWasLoaded() {
        assertTrue(getItems(emptyMap()).isEmpty())
    }

    @Test
    fun oldschoolItemMappingUsesInventoryModelAndRecolors() {
        val mapped = mapOldschoolItem(
            id = 11802,
            name = "Armadyl godsword",
            inventoryModel = 28000,
            colorFind = shortArrayOf(10, 11),
            colorReplace = shortArrayOf(20, 21),
        )
        assertEquals(OptionalInt.of(11802), mapped.getOptionalId())
        assertEquals("Armadyl godsword", mapped.name)
        assertTrue(mapped.modelIds.contentEquals(arrayOf("28000")))
        assertTrue(mapped.findColor!!.contentEquals(shortArrayOf(10, 11)))
        assertTrue(mapped.replaceColor!!.contentEquals(shortArrayOf(20, 21)))
    }

    companion object {
        internal fun getItems(items: Map<Int, ItemDefinition>): Array<ItemDefinition> =
            items.values.toTypedArray()

        internal fun mapOldschoolItem(
            id: Int,
            name: String,
            inventoryModel: Int,
            colorFind: ShortArray?,
            colorReplace: ShortArray?,
        ): ItemDefinition = object : ItemDefinition {
            override fun getOptionalId() = OptionalInt.of(id)
            override val name = name
            override val modelIds = arrayOf(inventoryModel.toString())
            override val findColor = colorFind
            override val replaceColor = colorReplace
        }
    }
}
