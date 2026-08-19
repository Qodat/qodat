package stan.qodat.util

import javafx.concurrent.Task
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.NpcManager
import net.runelite.cache.ObjectManager
import net.runelite.cache.fs.Index
import net.runelite.cache.fs.Store
import org.slf4j.LoggerFactory
import stan.qodat.Properties
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.AnimationSkeletonIndex.SequenceRef
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226

/**
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   04/09/2019
 * @version 1.0
 */

fun createNpcAnimsJsonDir(
    store: Store,
    npcManager: NpcManager,
) = object : LoadAnimationTask(store) {
    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val entities = npcManager.npcs.mapNotNull { npc ->
            val ids = NpcPrimaryAnimations.intIds(npc)
            if (ids.isEmpty()) null else npc.id to ids
        }
        AnimationSkeletonIndex.writeMatchesForEntities(
            outputDir = Properties.osrsCachePath.get().resolve("npc_anims"),
            entities = entities,
            skeletonIdsByAnimationId = skeletonIdsByAnimationId,
        ) { done, total ->
            updateProgress(done.toDouble(), total.toDouble())
            updateMessage("Matched npc ($done / $total)")
        }
    }
}

fun createObjectAnimsJsonDir(
    store: Store,
    objectManager: ObjectManager,
) = object : LoadAnimationTask(store) {
    override fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>) {
        val entities = objectManager.objects.mapNotNull { objectDefinition ->
            val animationId = objectDefinition.animationID.takeIf { it > 0 } ?: return@mapNotNull null
            objectDefinition.id to intArrayOf(animationId)
        }
        AnimationSkeletonIndex.writeMatchesForEntities(
            outputDir = Properties.osrsCachePath.get().resolve("object_anims"),
            entities = entities,
            skeletonIdsByAnimationId = skeletonIdsByAnimationId,
        ) { done, total ->
            updateProgress(done.toDouble(), total.toDouble())
            updateMessage("Matched object ($done / $total)")
        }
    }
}

abstract class LoadAnimationTask(
    private val store: Store,
) : Task<Void?>() {

    private val storage = store.storage
    private val seqArchive by lazy { store.getIndex(IndexType.CONFIGS).getArchive(ConfigType.SEQUENCE.id) }
    private val animIndex by lazy { store.getIndex(IndexType.ANIMATIONS) }

    override fun call(): Void? {
        val skeletonIdsByAnimationId = associateAnimationBySkeletonIds()
        updateMessage("Loaded all animation mappings!")
        matchAnimationsToSkeletons(skeletonIdsByAnimationId)
        return null
    }

    abstract fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>)

    private fun associateAnimationBySkeletonIds(): Map<Int, Set<Int>> {
        val refs = loadSequences()
        val mayaRefs = refs.filter { it.mayaId >= 0 }
        val legacyRefs = refs.filter { it.mayaId < 0 }
        val skeletonIdByFrameHash = loadLegacySkeletonIds(legacyRefs)
        val skeletonIdByMayaId = loadMayaSkeletonIds(mayaRefs)
        return AnimationSkeletonIndex.mapAnimationsToSkeletons(
            sequences = refs,
            skeletonIdByFrameHash = skeletonIdByFrameHash,
            skeletonIdByMayaId = skeletonIdByMayaId,
        )
    }

    private fun loadSequences(): List<SequenceRef> {
        val archiveData = storage.loadArchive(seqArchive)
        val files = seqArchive.getFiles(archiveData).files
        val loader = SequenceLoader226().apply { configureForRevision(seqArchive.revision) }
        val refs = ArrayList<SequenceRef>(files.size)
        val updateFrequency = (files.size / 50).coerceAtLeast(1)
        files.forEachIndexed { index, file ->
            try {
                val sequence = loader.load(file.fileId, file.contents)
                refs.add(
                    SequenceRef(
                        animationId = sequence.id.toInt(),
                        mayaId = sequence.animMayaId,
                        frameHashes = sequence.frameIDs,
                    )
                )
            } catch (e: Exception) {
                logger.debug("Failed to decode sequence {}: {}", file.fileId, e.message)
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == files.size) {
                updateProgress(index + 1.0, files.size.toDouble())
                updateMessage("Parsed sequences (${index + 1} / ${files.size})")
            }
        }
        return refs
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

        val result = HashMap<Int, Int>(hashesByArchive.values.sumOf { it.size })
        val archives = hashesByArchive.entries.toList()
        val updateFrequency = (archives.size / 50).coerceAtLeast(1)
        archives.forEachIndexed { index, (archiveId, hashes) ->
            val frameArchive = animIndex.getArchive(archiveId)
            if (frameArchive != null) {
                val frameFiles = try {
                    frameArchive.getFiles(storage.loadArchive(frameArchive))
                } catch (e: Exception) {
                    logger.debug("Failed to open frame archive {}: {}", archiveId, e.message)
                    null
                }
                if (frameFiles != null) {
                    for (hash in hashes) {
                        val fileId = hash and 0xFFFF
                        val contents = frameFiles.findFile(fileId)?.contents ?: continue
                        val skeletonId = AnimationSkeletonIndex.skeletonIdFromLegacyFrame(contents, archiveId)
                            ?: continue
                        result[hash] = skeletonId
                    }
                }
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == archives.size) {
                updateProgress(index + 1.0, archives.size.toDouble())
                updateMessage("Indexed frame archives (${index + 1} / ${archives.size})")
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
            val files = openMayaArchiveFiles(indexes, archiveId)
            if (files != null) {
                for (mayaId in ids) {
                    val fileId = mayaId and 0xFFFF
                    val contents = files.findFile(fileId)?.contents ?: continue
                    val skeletonId = AnimationSkeletonIndex.skeletonIdFromMayaAnimation(contents)
                        ?: continue
                    result[mayaId] = skeletonId
                }
            }
            if ((index + 1) % updateFrequency == 0 || index + 1 == archives.size) {
                updateProgress(index + 1.0, archives.size.toDouble())
                updateMessage("Indexed Maya archives (${index + 1} / ${archives.size})")
            }
        }
        return result
    }

    private fun mayaIndexes(): List<Index> =
        listOfNotNull(
            store.getIndex(IndexType.ANIMAYAS),
            animIndex,
        ).distinct()

    private fun openMayaArchiveFiles(indexes: List<Index>, archiveId: Int) =
        indexes.firstNotNullOfOrNull { index ->
            val archive = index.getArchive(archiveId) ?: return@firstNotNullOfOrNull null
            try {
                archive.getFiles(storage.loadArchive(archive))
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(LoadAnimationTask::class.java)
    }
}
