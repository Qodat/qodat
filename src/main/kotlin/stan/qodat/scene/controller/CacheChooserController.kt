package stan.qodat.scene.controller

import javafx.beans.property.ObjectProperty
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jsoup.Jsoup
import stan.qodat.Properties
import java.io.*
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

        osrsCacheDirChooser = DirChooserHBox("OSRS Cache", Properties.osrsCachePath, lblErrorText)
        rootDirChooser = DirChooserHBox("Root", Properties.rootPath, lblErrorText)
        downloadDirChooser = DirChooserHBox(("Downloads"), Properties.downloadsPath, lblErrorText, editable = false)
        projectFilesDirChooser = DirChooserHBox("Project Files", Properties.projectFilesPath, lblErrorText, editable = false)
        exportsDirChooser = DirChooserHBox("Exports", Properties.defaultExportsPath, lblErrorText, editable = false)
        qodatCacheDirChooser = DirChooserHBox("Qodat Cache", Properties.qodatCachePath, lblErrorText, editable = false)
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

        try {
            val doc = Jsoup.connect(RUNESTATS_URL).get()
            entries.addAll(doc.select("a")
                .map { col -> col.attr("href") }
                .filter { it.length > 10 } // get rid of ../ and ./types
                .reversed()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            listCachesPlaceholder.text += "\n\n${e.message}"
            if (e is SSLHandshakeException) {
                listCachesPlaceholder.text += "\n\nSSLHandshakeException is a known bug with certain Java versions, try updating."
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
                    newVal.isEmpty() || obj.toString().toLowerCase().contains(newVal.toLowerCase())
                }
            }


        btnDownload.setOnAction {
            btnDownload.isDisable = true
            downloadCache(selectedCache!!, osrsCacheDirChooser)
        }
    }

    private fun downloadCache(cacheName: String, dirChooser: DirChooserHBox) {
        lblStatusText.isVisible = true
        lblStatusText.text = "Downloading cache $cacheName please wait.."
        dirChooser.field.text = ""
        val destFolder = downloadDirChooser
            .pathProperty.get()
            .resolve(cacheName.removeSuffix(".tar.gz"))
            .toFile()

        GlobalScope.launch {
            try {
                val conn = URL("$RUNESTATS_URL/$cacheName").openConnection()
                conn.addRequestProperty("User-Agent", "qodat")
                BufferedInputStream(withContext(Dispatchers.IO) {
                    conn.getInputStream()
                }).use { inputStream ->
                    val tarIn = TarArchiveInputStream(GzipCompressorInputStream(inputStream))
                    var tarEntry: TarArchiveEntry? = tarIn.nextTarEntry
                    while (tarEntry != null) {
                        val dest = File(destFolder, tarEntry.name)
                        if (tarEntry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.createNewFile()
                            val btoRead = ByteArray(1024)
                            val bout = BufferedOutputStream(FileOutputStream(dest))
                            var len: Int

                            while (tarIn.read(btoRead).also { len = it } != -1) {
                                bout.write(btoRead, 0, len)
                            }

                            bout.close()
                        }
                        tarEntry = tarIn.nextTarEntry
                    }
                    tarIn.close()
                    lblStatusText.isVisible = false
                    dirChooser.field.text = destFolder.resolve("cache").absolutePath.toString()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val RUNESTATS_URL = "https://archive.runestats.com/osrs"
    }

    class DirChooserHBox(identifier: String, property: ObjectProperty<Path>, lblErrorText: Label, editable: Boolean = true) : VBox(5.0) {
        var pathProperty = SimpleObjectProperty(property.get())
        val field = TextField().apply {
            HBox.setHgrow(this, Priority.SOMETIMES)
            text = property.get().toString()
            disableProperty().set(!editable)
            textProperty().addListener { _, _, newVal ->
                if (newVal != "") {
                    lblErrorText.isVisible = false
                    try {
                        pathProperty.set(Paths.get(newVal))
                    } catch (e: Exception) {
                        lblErrorText.text = e.message
                        lblErrorText.isVisible = true
                    }
                }
            }
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
                            val f = directoryChooser.showDialog(null) ?: return@setOnAction
                            field.text = f.absolutePath
                        }
                    })
                }
            })
        }
    }
}