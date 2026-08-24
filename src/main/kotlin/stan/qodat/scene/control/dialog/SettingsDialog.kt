package stan.qodat.scene.control.dialog

import javafx.fxml.FXMLLoader
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Window
import stan.qodat.Qodat
import stan.qodat.util.runCatchingWithDialog

class SettingsDialog : Dialog<ButtonType>() {

    init {
        runCatchingWithDialog(activityName = "Creating SettingsDialog") {
            val loader = FXMLLoader(Qodat::class.java.getResource("settings.fxml"))
            val root = loader.load<VBox>()

            title = "Settings"
            headerText = null
            isResizable = true
            dialogPane.content = root
            dialogPane.prefWidth = 360.0
            dialogPane.buttonTypes.add(ButtonType.CLOSE)
            val style = root.stylesheets.firstOrNull()
                ?: Qodat::class.java.getResource("style.css")?.toExternalForm()
            style?.let { dialogPane.stylesheets.add(it) }
            dialogPane.styleClass.addAll("myDialog", "settings-dialog")
            if (Qodat.isStageInitialized()) {
                attachTo(Qodat.stage)
            }
        }.onFailure {
            Qodat.logException("Failed to create SettingsDialog", it)
        }
    }

    fun attachTo(window: Window) {
        if (owner == null) {
            initOwner(window)
            initModality(Modality.WINDOW_MODAL)
        }
    }
}
