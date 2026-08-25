package stan.qodat.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SoftwareUpdatesTest {

    @Test
    fun gradleLaunchesAreNotPackaged() {
        assertNull(SoftwareUpdates.controller())
        assertEquals(SoftwareUpdates.Result.NotPackaged, SoftwareUpdates.check())
    }

    @Test
    fun triggerWithoutAPackageFailsClearly() {
        val error = runCatching { SoftwareUpdates.trigger() }.exceptionOrNull()
        assertIs<IllegalStateException>(error)
        assertEquals("Not running inside a Conveyor package.", error.message)
    }
}
