package stan.qodat.cache.impl.legacy.decoder

import qodat.cache.io.InputStream

internal fun InputStream.readStringOld(): String {
    val start = offset
    while (true) {
        if (readByte().toInt() == 10) break
    }
    return String(array, start, offset - start - 1)
}
