package stan.qodat

import stan.qodat.javafx.NativeWindowTheme
import java.awt.Toolkit

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        NativeWindowTheme.applyEarly()
        Toolkit.getDefaultToolkit()
        Qodat.main(args)
    }
}
