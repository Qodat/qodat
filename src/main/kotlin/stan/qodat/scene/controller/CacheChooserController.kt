package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.OsrsCacheArchive
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.net.ssl.SSLHandshakeException


class CacheChooserController : Initializable {


    @FXML
    private lateinit var listCaches: ListView<String>
    private var entries: ObservableList<String> = FXCollections.observableArrayList()

    @FXML
    private lateinit var txtFilter: TextField

    @FXML
    private lateinit var btnDownload: Button

    @FXML
    private lateinit var lblStatusText: Label

    @FXML
    private lateinit var lblErrorText: Label

    @FXML
    lateinit var dirChoosersBox: VBox

    private var selectedCache: String? = null

    lateinit var osrsCacheDirChooser: DirChooserHBox
    lateinit var qodatCacheDirChooser: DirChooserHBox
    lateinit var projectFilesDirChooser: DirChooserHBox
    lateinit var exportsDirChooser: DirChooserHBox
    lateinit var rootDirChooser: DirChooserHBox
    lateinit var downloadDirChooser: DirChooserHBox


    override fun initialize(location: URL?, resources: ResourceBundle?) {

        osrsCacheDirChooser = DirChooserHBox(
            identifier = "OSRS Cache",
            property = Properties.osrsCachePath,
            lblErrorText = lblErrorText,
            disableOkButtonIfEmpty = true
        )
        rootDirChooser = DirChooserHBox(
            identifier = "Root",
            property = Properties.rootPath,
            lblErrorText = lblErrorText
        )
        downloadDirChooser = DirChooserHBox(
            identifier = ("Downloads"),
            property = Properties.downloadsPath,
            lblErrorText = lblErrorText, editable = false
        )
        projectFilesDirChooser = DirChooserHBox(
            identifier = "Project Files",
            property = Properties.projectFilesPath,
            lblErrorText = lblErrorText,
            editable = false,
        )
        exportsDirChooser = DirChooserHBox(
            identifier = "Exports",
            property = Properties.defaultExportsPath,
            lblErrorText = lblErrorText, editable = false
        )
        qodatCacheDirChooser = DirChooserHBox(
            identifier = "Qodat Cache",
            property = Properties.qodatCachePath,
            lblErrorText = lblErrorText,
            editable = false,
        )
        rootDirChooser.pathProperty.addListener { _, _, newValue ->
            qodatCacheDirChooser.field.text = newValue.resolve("caches/qodat").toString()
            projectFilesDirChooser.field.text = newValue.resolve("data").toString()
            exportsDirChooser.field.text = newValue.resolve("exports").toString()
        }
        dirChoosersBox.children.add(0, rootDirChooser)
        dirChoosersBox.children.add(1, downloadDirChooser)
        dirChoosersBox.children.add(2, projectFilesDirChooser)
        dirChoosersBox.children.add(3, exportsDirChooser)
        dirChoosersBox.children.add(4, qodatCacheDirChooser)
        dirChoosersBox.children.add(5, osrsCacheDirChooser)

        val listCachesPlaceholder = Label("No downloadable caches found.").apply {
            isWrapText = true
            textAlignment = TextAlignment.CENTER
        }
        listCaches.placeholder = listCachesPlaceholder

        Qodat.applicationScope.launch(Dispatchers.IO) {
            val result = runCatching { fetchRuneStatsCaches() }
            withContext(Dispatchers.JavaFx) {
                result.onSuccess { caches ->
                    entries.addAll(caches)
                }.onFailure { error ->
                    error.printStackTrace()
                    listCachesPlaceholder.text += "\n\n${error.message}"
                    if (error is SSLHandshakeException) {
                        listCachesPlaceholder.text +=
                            "\n\nSSLHandshakeException is a known bug with certain Java versions, try updating."
                    }
                    Qodat.logException("Failed to fetch caches", error)
                }
            }
        }

        val filterableEntries = FilteredList(entries)

        listCaches.items = filterableEntries

        listCaches.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedCache = newValue
                btnDownload.isDisable = false
            }
        }

        txtFilter.textProperty()
            .addListener { _: ObservableValue<out String>?, _: String?, newVal: String ->
                filterableEntries.setPredicate { obj ->
                    newVal.isEmpty() || obj.lowercase().contains(newVal.lowercase())
                }
            }


        btnDownload.setOnAction {
            btnDownload.isDisable = true
            downloadCache(selectedCache!!, osrsCacheDirChooser)
        }
    }

    private fun fetchRuneStatsCaches(): List<String> = OsrsCacheArchive.listArchiveNames()

    private fun downloadCache(cacheName: String, dirChooser: DirChooserHBox) {
        lblStatusText.isVisible = true
        lblStatusText.text = "Downloading cache $cacheName please wait.."
        dirChooser.field.text = ""
        Qodat.applicationScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = OsrsCacheArchive.download(
                    cacheName,
                    downloadDirChooser.pathProperty.get(),
                )
                withContext(Dispatchers.JavaFx) {
                    lblStatusText.isVisible = false
                    dirChooser.field.text = cacheDir.toAbsolutePath().toString()
                }
            } catch (e: Exception) {
                Qodat.logException("Failed to download cache $cacheName", e)
            }
        }
    }

    companion object {
        val disableOkButtonProperty = SimpleBooleanProperty(true)


    }

    class DirChooserHBox(
        identifier: String,
        property: ObjectProperty<Path>,
        lblErrorText: Label,
        editable: Boolean = true,
        disableOkButtonIfEmpty: Boolean = false,
    ) : VBox(5.0) {
        var pathProperty = SimpleObjectProperty(property.get())
        val field = TextField().apply {
            HBox.setHgrow(this, Priority.SOMETIMES)
            disableProperty().set(!editable)
            textProperty().addListener { _, _, newVal ->
                if (newVal != "") {
                    lblErrorText.isVisible = false
                    try {
                        val directory = Paths.get(newVal).toFile()
                        pathProperty.set(directory.toPath())
                        if (disableOkButtonIfEmpty)
                            checkIfValid(directory)
                    } catch (e: Exception) {
                        lblErrorText.text = e.message
                        lblErrorText.isVisible = true
                    }
                }
            }
            Platform.runLater {
                text = property.get().toString()
            }
        }

        private fun checkIfValid(directory: File) {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()?.takeIf { it.isNotEmpty() }
                if (files != null) {
                    disableOkButtonProperty.set(false)
                    field.style = ""
                    return
                }
            }
            field.style = "-fx-text-fill: #CC666E;"
            disableOkButtonProperty.set(true)
        }

        init {
            children.add(Label("$identifier Directory:"))
            children.add(HBox().apply {
                children.add(field)
                if (editable) {
                    children.add(Button("Browse").apply {
                        setOnAction {
                            val directoryChooser = DirectoryChooser()
                            val initDir = property.get().toFile()
                            initDir.mkdirs()
                            directoryChooser.initialDirectory = initDir
                            val owner = scene?.window
                                ?: if (Qodat.isStageInitialized()) Qodat.stage else null
                            val f = directoryChooser.showDialog(owner) ?: return@setOnAction
                            field.text = f.absolutePath
                        }
                    })
                }
            })
        }
    }
}
