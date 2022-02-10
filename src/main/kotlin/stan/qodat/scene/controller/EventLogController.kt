package stan.qodat.scene.controller

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ToggleButton
import javafx.scene.control.TreeView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.paint.Paint
import javafx.scene.text.TextFlow
import javafx.util.Callback
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.control.tree.ExceptionTreeItem
import stan.qodat.util.LOG_ERROR
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
class EventLogController : Initializable {

    @FXML lateinit var exceptionListView: ListView<TitledThrowable>

    private val exceptions = FXCollections.observableArrayList<TitledThrowable>()

    private var lastTitle = AtomicReference<String>()

    private val color = SimpleObjectProperty<Paint>()

    fun add(title: String, throwable: Throwable) {

        color.set(LOG_ERROR)

        if (lastTitle.get() == title)
            return

        lastTitle.set(title)

        val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        JavaFXExecutor.execute {
            exceptions.add(TitledThrowable(time, title, throwable))
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        exceptionListView.items = exceptions
        exceptionListView.cellFactory = Callback {
            val listCell = object : ListCell<TitledThrowable>() {
                override fun updateItem(item: TitledThrowable?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item != null) {
                        graphic = TreeView<TextFlow>().apply {
                            val exceptionTreeItem = ExceptionTreeItem(item)
                            root = exceptionTreeItem
                            val cellHeight = 30.0
                            prefHeight = cellHeight
                            expandedItemCountProperty().addListener { _, _, newValue ->
                                Platform.runLater {
                                    prefHeight =  (newValue.toInt() * cellHeight)
                                }
                            }
                            setOnDragDetected {
                                val dragboard = startDragAndDrop(TransferMode.COPY)
                                val clipboardContent = ClipboardContent()
                                clipboardContent.putString(Json.encodeToString(item))
                                dragboard.setContent(clipboardContent)
                                it.consume()
                            }
                            setOnDragExited {
                                it.consume()
                            }
                            setOnDragDone {
                                it.consume()
                            }
                        }
                    }
                }
            }
            listCell
        }
    }

    fun bind(bottomEventLogTab: ToggleButton) {
        val defaultColor = bottomEventLogTab.textFill
        color.set(defaultColor)
        bottomEventLogTab.textFillProperty().bindBidirectional(color)
        bottomEventLogTab.selectedProperty().addListener { _, _, newValue ->
            if (newValue)
                color.set(defaultColor)
        }
    }

    @Serializable
    data class TitledThrowable(
        val date: LocalDateTime,
        val title: String,
        @Serializable(with = TimeStampSurrogateSerializer::class)
        val throwable: Throwable
    )

    @Serializable
    @SerialName("Throwable")
    data class ThrowableSurrogate(val message: String, val stackTrace: Array<String>)

    object TimeStampSurrogateSerializer : KSerializer<Throwable> {
        override val descriptor: SerialDescriptor = ThrowableSurrogate.serializer().descriptor
        override fun serialize(encoder: Encoder, value: Throwable) {
            val surrogate = ThrowableSurrogate(value.message?:"???", value.stackTrace.map {
                "${it.className}:${it.methodName}(${it.fileName}:${it.lineNumber})"
            }.toTypedArray())
            encoder.encodeSerializableValue(ThrowableSurrogate.serializer(), surrogate)
        }
        override fun deserialize(decoder: Decoder): Throwable {
            TODO("Not yet implemented")
        }
    }
}
