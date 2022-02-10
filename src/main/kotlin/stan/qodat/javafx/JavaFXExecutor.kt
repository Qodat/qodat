package stan.qodat.javafx

import javafx.application.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.*


object JavaFXExecutor : Executor {

    private val logger = LoggerFactory.getLogger(JavaFXExecutor::class.java)

    override fun execute(command: Runnable) {
        kotlin.runCatching {
            runBlocking {
                withContext(Dispatchers.JavaFx) {
                    command.run()
                }
            }
        }.apply {
            onFailure {
                logger.error("Failed to execute command with JavaFX dispatcher", it)
            }
        }
    }
}