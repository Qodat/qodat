package stan.qodat.cache

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeSet

/**
 * Shared helpers for mapping sequences to skeleton / framemap ids
 * (legacy frames and Maya clips use the same id space in index 1).
 */
object AnimationSkeletonIndex {

    private val gson = Gson()
    private val logger = LoggerFactory.getLogger(AnimationSkeletonIndex::class.java)

    class SequenceRef(
        val animationId: Int,
        val mayaId: Int,
        val frameHashes: IntArray?,
    )

    fun skeletonIdFromMayaAnimation(data: ByteArray): Int? {
        if (data.size < 3) return null
        return (data[1].toInt() and 0xff shl 8) or (data[2].toInt() and 0xff)
    }

    fun skeletonIdFromLegacyFrame(data: ByteArray, frameArchiveId: Int): Int? {
        if (data.size < 2) return null
        return AnimationFrameCodec.framemapId(data, frameArchiveId)
    }

    fun mapAnimationsToSkeletons(
        sequences: List<SequenceRef>,
        skeletonIdByFrameHash: Map<Int, Int>,
        skeletonIdByMayaId: Map<Int, Int>,
    ): Map<Int, Set<Int>> {
        val result = HashMap<Int, Set<Int>>(sequences.size)
        for (sequence in sequences) {
            result[sequence.animationId] = if (sequence.mayaId >= 0) {
                skeletonIdByMayaId[sequence.mayaId]?.let { setOf(it) } ?: emptySet()
            } else {
                val hashes = sequence.frameHashes
                if (hashes == null || hashes.isEmpty()) emptySet()
                else {
                    val ids = HashSet<Int>()
                    for (hash in hashes) {
                        skeletonIdByFrameHash[hash]?.let { ids.add(it) }
                    }
                    ids
                }
            }
        }
        return result
    }

    fun invertToAnimsBySkeleton(skeletonIdsByAnimationId: Map<Int, Set<Int>>): Map<Int, IntArray> {
        val buckets = HashMap<Int, MutableList<Int>>()
        for ((animationId, skeletonIds) in skeletonIdsByAnimationId) {
            for (skeletonId in skeletonIds) {
                buckets.getOrPut(skeletonId) { ArrayList() }.add(animationId)
            }
        }
        return buckets.mapValues { (_, ids) -> ids.toIntArray() }
    }

    fun referenceSkeletonIds(
        animationIds: IntArray,
        skeletonIdsByAnimationId: Map<Int, Set<Int>>,
    ): Set<Int> {
        val skeletons = HashSet<Int>()
        for (animationId in animationIds) {
            val ids = skeletonIdsByAnimationId[animationId] ?: continue
            skeletons.addAll(ids)
        }
        return skeletons
    }

    fun animationIdsSharingSkeletons(
        referenceSkeletonIds: Set<Int>,
        animationIdsBySkeletonId: Map<Int, IntArray>,
    ): IntArray {
        if (referenceSkeletonIds.isEmpty()) return IntArray(0)
        val matched = TreeSet<Int>()
        for (skeletonId in referenceSkeletonIds) {
            val ids = animationIdsBySkeletonId[skeletonId] ?: continue
            for (id in ids) matched.add(id)
        }
        return matched.toIntArray()
    }

    fun writeMatchesForEntities(
        outputDir: Path,
        entities: List<Pair<Int, IntArray>>,
        skeletonIdsByAnimationId: Map<Int, Set<Int>>,
        onProgress: (done: Int, total: Int) -> Unit,
    ) {
        Files.createDirectories(outputDir)
        val animationIdsBySkeletonId = invertToAnimsBySkeleton(skeletonIdsByAnimationId)
        val total = entities.size
        val counter = java.util.concurrent.atomic.AtomicInteger()
        val updateFrequency = (total / 100).coerceAtLeast(1)
        CacheParallel.forEachIndexed(entities.size) { index ->
            val (entityId, referenceAnimationIds) = entities[index]
            val skeletons = referenceSkeletonIds(referenceAnimationIds, skeletonIdsByAnimationId)
            if (skeletons.isNotEmpty()) {
                val matches = animationIdsSharingSkeletons(skeletons, animationIdsBySkeletonId)
                if (matches.isNotEmpty()) {
                    try {
                        writeAnimationIds(outputDir.resolve("$entityId.json"), matches)
                    } catch (e: Exception) {
                        logger.warn("Failed to write anim matches for {}: {}", entityId, e.message)
                    }
                }
            }
            CacheParallel.nextProgressStep(counter, total, updateFrequency)?.let { done ->
                onProgress(done, total)
            }
        }
    }

    fun writeAnimationIds(file: Path, animationIds: IntArray) {
        Files.createDirectories(file.parent)
        Files.writeString(file, gson.toJson(animationIds))
    }
}
