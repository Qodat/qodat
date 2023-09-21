package stan.qodat.scene.paint

import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import javafx.scene.paint.PhongMaterial
import javafx.scene.text.Text
import qodat.cache.definition.SpriteDefinition
import qodat.cache.definition.TextureDefinition
import stan.qodat.Qodat
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.util.DEFAULT
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

/**
 * This class represents a [PhongMaterial] with its [PhongMaterial.diffuseMapProperty] bound to [imageProperty].
 *
 * Make sure to first load the image in [load].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
data class TextureMaterial(val definition: TextureDefinition) : Material {

    val imageProperty = SimpleObjectProperty<Image>()

    override val fxMaterial: PhongMaterial = PhongMaterial()

    private val viewNode by lazy {
        HBox().apply {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            children += Text("Texture\t").apply {
                fill = DEFAULT
            }
            children += ImageView().apply {
                fitWidth = 15.0
                fitHeight = 15.0
                imageProperty().bind(imageProperty)
            }
            children += Label(definition.id.toString())
        }
    }

    private val animator = object : AnimationTimer() {
        var previousTimeStamp = 0L
        override fun handle(now: Long) {
            val timeSinceUpdate = now - previousTimeStamp

            if (TimeUnit.NANOSECONDS.toMillis(timeSinceUpdate) > 10) {
                update()
                previousTimeStamp = now
            }
        }
    }


    fun load() = try {
        val textureSprite = OldschoolCacheRuneLite.getSprite(definition.fileIds[0], 0)
        fxMaterial.diffuseMapProperty().bind(imageProperty)
        val textureSpriteImage = try {
            textureSprite.let {
                val width = it.width
                val height = it.height
                val pixels = it.pixels
                BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                    setRGB(0, 0, width, height, pixels, 0, width)
                }
            }
        } catch (e: Exception) {
            Qodat.logException("Failed to export texture sprite ${definition.fileIds[0]}", e)
            null
        }
        if (textureSpriteImage != null) {
            imageProperty.set(SwingFXUtils.toFXImage(textureSpriteImage, null))
            true
        } else
            false
    } catch (e: Exception) {
        Qodat.logException("Failed to load texture sprite ${definition.fileIds[0]}", e)
        false
    }


    fun SpriteDefinition.export(): BufferedImage {
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        bi.setRGB(0, 0, width, height, pixels, 0, width)
        return bi
    }

    /**
     * Moves all pixels at the end of the [WritableImage] to the front,
     * and then shift all other values one row downwards.
     */
    private fun update() {

//        val image = imageProperty.get()
//        val width = image.width.toInt()
//        val height = image.height.toInt()
//        val writer = image.pixelWriter!!
//        val pixelBuffer = IntArray(width*height*4)
//        val format = PixelFormat.getIntArgbInstance()
//
//        image.pixelReader!!.getPixels(0, 0, width, height, format , pixelBuffer, 0, width*4)
//
//        writer.setPixels(0, 0, width, 1, format, pixelBuffer, width*4*(height-1), width*4)
//
//        for(i in 0 until height - 1){
//            writer.setPixels(0, i+1, width, 1, format, pixelBuffer, width*4*i, width*4)
//        }
    }

    fun startAnimation() {
        animator.start()
    }

    fun stopAnimation() {
        animator.stop()
    }

    override fun getViewNode(): Node = viewNode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextureMaterial

        if (definition.id != other.definition.id) return false

        return true
    }

    override fun hashCode(): Int {
        return definition.id
    }


}