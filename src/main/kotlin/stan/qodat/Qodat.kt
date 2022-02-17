package stan.qodat

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.javafx.JavaFXTrayIcon
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.dialog.CacheChooserDialog
import stan.qodat.scene.controller.MainController
import stan.qodat.scene.controller.ModelController
import stan.qodat.util.ActionCache
import stan.qodat.util.PropertiesManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class Qodat : Application() {

    override fun start(primaryStage: Stage) {

        stage = primaryStage
        val loadedProperties = propertiesManager.loadFromFile()

        Properties.bind(propertiesManager)

        SubScene3D.init()

        loadMainController(primaryStage, loadedProperties)
    }

    private fun loadMainController(primaryStage: Stage, loadedProperties: Boolean) {
        val mainLoader = FXMLLoader(Qodat::class.java.getResource("main.fxml"))

        val root = mainLoader.load<Parent>()
        mainController = mainLoader.getController()

        primaryStage.apply {
            title = "Qodat"
            val icon = Image(Qodat::class.java.getResourceAsStream("images/icon.png"))
            icons.add(icon)
            SwingUtilities.invokeLater {
                JavaFXTrayIcon.addAppToTray(this, icon)
            }
            scene = Scene(root, Properties.sceneInitialWidth.get(), Properties.sceneInitialHeight.get()).apply {
                Properties.sceneInitialWidth.bind(widthProperty())
                Properties.sceneInitialHeight.bind(heightProperty())
                setOnKeyPressed {
                    if (it.code == KeyCode.Z && it.isControlDown) {
                        if (it.isShiftDown)
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
                logger.info("Received close request")
                try {
                    propertiesManager.saveToFile()
                    executor.shutdown()
                    ModelController.watchThread?.interrupt()
                    SwingUtilities.invokeLater {
                        JavaFXTrayIcon.close()
                    }
                } catch (e: Exception) {
                    logger.info("Failed to close Qodat gracefully", e)
                } finally {
                    Platform.exit()
                    exitProcess(0)
                }
            }
            if (!loadedProperties) {
                val dialog = CacheChooserDialog()
                dialog.showAndWait().ifPresent { (rootDir, cacheDir) ->
                    Properties.osrsCachePath.set(cacheDir)
                    Properties.rootPath.set(rootDir)
                    Properties.downloadsPath.set(rootDir.resolve("downloads"))
                    Properties.qodatCachePath.set(rootDir.resolve("caches/qodat"))
                    Properties.legacyCachePath.set(rootDir.resolve("cache/667"))
                    Properties.projectFilesPath.set(rootDir.resolve("data"))
                    Properties.defaultExportsPath.set(rootDir.resolve("exports"))

                    propertiesManager.saveToFile()
                }
            }
            Properties.viewerCache.set(OldschoolCacheRuneLite)
            Properties.editorCache.set(QodatCache)
            propertiesManager.startSaveThread()
        }
    }

    companion object {

        var shutDown = false

        val logger = LoggerFactory.getLogger(Qodat::class.java)

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
        lateinit var mainController: MainController

        lateinit var stage: Stage

        /**
         * Starting point of the application.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Qodat::class.java, *args)
        }

        /**
         * TODO: add safety checks, (are controllers initialised, are we on FX thread)
         */
        fun logException(title: String, e: Throwable) {
            logger.error(title, e)
//            Platform.runLater {
//                System.err.println(title + " - " + e.localizedMessage + " - \n" + e.stackTrace?.let {
//                    it.copyOfRange(0, 5.coerceAtMost(it.size))
//                }?.joinToString("\n"))
//                mainController.eventLogController.add(title, e)
//            }
        }
    }
}