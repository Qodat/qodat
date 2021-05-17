package fxyz3d.scene.paint

import javafx.scene.paint.Color

interface ColorPalette {

    fun getNumColors(): Int

    fun getColor(i: Int): Color
}