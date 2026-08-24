package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatNameTest {

    @Test
    fun stripsOnlyALeadingColorTag() {
        assertEquals("Goblin", named("<col=ff0000>Goblin</col>").formatName())
        assertEquals("Guard", named("<col=00ff00>Guard").formatName())
        assertEquals("Iron plate", named("<col=aabbcc>Iron plate<body>").formatName())
        assertEquals("a", named("<col=1>a<b>c").formatName())
        assertEquals("", named("<col=ffffff>").formatName())
        assertEquals("", named("<col=>").formatName())
        assertEquals("", named("<col=ff").formatName())
    }

    @Test
    fun leavesNamesThatDoNotStartWithAColorTag() {
        assertEquals("", named("").formatName())
        assertEquals("plain name", named("plain name").formatName())
        assertEquals("has <col=x>inside", named("has <col=x>inside").formatName())
        assertEquals(" <col=ff>Name", named(" <col=ff>Name").formatName())
        assertEquals("<COL=ff>Name", named("<COL=ff>Name").formatName())
        assertEquals("  padded  ", named("  padded  ").formatName())
    }

    @Test
    fun exportFileNameReplacesSpacesAfterFormatName() {
        assertEquals("abyssal_demon", named("abyssal demon").formatName().replace(" ", "_"))
        assertEquals("a__b", named("a  b").formatName().replace(" ", "_"))
        assertEquals("King_Black_Dragon", named("<col=ff0000>King Black Dragon</col>").formatName().replace(" ", "_"))
        assertEquals("keeps\ttab", named("keeps\ttab").formatName().replace(" ", "_"))
    }

    private fun named(name: String) = object : Searchable {
        override fun getName() = name
    }
}
