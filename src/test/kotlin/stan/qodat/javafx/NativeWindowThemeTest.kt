package stan.qodat.javafx

import javafx.application.ColorScheme
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.Pane
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeWindowThemeTest {

    init {
        JFXPanel()
    }

    @Test
    fun applyEarlyRequestsSystemAppearanceBeforeAwtStarts() {
        NativeWindowTheme.applyEarly()
        assertEquals(NativeWindowTheme.SYSTEM_APPEARANCE, System.getProperty(NativeWindowTheme.APPEARANCE_PROPERTY))
    }

    @Test
    fun applyToSceneUsesTheDarkColorScheme() {
        val scene = Scene(Pane())
        NativeWindowTheme.applyTo(scene)
        assertEquals(ColorScheme.DARK, scene.preferences.colorScheme)
    }
}
