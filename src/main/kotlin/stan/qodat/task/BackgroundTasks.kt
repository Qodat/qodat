package stan.qodat.task

import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ProgressBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.slf4j.LoggerFactory
import stan.qodat.Qodat
import stan.qodat.util.DEFAULT
import stan.qodat.util.LOG_ERROR
import java.awt.Desktop
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.isRegularFile

object BackgroundTasks {

    private val logger = LoggerFactory.getLogger(BackgroundTasks::class.java)

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
        logger.info("Starting task {}", task.title)

        if (addProgressIndicator) {
            val stackPane = StackPane()
            GlobalScope.launch(Dispatchers.JavaFx) {

                val progressPane = ProgressIndicatorPane()
                progressPane.bindPrefWidth(mainPane)
                progressPane.bind(task)
                stackPane.children.add(progressPane)

                progressBox.children.add(stackPane)
                withContext(Dispatchers.Default) {
                    try {
                        task.run()
                        GlobalScope.launch(Dispatchers.IO) {
                            when (val result  = task.get(240, TimeUnit.SECONDS)) {
                                is ExportTaskResult.Success ->
                                    showOpenFileOption(result.saveDir, progressBox)
                                is Path ->
                                    showOpenFileOption(result, progressBox)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to execute task {}", task, e)
                    } finally {
                        withContext(Dispatchers.JavaFx) {
                            progressBox.children.remove(stackPane)
                        }
                    }
                }
            }
        } else {
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    task.run()
                } catch (e: Exception) {
                    logger.error("Failed to execute task {}", task, e)
                }
            }
        }
    }

    private suspend fun showOpenFileOption(result: Path, progressBox: HBox) {
        withContext(Dispatchers.JavaFx) {
            val openPathBox = HBox().apply {
                alignment = Pos.CENTER
                spacing = 10.0
            }

            openPathBox.children.add(Text(result.toString()).apply {
                textAlignment = TextAlignment.CENTER
                fontSmoothingType = FontSmoothingType.GRAY
                font = Font.font("menlo", 13.0)
                fill = DEFAULT
            })

            val buttonBox = HBox().apply {
                alignment = Pos.CENTER
                children.add(Button("Open ${if (result.isRegularFile()) "file" else "directory"}").apply {
                    setOnAction {
                        Desktop.getDesktop().open(result.toFile())
                    }
                })
                children.add(Button("Open enclosing folder").apply {
                    setOnAction {
                        Desktop.getDesktop().open(result.parent.toFile())
                    }
                })
            }
            openPathBox.children.add(buttonBox)

            val closeButton = Button("Dismiss").apply {
                textFill = LOG_ERROR
                setOnAction {
                    progressBox.children.remove(openPathBox)
                }
            }
            openPathBox.children.add(closeButton)

            progressBox.children.add(openPathBox)

            delay(10_000L)
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