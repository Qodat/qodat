package stan.qodat.scene.runescape.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelViewModeTest {

    @Test
    fun diagnosticModesAreTheSkinAndPriorityOverlays() {
        assertFalse(ModelViewMode.FILL.isDiagnostic())
        assertFalse(ModelViewMode.LINE.isDiagnostic())
        assertTrue(ModelViewMode.VERTEX_SKIN.isDiagnostic())
        assertTrue(ModelViewMode.FACE_SKIN.isDiagnostic())
        assertTrue(ModelViewMode.PRIORITY.isDiagnostic())
    }

    @Test
    fun labelsMatchMenuText() {
        assertEquals("Fill", ModelViewMode.FILL.toString())
        assertEquals("Wireframe", ModelViewMode.LINE.toString())
        assertEquals("Vertex Skin", ModelViewMode.VERTEX_SKIN.toString())
        assertEquals("Face Skin", ModelViewMode.FACE_SKIN.toString())
        assertEquals("Priority", ModelViewMode.PRIORITY.toString())
    }
}
