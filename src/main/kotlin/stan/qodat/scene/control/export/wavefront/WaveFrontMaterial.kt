package stan.qodat.scene.control.export.wavefront

import java.io.PrintWriter

sealed class WaveFrontMaterial {

    abstract fun encode(mtlWriter: PrintWriter)


    data class Color(private val r: Double, private val g: Double, private val b: Double, private val alpha: Double) : WaveFrontMaterial() {
        override fun encode(mtlWriter: PrintWriter) {
            mtlWriter.println("Kd $r $g $b")
            if (alpha > 0.0)
                mtlWriter.println("d $alpha")
        }
    }

}