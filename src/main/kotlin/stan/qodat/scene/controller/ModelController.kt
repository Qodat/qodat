package stan.qodat.scene.controller

import com.sun.javafx.application.PlatformImpl
import com.sun.nio.file.SensitivityWatchEventModifier
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import stan.qodat.Properties
import stan.qodat.scene.SceneContext
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.Searchable
import stan.qodat.util.onInvalidation
import stan.qodat.util.onItemSelected
import java.io.File
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ModelController : Initializable {

    @FXML private lateinit var modelListView: ViewNodeListView<Model>
    @FXML private lateinit var searchModelField: TextField
    @FXML private lateinit var setLabel: MenuItem

    val models: ObservableList<Model> = FXCollections.observableArrayList()
    lateinit var filteredModels: FilteredList<Model>
    private var sceneContextProperty = SimpleObjectProperty<SceneContext>()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        VBox.setVgrow(modelListView, Priority.ALWAYS)
        filteredModels = FilteredList(models) { true }
        modelListView.items = filteredModels
        modelListView.apply {
            onItemSelected { old, new ->
                val sceneContext = sceneContextProperty.get()
                if (sceneContext != null) {
                    if (old != null)
                        sceneContext.removeNode(old)
                    if (new != null)
                        sceneContext.addNode(new)
                }
            }
        }
    }

    fun bind(sceneContext: SceneContext) {
        sceneContextProperty.set(sceneContext)
    }

    fun syncWith(pathProperty: ObjectProperty<Path>) {
        val currentPath = pathProperty.get()
        for (file in currentPath.toFile().listFiles()) {
            models.add(Model.fromFile(file))
        }
        var pathKey = syncWithDirectory(currentPath)
        pathProperty.onInvalidation {
            pathKey.cancel()
            pathKey = syncWithDirectory(get())
        }
    }

    private fun syncWithDirectory(path: Path) : WatchKey {
        val watchService = FileSystems.getDefault().newWatchService()

        val pathKey = path.register(
            watchService,
            arrayOf(
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            ),
            SensitivityWatchEventModifier.HIGH
        )

        Thread {
            while(true) {
                val watchKey = watchService.poll()?:continue
                for (event in watchKey.pollEvents()) {
                    val context = event.context()
                    if (context is Path) {
                        val file = path.resolve(context).toFile()
                        val name = file.nameWithoutExtension
                        PlatformImpl.runAndWait {
                            when (event.kind()) {
                                StandardWatchEventKinds.ENTRY_DELETE -> models.removeIf { it.getName() == name }
                                StandardWatchEventKinds.ENTRY_CREATE -> models.add(Model.fromFile(file))
                                StandardWatchEventKinds.ENTRY_MODIFY -> {
                                    models.removeIf { it.getName() == name }
                                    models.add(Model.fromFile(file))
                                }
                            }
                        }
                    }
                }

                if (!watchKey.reset()) {
                    watchKey.cancel()
                    watchService.close()
                    break
                }
            }
        }.start()

        return pathKey
    }
}