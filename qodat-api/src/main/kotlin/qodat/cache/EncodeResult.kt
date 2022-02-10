package qodat.cache

import java.io.File

/**
 * Represents a result of an [Encoder.encode] result.
 *
 * @param file an optional [File] to be used for drag-and-drop support.
 */
data class EncodeResult(val file: File)