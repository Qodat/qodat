package stan.qodat

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.serialization.ExperimentalSerializationApi
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.controller.MainController
import stan.qodat.util.ActionCache
import stan.qodat.util.PropertiesManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Entry point of application.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class Qodat : Application() {

    override fun start(primaryStage: Stage) {

        stage = primaryStage
        propertiesManager.loadFromFile()

        Properties.bind(propertiesManager)

        SubScene3D.init()

        val mainLoader = FXMLLoader(Qodat::class.java.getResource("main.fxml"))
        val root = mainLoader.load<Parent>()
        mainController = mainLoader.getController()

        Properties.viewerCache.set(OldschoolCacheRuneLite)
        Properties.editorCache.set(QodatCache)

        primaryStage.apply {
            title = "Qodat"
            icons.add(Image(Qodat::class.java.getResourceAsStream("images/icon.png")))
            scene = Scene(root, Properties.sceneInitialWidth.get(), Properties.sceneInitialHeight.get()).apply {
                Properties.sceneInitialWidth.bind(widthProperty())
                Properties.sceneInitialHeight.bind(heightProperty())
                setOnKeyPressed {
                    if(it.code == KeyCode.Z && it.isControlDown){
                        if(it.isShiftDown)
                            ActionCache.redoLast()
                        else
                            ActionCache.undoLast()
                        it.consume()
                    }
                }
            }
            initStyle(StageStyle.DECORATED)
            show()
            setOnCloseRequest {
                propertiesManager.saveToFile()
            }
        }
    }

    companion object {

        /**
         * Handles the serialisation of [Properties].
         */
        private val propertiesManager = PropertiesManager()

        /**
         * Used to offload tasks to a different single-thread.
         */
        val executor: ExecutorService = Executors.newSingleThreadScheduledExecutor()

        /**
         * The main FXML controller.
         */
        lateinit var mainController : MainController

        lateinit var stage: Stage

        /**
         * Starting point of the application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Qodat::class.java, *args)
        }
    }
}