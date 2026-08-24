package stan.qodat.cache

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.file.Path

/**
 * Lists and downloads published OSRS caches from archive.runestats.com —
 * the same source [stan.qodat.scene.controller.CacheChooserController] uses.
 */
object OsrsCacheArchive {

    const val BASE_URL = "https://archive.runestats.com/osrs"
    const val USER_AGENT = "qodat"

    @JvmStatic
    fun main(args: Array<String>) {
        val destDir = Path.of(args.firstOrNull() ?: error("usage: OsrsCacheArchive <destDir>"))
        destDir.toFile().mkdirs()
        val name = latestArchiveName()
        println("archive=$name")
        val cacheDir = download(name, destDir)
        println("cacheDir=${cacheDir.toAbsolutePath()}")
        destDir.resolve(ARCHIVE_NAME_FILE).toFile().writeText(name)
    }

    fun listArchiveNames(): List<String> {
        val doc = Jsoup.connect(BASE_URL).userAgent(USER_AGENT).get()
        return doc.select("a")
            .map { col -> col.attr("href") }
            .filter { it.length > 10 }
            .reversed()
    }

    fun latestArchiveName(): String =
        listArchiveNames().firstOrNull()
            ?: error("No OSRS caches listed at $BASE_URL")

    /**
     * Downloads [archiveName] and extracts it under [destDir]/&lt;stem&gt;/.
     * Returns the extracted `cache/` directory.
     */
    fun download(archiveName: String, destDir: Path): Path {
        val destFolder = destDir.resolve(archiveName.removeSuffix(".tar.gz")).toFile()
        destFolder.mkdirs()
        val conn = URI.create("$BASE_URL/$archiveName").toURL().openConnection()
        conn.addRequestProperty("User-Agent", USER_AGENT)
        BufferedInputStream(conn.getInputStream()).use { inputStream ->
            TarArchiveInputStream(GzipCompressorInputStream(inputStream)).use { tarIn ->
                var tarEntry = tarIn.nextEntry
                while (tarEntry != null) {
                    val dest = File(destFolder, tarEntry.name)
                    if (tarEntry.isDirectory) {
                        dest.mkdirs()
                    } else {
                        dest.parentFile?.mkdirs()
                        dest.outputStream().buffered().use { out -> tarIn.copyTo(out) }
                    }
                    tarEntry = tarIn.nextEntry
                }
            }
        }
        val cacheDir = destFolder.resolve("cache")
        if (!cacheDir.isDirectory) {
            error("Extracted $archiveName did not contain a cache/ directory")
        }
        return cacheDir.toPath()
    }

    const val ARCHIVE_NAME_FILE = "archive-name.txt"
}
