package qodat.cache.definition

interface SpriteDefinition {

    val id: Int
    val frame: Int
    val offsetX: Int
    val offsetY: Int
    val width: Int
    val height: Int
    val pixels: IntArray
    val maxWidth: Int
    val maxHeight: Int

    var pixelIdx: ByteArray
    var palette: IntArray
}