package stan.export.mp4

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Rectangle2D
import javafx.scene.SnapshotParameters
import javafx.scene.SubScene
import javafx.scene.image.WritableImage
import javafx.util.Duration
import org.jcodec.api.awt.AWTSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import org.jcodec.scale.AWTUtil
import stan.qodat.javafx.JavaFXExecutor
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationPlayer
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReferenceArray

class AnimationToMp4Task(
    private val exportPath: Path,
    private val scene: SubScene,
    private val animationPlayer: AnimationPlayer,
    private val animation: Animation
) : Task<Unit>() {

    override fun call() {

        val animationName = animation.getName()
        val aniamtionFrames = animation.getFrameList()

        val transformables = animationPlayer.transformableList.toList()


        updateMessage("Generating MP4 for Animation $animationName")

        val totalFrames = aniamtionFrames.size
        val semaphore = Semaphore(0)
        val snapshots = AtomicReferenceArray<Pair<Duration, WritableImage>>(totalFrames)
        aniamtionFrames.forEachIndexed { index, frame ->
            Platform.runLater {
                try {
                    transformables.forEach { transformable ->
                        transformable.animate(frame)
                    }
                    val snapshotParameters = SnapshotParameters().apply {
                        fill = javafx.scene.paint.Color.BLACK
                        viewport = Rectangle2D(
                            0.0,
                            0.0,
                            scene.width.let { if (it.toInt() % 2 != 0) it - 1.0 else it },
                            scene.height.let { if (it.toInt() % 2 != 0) it - 1.0 else it }
                        )
                    }
                    val image = scene.snapshot(snapshotParameters, null)!!
                    val duration = frame.getDuration()
                    updateMessage("Capturing frame ${index + 1}/$totalFrames")
                    updateProgress(index.toLong(), totalFrames.toLong())
                    snapshots.set(index, duration to image)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    semaphore.release()
                }
            }
        }

        semaphore.acquire(totalFrames)


        val file = exportPath.resolve("mp4/$animationName.mp4").toFile().apply {
            if (!parentFile.exists())
                parentFile.mkdir()
        }

        var out : SeekableByteChannel? = null

        try {
            out = NIOUtils.writableChannel(file)

            val fps = (1000.0 / aniamtionFrames.minOf { it.getDuration() }.toMillis())
            val encoder = AWTSequenceEncoder.createWithFps(out, Rational.R(fps.toInt(), 1))

            var count = 0

            for (i in 0 until totalFrames) {
                val entry = snapshots.get(i) ?: continue
                val (duration, image) = entry
                count++
                JavaFXExecutor.execute {
                    updateMessage("Processing frame $count/$totalFrames...")
                    updateProgress(count.toLong(), totalFrames.toLong())
                }
                val picture = AWTUtil.fromBufferedImageRGB(SwingFXUtils.fromFXImage(image, null))
                val videoFrameDurationInMs = 1000.0/fps
                var animationFrameDuration = duration.toMillis()
                while (animationFrameDuration > 0) {
                    encoder.encodeNativeFrame(picture)
                    animationFrameDuration -= videoFrameDurationInMs
                }
            }
            encoder.finish()
        } finally {
            out?.close()
        }

        JavaFXExecutor.execute {
            updateProgress(0, 0)
            updateMessage("Generated MP4 at $file")
        }
    }
}