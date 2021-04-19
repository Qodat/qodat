package stan.qodat.scene.paint

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ColorPicker
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial

/**
 * This class represents a [PhongMaterial] with its
 * [PhongMaterial.diffuseMapProperty] bound to [imageProperty].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
class AtlasMaterial : PhongMaterial() {

    private lateinit var colors : Array<Color>
    private lateinit var uniqueColors : Set<Color>
    private val replacementColorMap = HashMap<Color, Color>()

    val imageProperty = SimpleObjectProperty<WritableImage>()

    init {
        diffuseMapProperty().bind(imageProperty)
    }

    private fun createImage(){
        val image = WritableImage(uniqueColors.size, 1)
        uniqueColors.forEachIndexed { index, color ->
            image.pixelWriter.setColor(index, 0, replacementColorMap[color]?:color)
        }
        imageProperty.set(image)
    }

    /**
     * Creates a new image of the unique colors of the specified array,
     * and sets this image in the [imageProperty].
     *
     * @param colors the colors array to use.
     */
    fun setColors(colors: Array<Color>){
        this.colors = colors
        uniqueColors = colors.toSet()
        createImage()
    }

    /**
     * Gets the U (x) coordinate of the color from a [WritableImage] in the [imageProperty].
     *
     * @param index the index corresponding to a color in [colors]
     *
     * @return the U coordinate of the color at the specified [index].
     */
    fun getU(index: Int) : Float {
        val colorIndex = uniqueColors.indexOf(colors[index])
        val onsetX = colorIndex.toDouble().div(uniqueColors.size)
        val offsetX = (1.0 / uniqueColors.size).div(2)
        return  (onsetX + offsetX).toFloat()
    }

    fun createPickers() : Set<ColorPicker> {
        replacementColorMap.clear()
        return uniqueColors.mapIndexed { index, color ->
            val picker = ColorPicker(color)
            picker.customColors.setAll(uniqueColors)
            picker.valueProperty().addListener { _, oldValue, newValue ->
                if(oldValue != newValue){
                    replacementColorMap[color] = newValue
                    createImage()
                }
            }
            picker
        }.toSet()
    }
}