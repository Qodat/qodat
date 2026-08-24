package stan.qodat.scene.control.export.gif

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.SnapshotParameters
import javafx.scene.SubScene
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.image.WritablePixelFormat
import javafx.scene.paint.Color
import stan.qodat.scene.control.export.gif.encoder.FastGifEncoder
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationPlayer
import stan.qodat.scene.transform.Transformable
import java.io.FileOutputStream
import java.nio.IntBuffer
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

class AnimationToGifTask(
    private val exportPath: Path,
    private val scene: SubScene,
    private val animationPlayer: AnimationPlayer,
    private val animation: Animation
) : Task<Path>() {

    override fun call(): Path {
        val frames = animation.getFrameList()
        if (frames.isEmpty()) {
            throw IllegalStateException("Animation ${animation.getName()} has no frames")
        }

        val previousIndex = animationPlayer.frameIndexProperty.get()
        val wasPlaying = animationPlayer.isPlaying()
        val transformables = onFx { animationPlayer.transformableList.toList() }

        val (width, height) = onFx {
            animationPlayer.pause()
            scene.width.toInt() to scene.height.toInt()
        }
        if (width <= 0 || height <= 0) {
            throw IllegalStateException("Scene has invalid size ${width}x${height}")
        }

        val outputFile = resolveOutputFile()
        val totalFrames = frames.size
        val snapshotParams = SnapshotParameters().apply { fill = Color.BLACK }
        val pixelFormat = PixelFormat.getIntArgbInstance()

        updateMessage("Generating GIF for Animation ${animation.getName()}")
        val wallStart = System.nanoTime()

        try {
            val sampleIndices = sampleIndices(totalFrames)

            val reusableImage = onFx { WritableImage(width, height) }
            val samplePixels = Array(sampleIndices.size) { IntArray(0) }
            val sampleDelays = IntArray(sampleIndices.size)
            for (i in sampleIndices.indices) {
                val frameIndex = sampleIndices[i]
                updateMessage("Sampling frame ${i + 1}/${sampleIndices.size} for palette")
                updateProgress(i.toLong(), (totalFrames + sampleIndices.size).toLong())
                val captured = captureFrame(
                    frameIndex = frameIndex,
                    transformables = transformables,
                    reusableImage = reusableImage,
                    snapshotParams = snapshotParams,
                    pixelFormat = pixelFormat,
                    width = width,
                    height = height
                )
                samplePixels[i] = captured.argb
                sampleDelays[i] = captured.delayCentiSeconds
            }

            // One palette for the whole animation: cheaper than per-frame median-cut and avoids inter-frame flicker.
            updateMessage("Building shared palette from ${sampleIndices.size} frames")
            val palette = FastGifEncoder.buildPalette(samplePixels)
            val dither = palette.isReduced

            val parallelism = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
            val inFlight = Semaphore(parallelism)
            val pool = Executors.newFixedThreadPool(parallelism)
            val encoded = arrayOfNulls<FastGifEncoder.EncodedFrame>(totalFrames)
            val encodedCount = AtomicInteger(0)
            val encodeError = AtomicReference<Throwable>()
            val sampleByFrame = sampleIndices.toHashSet()

            try {
                FileOutputStream(outputFile).use { fileOut ->
                    val encoder = FastGifEncoder(fileOut, width, height, 0, palette, dither)

                    fun submit(frameIndex: Int, argb: IntArray, delayCentiSeconds: Int) {
                        inFlight.acquire()
                        pool.execute {
                            try {
                                encoded[frameIndex] = encoder.encodeFrame(argb, delayCentiSeconds)
                                val done = encodedCount.incrementAndGet()
                                updateMessage("Encoding frame $done/$totalFrames")
                                updateProgress(
                                    (sampleIndices.size + done).toLong(),
                                    (totalFrames + sampleIndices.size).toLong()
                                )
                            } catch (t: Throwable) {
                                encodeError.compareAndSet(null, t)
                            } finally {
                                inFlight.release()
                            }
                        }
                    }

                    for (i in sampleIndices.indices) {
                        submit(sampleIndices[i], samplePixels[i], sampleDelays[i])
                    }

                    for (frameIndex in 0 until totalFrames) {
                        if (isCancelled) {
                            throw InterruptedException("GIF export cancelled")
                        }
                        if (frameIndex in sampleByFrame) {
                            continue
                        }
                        updateMessage("Capturing frame ${frameIndex + 1}/$totalFrames")
                        val captured = captureFrame(
                            frameIndex = frameIndex,
                            transformables = transformables,
                            reusableImage = reusableImage,
                            snapshotParams = snapshotParams,
                            pixelFormat = pixelFormat,
                            width = width,
                            height = height
                        )
                        submit(frameIndex, captured.argb, captured.delayCentiSeconds)
                    }

                    pool.shutdown()
                    if (!pool.awaitTermination(10, TimeUnit.MINUTES)) {
                        pool.shutdownNow()
                        throw IllegalStateException("GIF encode timed out")
                    }
                    encodeError.get()?.let { throw it }

                    for (i in 0 until totalFrames) {
                        val frame = encoded[i]
                            ?: throw IllegalStateException("Missing encoded GIF frame $i")
                        updateMessage("Writing frame ${i + 1}/$totalFrames")
                        encoder.writeFrame(frame)
                    }
                    encoder.finish()
                }
            } finally {
                if (!pool.isTerminated) {
                    pool.shutdownNow()
                }
            }
        } finally {
            onFx {
                animationPlayer.restorePlayback(wasPlaying, previousIndex)
            }
        }

        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - wallStart)
        updateProgress(1, 1)
        updateMessage("Generated GIF at $outputFile (${totalFrames} frames, ${elapsedMs}ms)")
        return outputFile.toPath()
    }

    private fun resolveOutputFile(): java.io.File {
        val file = if (!exportPath.isDirectory() && exportPath.extension.equals("gif", ignoreCase = true)) {
            exportPath.toFile()
        } else {
            exportPath.resolve("${animation.getName()}.gif").toFile()
        }
        file.parentFile?.mkdirs()
        return file
    }

    private fun sampleIndices(totalFrames: Int): IntArray {
        val sampleCount = minOf(SAMPLE_FRAME_COUNT, totalFrames)
        return if (totalFrames <= SAMPLE_FRAME_COUNT) {
            IntArray(totalFrames) { it }
        } else {
            IntArray(sampleCount) { it * (totalFrames - 1) / (sampleCount - 1) }
                .distinct()
                .toIntArray()
        }
    }

    private fun captureFrame(
        frameIndex: Int,
        transformables: List<Transformable>,
        reusableImage: WritableImage,
        snapshotParams: SnapshotParameters,
        pixelFormat: WritablePixelFormat<IntBuffer>,
        width: Int,
        height: Int
    ): CapturedFrame {
        val frame = animation.getFrameList()[frameIndex]
        val delayCentiSeconds = (frame.getDuration().toMillis() / 10.0).toInt().coerceAtLeast(2)
        val argb = IntArray(width * height)
        onFx {
            transformables.forEach { it.animate(frame) }
            scene.snapshot(snapshotParams, reusableImage)
            reusableImage.pixelReader.getPixels(0, 0, width, height, pixelFormat, argb, 0, width)
        }
        return CapturedFrame(argb, delayCentiSeconds)
    }

    private fun <T> onFx(block: () -> T): T {
        if (Platform.isFxApplicationThread()) {
            return block()
        }
        val future = CompletableFuture<T>()
        Platform.runLater {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future.get()
    }

    private data class CapturedFrame(val argb: IntArray, val delayCentiSeconds: Int)

    private companion object {
        const val SAMPLE_FRAME_COUNT = 24
    }
}
