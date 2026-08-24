package stan.qodat.scene

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntitySelectionCoordinatorTest {

    @Test
    fun settleWindowIsShortEnoughToCoalesceKeyRepeat() {
        assertEquals(80.0, EntitySelectionCoordinator.SETTLE_MS)
        assertTrue(EntitySelectionCoordinator.SETTLE_MS > 0.0)
        assertTrue(EntitySelectionCoordinator.SETTLE_MS < 250.0)
    }
}
