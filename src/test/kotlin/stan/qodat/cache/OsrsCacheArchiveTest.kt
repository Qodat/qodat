package stan.qodat.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OsrsCacheArchiveTest {

    @Test
    fun listsFromTheSameRunestatsUrlTheDesktopChooserUses() {
        assertEquals("https://archive.runestats.com/osrs", OsrsCacheArchive.BASE_URL)
        assertEquals("qodat", OsrsCacheArchive.USER_AGENT)
        assertTrue(OsrsCacheArchive.ARCHIVE_NAME_FILE.endsWith(".txt"))
    }
}