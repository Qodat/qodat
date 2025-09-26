package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.InterfaceDefinition
import net.runelite.cache.definitions.loaders.InterfaceLoader

class InterfaceManager(private val cacheLibrary: CacheLibrary) {

    private lateinit var interfaces : Array<Array<InterfaceDefinition?>?>

    fun load() {
        val loader = InterfaceLoader()

        val interfaceIndex = cacheLibrary.index(3)
        val max = interfaceIndex.archiveIds().max()

        interfaces = arrayOfNulls(max + 1)

        interfaceIndex.archives().forEach { archive ->
            val archiveId = archive.id
            val archiveData = archive.file(0)?.data ?: return@forEach
            var ifaces = interfaces[archiveId]
            if (ifaces == null) {
                ifaces = arrayOfNulls(archive.files.size)
                interfaces[archiveId] = ifaces
            }
            archive.files.forEach { (fileId, file) ->
                val widgetId = (archiveId shl 16) + fileId
                val definition = loader.load(widgetId, file.data)
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