package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.InterfaceDefinition
import org.slf4j.LoggerFactory
import stan.qodat.cache.CacheParallel
import stan.qodat.cache.impl.oldschool.loader.InterfaceLoader237
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class InterfaceManager(private val cacheLibrary: CacheLibrary) {

    private lateinit var interfaces: Array<Array<InterfaceDefinition?>?>
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val interfaceIndex = cacheLibrary.index(3)
        val archiveIds = interfaceIndex.archiveIds()
        val max = archiveIds.max()
        val groups = arrayOfNulls<Array<InterfaceDefinition?>?>(max + 1)
        val loadedCount = AtomicInteger()
        val skipped = AtomicInteger()
        val decoded = ConcurrentHashMap<Int, Array<InterfaceDefinition?>>()

        CacheParallel.forEachIndexed(archiveIds.size) { i ->
            val archiveId = archiveIds[i]
            val archive = interfaceIndex.archive(archiveId) ?: return@forEachIndexed
            val maxFileId = archive.fileIds().maxOrNull() ?: return@forEachIndexed
            val ifaces = arrayOfNulls<InterfaceDefinition>(maxFileId + 1)
            val loader = InterfaceLoader237()
            archive.files.forEach { (fileId, file) ->
                val data = file.data ?: return@forEach
                val widgetId = (archiveId shl 16) + fileId
                try {
                    ifaces[fileId] = loader.load(widgetId, data)
                    loadedCount.incrementAndGet()
                } catch (e: Exception) {
                    skipped.incrementAndGet()
                    logger.warn("Failed to unpack interface {}.{}: {}", archiveId, fileId, e.message)
                }
            }
            decoded[archiveId] = ifaces
        }

        decoded.forEach { (archiveId, group) -> groups[archiveId] = group }
        interfaces = groups
        loaded = true
        logger.info("Loaded {} interfaces ({} skipped)", loadedCount.get(), skipped.get())
    }

    fun getNumInterfaceGroups(): Int {
        load()
        return interfaces.size
    }

    fun getNumChildren(groupId: Int): Int {
        load()
        return interfaces[groupId]!!.size
    }

    fun getIntefaceGroup(groupId: Int): Array<InterfaceDefinition?>? {
        load()
        return interfaces[groupId]
    }

    fun getInterface(groupId: Int, childId: Int): InterfaceDefinition? {
        load()
        return interfaces[groupId]!![childId]
    }

    fun getInterfaces(): Array<Array<InterfaceDefinition?>?> {
        load()
        return interfaces
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InterfaceManager::class.java)
    }
}
