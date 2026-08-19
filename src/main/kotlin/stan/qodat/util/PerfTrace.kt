package stan.qodat.util

import org.slf4j.LoggerFactory
import kotlin.system.measureNanoTime

/**
 * Lightweight span logger for selection / load hot paths.
 *
 * Enabled by default. Disable with `-Dqodat.perf.selection=false`
 * or by setting [enabled]. [stan.qodat.Properties.logSelectionPerf] mirrors this flag.
 */
object PerfTrace {

    private val logger = LoggerFactory.getLogger(PerfTrace::class.java)

    @Volatile
    var enabled: Boolean = System.getProperty("qodat.perf.selection", "true").toBoolean()

    fun enabled(): Boolean = enabled

    inline fun <T> span(name: String, block: () -> T): T {
        if (!enabled()) return block()
        var result: T
        val nanos = measureNanoTime { result = block() }
        log(name, nanos)
        return result
    }

    fun log(name: String, nanos: Long) {
        if (!enabled()) return
        val ms = nanos / 1_000_000.0
        val line = "perf  %-36s %7.2f ms".format(name, ms)
        logger.info(line)
        println(line)
    }

    fun begin(): Long = System.nanoTime()

    fun end(name: String, startNanos: Long) {
        log(name, System.nanoTime() - startNanos)
    }
}
