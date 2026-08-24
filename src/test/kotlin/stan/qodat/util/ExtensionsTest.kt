package stan.qodat.util

import javafx.beans.property.SimpleStringProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExtensionsTest {

    @Test
    fun filterAndMapKeepsOnlyTheRequestedRuntimeType() {
        val mixed: List<Any?> = listOf(1, "a", 2, "b", null, 3L)
        assertEquals(listOf("a", "b"), mixed.filterAndMap<String>())
        assertEquals(listOf(1, 2), mixed.filterAndMap<Int>())
        assertEquals(listOf(3L), mixed.filterAndMap<Long>())
        val numbers = mixed.filterAndMap<Number>()
        assertEquals(3, numbers.size)
        assertEquals(1, numbers[0])
        assertEquals(2, numbers[1])
        assertEquals(3L, numbers[2])
        assertEquals(emptyList(), mixed.filterAndMap<Boolean>())
        assertEquals(emptyList(), emptyList<Any>().filterAndMap<String>())
    }

    @Test
    fun setAndBindCopiesThenFollowsTheSource() {
        val source = SimpleStringProperty("a")
        val dest = SimpleStringProperty("ignored")
        dest.setAndBind(source)
        assertEquals("a", dest.value)

        source.value = "b"
        assertEquals("b", dest.value)
        assertFailsWith<RuntimeException> { dest.value = "nope" }
    }

    @Test
    fun setAndBindBidirectionalKeepsBothSidesInSync() {
        val left = SimpleStringProperty("x")
        val right = SimpleStringProperty("y")
        left.setAndBind(right, biDirectional = true)
        assertEquals("y", left.value)

        left.value = "z"
        assertEquals("z", right.value)
        right.value = "w"
        assertEquals("w", left.value)
    }

    @Test
    fun unbindBidirectionalSafelyIgnoresWhenNotPaired() {
        val left = SimpleStringProperty("1")
        val right = SimpleStringProperty("2")
        left.unbindBidirectionalSafely(right)
        assertEquals("1", left.value)
        assertEquals("2", right.value)
    }

    @Test
    fun unbindBidirectionalSafelyStopsALivePairing() {
        val left = SimpleStringProperty("1")
        val right = SimpleStringProperty("2")
        left.bindBidirectional(right)
        left.unbindBidirectionalSafely(right)
        left.value = "3"
        assertEquals("3", left.value)
        assertEquals("2", right.value)
        left.unbindBidirectionalSafely(right)
    }
}
