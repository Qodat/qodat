package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.InterfaceDefinition
import org.slf4j.LoggerFactory
import stan.qodat.cache.CacheParallel
import stan.qodat.cache.impl.oldschool.loader.InterfaceLoader237
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
        val payloads = ArrayList<InterfaceArchivePayload>(archiveIds.size)

        for (archiveId in archiveIds) {
            val archive = interfaceIndex.archive(archiveId) ?: continue
            val maxFileId = archive.fileIds().maxOrNull() ?: continue
            val files = ArrayList<Pair<Int, ByteArray>>(archive.files.size)
            archive.files.forEach { (fileId, file) ->
                val data = file.data ?: return@forEach
                files.add(fileId to data)
            }
            if (files.isNotEmpty()) {
                payloads.add(InterfaceArchivePayload(archiveId, maxFileId, files))
            }
        }

        val loadedCount = AtomicInteger()
        val skipped = AtomicInteger()
        val decoded = CacheParallel.decode(payloads.map { it.archiveId to it }) { _, payload ->
            val loader = InterfaceLoader237()
            val ifaces = arrayOfNulls<InterfaceDefinition>(payload.maxFileId + 1)
            for ((fileId, data) in payload.files) {
                val widgetId = (payload.archiveId shl 16) + fileId
                try {
                    ifaces[fileId] = loader.load(widgetId, data)
                    loadedCount.incrementAndGet()
                } catch (e: Exception) {
                    skipped.incrementAndGet()
                    logger.warn("Failed to unpack interface {}.{}: {}", payload.archiveId, fileId, e.message)
                }
            }
            ifaces
        }

        decoded.forEach { (archiveId, group) -> groups[archiveId] = group }
        interfaces = groups
        loaded = true
        logger.info("Loaded {} interfaces ({} skipped)", loadedCount.get(), skipped.get())
        if (loadedCount.get() == 0) {
            throw IllegalStateException("Interface archive produced 0 widgets")
        }
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

    private class InterfaceArchivePayload(
        val archiveId: Int,
        val maxFileId: Int,
        val files: List<Pair<Int, ByteArray>>
    )

    companion object {
        private val logger = LoggerFactory.getLogger(InterfaceManager::class.java)
    }
}
