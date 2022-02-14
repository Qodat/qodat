package stan.qodat.scene.dialog

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
            val cacheChooserController: CacheChooserController = cacheChooserLoader.getController()

            dialogPane.content = root
            dialogPane.stylesheets.add(root.stylesheets.first())
            dialogPane.styleClass.add("myDialog")

            setResultConverter {
                when(it) {
                    ButtonType.OK -> {
                        val rootDir = cacheChooserController.rootDirChooser.pathProperty.get()
                        val cacheDir = cacheChooserController.osrsCacheDirChooser.pathProperty.get()
                        rootDir to cacheDir
                    }
                    else -> throw Exception("Unexpected button type {$it}")
                }
            }

            dialogPane.buttonTypes.add(ButtonType.OK)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}