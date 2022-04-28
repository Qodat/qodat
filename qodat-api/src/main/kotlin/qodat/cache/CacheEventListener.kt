package qodat.cache

fun interface CacheEventListener {

    fun on(event: CacheEvent)
}