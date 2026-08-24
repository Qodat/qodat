package stan.qodat.scene.runescape.ui

import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InterfaceNamesTest {

    @Test
    fun prefersWidgetNameOverGenericAndTags() {
        val defs = listOf(
            iface { name = "Close"; text = "Close" },
            iface { name = "<col=ff9040>Bank</col>" },
        )
        assertEquals("Bank", InterfaceNames.derive(defs))
        assertEquals("12  Bank", InterfaceNames.display(12, defs))
    }

    @Test
    fun fallsBackToTitleTextThenId() {
        val titled = listOf(iface { type = 4; text = "Grand Exchange" })
        assertEquals("Grand Exchange", InterfaceNames.derive(titled))
        assertNull(InterfaceNames.derive(listOf(iface { type = 4; text = "%1" })))
        assertEquals("7", InterfaceNames.display(7, emptyList()))
    }

    private fun iface(init: InterfaceDefinition.() -> Unit) =
        InterfaceDefinition().also(init)
}
