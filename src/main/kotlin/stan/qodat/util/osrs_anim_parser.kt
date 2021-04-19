package stan.qodat.util

import com.google.gson.GsonBuilder
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.definitions.SequenceDefinition
import net.runelite.cache.definitions.loaders.FramemapLoader
import net.runelite.cache.definitions.loaders.SequenceLoader
import net.runelite.cache.fs.ArchiveFiles
import stan.qodat.Properties
import stan.qodat.cache.impl.oldschool.OldschoolCache
import java.io.FileWriter

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   04/09/2019
 * @version 1.0
 */

private var animFramemaps = HashMap<Int, HashSet<Int>>()
private val gson = GsonBuilder().setPrettyPrinting().create()

fun main(){

    val cache = OldschoolCache
    val store = cache.store
    val npcManager = cache.npcManager

    val storage = store.storage
    val count = npcManager.npcs.size

    val map = HashMap<Int, ArchiveFiles>()

    run {
        val index = store.getIndex(IndexType.CONFIGS)
        val seqArchive = index.getArchive(ConfigType.SEQUENCE.id)
        val archiveData = storage.loadArchive(seqArchive)
        val files = seqArchive.getFiles(archiveData)
        val frameIndex = store.getIndex(IndexType.FRAMES)
        val framemapIndex = store.getIndex(IndexType.FRAMEMAPS)
        val loader = SequenceLoader()

        val animations = HashMap<Int, SequenceDefinition>()

        for(file in files.files){
            val anim = loader.load(file.fileId, file.contents)
            animations[anim.id] = anim
            if(anim.id in 8534..8538){

            }
            anim.frameIDs?.forEach {
                val hexString = Integer.toHexString(it)

                if(hexString.length < 6){
                    println("Could not parse frame[$it] in anim[${anim.id}]")
                    return@forEach
                }
                val frameArchiveId = decodeArchiveId(hexString)
                val frameArchiveFileId = decodeArchiveFileId(hexString)

                val frameArchive = frameIndex.getArchive(frameArchiveId)!!
                val frameArchiveFiles = map.getOrPut(frameArchiveId) {
                    frameArchive.getFiles(storage.loadArchive(frameArchive))!!
                }
                val frameFile = frameArchiveFiles.findFile(frameArchiveFileId)!!
                val frameContents = frameFile.contents

                val frameMapArchiveId = frameContents[0].toInt() and 0xff shl 8 or (frameContents[1].toInt() and 0xff)
                val frameMapArchive = framemapIndex.getArchive(frameMapArchiveId)
                val frameMapContents = frameMapArchive.decompress(storage.loadArchive(frameMapArchive))
                val frameMapDefinition = FramemapLoader().load(frameMapArchive.archiveId, frameMapContents)


                animFramemaps.putIfAbsent(anim.id, HashSet())
                animFramemaps[anim.id]!!.add(frameMapDefinition.id)
            }
            println("Parsed[${anim.id}]")
        }
    }

    println("Loaded all animation mappings!")


    val parsedNames = HashSet<String>()

    for ((i, npc) in npcManager.npcs.withIndex()) {

        if(npc.walkAnimation > 0) {

            if (parsedNames.contains(npc.name))
                continue

            parsedNames.add(npc.name)
            if (i % 50 == 0)
                println("Finished dumping $i out of $count npcs")


            val referenceFrames = animFramemaps[npc.walkAnimation] ?: continue

            val matches = animFramemaps.filter { entry ->
                entry.value.any {
                    referenceFrames.any { reference ->
                        reference == it
                    }
                }
            }
            var name = npc.name
            if (name.contains(">")) {
                name = name.substringAfter('>').substringBeforeLast('<')
            }
            try {
                val file = Properties.osrsCachePath.get().resolve("npc_anims/${name}.json").toFile()
                val writer = FileWriter(file)
                gson.toJson(matches.keys.toIntArray(), writer)
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun decodeArchiveId(hexString: String): Int {
    return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
}

private fun decodeArchiveFileId(hexString: String): Int {
    return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
}