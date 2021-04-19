package stan.qodat.scene.controller

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.control.ScrollBar
import stan.qodat.Properties
import stan.qodat.scene.SubScene3D
import stan.qodat.util.setAndBind
import java.net.URL
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class NavigationController : Initializable {

    @FXML lateinit var zoomBar: ScrollBar
    @FXML lateinit var fpsLabel: Label

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        zoomBar.minProperty().setAndBind(Properties.cameraMinZoom)
        zoomBar.maxProperty().setAndBind(Properties.cameraMaxZoom)
        zoomBar.valueProperty().setAndBind(SubScene3D.cameraHandler.position.zProperty())

        fpsLabel.visibleProperty().setAndBind(Properties.showFPS)
        fpsLabel.textProperty().setAndBind(SubScene3D.animationPlayer.frameRateProperty)
    }
}