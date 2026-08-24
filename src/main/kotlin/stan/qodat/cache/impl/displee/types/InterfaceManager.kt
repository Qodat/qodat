package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import org.slf4j.LoggerFactory
import qodat.cache.definition.InterfaceDefinition as QodatInterfaceDefinition
import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
import stan.qodat.cache.impl.oldschool.loader.InterfaceLoader237

class InterfaceManager(private val cacheLibrary: CacheLibrary) {

    private var groups: Array<Array<InterfaceDefinition?>?> = emptyArray()
    private val decoded = HashSet<Int>()

    fun listGroupIds(): IntArray = cacheLibrary.index(3).archiveIds()

    @Synchronized
    fun load() {
        for (archiveId in listGroupIds()) {
            decodeGroup(archiveId)
        }
        val loadedCount = groups.sumOf { group -> group?.count { it != null } ?: 0 }
        logger.info("Loaded {} interfaces ({} groups)", loadedCount, decoded.size)
        if (loadedCount == 0) {
            throw IllegalStateException("Interface archive produced 0 widgets")
        }
    }

    fun getInterfaceGroup(groupId: Int): Array<InterfaceDefinition?>? {
        decodeGroup(groupId)
        return getInterfaceGroup(groups, groupId)
    }

    fun getInterfaces(): Array<Array<InterfaceDefinition?>?> {
        load()
        return groups
    }

    @Synchronized
    private fun decodeGroup(groupId: Int) {
        if (!decoded.add(groupId)) return
        val archive = cacheLibrary.index(3).archive(groupId) ?: return
        val fileIds = archive.fileIds()
        val maxFileId = fileIds.maxOrNull() ?: return
        ensureGroupsCapacity(groupId)
        val loader = InterfaceLoader237().configureForRevision(archive.revision)
        val ifaces = arrayOfNulls<InterfaceDefinition>(maxFileId + 1)
        var loaded = 0
        for (fileId in fileIds) {
            val data = archive.file(fileId)?.data ?: continue
            if (data.size < 2) {
                logger.debug("Skipping empty interface {}.{} ({} bytes)", groupId, fileId, data.size)
                continue
            }
            val widgetId = widgetId(groupId, fileId)
            try {
                ifaces[fileId] = loader.load(widgetId, data)
                loaded++
            } catch (e: Exception) {
                val leftover = e.message?.contains("No data left to read") == true
                if (leftover) {
                    logger.debug("Failed to unpack interface {}.{}: {}", groupId, fileId, e.message)
                } else {
                    logger.warn("Failed to unpack interface {}.{}: {}", groupId, fileId, e.message)
                }
            }
        }
        groups[groupId] = ifaces
        if (loaded == 0) {
            logger.debug("Interface group {} produced 0 widgets", groupId)
        }
    }

    private fun ensureGroupsCapacity(groupId: Int) {
        if (groupId < groups.size) return
        val maxId = listGroupIds().maxOrNull() ?: groupId
        groups = groups.copyOf(maxOf(maxId, groupId) + 1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InterfaceManager::class.java)

        internal fun widgetId(groupId: Int, childId: Int): Int = (groupId shl 16) + childId

        internal fun interfaceGroupName(groupId: Int): String = groupId.toString()

        internal fun getInterfaceGroup(
            interfaces: Array<Array<InterfaceDefinition?>?>,
            groupId: Int,
        ): Array<InterfaceDefinition?>? = interfaces.getOrNull(groupId)

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

        internal fun indexGroupIds(archiveIds: IntArray): Map<Int, List<QodatInterfaceDefinition>> =
            archiveIds.associateWith { emptyList() }
    }
}
