package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.InterfaceDefinition
import net.runelite.cache.definitions.loaders.InterfaceLoader

class InterfaceManager(private val cacheLibrary: CacheLibrary) {

    private lateinit var interfaces: Array<Array<InterfaceDefinition?>?>

    fun load() {
        val loader = InterfaceLoader()

        val interfaceIndex = cacheLibrary.index(3)
        loader.configureForRevision(interfaceIndex.revision)

        val max = interfaceIndex.archiveIds().max()

        interfaces = arrayOfNulls(max + 1)

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
                val definition = loader.load(widgetId, data)
                ifaces[fileId] = definition
            }
        }
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
}