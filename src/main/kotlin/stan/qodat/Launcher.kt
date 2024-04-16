package stan.qodat

import java.awt.Toolkit
import kotlin.time.ExperimentalTime

object Launcher {
    @ExperimentalTime
    @JvmStatic
    fun main(args: Array<String>) {
        Toolkit.getDefaultToolkit()
        Qodat.main(args)
    }
}
