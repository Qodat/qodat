package stan.qodat.cache.impl.displee

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import net.runelite.cache.IndexType


fun CacheLibrary.getIndex(indexType: IndexType): Index =
    index(indexType.number)
