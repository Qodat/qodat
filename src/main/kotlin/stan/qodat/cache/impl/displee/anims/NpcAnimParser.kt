package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import javafx.application.Platform
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.types.NpcManager
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger

class NpcAnimParser(
    cacheLibrary: CacheLibrary,
    private val npcManager: NpcManager
) : AnimParser(cacheLibrary) {

    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val total = npcManager.npcs.size
        val counter = AtomicInteger(0)

        npcManager.npcs.values.parallelStream().forEach { npc ->
            val animationRef = intArrayOf(
                npc.walkingAnimation,
                npc.standingAnimation,
                npc.idleRotateLeftAnimation ,
                npc.idleRotateRightAnimation ,
                npc.rotateLeftAnimation ,
                npc.rotateRightAnimation,
                npc.rotate180Animation
            ).filter { it > 0 }
            try {

                if (animationRef.isNotEmpty()) {

                    val referenceFrames = animationRef.flatMap {
                        requireNotNull(skeletonIdsByAnimationId[it]) {
                            "Animation $it was null for npc ${npc.name}"
                        }
                    }.toSet()

                    val matches = skeletonIdsByAnimationId.filter { entry ->
                        entry.value.any {
                            referenceFrames.any { reference ->
                                reference == it
                            }
                        }
                    }
                    try {
                        val file = Properties.osrsCachePath.get().resolve("npc_anims/${npc.id}.json").toFile()
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
                        updateMessage("Parsed npc ($i / $total})")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}