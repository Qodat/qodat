package stan.qodat.scene.controller

import com.sun.nio.file.SensitivityWatchEventModifier
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.SnapshotParameters
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import stan.qodat.Qodat
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.SceneContext
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.onInvalidation
import stan.qodat.util.onItemSelected
import java.net.URL
import java.nio.file.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class ModelController : Initializable {

    @FXML lateinit var modelListView: ViewNodeListView<Model>
    @FXML private lateinit var searchModelField: TextField
    @FXML private lateinit var setLabel: MenuItem

    private var syncPath = SimpleObjectProperty<Path>()
    private var ignorePaths = ConcurrentLinkedDeque<Path>()

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

    fun enableDragAndDrop() {
        modelListView.enableDragAndDrop(
            toFile = { QodatCache.encode(this).file }, // currently only support json
            fromFile = { Model.fromFile(this) },
            onDropFrom = {
                val syncPath = syncPath.get()
                 if (syncPath != null) {
                     for ((file, _) in it) {
                         val savePath = syncPath.resolve(file.name)
                         file.copyTo(savePath.toFile())
                     }
                 } else {
                     for ((_, model) in it)
                         models.add(model)
                 }
            },
            imageProvider = {
                getSceneNode().snapshot(
                    SnapshotParameters().apply { fill = Color.TRANSPARENT },
                    null
                )
            },
            supportedExtensions = Model.supportedExtensions
        )
    }

    fun syncWith(pathProperty: ObjectProperty<Path>) {

        syncPath.bind(pathProperty)

        val currentPath = pathProperty.get().toFile().apply {
            if (!exists())
                mkdir()
        }
        val files = currentPath.listFiles()
        if (files != null) {
            for (file in files)
                models.add(Model.fromFile(file))
        }
        var pathKey = syncWithDirectory(currentPath.toPath())
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

        if (watchThread?.isAlive != true) {
            watchThread = Thread {
                while (true) {
                    var stop: Boolean
                    if (Qodat.shutDown) {
                        stop = true
                    } else {
                        val watchKey = watchService.poll() ?: continue
                        for (event in watchKey.pollEvents()) {
                            val context = event.context()
                            if (context is Path) {
                                val file = path.resolve(context).toFile()
                                val name = file.nameWithoutExtension
                                JavaFXExecutor.execute {
                                    when (event.kind()) {
                                        StandardWatchEventKinds.ENTRY_DELETE -> {
                                            models.removeIf { it.getName() == name }
                                        }
                                        StandardWatchEventKinds.ENTRY_CREATE -> {
                                            models.removeIf { it.getName() == name }
                                            models.add(Model.fromFile(file))
                                        }
                                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                                            models.removeIf { it.getName() == name }
                                            models.add(Model.fromFile(file))
                                        }
                                    }
                                }
                            }
                        }
                        stop = !watchKey.reset()
                        if (stop)
                            watchKey.cancel()
                    }

                    if (stop) {
                        watchService.close()
                        break
                    }
                }
            }.also {
                it.start()
            }
        }


        return pathKey
    }

    companion object {
        var watchThread: Thread? = null
    }
}