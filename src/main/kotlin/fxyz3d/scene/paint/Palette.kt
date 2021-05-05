/**
 * Palette.java
 *
 * Copyright (c) 2013-2019, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fxyz3d.scene.paint

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import javax.imageio.ImageIO

/**
 *
 * @author Stan van der Bend
 * @author jpereda
 */
public class Palette @JvmOverloads constructor(
    val numColors: Int = DEFAULT_NUMCOLORS,
    val colorPalette: ColorPalette? = DEFAULT_COLOR_PALETTE,
    private val opacity: Double = DEFAULT_OPACITY
) {

    interface ColorPalette {
        fun getNumColors(): Int
        fun getColor(i: Int): Color
        val linearGradient: LinearGradient?
            get() {
                val stops: MutableList<Stop> = ArrayList()
                for (i in 0..5) {
                    val p = i.toDouble() / 5.0
                    stops.add(Stop(p, getColor((p * getNumColors()).toInt())))
                }
                return LinearGradient(0.0, 1.0, 0.0, 0.0, true, CycleMethod.NO_CYCLE, stops)
            }
    }

    class ListColorPalette(private val colors: MutableList<Color>?) : ColorPalette {
        constructor(vararg colors: Color?) : this(ArrayList<Color>(Arrays.asList<Color>(*colors))) {}

        fun setColors(vararg colors: Color?) {
            setColors(ArrayList(Arrays.asList(*colors)))
        }

        fun setColors(colors: List<Color>?) {
            this.colors!!.clear()
            this.colors.addAll(colors!!)
        }

        fun getColors(): List<Color>? {
            return colors
        }

        override fun getColor(i: Int): Color {
            return if (colors != null && !colors.isEmpty()) colors[Math.max(
                0,
                Math.min(i, colors.size - 1)
            )] else Color.BLACK
        }
        /**
         * Gets the U (x) coordinate of the color from a [WritableImage] in the [imageProperty].
         *
         * @param index the index corresponding to a color in [colors]
         *
         * @return the U coordinate of the color at the specified [index].
         */
        fun getU(index: Int) : Float {
            val onsetX = index.toDouble().div(colors!!.size)
            val offsetX = (1.0 / colors.size).div(2)
            return  (onsetX + offsetX).toFloat()
        }
        override fun getNumColors(): Int {
            return colors?.size ?: 0
        }
    }

    class FunctionColorPalette(private val numColors: Int, private val function: Function<Double, Color>?) :
        ColorPalette {
        override fun getColor(i: Int): Color {
            return function?.apply(i.toDouble() / numColors.toDouble()) ?: Color.BLACK
        }

        override fun getNumColors(): Int {
            return numColors
        }
    }

    var width = 0
        private set
    var height = 0
        private set
    var paletteImage: Image? = null
        private set

    fun createPalette(save: Boolean): Image? {
        if (colorPalette == null || colorPalette.getNumColors() < 1) {
            return null
        }

        // try to create a square image
        width = Math.sqrt(colorPalette.getNumColors().toDouble()).toInt()
        height = Math.ceil(numColors.toDouble() / width.toDouble()).toInt()
        paletteImage = WritableImage(width, height)
        val pw = (paletteImage as WritableImage).pixelWriter
        val count = AtomicInteger()
        IntStream.range(0, height).boxed()
            .forEach { y: Int? ->
                IntStream.range(0, width).boxed()
                    .forEach { x: Int? -> pw.setColor(x!!, y!!, getColor(count.getAndIncrement())) }
            }
        if (save) {
            saveImage()
        }
        return paletteImage
    }

    fun getTextureLocation(iPoint: Int): DoubleStream {
        if (width == 0 || height == 0) {
            return DoubleStream.of(0.0, 0.0)
        }
        val y = iPoint / width
        val x = iPoint - width * y
        // add 0.5 to interpolate colors from the middle of the pixel
        return DoubleStream.of(
            ((x.toFloat() + 0.5f) / width.toFloat()).toDouble(),
            ((y.toFloat() + 0.5f) / height.toFloat()).toDouble()
        )
    }

    private fun saveImage() {
        try {
            // save
            ImageIO.write(SwingFXUtils.fromFXImage(paletteImage, null), "png", File("palette_$numColors.png"))
        } catch (ex: IOException) {
            println("Error saving image")
        }
    }

    /*
        int iColor [0-numColors]
    */
    private fun getColor(iColor: Int): Color {
        return colorPalette!!.getColor(iColor)
    }

    companion object {
        private const val DEFAULT_OPACITY = 1.0
        private const val DEFAULT_NUMCOLORS = 1530 // 39x40 palette image
        @JvmStatic
        val DEFAULT_COLOR_PALETTE: ColorPalette =
            FunctionColorPalette(DEFAULT_NUMCOLORS) { d: Double -> Color.hsb(360.0 * d, 1.0, 1.0, DEFAULT_OPACITY) }
    }
}