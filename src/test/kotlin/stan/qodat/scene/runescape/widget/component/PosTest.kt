package stan.qodat.scene.runescape.widget.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PosTest {

    @Test
    fun fromIdHorMapsAbsoluteAndProportionalSlots() {
        assertEquals(Pos.abs_left, Pos.fromIdHor(0))
        assertEquals(Pos.abs_centre, Pos.fromIdHor(1))
        assertEquals(Pos.abs_right, Pos.fromIdHor(2))
        assertEquals(Pos.proportion_left, Pos.fromIdHor(3))
        assertEquals(Pos.proportion_centre, Pos.fromIdHor(4))
        assertEquals(Pos.proportion_right, Pos.fromIdHor(5))
    }

    @Test
    fun fromIdVerMapsVerticalSlots() {
        assertEquals(Pos.abs_top, Pos.fromIdVer(0))
        assertEquals(Pos.abs_centre, Pos.fromIdVer(1))
        assertEquals(Pos.abs_bottom, Pos.fromIdVer(2))
        assertEquals(Pos.proportion_top, Pos.fromIdVer(3))
        assertEquals(Pos.proportion_centre, Pos.fromIdVer(4))
        assertEquals(Pos.proportion_bottom, Pos.fromIdVer(5))
    }

    @Test
    fun unknownPosIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Pos.fromIdHor(-1) }
        assertFailsWith<IllegalArgumentException> { Pos.fromIdHor(6) }
        assertFailsWith<IllegalArgumentException> { Pos.fromIdVer(9) }
    }
}
