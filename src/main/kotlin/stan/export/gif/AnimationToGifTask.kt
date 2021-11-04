package stan.export.gif

import com.sun.javafx.application.PlatformImpl
import javafx.concurrent.Task
import javafx.scene.SnapshotParameters
import javafx.scene.SubScene
import javafx.scene.image.WritableImage
import javafx.util.Duration
import stan.export.gif.encoder.*
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationPlayer
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class AnimationToGifTask(
    private val exportPath: Path,
    private val scene: SubScene,
    private val animationPlayer: AnimationPlayer,
    private val animation: Animation
) : Task<Unit>() {

    override fun call() {

        val animationName = animation.getName()
        val aniamtionFrames = animation.getFrameList()

        updateMessage("Generating GIF for Animation $animationName")

        val snapshots = mutableMapOf<Duration, WritableImage>()
        PlatformImpl.runAndWait {
            val totalFrames = aniamtionFrames.size
            snapshots.putAll(aniamtionFrames.mapIndexed { index, frame ->
                animationPlayer.jumpToFrame(index)
                val snapshotParameters = SnapshotParameters().apply { fill = javafx.scene.paint.Color.BLACK }
                val image = scene.snapshot(snapshotParameters, null)!!
                val duration = frame.getDuration()
                updateMessage("Capturing frame ${index+1}/$totalFrames")
                updateProgress(index.toLong(), totalFrames.toLong())
                duration to image
            })
        }

        val path = exportPath.resolve("gifs/$animationName.gif").toFile().apply {
            if (!parentFile.exists())
                parentFile.mkdir()
        }

        val out = FileOutputStream(path)
        val options = ImageOptions()
        val encoder =
            GifEncoder(out, scene.width.toInt(), scene.height.toInt(), 0)

        val total = snapshots.size
        var count = 0

        for ((duration, image) in snapshots) {

            count++
            PlatformImpl.runAndWait {
                updateMessage("Processing frame $count/$total...")
                updateProgress(count.toLong(), total.toLong())
            }
            val reader = image.pixelReader
            val colors = Array(image.height.toInt()) { y ->
                Array(image.width.toInt()) { x ->
                    val color = reader.getColor(x, y)
                    Color(color.red, color.green, color.blue)
                }
            }
            val gifImage = Image.fromColors(colors)
            options.setDelay(duration.toMillis().toLong(), TimeUnit.MILLISECONDS)
            options.setTransparencyColor(Color.BLACK.rgbInt)
            options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)
            encoder.addImage(gifImage, options)
        }

        encoder.finishEncoding()
        out.flush()
        out.close()
        PlatformImpl.runAndWait {
            updateProgress(0, 0)
            updateMessage("Generated GIF at $path")
        }
        Thread.sleep(1000)
    }
}