package stan.qodat.scene

import kotlin.test.Test
import kotlin.test.assertEquals

class EntitySceneLayoutTest {

    @Test
    fun tileGridUsesClientSizedCellsWithQuarterTileGaps() {
        assertEquals(128.0, EntitySceneLayout.TILE_SIZE)
        assertEquals(EntitySceneLayout.TILE_SIZE / 4.0, EntitySceneLayout.GAP)
        assertEquals(32.0, EntitySceneLayout.GAP)
    }
}
