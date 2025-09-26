package stan.qodat.scene.control.export.wavefront

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import qodat.cache.definition.SpriteDefinition
import stan.qodat.cache.impl.displee.DispleeCache
import java.awt.image.BufferedImage
import java.io.PrintWriter
import java.nio.file.Path
import javax.imageio.ImageIO

sealed class WaveFrontMaterial(private val alpha: Double) {

    abstract fun encodeMtl(mtlWriter: PrintWriter, directory: Path)
    abstract fun encodeObj(objWriter: PrintWriter, face: Int, x: Int, y: Int, z: Int)

    fun tryEncodeAlpha(mtlWriter: PrintWriter) {
        if (alpha > 0.0)
            mtlWriter.println("d $alpha")
    }

    data class Color(val r: Double, val g: Double, val b: Double, val alpha: Double) : WaveFrontMaterial(alpha) {
        override fun encodeMtl(mtlWriter: PrintWriter, directory: Path) {
            mtlWriter.println("Kd $r $g $b")
            tryEncodeAlpha(mtlWriter)
        }

        override fun encodeObj(objWriter: PrintWriter, face: Int, x: Int, y: Int, z: Int) {
            objWriter.println("f $x $y $z")
        }
    }

    data class Texture(private val spriteFileId: Int, private val alpha: Double): WaveFrontMaterial(alpha) {

        override fun encodeMtl(mtlWriter: PrintWriter, directory: Path) {

            val spriteDefinition = DispleeCache.getSprite(spriteFileId, 0)
            val relativePath = "sprites/$spriteFileId.png"
            val spritePath = directory.resolve(relativePath).toFile()

            GlobalScope.launch(Dispatchers.IO) {
                spritePath.apply {
                    if (!parentFile.exists()){
                        parentFile.mkdirs()
                    }
                }
                val spriteImage = spriteDefinition.export()
                ImageIO.write(spriteImage, "png", spritePath)
            }

            mtlWriter.println("map_Kd $relativePath")
            tryEncodeAlpha(mtlWriter)
        }
        override fun encodeObj(objWriter: PrintWriter, face: Int, x: Int, y: Int, z: Int) {
            objWriter.println(
                "f "
                        + x + "/" + (face * 3 + 1) + " "
                        + y + "/" + (face * 3 + 2) + " "
                        + z + "/" + (face * 3 + 3)
            )
        }
        private fun SpriteDefinition.export(): BufferedImage {
            val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, width, height, pixels, 0, width)
            return bi
        }
    }
}
