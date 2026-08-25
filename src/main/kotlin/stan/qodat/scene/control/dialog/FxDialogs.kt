package stan.qodat.scene.control.dialog

import javafx.application.Platform
import javafx.scene.control.Dialog
import javafx.stage.Modality
import javafx.stage.Window
import stan.qodat.Qodat
import stan.qodat.javafx.NativeWindowTheme

/**
 * Helpers for showing JavaFX dialogs without aborting the macOS nested run loop.
 *
 * Glass treats an uncaught Java exception during [Dialog.showAndWait] as
 * `NSGenericException` and kills the process. Menu actions (especially with
 * the system menu bar) must also leave the native menu callback before
 * entering that nested loop.
 */
object FxDialogs {

    fun runAfterCurrentEvent(block: () -> Unit) {
        Platform.runLater(block)
    }

    fun attachToMainWindow(dialog: Dialog<*>) {
        if (dialog.owner == null && Qodat.isStageInitialized())
            attachTo(dialog, Qodat.stage)
    }

    fun attachTo(dialog: Dialog<*>, window: Window) {
        if (dialog.owner != null) return
        dialog.initOwner(window)
        dialog.initModality(Modality.WINDOW_MODAL)
        NativeWindowTheme.applyTo(dialog)
    }
}
