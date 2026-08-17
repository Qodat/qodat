package stan.qodat.cache.impl.displee.anims

import com.displee.cache.CacheLibrary
import com.google.gson.GsonBuilder
import jagex.Buffer
import javafx.application.Platform
import javafx.concurrent.Task
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.runelite.cache.IndexType
import stan.qodat.cache.impl.displee.getIndex
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226
import java.util.stream.Collectors


abstract class AnimParser(
    private val cacheLibrary: CacheLibrary
) : Task<Void?>() {

    protected val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val globalMutex = Mutex(false)
    }

    private val animIndex by lazy {
        cacheLibrary.index(0)
    }
    private val seqArchive by lazy { cacheLibrary.index(2).archive(12)!! }
    private val animationFiles get() =
        seqArchive.files

    val loader by lazy {
        SequenceLoader226().apply { configureForRevision(seqArchive.revision) }
    }

    override fun call(): Void? {
        val skeletonIdsByAnimationId = associateAnimationBySkeletonIds()
        updateMessage("Loaded all animation mappings!")
        matchAnimationsToSkeletons(skeletonIdsByAnimationId)
        return null
    }

    abstract fun matchAnimationsToSkeletons(skeletonIdsByAnimationId: Map<Int, Set<Int>>)

    private fun associateAnimationBySkeletonIds(): Map<Int, Set<Int>> {
        val rev229 = cacheLibrary.getIndex(IndexType.MODELS).revision >= 969
        return animationFiles.values.parallelStream().map { file ->
            try {
                val data = file.data ?: return@map (file.id to emptySet<Int>())
                val anim = loader.load(file.id, data)
                Platform.runLater {
                    val progress = (100.0 * anim.id.toFloat().div(animationFiles.size))
                    updateProgress(progress, 100.0)
                    updateMessage("Parsed animation (${anim.id + 1} / ${animationFiles.size}})")
                }
                val frameGroupIds: Set<Int> = if (anim.animMayaId >= 0) {
                    val animArchive = runBlocking {
                        globalMutex.withLock {
                            (if (rev229)
                                cacheLibrary.index(22)
                            else
                                animIndex).archive(anim.animMayaId shr 16)
                        }
                    } ?: return@map (anim.id.toInt() to emptySet())
                    val animFile = animArchive.file(anim.animMayaId and 65535)
                        ?: return@map (anim.id.toInt() to emptySet())
                    val animData = animFile.data ?: return@map (anim.id.toInt() to emptySet())
                    val buffer = Buffer(animData)
                    buffer.readUnsignedByte()
                    setOf(buffer.readUnsignedShort())
                } else anim.frameIDs?.map { frameHash ->
                    val frameArchiveId = frameHash shr 16
                    val frameArchiveFileId = frameHash and 65535
                    val frameArchive = runBlocking {
                        globalMutex.withLock {
                            animIndex.archive(frameArchiveId)
                        }
                    } ?: return@map null
                    val frameFile = frameArchive.file(frameArchiveFileId) ?: return@map null
                    val frameContents = frameFile.data ?: return@map null
                    if (frameContents.size < 2) return@map null
                    frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                }?.filterNotNull()?.toSet() ?: emptySet()
                (anim.id.toInt() to frameGroupIds)
            } catch (e: Exception) {
                (file.id to emptySet<Int>())
            }
        }.collect(Collectors.toList()).toMap()
    }
}