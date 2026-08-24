package stan.qodat.bench

import com.displee.cache.CacheLibrary
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.measureTimeMillis

/**
 * Index-only sprite list vs decompressing every index-8 archive.
 *
 * Isolated from `./gradlew test`. Run:
 *   ./gradlew :qodat-bench:benchSpriteList
 */
fun main() {
    val cacheDir = Path.of(System.getProperty("user.home"), ".qodat", "downloads", "2026-06-30-rev239-2", "cache")
    if (!cacheDir.resolve("main_file_cache.dat2").exists()) {
        println("No rev239 cache at $cacheDir — skip")
        return
    }
    val library = CacheLibrary(cacheDir.toAbsolutePath().toString())
    val index = library.index(8)
    val ids = index.archiveIds()
    println("interface-3 revision=${library.index(3).revision}")
    val indexMs = measureTimeMillis {
        var sink = 0
        for (i in ids.indices) sink += ids[i]
        check(sink != Int.MIN_VALUE)
    }
    val fullMs = measureTimeMillis {
        var sink = 0
        for (i in ids.indices) {
            sink += index.archive(ids[i])?.file(0)?.data?.size ?: 0
        }
        check(sink >= 0)
    }
    val ratio = if (indexMs <= 0) fullMs.toDouble() else fullMs.toDouble() / indexMs
    println("sprite list n=${ids.size}")
    println("index-only ${indexMs}ms")
    println("full-archive ${fullMs}ms")
    println("full/index ${"%.1f".format(ratio)}x")
    library.close()
}
