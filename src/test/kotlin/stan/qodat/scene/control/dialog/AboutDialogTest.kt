package stan.qodat.scene.control.dialog

import javafx.embed.swing.JFXPanel
import javafx.scene.control.Label
import stan.qodat.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AboutDialogTest {

    init {
        JFXPanel()
    }

    @Test
    fun contentShowsNameVersionAndDescription() {
        val labels = AboutDialog.createContent().children.filterIsInstance<Label>()
        assertEquals("Qodat", labels[0].text)
        assertEquals(AppVersion.value, labels[1].text)
        assertTrue(labels[2].text.contains("cache explorer"))
        assertTrue(labels[0].styleClass.contains("about-title"))
        assertTrue(labels[1].styleClass.contains("about-version"))
        assertTrue(labels[2].isWrapText)
    }
}
