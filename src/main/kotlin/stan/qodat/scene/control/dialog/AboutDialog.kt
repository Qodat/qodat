package stan.qodat.scene.control.dialog

import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.Window
import stan.qodat.AppVersion
import stan.qodat.Qodat

class AboutDialog : Dialog<ButtonType>() {

    init {
        title = "About Qodat"
        headerText = null
        graphic = null
        isResizable = false
        dialogPane.header = null
        dialogPane.graphic = null
        dialogPane.content = createContent()
        dialogPane.prefWidth = 380.0
        dialogPane.buttonTypes.setAll(ButtonType.CLOSE)
        dialogPane.styleClass.addAll("myDialog", "about-dialog")
        Qodat::class.java.getResource("style.css")?.toExternalForm()?.let {
            dialogPane.stylesheets.add(it)
        }
        setResultConverter { it }
        if (Qodat.isStageInitialized()) {
            attachTo(Qodat.stage)
        }
    }

    fun attachTo(window: Window) {
        FxDialogs.attachTo(this, window)
    }

    fun present() {
        result = null
        showAndWait()
    }

    companion object {
        fun createContent(): VBox {
            val root = VBox(10.0).apply {
                alignment = Pos.CENTER
                styleClass += "about-root"
            }
            iconView()?.let { root.children += it }
            root.children += Label("Qodat").apply { styleClass += "about-title" }
            root.children += Label(AppVersion.value).apply { styleClass += "about-version" }
            root.children += Label(
                "A RuneScape cache explorer for models, animations, and interfaces."
            ).apply {
                styleClass += "about-copy"
                isWrapText = true
                maxWidth = 320.0
            }
            return root
        }

        private fun iconView(): ImageView? {
            val stream = Qodat::class.java.getResourceAsStream("images/icon.png") ?: return null
            return stream.use { input ->
                ImageView(Image(input)).apply {
                    fitWidth = 48.0
                    fitHeight = 48.0
                    isPreserveRatio = true
                    isSmooth = true
                }
            }
        }
    }
}
