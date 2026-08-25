package stan.qodat.javafx

import javafx.application.ColorScheme
import javafx.scene.Scene
import javafx.scene.control.Dialog

/**
 * Keeps native window chrome (the macOS title bar) in sync with Qodat's dark UI.
 *
 * macOS only opts a process into Dark Aqua if
 * `apple.awt.application.appearance` is set before the AWT toolkit starts.
 * JavaFX 26 then paints the title bar from [Scene.Preferences.colorScheme].
 */
object NativeWindowTheme {

    const val APPEARANCE_PROPERTY = "apple.awt.application.appearance"
    const val SYSTEM_APPEARANCE = "system"

    fun applyEarly() {
        System.setProperty(APPEARANCE_PROPERTY, SYSTEM_APPEARANCE)
    }

    fun applyTo(scene: Scene) {
        scene.preferences.colorScheme = ColorScheme.DARK
    }

    fun applyTo(dialog: Dialog<*>) {
        dialog.dialogPane.scene?.let { applyTo(it) }
        dialog.setOnShown {
            dialog.dialogPane.scene?.let { applyTo(it) }
        }
    }
}
