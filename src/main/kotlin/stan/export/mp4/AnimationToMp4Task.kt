package stan.export.mp4

import com.sun.javafx.application.PlatformImpl
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
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationPlayer
import java.nio.file.Path
import java.util.concurrent.Semaphore

class AnimationToMp4Task(
    private val exportPath: Path,
    private val scene: SubScene,
    private val animationPlayer: AnimationPlayer,
    private val animation: Animation
) : Task<Unit>() {

    override fun call() {

        val animationName = animation.getName()
        val aniamtionFrames = animation.getFrameList()

        updateMessage("Generating MP4 for Animation $animationName")

        val snapshots = mutableListOf<Pair<Duration, WritableImage>>()
        val semaphore = Semaphore(1)
        PlatformImpl.runAndWait {
            val totalFrames = aniamtionFrames.size
            aniamtionFrames.forEachIndexed { index, frame ->
                animationPlayer.jumpToFrame(index)
                val snapshotParameters = SnapshotParameters().apply {
                    fill = javafx.scene.paint.Color.BLACK
                    viewport = Rectangle2D(
                        0.0,
                        0.0,
                        scene.width.let { if (it.toInt() % 2 != 0) it - 1.0 else it},
                        scene.height.let { if (it.toInt() % 2 != 0) it - 1.0 else it}
                    )
                }
                val image = scene.snapshot(snapshotParameters, null)!!
                val duration = frame.getDuration()
                updateMessage("Capturing frame ${index+1}/$totalFrames")
                updateProgress(index.toLong(), totalFrames.toLong())
                snapshots.add(duration to image)
            }
            semaphore.release()
        }
        semaphore.acquire()

        val file = exportPath.resolve("mp4/$animationName.mp4").toFile().apply {
            if (!parentFile.exists())
                parentFile.mkdir()
        }

        var out : SeekableByteChannel? = null

        try {
            out = NIOUtils.writableChannel(file)

            val fps = (1000.0 / aniamtionFrames.minOf { it.getDuration() }.toMillis())
            val encoder = AWTSequenceEncoder.createWithFps(out, Rational.R(fps.toInt(), 1))
            val total = snapshots.size
            var count = 0

            for ((duration, image) in snapshots) {
                count++
                PlatformImpl.runAndWait {
                    updateMessage("Processing frame $count/$total...")
                    updateProgress(count.toLong(), total.toLong())
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

        PlatformImpl.runAndWait {
            updateProgress(0, 0)
            updateMessage("Generated MP4 at $file")
        }
        Thread.sleep(1000)
    }
}