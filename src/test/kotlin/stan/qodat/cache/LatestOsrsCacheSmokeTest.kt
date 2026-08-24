package stan.qodat.cache

import com.displee.cache.CacheLibrary
import qodat.cache.models.RSModelLoader
import stan.qodat.cache.impl.oldschool.loader.InterfaceLoader237
import stan.qodat.cache.impl.oldschool.loader.ItemLoader226
import stan.qodat.cache.impl.oldschool.loader.NpcLoader
import stan.qodat.cache.impl.oldschool.loader.ObjectLoader
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226
import stan.qodat.cache.impl.oldschool.loader.SpotAnimLoader
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader
import java.io.File
import org.junit.Assume
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opens a real OSRS cache and decodes a sample of each asset type.
 *
 * Regular `./gradlew test` skips this unless [CACHE_DIR_PROPERTY] is set.
 * The watchdog runs `:cacheSmoke`, which sets [REQUIRED_PROPERTY] so a missing
 * cache fails instead of skip.
 */
class LatestOsrsCacheSmokeTest {

    @Test
    fun sampleIdsCoversHeadTailAndStaysWithinLimit() {
        val ids = IntArray(1000) { it }
        val sampled = sampleIds(ids, 256)
        assertTrue(sampled.size <= 256)
        assertTrue(sampled.size >= 3)
        assertTrue(0 in sampled.toSet())
        assertTrue(999 in sampled.toSet())
        assertTrue(sampleIds(intArrayOf(1, 2, 3), 256).contentEquals(intArrayOf(1, 2, 3)))
    }

    @Test
    fun decodeSampleOfEachType() {
        val cacheDir = cacheDirectory()
        val report = StringBuilder()
        val failures = ArrayList<String>()
        report.appendLine("cacheDir=$cacheDir")

        val store = CacheLibrary(cacheDir.absolutePath)
        try {
            decodeConfigArchive(
                store, report, failures,
                kind = "npc", indexId = 2, archiveId = 9, sample = SAMPLE,
            ) { id, data, revision ->
                NpcLoader().configureForRevision(revision).load(id, data)
            }
            decodeConfigArchive(
                store, report, failures,
                kind = "object", indexId = 2, archiveId = 6, sample = SAMPLE,
            ) { id, data, revision ->
                ObjectLoader().configureForRevision(revision).load(id, data)
            }
            decodeConfigArchive(
                store, report, failures,
                kind = "item", indexId = 2, archiveId = 10, sample = SAMPLE,
            ) { id, data, _ ->
                ItemLoader226().load(id, data)
            }
            decodeConfigArchive(
                store, report, failures,
                kind = "sequence", indexId = 2, archiveId = 12, sample = SAMPLE,
            ) { id, data, revision ->
                decodeSequence(id, data, revision)
            }
            decodeConfigArchive(
                store, report, failures,
                kind = "spotanim", indexId = 2, archiveId = 13, sample = SAMPLE,
            ) { id, data, _ ->
                SpotAnimLoader().load(id, data)
            }
            decodeSpriteIndex(store, report, failures)
            decodeInterfaces(store, report, failures)
            decodeModels(store, report, failures)
        } finally {
            store.close()
        }

        writeReport(report)
        assertTrue(failures.isEmpty(), failures.joinToString(separator = "\n", prefix = "decode failures:\n"))
    }

    private fun cacheDirectory(): File {
        val raw = System.getProperty(CACHE_DIR_PROPERTY).orEmpty()
            .ifBlank { System.getenv("QODAT_CACHE_DIR").orEmpty() }
        val required = System.getProperty(REQUIRED_PROPERTY, "false").equals("true", ignoreCase = true)
        if (required) {
            assertTrue(raw.isNotBlank(), "Set -Pqodat.cache.dir=/path/to/cache (extracted OSRS cache)")
        } else {
            Assume.assumeTrue("skip: $CACHE_DIR_PROPERTY not set", raw.isNotBlank())
        }
        val dir = File(raw)
        assertTrue(dir.isDirectory, "qodat.cache.dir is not a directory: $raw")
        return dir
    }

    private fun decodeSequence(id: Int, data: ByteArray, revision: Int): Any {
        val loader226 = SequenceLoader226().also { it.configureForRevision(revision) }
        return try {
            loader226.load(id, data)
        } catch (_: Exception) {
            SequenceLoader206().load(id, data)
        }
    }

    private fun decodeConfigArchive(
        store: CacheLibrary,
        report: StringBuilder,
        failures: MutableList<String>,
        kind: String,
        indexId: Int,
        archiveId: Int,
        sample: Int,
        decode: (id: Int, data: ByteArray, revision: Int) -> Any,
    ) {
        val archive = store.index(indexId).archive(archiveId)
        if (archive == null) {
            failures += "$kind: archive $indexId/$archiveId missing"
            report.appendLine("$kind: MISSING archive $indexId/$archiveId")
            return
        }
        val ids = archive.fileIds()
        val picked = sampleIds(ids, sample)
        var ok = 0
        for (id in picked) {
            val data = archive.file(id)?.data
            if (data == null) {
                failures += "$kind $id: null data"
                continue
            }
            try {
                decode(id, data, archive.revision)
                ok++
            } catch (e: Exception) {
                failures += "$kind $id: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
        report.appendLine(
            "$kind: revision=${archive.revision} files=${ids.size} sampled=${picked.size} ok=$ok fail=${picked.size - ok}",
        )
        if (ok == 0) {
            failures += "$kind: 0 successful decodes (sampled ${picked.size} of ${ids.size})"
        }
    }

    private fun decodeSpriteIndex(
        store: CacheLibrary,
        report: StringBuilder,
        failures: MutableList<String>,
    ) {
        val index = store.index(8)
        val ids = index.archiveIds()
        report.appendLine("sprites-index: archives=${ids.size}")
        if (ids.isEmpty()) {
            failures += "sprites-index: 0 archives"
            return
        }
        val picked = sampleIds(ids, SPRITE_SAMPLE)
        val loader = SpriteLoader()
        var ok = 0
        for (id in picked) {
            val data = store.data(8, id)
            if (data == null) {
                failures += "sprite $id: null data"
                continue
            }
            try {
                val frames = loader.load(id, data)
                if (frames.isEmpty()) {
                    failures += "sprite $id: 0 frames"
                } else {
                    ok++
                }
            } catch (e: Exception) {
                failures += "sprite $id: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
        report.appendLine("sprites-decode: sampled=${picked.size} ok=$ok fail=${picked.size - ok}")
        if (ok == 0) {
            failures += "sprites-decode: 0 successful decodes"
        }
    }

    private fun decodeInterfaces(
        store: CacheLibrary,
        report: StringBuilder,
        failures: MutableList<String>,
    ) {
        val ids = store.index(3).archiveIds()
        report.appendLine("interfaces-index: groups=${ids.size}")
        if (ids.isEmpty()) {
            failures += "interfaces-index: 0 groups"
            return
        }
        val picked = sampleIds(ids, INTERFACE_GROUPS)
        var ok = 0
        var widgets = 0
        for (groupId in picked) {
            val archive = store.index(3).archive(groupId)
            if (archive == null) {
                failures += "interface group $groupId: missing archive"
                continue
            }
            val loader = InterfaceLoader237().configureForRevision(archive.revision)
            var groupOk = true
            for (fileId in archive.fileIds()) {
                val data = archive.file(fileId)?.data ?: continue
                if (data.size < 2) continue
                val widgetId = (groupId shl 16) + fileId
                try {
                    loader.load(widgetId, data)
                    widgets++
                } catch (e: Exception) {
                    groupOk = false
                    failures += "interface $groupId.$fileId: ${e.javaClass.simpleName}: ${e.message}"
                }
            }
            if (groupOk) ok++
        }
        report.appendLine(
            "interfaces-decode: sampledGroups=${picked.size} okGroups=$ok widgets=$widgets",
        )
    }

    private fun decodeModels(
        store: CacheLibrary,
        report: StringBuilder,
        failures: MutableList<String>,
    ) {
        val ids = store.index(7).archiveIds()
        report.appendLine("models-index: archives=${ids.size}")
        if (ids.isEmpty()) {
            failures += "models-index: 0 archives"
            return
        }
        val picked = sampleIds(ids, MODEL_SAMPLE)
        val loader = RSModelLoader()
        var ok = 0
        for (id in picked) {
            val data = store.data(7, id)
            if (data == null) {
                failures += "model $id: null data"
                continue
            }
            try {
                loader.load(id.toString(), data)
                ok++
            } catch (e: Exception) {
                failures += "model $id: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
        report.appendLine("models-decode: sampled=${picked.size} ok=$ok fail=${picked.size - ok}")
        if (ok == 0) {
            failures += "models-decode: 0 successful decodes"
        }
    }

    private fun writeReport(report: StringBuilder) {
        val outDir = File("build/reports/cache-smoke")
        outDir.mkdirs()
        val file = File(outDir, "summary.txt")
        file.writeText(report.toString())
        println(report)
    }

    companion object {
        const val CACHE_DIR_PROPERTY = "qodat.cache.dir"
        const val REQUIRED_PROPERTY = "qodat.cache.required"
        private const val SAMPLE = 256
        private const val SPRITE_SAMPLE = 48
        private const val INTERFACE_GROUPS = 48
        private const val MODEL_SAMPLE = 48

        internal fun sampleIds(ids: IntArray, limit: Int): IntArray {
            if (ids.size <= limit) return ids
            val out = LinkedHashSet<Int>(limit)
            val edge = (limit / 3).coerceAtLeast(1)
            for (i in 0 until edge) out.add(ids[i])
            for (i in ids.size - edge until ids.size) out.add(ids[i])
            val stride = (ids.size / (limit - out.size).coerceAtLeast(1)).coerceAtLeast(1)
            var i = 0
            while (out.size < limit && i < ids.size) {
                out.add(ids[i])
                i += stride
            }
            return out.toIntArray()
        }
    }
}
