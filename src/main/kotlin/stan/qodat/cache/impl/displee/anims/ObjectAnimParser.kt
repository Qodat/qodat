package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import javafx.application.Platform
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.types.ObjectManager
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

class ObjectAnimParser(
    cacheLibrary: CacheLibrary,
    private val objectManager: ObjectManager
) : AnimParser(cacheLibrary) {
    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val total = objectManager.objects.size
        val counter = AtomicInteger(0)
        objectManager.objects.values.parallelStream().forEach { objectDefinition ->
            val animationRef = objectDefinition.animationIds.first().toInt()
            if (animationRef > 0) {
                val referenceFrames = skeletonIdsByAnimationId[animationRef]!!
                val matches = skeletonIdsByAnimationId.filter { entry ->
                    entry.value.any { referenceFrames.any { reference -> reference == it } }
                }
                try {
                    val file =
                        Properties.osrsCachePath.get().resolve("object_anims/${objectDefinition.getOptionalId().asInt}.json").toFile()
                    val writer = FileWriter(file)
                    gson.toJson(matches.keys.toIntArray(), writer)
                    writer.flush()
                    writer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Platform.runLater {
                    val i = counter.incrementAndGet()
                    val progress = (100.0 * i.toFloat().div(total))
                    updateProgress(progress, 100.0)
                    updateMessage("Parsed object ($i / $total})")
                }
            }
        }
    }

}