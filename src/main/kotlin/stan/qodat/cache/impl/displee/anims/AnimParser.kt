package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import javafx.concurrent.Task
import org.slf4j.LoggerFactory
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.AnimationSkeletonIndex.SequenceRef
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226

abstract class AnimParser(
    private val cacheLibrary: CacheLibrary
) : Task<Void?>() {

    private val seqArchive by lazy { cacheLibrary.index(2).archive(12)!! }
    private var progressSink: ((String, Double, Double) -> Unit)? = null

    override fun call(): Void? {
        executeScan()
        return null
    }

    fun executeScan(sink: ((String, Double, Double) -> Unit)? = null) {
        progressSink = sink
        try {
            val animationMap = associateAnimationBySkeletonIds()
            report("Loaded all animation mappings!")
            matchAnimationsToSkeletons(animationMap)
        } finally {
            progressSink = null
        }
    }

    abstract fun matchAnimationsToSkeletons(animationMap: AnimationSkeletonIndex.AnimationMap)

    protected fun report(message: String, work: Double = 0.0, total: Double = 1.0) {
        // Do not read Task.workDone / totalWork here: those getters must run on
        // the FX thread, and both auto-scan and File→Rescan call this off-thread.
        updateMessage(message)
        updateProgress(work, total.coerceAtLeast(1.0))
        progressSink?.invoke(message, work, total.coerceAtLeast(1.0))
    }

    private fun associateAnimationBySkeletonIds(): AnimationSkeletonIndex.AnimationMap {
        report("Parsing sequences...")
        val sequences = loadSequences()
        val refs = sequences.map { sequence ->
            SequenceRef(
                animationId = sequence.id.toInt(),
                mayaId = sequence.animMayaId,
                frameHashes = sequence.frameIDs,
            )
        }

        val mayaRefs = refs.filter { it.mayaId >= 0 }
        val legacyRefs = refs.filter { it.mayaId < 0 }

        report("Indexing legacy frame skeletons...")
        val skeletonIdByFrameHash = loadLegacySkeletonIds(legacyRefs)
        report("Indexing Maya skeletons...")
        val skeletonIdByMayaId = loadMayaSkeletonIds(mayaRefs)

        val mapped = AnimationSkeletonIndex.mapAnimationsToSkeletons(
            sequences = refs,
            skeletonIdByFrameHash = skeletonIdByFrameHash,
            skeletonIdByMayaId = skeletonIdByMayaId,
        )
        val usedSkeletonIds = mapped.values.flatten().toSet()
        report("Indexing skeleton signatures...")
        val signatures = loadSkeletonSignatures(usedSkeletonIds)
        val mayaResolved = mayaRefs.count { mapped[it.animationId]?.isNotEmpty() == true }
        logger.info(
            "Indexed {} sequences ({} legacy, {} maya, {} maya resolved) across {} skeletons",
            refs.size,
            legacyRefs.size,
            mayaRefs.size,
            mayaResolved,
            usedSkeletonIds.size,
        )
        return AnimationSkeletonIndex.AnimationMap(
            skeletonIdsByAnimationId = mapped,
            signatureBySkeletonId = signatures,
            mayaAnimationIds = mayaRefs.map { it.animationId }.toSet(),
        )
    }

    private fun loadSkeletonSignatures(skeletonIds: Set<Int>): Map<Int, AnimationSkeletonIndex.SkeletonSignature> {
        if (skeletonIds.isEmpty()) return emptyMap()
        val result = HashMap<Int, AnimationSkeletonIndex.SkeletonSignature>(skeletonIds.size)
        val ids = skeletonIds.toList()
        val updateFrequency = (ids.size / 50).coerceAtLeast(1)
        ids.forEachIndexed { index, skeletonId ->
            val data = try {
                cacheLibrary.data(1, skeletonId)
            } catch (_: Exception) {
                null
            }
            if (data != null) {
                AnimationSkeletonIndex.skeletonSignature(data)?.let { result[skeletonId] = it }
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == ids.size) {
                report("Indexed skeleton signatures (${index + 1} / ${ids.size})", index + 1.0, ids.size.toDouble())
            }
        }
        return result
    }

    private fun loadSequences(): List<SequenceDefinition226> {
        val files = seqArchive.files.values.toList()
        val loader = SequenceLoader226().apply { configureForRevision(seqArchive.revision) }
        val sequences = ArrayList<SequenceDefinition226>(files.size)
        val updateFrequency = (files.size / 50).coerceAtLeast(1)
        files.forEachIndexed { index, file ->
            val data = file.data ?: return@forEachIndexed
            try {
                sequences.add(loader.load(file.id, data))
            } catch (e: Exception) {
                logger.debug("Failed to decode sequence {}: {}", file.id, e.message)
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == files.size) {
                report("Parsed sequences (${index + 1} / ${files.size})", index + 1.0, files.size.toDouble())
            }
        }
        return sequences
    }

    private fun loadLegacySkeletonIds(legacyRefs: List<SequenceRef>): Map<Int, Int> {
        val hashesByArchive = HashMap<Int, MutableSet<Int>>()
        for (ref in legacyRefs) {
            val hashes = ref.frameHashes ?: continue
            for (hash in hashes) {
                hashesByArchive.getOrPut(hash ushr 16) { HashSet() }.add(hash)
            }
        }
        if (hashesByArchive.isEmpty()) return emptyMap()

        val animIndex = cacheLibrary.index(0)
        val result = HashMap<Int, Int>(hashesByArchive.values.sumOf { it.size })
        val archives = hashesByArchive.entries.toList()
        val updateFrequency = (archives.size / 50).coerceAtLeast(1)
        archives.forEachIndexed { index, (archiveId, hashes) ->
            val archive = try {
                animIndex.archive(archiveId)
            } catch (e: Exception) {
                logger.debug("Failed to open frame archive {}: {}", archiveId, e.message)
                null
            }
            if (archive != null) {
                for (hash in hashes) {
                    val fileId = hash and 0xFFFF
                    val data = archive.file(fileId)?.data ?: continue
                    val skeletonId = AnimationSkeletonIndex.skeletonIdFromLegacyFrame(data, archiveId)
                        ?: continue
                    result[hash] = skeletonId
                }
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == archives.size) {
                report("Indexed frame archives (${index + 1} / ${archives.size})", index + 1.0, archives.size.toDouble())
            }
        }
        return result
    }

    private fun loadMayaSkeletonIds(mayaRefs: List<SequenceRef>): Map<Int, Int> {
        val mayaIds = mayaRefs.map { it.mayaId }.toSet()
        if (mayaIds.isEmpty()) return emptyMap()

        val indexes = mayaIndexes()
        val idsByArchive = mayaIds.groupBy { it ushr 16 and 0xFFFF }
        val result = HashMap<Int, Int>(mayaIds.size)
        val archives = idsByArchive.entries.toList()
        val updateFrequency = (archives.size / 50).coerceAtLeast(1)
        archives.forEachIndexed { index, (archiveId, ids) ->
            val archive = openFirstArchive(indexes, archiveId)
            if (archive != null) {
                for (mayaId in ids) {
                    val fileId = mayaId and 0xFFFF
                    val data = archive.file(fileId)?.data ?: continue
                    val skeletonId = AnimationSkeletonIndex.skeletonIdFromMayaAnimation(data)
                        ?: continue
                    result[mayaId] = skeletonId
                }
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == archives.size) {
                report("Indexed Maya archives (${index + 1} / ${archives.size})", index + 1.0, archives.size.toDouble())
            }
        }
        val missing = mayaIds.size - result.size
        if (missing > 0) {
            logger.warn("Could not resolve skeletons for {} / {} Maya animations", missing, mayaIds.size)
        }
        return result
    }

    private fun mayaIndexes(): List<Index> {
        val indexes = ArrayList<Index>(2)
        if (cacheLibrary.exists(22)) {
            indexes.add(cacheLibrary.index(22))
        }
        indexes.add(cacheLibrary.index(0))
        return indexes
    }

    private fun openFirstArchive(indexes: List<Index>, archiveId: Int) =
        indexes.firstNotNullOfOrNull { index ->
            try {
                val archive = index.archive(archiveId)
                if (archive != null && archive.files.isNotEmpty()) archive else null
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(AnimParser::class.java)
    }
}
