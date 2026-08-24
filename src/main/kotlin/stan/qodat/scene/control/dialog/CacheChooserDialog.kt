package stan.qodat.scene.control.dialog

import javafx.fxml.FXMLLoader
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.AnchorPane
import stan.qodat.Qodat
import stan.qodat.scene.controller.CacheChooserController
import java.nio.file.Path

class CacheChooserDialog : Dialog<Pair<Path, Path>>() {

    init {
        title = "Change Cache"
        headerText = null
        try {
            val cacheChooserLoader = FXMLLoader(Qodat::class.java.getResource("cachechooser.fxml"))
            val root = cacheChooserLoader.load<AnchorPane>()

            dialogPane.content = root
            root.stylesheets.firstOrNull()?.let { dialogPane.stylesheets.add(it) }
            dialogPane.styleClass.add("myDialog")
            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            dialogPane
                .lookupButton(ButtonType.OK)
                .disableProperty()
                .bind(CacheChooserController.disableOkButtonProperty)

            val controller: CacheChooserController = cacheChooserLoader.getController()
            setResultConverter { button ->
                resultOf(
                    button,
                    controller.rootDirChooser.pathProperty.get(),
                    controller.osrsCacheDirChooser.pathProperty.get()
                )
            }
            FxDialogs.attachToMainWindow(this)
        } catch (e: Exception) {
            Qodat.logException("Failed to create CacheChooserDialog", e)
            setResultConverter { null }
        }
    }

    companion object {
        fun resultOf(button: ButtonType?, rootDir: Path?, cacheDir: Path?): Pair<Path, Path>? {
            if (button != ButtonType.OK || rootDir == null || cacheDir == null)
                return null
            return rootDir to cacheDir
        }
    }
}
