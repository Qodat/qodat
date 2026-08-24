package stan.qodat.bench

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer

/**
 * Leftover disio 2.2 patterns vs cheaper alternatives we would add in a fork.
 */
internal object InputBufferBenches {

    fun run(args: BenchArgs, sink: BenchSink) {
        val warmup = args.warmup
        val iters = args.iters
        println()
        println("InputBuffer leftover patterns")
        println("%-42s %12s %12s %10s".format("pattern", "left ns/op", "right ns/op", "left/right"))

        val opcodeBytes = SyntheticPayloads.npcOldest()
        val alloc = measureNs(warmup, iters) {
            val buf = InputBuffer(opcodeBytes)
            sink.consume(walkOpcodes(buf))
        }
        val reused = InputBuffer(opcodeBytes)
        val reuse = measureNs(warmup, iters) {
            reused.offset = 0
            sink.consume(walkOpcodes(reused))
        }
        row("alloc-per-decode vs reuse+offset=0", alloc, reuse)

        val growSmall = measureNs(warmup, iters) {
            sink.consume(fillOutput(OutputBuffer(16), 2_048).size)
        }
        val growFit = measureNs(warmup, iters) {
            sink.consume(fillOutput(OutputBuffer(2_048), 2_048).size)
        }
        row("OutputBuffer grow-16 vs grow-sized", growSmall, growFit)

        val blob = ByteArray(4_096) { it.toByte() }
        val perByte = measureNs(warmup, iters) {
            sink.consume(InputBuffer(blob).readBytes(blob.size).size)
        }
        val arrayCopy = measureNs(warmup, iters) {
            val dest = ByteArray(blob.size)
            System.arraycopy(blob, 0, dest, 0, blob.size)
            sink.consume(dest)
        }
        row("readBytes loop vs System.arraycopy", perByte, arrayCopy)

        val smartBytes = smartPayload()
        val smart = measureNs(warmup, iters) {
            val buf = InputBuffer(smartBytes)
            var acc = 0
            repeat(64) {
                acc = acc xor buf.readUnsignedSmart()
                acc = acc xor buf.readSmart()
                acc = acc xor buf.readBigSmart()
            }
            sink.consume(acc)
        }
        println("%-42s %12s %12s %10s".format("smart/unsignedSmart/bigSmart burst", formatNs(smart.nsPerOp), "—", "—"))
    }

    private fun row(name: String, left: Timed, right: Timed) {
        println(
            "%-42s %12s %12s %10s".format(
                name,
                formatNs(left.nsPerOp),
                formatNs(right.nsPerOp),
                formatRatio(left.nsPerOp, right.nsPerOp),
            )
        )
    }

    private fun walkOpcodes(buf: InputBuffer): Int {
        var n = 0
        while (buf.remaining() > 0) {
            n += buf.readUnsignedByte()
        }
        return n
    }

    private fun fillOutput(out: OutputBuffer, bytes: Int): ByteArray {
        repeat(bytes) { out.writeByte(it) }
        return out.array()
    }

    private fun smartPayload(): ByteArray = OutputBuffer(512).apply {
        repeat(64) {
            writeByte(40)
            writeByte(61)
            writeShort(12)
        }
    }.array()
}
