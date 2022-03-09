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
        try {
            val cacheChooserLoader = FXMLLoader(Qodat::class.java.getResource("cachechooser.fxml"))
            val root = cacheChooserLoader.load<AnchorPane>()

            dialogPane.content = root
            dialogPane.stylesheets.add(root.stylesheets.first())
            dialogPane.styleClass.add("myDialog")
            dialogPane.buttonTypes.add(ButtonType.OK)
            dialogPane
                .lookupButton(ButtonType.OK)
                .disableProperty()
                .bind(CacheChooserController.disableOkButtonProperty)

            val controller: CacheChooserController = cacheChooserLoader.getController()
            setResultConverter {
                when(it) {
                    ButtonType.OK -> {
                        val rootDir = controller.rootDirChooser.pathProperty.get()
                        val cacheDir = controller.osrsCacheDirChooser.pathProperty.get()
                        rootDir to cacheDir
                    }
                    else -> throw Exception("Unexpected button type {$it}")
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}