package stan.qodat

import java.awt.Toolkit

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        Toolkit.getDefaultToolkit()
        Qodat.main(args)
    }
}
