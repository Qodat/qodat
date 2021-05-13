package stan.qodat

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import javafx.stage.StageStyle
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.controller.MainController
import stan.qodat.util.Action
import stan.qodat.util.ActionCache
import stan.qodat.util.SessionManager
import java.util.concurrent.Executors

/**
 * Entry point of application.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class Qodat : Application() {

    override fun start(primaryStage: Stage) {

        sessionManager.loadFromFile()

        Properties.bind(sessionManager)

        SubScene3D.init()

        val mainLoader = FXMLLoader(Qodat::class.java.getResource("main.fxml"))
        val root = mainLoader.load<Parent>()
        mainController = mainLoader.getController()

        Properties.cache.set(OldschoolCacheRuneLite)

        primaryStage.title = "Qodat"
        primaryStage.icons.add(Image(Qodat::class.java.getResourceAsStream("images/icon.png")))
        primaryStage.scene = Scene(root, Properties.sceneInitialWidth.get(), Properties.sceneInitialHeight.get()).also {
            Properties.sceneInitialWidth.bind(it.widthProperty())
            Properties.sceneInitialHeight.bind(it.heightProperty())
        }
        primaryStage.scene.setOnKeyPressed {
            if(it.code == KeyCode.Z && it.isControlDown){
                if(it.isShiftDown)
                    actionCache.redoLast()
                else
                    actionCache.undoLast()
                it.consume()
            }
        }
        primaryStage.initStyle(StageStyle.DECORATED)
        primaryStage.show()
        primaryStage.setOnCloseRequest {
            sessionManager.saveToFile()
        }
    }

    companion object {

        private val sessionManager = SessionManager()

        private val actionCache = ActionCache()

        val executor = Executors.newSingleThreadExecutor()

        lateinit var mainController : MainController

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Qodat::class.java, *args)
        }

        fun addAction(action: Action) {
            actionCache.cache(action)
        }
    }
}