package stan.qodat.bench

import kotlin.system.measureNanoTime

internal class BenchSink {
    var bits = 0
        private set

    fun consume(value: Int) {
        bits = bits xor value
    }

    fun consume(value: String?) {
        bits = bits xor (value?.hashCode() ?: 0)
    }

    fun consume(value: IntArray?) {
        bits = bits xor (value?.size ?: 0)
        if (value != null && value.isNotEmpty()) bits = bits xor value[0]
    }

    fun consume(value: ByteArray?) {
        bits = bits xor (value?.size ?: 0)
    }
}

internal data class Timed(val nsPerOp: Double)

internal fun measureNs(warmup: Int, iters: Int, block: () -> Unit): Timed {
    repeat(warmup) { block() }
    val elapsed = measureNanoTime { repeat(iters) { block() } }
    return Timed(elapsed.toDouble() / iters)
}

internal fun formatNs(ns: Double): String = "%.1f".format(ns)

internal fun formatRatio(ours: Double, theirs: Double): String {
    if (theirs == 0.0) return "—"
    return "%.2f".format(ours / theirs)
}

internal data class BenchArgs(val warmup: Int, val iters: Int) {
    companion object {
        fun parse(args: Array<String>): BenchArgs {
            var warmup = 80
            var iters = 2_000
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--warmup" -> warmup = args[++i].toInt()
                    "--iters" -> iters = args[++i].toInt()
                }
                i++
            }
            return BenchArgs(warmup, iters)
        }
    }
}
