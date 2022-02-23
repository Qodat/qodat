package stan.qodat.scene.controller

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.SceneAntialiasing
import javafx.scene.control.CheckBox
import javafx.scene.control.ColorPicker
import javafx.scene.control.TitledPane
import stan.qodat.Properties
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class SettingsController : Initializable {

    @FXML lateinit var root: TitledPane
    @FXML lateinit var showAxisCheckBox : CheckBox
    @FXML lateinit var msaaCheckBox: CheckBox
    @FXML lateinit var fpsCheckBox: CheckBox
    @FXML lateinit var invertCameraCheckBox: CheckBox
    @FXML lateinit var renderTexturesCheckBox: CheckBox
    @FXML lateinit var backgroundColorPicker: ColorPicker
    @FXML lateinit var ambientLightColorPicker: ColorPicker

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        msaaCheckBox.selectedProperty().set(Properties.antialiasing.get() == SceneAntialiasing.BALANCED)
        msaaCheckBox.selectedProperty().addListener { _, _, newValue ->
            if (newValue)
                Properties.antialiasing.set(SceneAntialiasing.BALANCED)
            else
                Properties.antialiasing.set(SceneAntialiasing.DISABLED)
        }
        showAxisCheckBox.selectedProperty().setAndBind(Properties.showAxis, true)
        fpsCheckBox.selectedProperty().setAndBind(Properties.showFPS, true)
        invertCameraCheckBox.selectedProperty().setAndBind(Properties.cameraInvert, true)
        renderTexturesCheckBox.selectedProperty().setAndBind(Properties.alwaysRenderUsingAtlas, true)
        backgroundColorPicker.valueProperty().setAndBind(Properties.subSceneBackgroundColor, true)
        ambientLightColorPicker.valueProperty().setAndBind(Properties.ambientLightColor, true)
    }
}