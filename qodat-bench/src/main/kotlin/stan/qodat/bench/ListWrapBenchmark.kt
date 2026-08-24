package stan.qodat.bench

import stan.qodat.cache.CacheListWrap
import java.util.Arrays

/**
 * Cheap list-wrap comparison: sequential chunks vs common-pool parallel stream.
 *
 * Isolated from `./gradlew test`. Run:
 *   ./gradlew :qodat-bench:benchListWrap
 */
fun main(args: Array<String>) {
    val bench = BenchArgs.parse(args)
    val n = 20_000
    val items = Array(n) { it }
    println("List wrap benchmark (n=$n, warmup=${bench.warmup}, iters=${bench.iters})")
    println()
    println("%-16s %12s".format("approach", "ns/op"))

    val chunked = measureNs(bench.warmup, bench.iters) {
        CacheListWrap.mapNotNull(items) { value -> if (value and 1 == 0) value else null }
    }
    val parallel = measureNs(bench.warmup, bench.iters) {
        Arrays.stream(items).parallel().map { value ->
            if (value and 1 == 0) value else null
        }.toArray().filterNotNull()
    }
    println("%-16s %12s".format("chunked-256", formatNs(chunked.nsPerOp)))
    println("%-16s %12s".format("parallel-stream", formatNs(parallel.nsPerOp)))
    println()
    println("ratio chunked/parallel=${formatRatio(chunked.nsPerOp, parallel.nsPerOp)}")
}
