package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.InterfaceDefinition
import org.slf4j.LoggerFactory
import stan.qodat.cache.impl.oldschool.loader.InterfaceLoader237

class InterfaceManager(private val cacheLibrary: CacheLibrary) {

    private lateinit var interfaces: Array<Array<InterfaceDefinition?>?>

    fun load() {
        val loader = InterfaceLoader237()
        val interfaceIndex = cacheLibrary.index(3)
        val max = interfaceIndex.archiveIds().max()

        interfaces = arrayOfNulls(max + 1)
        var loaded = 0
        var skipped = 0

        interfaceIndex.archiveIds().forEach { archiveId ->
            val archive = interfaceIndex.archive(archiveId) ?: return@forEach
            val maxFileId = archive.fileIds().maxOrNull() ?: return@forEach
            var ifaces = interfaces[archiveId]
            if (ifaces == null) {
                ifaces = arrayOfNulls(maxFileId + 1)
                interfaces[archiveId] = ifaces
            }
            archive.files.forEach { (fileId, file) ->
                val data = file.data ?: return@forEach
                val widgetId = (archiveId shl 16) + fileId
                try {
                    ifaces[fileId] = loader.load(widgetId, data)
                    loaded++
                } catch (e: Exception) {
                    skipped++
                    logger.warn("Failed to unpack interface {}.{}: {}", archiveId, fileId, e.message)
                }
            }
        }
        logger.info("Loaded {} interfaces ({} skipped)", loaded, skipped)
    }

    fun getNumInterfaceGroups(): Int {
        return interfaces.size
    }

    fun getNumChildren(groupId: Int): Int {
        return interfaces[groupId]!!.size
    }

    fun getIntefaceGroup(groupId: Int): Array<InterfaceDefinition?>? {
        return interfaces[groupId]
    }

    fun getInterface(groupId: Int, childId: Int): InterfaceDefinition? {
        return interfaces[groupId]!![childId]
    }

    fun getInterfaces(): Array<Array<InterfaceDefinition?>?> {
        return interfaces
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InterfaceManager::class.java)
    }
}