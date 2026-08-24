package stan.qodat.bench

import net.runelite.cache.definitions.loaders.ItemLoader as RuneLiteItemLoader
import net.runelite.cache.definitions.loaders.NpcLoader as RuneLiteNpcLoader
import net.runelite.cache.definitions.loaders.ObjectLoader as RuneLiteObjectLoader
import net.runelite.cache.definitions.loaders.SequenceLoader as RuneLiteSequenceLoader
import net.runelite.cache.definitions.loaders.SpotAnimLoader as RuneLiteSpotAnimLoader
import net.runelite.cache.definitions.loaders.SpriteLoader as RuneLiteSpriteLoader
import net.runelite.cache.definitions.loaders.TextureLoader as RuneLiteTextureLoader
import stan.qodat.cache.impl.oldschool.loader.ItemLoader226
import stan.qodat.cache.impl.oldschool.loader.NpcLoader
import stan.qodat.cache.impl.oldschool.loader.ObjectLoader
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader206
import stan.qodat.cache.impl.oldschool.loader.SequenceLoader226
import stan.qodat.cache.impl.oldschool.loader.SpotAnimLoader
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader
import stan.qodat.cache.impl.oldschool.loader.TextureLoader

/**
 * Synthetic decode speed vs RuneLite on the same bytes.
 *
 * Isolated from `./gradlew test`. Run:
 *   ./gradlew :qodat-bench:benchDecode
 *   ./gradlew :qodat-bench:benchDecode --args="--iters 5000 --warmup 100"
 */
fun main(args: Array<String>) {
    val bench = BenchArgs.parse(args)
    val sink = BenchSink()
    println("Decode benchmark (warmup=${bench.warmup}, iters=${bench.iters})")
    println("Synthetic payloads only — no live cache. Ratio is ours/RL (<1 = we are faster).")
    println()
    println("%-10s %-22s %12s %12s %10s".format("type", "payload", "ours ns/op", "rl ns/op", "ours/rl"))

    npc(bench, sink)
    obj(bench, sink)
    spot(bench, sink)
    texture(bench, sink)
    sprite(bench, sink)
    item(bench, sink)
    sequence(bench, sink)

    InputBufferBenches.run(bench, sink)
    println()
    println("sink=${sink.bits} (ignore; keeps work live)")
}

private fun npc(args: BenchArgs, sink: BenchSink) {
    val ours = NpcLoader()
    val rl = RuneLiteNpcLoader()
    compare("npc", "oldest", args, sink, SyntheticPayloads.npcOldest(), { ours.load(1, it) }, { rl.load(1, it) })
    compare("npc", "mid-int-model", args, sink, SyntheticPayloads.npcMidIntModel(), { ours.load(2, it) }, { rl.load(2, it) })
    compare("npc", "newest", args, sink, SyntheticPayloads.npcNewest(), { ours.load(3, it) }, { rl.load(3, it) })

    val oursPre = NpcLoader().configureForRevision(SyntheticPayloads.NPC_REV_PRE_210)
    val rlPre = RuneLiteNpcLoader().also { it.configureForRevision(SyntheticPayloads.NPC_REV_PRE_210) }
    compare("npc", "headicon-pre210", args, sink, SyntheticPayloads.npcPre210HeadIcon(), { oursPre.load(4, it) }, { rlPre.load(4, it) })

    val ours210 = NpcLoader().configureForRevision(SyntheticPayloads.NPC_REV_210)
    val rl210 = RuneLiteNpcLoader().also { it.configureForRevision(SyntheticPayloads.NPC_REV_210) }
    compare("npc", "headicon-rev210", args, sink, SyntheticPayloads.npcRev210HeadIcon(), { ours210.load(5, it) }, { rl210.load(5, it) })
}

private fun obj(args: BenchArgs, sink: BenchSink) {
    val ours = ObjectLoader()
    val rl = RuneLiteObjectLoader()
    compare("object", "oldest", args, sink, SyntheticPayloads.objectOldest(), { ours.load(1, it) }, { rl.load(1, it) })
    compare("object", "mid-int-model", args, sink, SyntheticPayloads.objectMidIntModel(), { ours.load(2, it) }, { rl.load(2, it) })
    compare("object", "newest", args, sink, SyntheticPayloads.objectNewest(), { ours.load(3, it) }, { rl.load(3, it) })

    val oursPre = ObjectLoader().configureForRevision(SyntheticPayloads.OBJ_REV_PRE_220)
    val rlPre = RuneLiteObjectLoader().also { it.configureForRevision(SyntheticPayloads.OBJ_REV_PRE_220) }
    compare("object", "sound-pre220", args, sink, SyntheticPayloads.objectPre220Sound(), { oursPre.load(4, it) }, { rlPre.load(4, it) })

    val ours220 = ObjectLoader().configureForRevision(SyntheticPayloads.OBJ_REV_220)
    val rl220 = RuneLiteObjectLoader().also { it.configureForRevision(SyntheticPayloads.OBJ_REV_220) }
    compare("object", "sound-rev220", args, sink, SyntheticPayloads.objectRev220Sound(), { ours220.load(5, it) }, { rl220.load(5, it) })
}

private fun spot(args: BenchArgs, sink: BenchSink) {
    val ours = SpotAnimLoader()
    val rl = RuneLiteSpotAnimLoader()
    compare("spotanim", "oldest", args, sink, SyntheticPayloads.spotOldest(), { ours.load(1, it) }, { rl.load(1, it) })
    compare("spotanim", "mid-int-model", args, sink, SyntheticPayloads.spotMidIntModel(), { ours.load(2, it) }, { rl.load(2, it) })
    compare("spotanim", "newest", args, sink, SyntheticPayloads.spotNewest(), { ours.load(3, it) }, { rl.load(3, it) })
}

private fun texture(args: BenchArgs, sink: BenchSink) {
    val ours = TextureLoader()
    val rl233 = RuneLiteTextureLoader().setRev233(true)
    val rlLegacy = RuneLiteTextureLoader().setRev233(false)
    compare("texture", "rev233", args, sink, SyntheticPayloads.textureRev233(), { ours.load(9, it) }, { rl233.load(9, it) })
    compare("texture", "legacy-multi", args, sink, SyntheticPayloads.textureLegacyMulti(), { ours.load(4, it) }, { rlLegacy.load(4, it) })
    compare("texture", "legacy-single", args, sink, SyntheticPayloads.textureLegacySingle(), { ours.load(1, it) }, { rlLegacy.load(1, it) })
}

private fun sprite(args: BenchArgs, sink: BenchSink) {
    val ours = SpriteLoader()
    val rl = RuneLiteSpriteLoader()
    val smallIters = (args.iters / 4).coerceAtLeast(50)
    val small = args.copy(iters = smallIters)
    compare("sprite", "1x1", args, sink, SyntheticPayloads.spriteSingle(), { ours.load(1, it) }, { rl.load(1, it) })
    compare("sprite", "vertical-2x2", args, sink, SyntheticPayloads.spriteVertical(), { ours.load(2, it) }, { rl.load(2, it) })
    compare("sprite", "alpha", args, sink, SyntheticPayloads.spriteAlpha(), { ours.load(3, it) }, { rl.load(3, it) })
    compare("sprite", "multi-frame", args, sink, SyntheticPayloads.spriteMultiFrame(), { ours.load(4, it) }, { rl.load(4, it) })
    compare("sprite", "32x32", small, sink, SyntheticPayloads.sprite32(), { ours.load(5, it) }, { rl.load(5, it) })
}

private fun item(args: BenchArgs, sink: BenchSink) {
    val ours = ItemLoader226()
    val rl = RuneLiteItemLoader()
    compare("item", "core", args, sink, SyntheticPayloads.itemCore(), { ours.load(4151, it) }, { rl.load(4151, it) })
    oursOnly("item", "int-model-226", args, sink, SyntheticPayloads.itemIntModel()) { ours.load(6, it) }
}

private fun sequence(args: BenchArgs, sink: BenchSink) {
    val ours226 = SequenceLoader226()
    val ours226Maya = SequenceLoader226().also { it.configureForRevision(SyntheticPayloads.SEQ_REV_226) }
    val ours206 = SequenceLoader206()
    val rl = RuneLiteSequenceLoader()
    val rl206 = RuneLiteSequenceLoader().also { it.configureForRevision(1141) }
    compare("seq", "frame-tables", args, sink, SyntheticPayloads.seqFrameTables(), { ours226.load(42, it) }, { rl.load(42, it) })
    oursOnly("seq", "226-maya-remap", args, sink, SyntheticPayloads.seq226Maya()) { ours226Maya.load(2, it) }
    compare("seq", "206-packed-sounds", args, sink, SyntheticPayloads.seq206PackedSounds(), { ours206.load(7, it) }, { rl206.load(7, it) })
}

private fun compare(
    type: String,
    payload: String,
    args: BenchArgs,
    sink: BenchSink,
    bytes: ByteArray,
    ours: (ByteArray) -> Any,
    rl: (ByteArray) -> Any,
) {
    val oursT = measureNs(args.warmup, args.iters) { sink.consume(ours(bytes)) }
    val rlT = try {
        sink.consume(rl(bytes))
        measureNs(args.warmup, args.iters) { sink.consume(rl(bytes)) }
    } catch (_: Throwable) {
        println("%-10s %-22s %12s %12s %10s".format(type, payload, formatNs(oursT.nsPerOp), "rl-err", "—"))
        return
    }
    println(
        "%-10s %-22s %12s %12s %10s".format(
            type,
            payload,
            formatNs(oursT.nsPerOp),
            formatNs(rlT.nsPerOp),
            formatRatio(oursT.nsPerOp, rlT.nsPerOp),
        )
    )
}

private fun oursOnly(
    type: String,
    payload: String,
    args: BenchArgs,
    sink: BenchSink,
    bytes: ByteArray,
    ours: (ByteArray) -> Any,
) {
    val oursT = measureNs(args.warmup, args.iters) { sink.consume(ours(bytes)) }
    println("%-10s %-22s %12s %12s %10s".format(type, payload, formatNs(oursT.nsPerOp), "—", "—"))
}

private fun BenchSink.consume(value: Any) {
    when (value) {
        is Array<*> -> consume(value.size)
        else -> consume(value.hashCode())
    }
}
