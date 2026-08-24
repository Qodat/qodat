package stan.qodat.javafx

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertTrue

class JavaFXExecutorTest {

    init {
        JFXPanel()
    }

    @Test
    fun executeRunsInlineOnFxThread() {
        val done = CountDownLatch(1)
        val ranOnCaller = AtomicBoolean(false)
        Platform.runLater {
            val fx = Thread.currentThread()
            JavaFXExecutor.execute {
                ranOnCaller.set(Thread.currentThread() === fx)
            }
            done.countDown()
        }
        assertTrue(done.await(5, TimeUnit.SECONDS), "FX task did not finish")
        assertTrue(ranOnCaller.get(), "execute must not runBlocking the FX thread")
    }

    @Test
    fun executeFromBackgroundRunsOnFxThread() {
        val done = CountDownLatch(1)
        val onFx = AtomicBoolean(false)
        Thread {
            JavaFXExecutor.execute {
                onFx.set(Platform.isFxApplicationThread())
                done.countDown()
            }
        }.start()
        assertTrue(done.await(5, TimeUnit.SECONDS), "background execute did not finish")
        assertTrue(onFx.get())
    }
}
