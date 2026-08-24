package stan.qodat.bench

import jagex.Skeleton
import stan.qodat.cache.AnimationSkeletonIndex
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec

/**
 * Framemap / frame / Maya skeleton decode speed.
 *
 * Isolated from `./gradlew test`. Run:
 *   ./gradlew :qodat-bench:benchSkeleton
 *   ./gradlew :qodat-bench:benchSkeleton --args="--iters 8000 --warmup 200"
 */
fun main(args: Array<String>) {
    val bench = BenchArgs.parse(args)
    val sink = BenchSink()
    println("Skeleton benchmark (warmup=${bench.warmup}, iters=${bench.iters})")
    println("Synthetic payloads only — no live cache.")
    println()
    println("%-12s %-22s %12s".format("kind", "payload", "ns/op"))

    framemap(bench, sink)
    frame(bench, sink)
    signature(bench, sink)
    maya(bench, sink)

    println()
    println("sink=${sink.bits} (ignore; keeps work live)")
}

private fun framemap(args: BenchArgs, sink: BenchSink) {
    ours("framemap", "osrs-tiny", args, sink, SyntheticPayloads.osrsFramemapTiny()) { data ->
        sink.consume(AnimationFrameCodec.loadFramemap(1, data).hashCode())
    }
    ours("framemap", "osrs-64", args, sink, SyntheticPayloads.osrsFramemap(64)) { data ->
        sink.consume(AnimationFrameCodec.loadFramemap(2, data).hashCode())
    }
    ours("framemap", "nr317", args, sink, SyntheticPayloads.nr317Framemap()) { data ->
        sink.consume(AnimationFrameCodec.loadFramemap(3, data).hashCode())
    }
}

private fun frame(args: BenchArgs, sink: BenchSink) {
    val tinyMap = AnimationFrameCodec.loadFramemap(9, SyntheticPayloads.osrsFramemapTiny())
    val largeMap = AnimationFrameCodec.loadFramemap(2, SyntheticPayloads.osrsFramemap(64))
    val nrMap = AnimationFrameCodec.loadFramemap(3, SyntheticPayloads.nr317Framemap())
    ours("frame", "osrs-tiny", args, sink, SyntheticPayloads.osrsFrameTiny()) { data ->
        sink.consume(AnimationFrameCodec.loadFrame(tinyMap, 4, data).hashCode())
    }
    ours("frame", "osrs-64", args, sink, SyntheticPayloads.osrsFrame(64)) { data ->
        sink.consume(AnimationFrameCodec.loadFrame(largeMap, 5, data).hashCode())
    }
    ours("frame", "nr317", args, sink, SyntheticPayloads.nr317Frame()) { data ->
        sink.consume(AnimationFrameCodec.loadFrame(nrMap, 8, data).hashCode())
    }
}

private fun signature(args: BenchArgs, sink: BenchSink) {
    ours("signature", "labels", args, sink, SyntheticPayloads.skeletonLabels()) { data ->
        val sig = AnimationSkeletonIndex.skeletonSignature(data)
        sink.consume(sig?.mayaBoneCount ?: 0)
        sink.consume(sig?.labelKey)
    }
}

private fun maya(args: BenchArgs, sink: BenchSink) {
    val noBones = SyntheticPayloads.mayaSkeletonNoBones()
    try {
        sink.consume(Skeleton(1, noBones).getCount())
        ours("maya-skel", "no-bones", args, sink, noBones) { data ->
            sink.consume(Skeleton(1, data).getCount())
        }
    } catch (_: Throwable) {
        println("%-12s %-22s %12s".format("maya-skel", "no-bones", "skip"))
    }
}

private fun ours(
    kind: String,
    payload: String,
    args: BenchArgs,
    sink: BenchSink,
    bytes: ByteArray,
    decode: (ByteArray) -> Unit,
) {
    val timed = measureNs(args.warmup, args.iters) { decode(bytes) }
    println("%-12s %-22s %12s".format(kind, payload, formatNs(timed.nsPerOp)))
    sink.consume(bytes)
}
