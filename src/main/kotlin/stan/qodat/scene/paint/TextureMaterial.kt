package stan.qodat.scene.paint

import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.paint.PhongMaterial
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.TextureDefinition
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
class TextureMaterial(val definition: TextureDefinition) : PhongMaterial() {

    private val imageProperty = SimpleObjectProperty<WritableImage>()
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

    init {
        diffuseMapProperty().bind(imageProperty)
    }

    /**
     * Moves all pixels at the end of the [WritableImage] to the frond,
     * and then shift all other values one row downwards.
     */
    private fun update(){

        val image = imageProperty.get()
        val width = image.width.toInt()
        val height = image.height.toInt()
        val writer = image.pixelWriter!!
        val pixelBuffer = IntArray(width*height*4)
        val format = PixelFormat.getIntArgbInstance()

        image.pixelReader!!.getPixels(0, 0, width, height, format , pixelBuffer, 0, width*4)

        writer.setPixels(0, 0, width, 1, format, pixelBuffer, width*4*(height-1), width*4)

        for(i in 0 until height - 1){
            writer.setPixels(0, i+1, width, 1, format, pixelBuffer, width*4*i, width*4)
        }
    }

    fun load(cache: Cache){
//        val image = cache.provideSprite(definition.fileIds[0], 0).map { sprite ->
//            SwingFXUtils.toFXImage(SpriteExporter(sprite).export(), null)
//        }.get()
//        imageProperty.set(image)
    }

    fun startAnimation(){
        animator.start()
    }

    fun stopAnimation(){
        animator.stop()
    }
}