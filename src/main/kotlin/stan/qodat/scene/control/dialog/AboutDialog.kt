package stan.qodat.scene.control.dialog

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Window
import stan.qodat.Qodat

class AboutDialog : Alert(AlertType.INFORMATION) {

    init {
        title = "About Qodat"
        headerText = "Qodat"
        contentText = "A RuneScape cache explorer for models, animations, and interfaces."
        dialogPane.buttonTypes.setAll(ButtonType.CLOSE)
        dialogPane.styleClass.add("myDialog")
        Qodat::class.java.getResource("style.css")?.toExternalForm()?.let {
            dialogPane.stylesheets.add(it)
        }
        if (Qodat.isStageInitialized()) {
            initOwner(Qodat.stage)
        }
    }

    fun attachTo(window: Window) {
        if (owner == null) {
            initOwner(window)
        }
    }
}
