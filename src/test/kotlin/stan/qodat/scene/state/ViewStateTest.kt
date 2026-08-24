package stan.qodat.scene.state

import stan.qodat.util.Searchable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewStateTest {

    @Test
    fun namedIdentityIsEmptyWhenIdAndNameAreBlank() {
        assertTrue(NamedIdentity().isEmpty())
        assertTrue(NamedIdentity(id = "  ", name = "").isEmpty())
        assertFalse(NamedIdentity(id = "12").isEmpty())
        assertFalse(NamedIdentity(name = "Guard").isEmpty())
    }

    @Test
    fun findByIdentityPrefersIdThenName() {
        val items = listOf(Item("1", "alpha"), Item("2", "beta"), Item("3", "alpha"))
        assertEquals("1", items.findByIdentity(NamedIdentity(id = "1", name = "beta"), { it.id }, { it.name })?.id)
        assertEquals("2", items.findByIdentity(NamedIdentity(name = "beta"), { it.id }, { it.name })?.id)
        assertEquals("1", items.findByIdentity(NamedIdentity(name = "alpha"), { it.id }, { it.name })?.id)
        assertNull(items.findByIdentity(null, { it.id }, { it.name }))
        assertNull(items.findByIdentity(NamedIdentity(), { it.id }, { it.name }))
        assertNull(items.findByIdentity(NamedIdentity(id = "9", name = "missing"), { it.id }, { it.name }))
    }

    @Test
    fun searchableFindByIdentityUsesGetName() {
        val items = listOf(Named("10", "wolf"), Named("11", "bear"))
        assertEquals("bear", items.findByIdentity(NamedIdentity(name = "bear")) { it.id }?.getName())
        assertEquals("wolf", items.findByIdentity(NamedIdentity(id = "10", name = "bear")) { it.id }?.getName())
    }

    private data class Item(val id: String, val name: String)

    private data class Named(val id: String, private val label: String) : Searchable {
        override fun getName() = label
    }
}
