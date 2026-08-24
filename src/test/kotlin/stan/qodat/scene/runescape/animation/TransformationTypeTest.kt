package stan.qodat.scene.runescape.animation

import kotlin.test.Test
import kotlin.test.assertEquals

class TransformationTypeTest {

    @Test
    fun getMapsOrdinalsAndFallsBackToUndefined() {
        assertEquals(TransformationType.SET_OFFSET, TransformationType.get(0))
        assertEquals(TransformationType.TRANSLATE, TransformationType.get(1))
        assertEquals(TransformationType.ROTATE, TransformationType.get(2))
        assertEquals(TransformationType.SCALE, TransformationType.get(3))
        assertEquals(TransformationType.TRANSPARENCY, TransformationType.get(4))
        assertEquals(TransformationType.UNDEFINED, TransformationType.get(5))
        assertEquals(TransformationType.UNDEFINED, TransformationType.get(-1))
        assertEquals(TransformationType.UNDEFINED, TransformationType.get(99))
    }
}
