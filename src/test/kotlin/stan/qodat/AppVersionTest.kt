package stan.qodat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun readsThePackagedVersionResource() {
        assertTrue(AppVersion.value.matches(Regex("""\d+\.\d+\.\d+.*""")))
        assertEquals("Qodat ${AppVersion.value}", AppVersion.windowTitle)
    }

    @Test
    fun parseReadsTheVersionProperty() {
        assertEquals("0.4.2", AppVersion.parse("version=0.4.2\n"))
        assertEquals("1.0.0-SNAPSHOT", AppVersion.parse("# comment\nversion=1.0.0-SNAPSHOT\n"))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("name=qodat\n"))
    }
}
