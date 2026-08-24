package stan.qodat.task

import javafx.application.Platform
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle
import javafx.scene.control.ProgressBar
import javafx.scene.control.Tooltip
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.slf4j.LoggerFactory
import stan.qodat.Qodat
import stan.qodat.task.export.ExportTaskResult
import stan.qodat.util.LOG_ERROR
import java.awt.Desktop
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isRegularFile

object BackgroundTasks {

    private val logger = LoggerFactory.getLogger(BackgroundTasks::class.java)
    private val active = AtomicInteger(0)
    private val busyProperty = SimpleBooleanProperty(false)

    val busy: ReadOnlyBooleanProperty = busyProperty

    private fun markBusy() {
        if (active.incrementAndGet() == 1)
            setBusy(true)
    }

    private fun markIdle() {
        if (active.decrementAndGet() <= 0) {
            active.set(0)
            setBusy(false)
        }
    }

    private fun setBusy(value: Boolean) {
        if (Platform.isFxApplicationThread())
            busyProperty.set(value)
        else
            Platform.runLater { busyProperty.set(value) }
    }

    fun submit(addProgressIndicator: Boolean, runnable: () -> Unit) {
        submit(addProgressIndicator, object : Task<Unit>() {
            override fun call() {
                runnable()
            }
        })
    }

    fun submit(addProgressIndicator: Boolean, vararg tasks: Task<*>) {

        val mainPane = Qodat.mainController.mainPane
        val progressBox = Qodat.mainController.progressSpace

        for (task in tasks)
            submit(task, addProgressIndicator, mainPane, progressBox)
    }

    private fun submit(
        task: Task<*>,
        addProgressIndicator: Boolean,
        mainPane: BorderPane,
        progressBox: HBox
    ) {
        val taskTitle = task.title
        logger.info("Starting task {}", taskTitle)
        markBusy()

        if (addProgressIndicator) {
            val stackPane = StackPane()
            CoroutineScope(Dispatchers.JavaFx).launch {

                val progressPane = ProgressIndicatorPane()
                progressPane.bindPrefWidth(mainPane)
                progressPane.bind(task)
                stackPane.children.add(progressPane)

                progressBox.children.add(stackPane)
                var saved: Path? = null
                try {
                    withContext(Dispatchers.Default) {
                        task.run()
                    }
                    val failure = task.exception
                    if (failure != null) {
                        logger.error("Failed to execute task {}", taskTitle, failure)
                        Qodat.logException("Failed to execute task $taskTitle", failure)
                    } else {
                        saved = when (val result = task.value) {
                            is ExportTaskResult.Success -> result.saveDir
                            is Path -> result
                            else -> null
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to execute task {}", taskTitle, e)
                    Qodat.logException("Failed to execute task $taskTitle", e)
                } finally {
                    progressBox.children.remove(stackPane)
                    markIdle()
                }
                saved?.let { showOpenFileOption(it, progressBox) }
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    task.run()
                } catch (e: Exception) {
                    logger.error("Failed to execute task {}", taskTitle, e)
                    Qodat.logException("Failed to execute task $taskTitle", e)
                } finally {
                    markIdle()
                }
            }
        }
    }

    private suspend fun showOpenFileOption(result: Path, progressBox: HBox) {
        withContext(Dispatchers.JavaFx) {
            val openPathBox = HBox().apply {
                alignment = Pos.CENTER_LEFT
                spacing = 8.0
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }

            val name = result.fileName?.toString() ?: result.toString()
            openPathBox.children.add(Label(name).apply {
                font = Font.font("menlo", 13.0)
                textOverrun = OverrunStyle.CENTER_ELLIPSIS
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
                tooltip = Tooltip(result.toString())
            })

            fun actionButton(text: String, action: () -> Unit) = Button(text).apply {
                minWidth = Button.USE_PREF_SIZE
                setOnAction { action() }
            }

            openPathBox.children.add(actionButton("Open ${if (result.isRegularFile()) "file" else "folder"}") {
                Desktop.getDesktop().open(result.toFile())
            })
            result.parent?.let { parent ->
                openPathBox.children.add(actionButton("Open folder") {
                    Desktop.getDesktop().open(parent.toFile())
                })
            }
            openPathBox.children.add(actionButton("Dismiss") {
                progressBox.children.remove(openPathBox)
            }.apply { textFill = LOG_ERROR })

            progressBox.children.add(openPathBox)

            delay(15_000L)
            if (progressBox.children.contains(openPathBox))
                progressBox.children.remove(openPathBox)
        }
    }

    class ProgressIndicatorPane : StackPane()  {

        private val progressLabel = Text().apply {
            fill = Color.rgb(100, 100, 100)
        }

        private val progressBar = ProgressBar().apply {
            prefWidthProperty().bind(widthProperty())
        }

        init {
            children.addAll(progressBar, progressLabel)
        }

        fun bindPrefWidth(mainPane: Pane) {
            progressBar.prefWidthProperty().bind(mainPane.widthProperty())
        }

        fun bind(task: Task<*>) {
            progressLabel.textProperty().bind(task.messageProperty())
            progressBar.progressProperty().bind(task.progressProperty())
        }
    }
}
