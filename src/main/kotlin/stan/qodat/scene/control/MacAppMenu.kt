package stan.qodat.scene.control

import javafx.application.Platform
import java.awt.Desktop
import java.awt.desktop.QuitResponse

/**
 * Fills the native macOS application menu (the "Qodat" menu next to the Apple
 * menu) with About, Settings, and Quit. The in-window JavaFX [javafx.scene.control.MenuBar]
 * stays inside the stage.
 */
object MacAppMenu {

    fun install(actions: MainMenuBar.Actions) {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler { onFx(actions.showAbout) }
        }
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler { onFx(actions.openSettings) }
        }
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler { _, response ->
                onFx { quit(actions, response) }
            }
        }
    }

    private fun quit(actions: MainMenuBar.Actions, response: QuitResponse) {
        try {
            actions.quit()
            response.performQuit()
        } catch (e: Exception) {
            response.cancelQuit()
            throw e
        }
    }

    private fun onFx(block: () -> Unit) {
        if (Platform.isFxApplicationThread())
            block()
        else
            Platform.runLater(block)
    }
}
