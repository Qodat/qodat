package stan.qodat.cache.impl.legacy.storage

import qodat.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyAnimationFrameDefinition
import stan.qodat.cache.impl.legacy.LegacyAnimationSkeletonDefinition
import stan.qodat.cache.impl.legacy.decoder.LegacyFrameDecoder
import stan.qodat.cache.impl.legacy.decoder.LegacyAnimationSkeletonDecoder
import stan.qodat.cache.util.CompressionUtil

import java.nio.file.Files
import java.nio.file.Path
import java.util.HashMap

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-08
 */
object LegacyFrameStorage {

    private val frames = HashMap<Int, Array<LegacyAnimationFrameDefinition?>>()

    fun decode(cachePath: Path, id: Int): LegacyAnimationFrameDefinition? {

        val hexString = Integer.toHexString(id)

        if(hexString.length < 6)
            return null

        val fileId = getFileId(hexString).let { if(it == 65535) 0 else it }
        val frameId = getFrameId(hexString).let { if(it == 65535) 0 else it }

        if (!frames.containsKey(fileId)) {
            try {
                // TODO(perf): gunzips and decodes the entire frame archive on first access of any hash in that file
                val compressedData = Files.readAllBytes(cachePath.resolve("frames").resolve("$fileId.gz"))
                val uncompressedData = CompressionUtil.uncompressGzip(compressedData)

                val `in` = InputStream(uncompressedData)

                val frameMapDecoder = LegacyAnimationSkeletonDecoder()
                val frameMap = frameMapDecoder.load(fileId, `in`)

                val frameLoader = LegacyFrameDecoder()

                frames[fileId] = frameLoader.loadAll(frameMap, `in`)

            } catch (_: Exception) {
            }

        }

        return frames[fileId]?.get(frameId)
    }

    fun getSkeleton(cachePath: Path, frameHash: Int) : LegacyAnimationSkeletonDefinition? {
        return decode(cachePath, frameHash)?.transformationGroup
    }

    private fun getFileId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)
    }

    private fun getFrameId(hexString: String): Int {
        return Integer.parseInt(hexString.substring(hexString.length - 4), 16)
    }
}
