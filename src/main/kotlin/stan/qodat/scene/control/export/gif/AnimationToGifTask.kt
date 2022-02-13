package stan.qodat.scene.control.export.gif

import javafx.concurrent.Task
import javafx.scene.SnapshotParameters
import javafx.scene.SubScene
import javafx.scene.image.WritableImage
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.control.export.gif.encoder.*
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationPlayer
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AnimationToGifTask(
    private val exportPath: Path,
    private val scene: SubScene,
    private val animationPlayer: AnimationPlayer,
    private val animation: Animation
) : Task<Path>() {

    override fun call(): Path {

        val animationName = animation.getName()
        val aniamtionFrames = animation.getFrameList()

        GlobalScope.launch(Dispatchers.JavaFx) {
            updateMessage("Generating GIF for Animation $animationName")
        }

        val snapshots = mutableListOf<Pair<Duration, WritableImage>>()
        runBlocking {
            snapshots.addAll(aniamtionFrames.mapIndexed { index, frame ->
                GlobalScope.async(Dispatchers.JavaFx) {
                    animationPlayer.jumpToFrame(index)
                    val snapshotParameters = SnapshotParameters().apply { fill = javafx.scene.paint.Color.BLACK }
                    val image = scene.snapshot(snapshotParameters, null)!!
                    val duration = frame.getDuration()

                    duration to image
                }
            }.awaitAll())
        }

        val path = exportPath.resolve("gifs/$animationName.gif").toFile().apply {
            if (!parentFile.exists())
                parentFile.mkdirs()
        }

        val out = FileOutputStream(path)
        val options = ImageOptions()
        val encoder =
            GifEncoder(out, scene.width.toInt(), scene.height.toInt(), 0)

        val total = snapshots.size
        val counter = AtomicInteger(0)

        runBlocking {
            val imageResults = snapshots.mapIndexed { index, (duration, image) ->
                GlobalScope.async(Dispatchers.IO) {
                    GlobalScope.launch(Dispatchers.JavaFx) {
                        val count = counter.incrementAndGet()
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
                    index to gifImage
                }
            }.awaitAll().sortedBy { it.first }
            val totalImages = imageResults.size
            for ((index, image) in imageResults) {
                GlobalScope.launch(Dispatchers.JavaFx) {
                    updateMessage("Encoding frame ($index/$totalImages)")
                    updateProgress(index.toLong(), totalImages.toLong())
                }
                encoder.addImage(image, options)
            }
        }

        encoder.finishEncoding()
        out.flush()
        out.close()
        return path.toPath()
    }
}