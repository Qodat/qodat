package stan.qodat.scene.control.dialog

import javafx.beans.binding.Bindings
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
            val controller: CacheChooserController = cacheChooserLoader.getController()

            dialogPane.content = root
            dialogPane.stylesheets.add(root.stylesheets.first())
            dialogPane.styleClass.add("myDialog")

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

            dialogPane.buttonTypes.add(ButtonType.OK)
            dialogPane.lookupButton(ButtonType.OK).disableProperty()
                .bind(
                    Bindings.createBooleanBinding(
                        {
                            val path = controller.qodatCacheDirChooser.pathProperty.get()
                            path != null && path.toFile().let {
                                it.exists() && it.isDirectory && it.listFiles().isNotEmpty()
                            }
                        },
                        controller.qodatCacheDirChooser.pathProperty
                    )
                )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}