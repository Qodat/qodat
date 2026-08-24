package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import org.slf4j.LoggerFactory
import qodat.cache.definition.InterfaceDefinition as QodatInterfaceDefinition
import stan.qodat.cache.CacheParallel
import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
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
                val widgetId = Companion.widgetId(payload.archiveId, fileId)
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

    fun getInterfaceGroup(groupId: Int): Array<InterfaceDefinition?>? {
        load()
        return getInterfaceGroup(interfaces, groupId)
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

        internal fun widgetId(groupId: Int, childId: Int): Int = (groupId shl 16) + childId

        internal fun interfaceGroupName(groupId: Int): String = groupId.toString()

        internal fun getInterfaceGroup(
            interfaces: Array<Array<InterfaceDefinition?>?>,
            groupId: Int,
        ): Array<InterfaceDefinition?>? = interfaces[groupId]

        internal fun mapDispleeInterface(
            group: Array<InterfaceDefinition?>?,
        ): Array<QodatInterfaceDefinition> =
            group?.filterNotNull()?.toTypedArray() ?: emptyArray()

        internal fun mapDispleeRootInterfaces(
            raw: Array<Array<InterfaceDefinition?>?>,
        ): Map<Int, List<QodatInterfaceDefinition>> {
            val groups = LinkedHashMap<Int, List<QodatInterfaceDefinition>>()
            for (groupId in raw.indices) {
                val components = raw[groupId] ?: continue
                if (components.all { it == null }) continue
                groups[groupId] = components.filterNotNull()
            }
            return groups
        }
    }
}
