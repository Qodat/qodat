# WIP: Remove `net.runelite:cache` — migration plan

Temp coordination file. Do not treat as shipped docs. No compile / commit in the investigation pass that wrote this.

**Goal:** drop the RuneLite cache artifact entirely. Decode/encode lives in the cache fork (`qodat-cache` = [Qodat/rs-cache](https://github.com/Qodat/rs-cache)), aligned with the client deob, with one loader family that reads older caches.

**Production today:** `DispleeCache` + composite `qodat-cache` (`CacheLibrary`). RuneLite is still on the `qodat-api` compile/runtime classpath and is still used for many definition loaders and types.

---

## 1. Every RuneLite dependency

### Gradle

| Where | What |
|---|---|
| `gradle/libs.versions.toml` | `runeliteCache = "1.12.30"`; `runelite-cache = { module = "net.runelite:cache", version.ref = "runeliteCache" }` |
| `qodat-api/build.gradle.kts` | `api(libs.runelite.cache) { exclude(group = "com.google.guava") }` — **this is the only declaration**. It leaks RL to the desktop app and all tests. |
| `build.gradle.kts` | `maven(url = "https://repo.runelite.net")` in `allprojects.repositories` |
| `qodat-cache/build.gradle.kts` | **no** RuneLite |

No other `net.runelite:*` artifacts.

### Types we actually use

**IO** — **Phase A complete.** No remaining `net.runelite.cache.io` in source. Owned loaders, legacy decoders, `io_extensions`, `model_exporter`, `ModelLoader.java`, and tests use `qodat.cache.io.*`.

**FS (OldschoolCacheRuneLite + AnimationExporter only)**

- `net.runelite.cache.fs.Store`, `Index`, `ArchiveFiles`, `Container`, `FSFile`
- `net.runelite.cache.index.FileData`

**Index / config enums**

- `net.runelite.cache.IndexType` — `CONFIGS`, `MODELS`, `ANIMATIONS`, `SKELETONS` (also via `DispleeCacheExt.getIndex`)
- `net.runelite.cache.ConfigType` — `SEQUENCE`, `SPOTANIM` (exporter + Oldschool path)

**RuneLite manager classes (OldschoolCacheRuneLite only)**

- `NpcManager`, `ItemManager`, `ObjectManager`, `TextureManager`, `SpriteManager`
- `InterfaceManager` — call site uses the typo API `getIntefaceGroup(groupId)`

**Loaders still used on the live Displee path**

- `NpcLoader` + `configureForRevision`
- `ObjectLoader` + `configureForRevision`
- `SpotAnimLoader`
- `SpriteLoader`
- `TextureLoader` (`DispleeCache.getTexture` + `method2680` sprite callback)

**Loaders used only on OldschoolCacheRuneLite**

- `SequenceLoader` (RL) with `configureForRevision`; fallback is our `SequenceLoader206`
- `SpotAnimLoader` again

**Definition classes**

- `NpcDefinition` — `NpcManager`, `NpcPrimaryAnimations`, `NpcAnimParser`, tests
- `ObjectDefinition` — `ObjectManager.convert`
- `SpotAnimDefinition` — `SpotAnimManager`
- `SpriteDefinition` — `SpriteManager`, `RuneliteSpriteDefinition`
- `InterfaceDefinition` — `InterfaceLoader237`, `InterfaceManager`, `RuneliteInterfaceDefinition`
- `ClientScript1Instruction` — `InterfaceLoader237`, wrapper opcode map
- `FrameDefinition`, `FramemapDefinition` — `AnimationFrameCodec`, `AnimManager`
- `SequenceDefinition` + nested `Sound` — leaked through `qodat.cache.definition.AnimationMayaDefinition`
- `ModelDefinition` — `qodat-api` `ModelLoader.java` (OSRS type1/2/3)

**Util**

- `net.runelite.cache.util.GZip` — `ModelTreeItem` RS2 export

### Import map (source only; ignore `build/` and `.idea/`)

**qodat-api (leaks RL to everyone)**

- `qodat-api/src/main/kotlin/qodat/cache/definition/AnimationMayaDefinition.kt` — `SequenceDefinition.Sound`
- `qodat-api/src/main/kotlin/qodat/cache/models/ModelLoader.java` — `ModelDefinition`, `InputStream`
- `qodat-api/src/main/kotlin/qodat/cache/models/RSModelLoader.kt` — `fromOsrsModel(net.runelite.cache.definitions.ModelDefinition)`

**Live Displee path**

- `src/main/kotlin/stan/qodat/cache/impl/displee/DispleeCache.kt` — `TextureLoader`, RL `InterfaceDefinition` in `LazyInterfaceList`
- `src/main/kotlin/stan/qodat/cache/impl/displee/DispleeCacheExt.kt` — `IndexType`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/NpcManager.kt` — `NpcDefinition`, `NpcLoader`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/ObjectManager.kt` — `ObjectLoader`, RL `ObjectDefinition`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/SpriteManager.kt` — `SpriteDefinition`, `SpriteLoader`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/SpotAnimManager.kt` — `SpotAnimDefinition`, `SpotAnimLoader`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/AnimManager.kt` — `FramemapDefinition`, `SequenceDefinition`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/InterfaceManager.kt` — RL `InterfaceDefinition`
- `src/main/kotlin/stan/qodat/cache/impl/displee/anims/NpcAnimParser.kt` — `NpcDefinition`
- `src/main/kotlin/stan/qodat/cache/NpcPrimaryAnimations.kt` — `NpcDefinition`
- `src/main/kotlin/stan/qodat/scene/runescape/animation/AnimationMaya.kt` — `IndexType`

**Owned loaders still on RL IO / RL defs**

- `.../oldschool/loader/ItemLoader226.kt` — **Phase A first slice: now `qodat.cache.io.InputStream`**
- `.../oldschool/loader/SequenceLoader206.kt`, `SequenceLoader226.kt`, `SequenceStream.kt` — **Phase A first slice: now `qodat.cache.io.InputStream`**
- `.../oldschool/loader/InterfaceLoader237.kt` — **Phase A frames/interfaces/legacy: now `qodat.cache.io.InputStream`**; still RL interface/script types
- `.../oldschool/loader/AnimationFrameCodec.kt` — **Phase A frames/interfaces/legacy: now `qodat.cache.io.InputStream`**; still RL `FrameDefinition` / `FramemapDefinition`
- `.../oldschool/definition/RuneliteInterfaceDefinition.kt`, `RuneliteSpriteDefinition.kt`

**Oldschool / encode leftover**

- `.../oldschool/OldschoolCacheRuneLite.kt` — star-import `net.runelite.cache.*`, Store, SequenceLoader, SpotAnimLoader, Framemap, Sequence
- `.../animation/AnimationExporter.kt` — ConfigType, IndexType, ArchiveFiles, Container, FSFile, FileData; writes via `OldschoolCacheRuneLite.store`
- `.../util/io_extensions.kt`, `model_exporter.kt` — **Phase A remainder: now `qodat.cache.io.OutputStream`**
- `.../scene/control/tree/ModelTreeItem.kt` — `GZip`

**Legacy (317 DAT/IDX)** — **Phase A frames/interfaces/legacy: now `qodat.cache.io.InputStream`**

- `LegacyStream`, `LegacyNpcDecoder`, `LegacyObjectDecoder`, `LegacyItemDecoder`, `LegacyKitDecoder`, `LegacySequenceDecoder`, `LegacyFrameDecoder`, `LegacyAnimationSkeletonDecoder`
- `LegacyIndexDat`, `LegacyAnimationStorage`, `LegacyFrameStorage`, `LegacyBodyKitStorage`

**Do not touch this pass (per hard rules)**

- `src/main/kotlin/stan/qodat/cache/impl/displee/DispleeMain.kt` — `ConfigType`, `IndexType`

**Tests** (all synthesize via `qodat.cache.io.OutputStream` unless noted)

- Item / Sequence 206 / 226 / SequenceStream / AnimationFrameCodec / InterfaceLoader237
- NpcLoader / ObjectLoader / SpriteLoader / TextureLoader
- `RuneliteInterfaceDefinitionTest`, `RuneliteSpriteDefinitionTest`
- `OldschoolCacheDefinitionMappingTest`, manager mapping tests
- `WidgetLayoutTest`, `NpcPrimaryAnimationsTest`, `AnimationMayaDefinitionFieldsTest` (FQCN `SequenceDefinition.Sound`)
- `IoExtensionsTest`, `RSModelLoaderDecodeTest`, `LegacyNpcDecoderTest`, `LegacyObjectDecoderTest`

---

## 2. What we already own

### Owned codecs (keep / move, do not rewrite from RL)

| Codec | Output type | Still needs RL? |
|---|---|---|
| `ItemLoader226` | our `ItemDefinition226` : `qodat.cache.definition.ItemDefinition` | no (IO swapped) |
| `SequenceLoader226` + `configureForRevision` | our `SequenceDefinition226` | `Sound.toRuneliteSound()` only |
| `SequenceLoader206` | our `SequenceDefinition206` | no (IO swapped) |
| `SequenceStream` (`forEachOpcode`, shared opcodes 1–12, 18) | — | no (IO swapped) |
| `InterfaceLoader237` | **RL** `InterfaceDefinition` | defs only (IO swapped) |
| `AnimationFrameCodec` | maps to our frame/skeleton interfaces; internals are RL Frame/Framemap | defs only (IO swapped) |
| Legacy `*Decoder` / `*Storage` | our builders | no (IO swapped) |
| `RSModelLoader` | our `RS2Model` | OSRS type1/2/3 still delegates to `ModelLoader.java` (now `qodat.cache.io`) |
| `qodat.cache.io.InputStream` | copy of RL InputStream (Adam BSD header) | all owned + legacy loaders; `RSModelLoader`; `ModelLoader.java` |
| `qodat.cache.io.OutputStream` | pair of RL OutputStream (Adam BSD header; no Guava) | tests + `io_extensions` + `model_exporter` |

### Cache implementations — who is production?

| Impl | Backing | Role |
|---|---|---|
| **`DispleeCache`** | `com.displee.cache.CacheLibrary` from included `qodat-cache` | **Production viewer.** Set in `Qodat.kt` / `MainController` as `Properties.viewerCache`. |
| **`QodatCache`** | JSON under `Properties.qodatCachePath` | **Production editor.** Encodes models/NPCs to JSON. Delegates live animations/frames to `DispleeCache`. |
| **`OldschoolCacheRuneLite`** | RL `Store` on the same `osrsCachePath` | Legacy `"LIVE"` plugin. Not assigned as viewer. Still used by `AnimationExporter` (mostly commented-out save). `getIntefaceGroup` typo. Sequence path = RL `SequenceLoader` then our 206 fallback. |
| **`LegacyCache`** | 317 DAT/IDX via `Properties.legacyCachePath` | Implemented, **never selected** as viewer/editor. Chooser still stores a 667 path. |

`qodat-api` `Cache` / definition interfaces stay. They are the viewer contract.

### Revision gating today

**Sequence (ours)** — archive revision of config archive 12:

- `REV_220_SEQ_ARCHIVE_REV = 1141` → `rev > 1141` uses unpacked frame-sound fields (`rev220FrameSounds`)
- `REV_226_SEQ_ARCHIVE_REV = 1268` → `rev > 1268` remaps opcodes 13–16 (Maya id / sounds / start-end / verticalOffset)
- Tests: `configureForRevision(1141)` vs `1142`, `1268` vs `1269` in `SequenceLoader226ExtraTest`
- **Fallback:** `AnimManager.loadSeq` tries 226, on any exception decodes with `SequenceLoader206` (opcode 13 = packed 24-bit sounds, 14 = Maya id)

**Items (ours)** — single `ItemLoader226`, no rev gate. Unknown opcodes ignored (`else -> Unit`). Short model ids (1/23–26/78–93) and int model ids (44–54) coexist so a newer decoder reads older payloads.

**NPC / object (RL)** — `NpcLoader.configureForRevision(archive.revision)`, `ObjectLoader.configureForRevision`. Tests use dummy rev `1000`.

**Interfaces (ours)** — magic `0xAABBCCDD` (`InterfaceLoader237.hasRev237Magic`). No magic → vanilla IF1 (`data[0] != -1`) or IF3 (`data[0] == -1`). Rev237 IF3 model id is `int`; vanilla is `unsignedShort`/`0xFFFF`.

**Frames (ours)** — magic `0xF9 0xF9` (`AnimationFrameCodec.NR_317_MAGIC`). Else official OSRS frame (copied from RL `FrameLoader`).

**Models** — trailer bytes: type3 `-1,-3`, type2 `-1,-2`, type1 `-1,-1`; else `loadLowRev` (18-byte header). High-rev header 23. RS3 branch separate.

**Maya index pick** — `AnimationMaya`: if models index revision `>= 969` use index 22, else `IndexType.ANIMATIONS`; skeletons always `IndexType.SKELETONS`.

**Maps** — not implemented. `docs/TODO-scene-background-map.md` still assumes RL `Region` / `MapLoader`. Out of scope until sprites/textures are ours.

---

## 3. The cache fork

- **Path:** `/Users/stan/Documents/Work/rs/qodat/qodat-cache`
- **Git:** submodule `.gitmodules` → `git@github.com:Qodat/rs-cache.git`
- **Composite:** `settings.gradle.kts` `includeBuild("qodat-cache")` substitutes `com.displee:rs-cache-library` → project `:`
- **Identity:** `group = com.displee`, `version = 7.3.0`, `rootProject.name = "rs-cache-library"` — Displee library, forked
- **Depends on:** `disio`, lzma, ant, coroutines. **Not** RuneLite.

**What it already does:** FS only.

- Read/write all RS cache formats (317 + modern)
- Index / archive / file CRUD, XTEA, BZIP2/GZIP/LZMA, CRC/Whirlpool, ukeys, rebuild
- `archive.revision` / `index.revision` (this is what our loaders already pass to `configureForRevision`)

**What it does not do:** no item/npc/object/seq/sprite/texture/interface/frame/model **definition** decode or encode.

**“Move everything to the fork” in module terms**

1. In `Qodat/rs-cache` (`qodat-cache`), add a codec layer (suggested packages):
   - `com.displee.cache.io` — move/adapt `qodat.cache.io.InputStream` + new `OutputStream`
   - `com.displee.cache.def.*` — mutable decode targets + opcode-table loaders
   - Keep `CacheLibrary` as the store
2. `qodat-api` drops `api(libs.runelite.cache)`. Viewer interfaces stay (`Cache`, `ItemDefinition`, `NPCDefinition`, …). Add a small `AnimationSound` so `AnimationMayaDefinition` no longer imports RL.
3. Desktop `DispleeCache` managers call fork loaders and map to `qodat-api` interfaces (same pattern as `ItemManager` + `ItemLoader226` today).
4. Delete or gut `OldschoolCacheRuneLite` after `AnimationExporter` is rewritten onto `CacheLibrary`.
5. Remove `repo.runelite.net` once nothing resolves `net.runelite:*`.

Do **not** put viewer/JavaFX types in the fork. The fork is bytes ↔ defs. Qodat is defs ↔ scene.

Until the submodule is ready to take files, keep new codecs in `stan.qodat.cache.impl.oldschool.loader` and only retarget imports — first slice below.

---

## 4. Deob alignment

| Area | Closest source | vs RuneLite |
|---|---|---|
| Widgets layout | NR deob `class76.method3243` / `class59.method1558` (`WidgetLayout.kt`) | Independent; already on `qodat` `InterfaceDefinition` |
| IF3 rev237 | NR prefix `0xAABBCCDD`; int model ids | RL has no 237 magic |
| Frames | Comments cite NR 235 `osrs.RSAnimation` / `RSFrames` / `RSFramesBase`; 317 `RSBuffer.getShort2` (`65537` wrap) | OSRS path “Copied from RuneLite FrameLoader” |
| Maya runtime | Vendored `src/main/kotlin/jagex/*` (`MayaAnimation`, `Buffer`, …) | Not RL; index numbers still via RL `IndexType` |
| Sequence 206/226 | Comments: “based of RuneLite SequenceLoader” | Opcode remap + 226→206 fallback are ours; not deob-cited |
| Items | Our 226 table (int model opcodes 44–54, skip 200–202) | Production already off RL `ItemLoader` |
| Models | `RS2Model` / `InputStream.java` / `ModelLoader.java` are RL-copyright copies | `RSModelLoader` own low-rev + RS3; type1/2/3 still RL `ModelLoader` |
| Maps | Not owned; plan doc points at RL `Region` / deob `SceneTileModel` | Future |

Target for new/replaced loaders: match the deob stream, keep RL only as a behavior oracle in tests until the artifact is gone.

---

## 5. Multi-revision strategy

One loader class per type. Revision is a **decode context**, not a class explosion.

```
LoaderContext(archiveRevision, formatHints)
  flags = OpcodeTable.forType(ITEM|NPC|SEQ|…).flagsAt(archiveRevision)
decode(bytes):
  for opcode in stream:
    handler = table[opcode]
    if handler == null: skip or fail-closed per type
    handler.read(stream, flags)
```

**Rules**

1. **Newer reads older:** extra opcodes are no-ops (items already do this). Missing fields keep defaults.
2. **Same opcode, new layout:** gate on archive revision (1141/1142, 1268/1269), not on “which class”.
3. **Magic prefixes** (IF3 `AABBCCDD`, frame `F9F9`, model trailers) override numeric rev when the client does.
4. **Fallback only when tables are disjoint:** keep 226→206 for sequences until the 206 opcodes are rows in the same table with `rev <= 1268`.
5. **Encoders** write the **selected** target rev (explicit), never “whatever we last decoded”.

### Testing without a live cache

Already the house style: `OutputStream { writeByte(opcode); …; writeByte(0) }.flip()` then `loader.load(id, bytes)`.

**No fixture directory exists.** Do not add a full-cache fixture in the first slices.

Per type, add a small matrix:

| Case | How |
|---|---|
| Oldest interesting rev | payload using only old opcodes / old field widths |
| Gate − 1 and gate + 1 | e.g. seq archive 1141 vs 1142, 1268 vs 1269 |
| Newest | extra opcodes present; older decoder path must still consume or ignore |
| Newer decoder × older payload | `configureForRevision(new)` + old bytes → same fields as old decoder |
| Magic formats | IF3 with/without 237 prefix; frame with/without `F9F9` |

Optional later: `src/test/resources/cache-fixtures/{rev}/{index}/{archive}/{file}.bin` for a handful of real files. Not required to start.

Replace RL `OutputStream` in tests with ours as soon as it exists so tests do not keep the Gradle coord alive.

---

## 6. Phased plan (safest first)

Stay on `qodat-api` interfaces the whole way. Do not delete RL usage until a replacement is wired on `DispleeCache`.

### Phase A — IO (unblocks everything)

**First slice (steps 1–2) is done.** `ItemLoader226`, `SequenceStream`, `SequenceLoader206/226` and their five tests now use `qodat.cache.io.*`.

**Second slice (frames / interfaces / legacy) is done.** `AnimationFrameCodec`, `InterfaceLoader237`, legacy decoders/storage, and their tests now use `qodat.cache.io.*`.

1. ~~Add `qodat.cache.io.OutputStream` with the methods tests use (`writeByte`, `writeShort`, `writeInt`, `writeString`, `flip`).~~
2. ~~Retarget owned loaders + tests from `net.runelite.cache.io.*` → `qodat.cache.io.*` (item + seq).~~
3. ~~Same IO swap for `AnimationFrameCodec`, `InterfaceLoader237`, and legacy decoders/storage.~~
4. ~~Same IO swap for `io_extensions`, `model_exporter`, `ModelLoader.java`, and remaining loader tests.~~ **Phase A IO complete.** Zero `net.runelite.cache.io` in source.

`qodat-api` still depends on RL after this phase (defs/loaders). That is OK.

### Phase B — stop leaking RL types through `qodat-api`

1. Replace `AnimationMayaDefinition.animMayaFrameSounds` `Map<Int, SequenceDefinition.Sound>` with our sound type (`Sound` already exists next to `SequenceLoader226`).
2. Teach `RSModelLoader` to decode type1/2/3 with our `InputStream` (port `ModelLoader.java` or fold it in). Delete or isolate `ModelLoader.java`.
3. After that, `qodat-api` should compile **without** `api(libs.runelite.cache)`. Move any remaining RL use down to the app module as `implementation` until Phase C/D finish — or keep the api dep until those call sites die.

### Phase C — replace RL loaders on the live Displee path (order)

1. **SpotAnim** — small, already mapped to `qodat` interface
2. **NPC** — own loader + change `NpcPrimaryAnimations` / `NpcAnimParser` off `net.runelite.cache.definitions.NpcDefinition`
3. **Object** — same pattern as NPC
4. **Texture** — `TextureLoader` + `method2680` sprite callback is the sharp edge
5. **Sprite** — palette / `pixelIdx` / frame grouping; `RuneliteSpriteDefinition` wrapper goes away
6. **Interface** — decode into `qodat` `InterfaceDefinition` (drop RL type + `RuneliteInterfaceDefinition`). Keep IF1 / IF3 / 237 magic. CS1 opcode map already exists in the wrapper.
7. **Frames** — own `Frame` / `Framemap` structs inside `AnimationFrameCodec`; drop RL Frame/Framemap
8. **IndexType / ConfigType** — tiny enum in the fork or `qodat-api` (`MODELS=7`, `ANIMATIONS=0`, `SKELETONS=1`, `CONFIGS=2`, Maya `22`). Update `DispleeCacheExt`, `AnimationMaya`.

### Phase D — retire OldschoolCacheRuneLite

- Rewrite `AnimationExporter` onto `CacheLibrary` (or leave encode disabled until then — current `storage.save` is commented out)
- Replace `getIntefaceGroup` call sites (already spelled correctly on Displee `InterfaceManager`)
- Delete `OldschoolCacheRuneLite` and RL manager usage
- Drop `repo.runelite.net` and the catalog entry

### Phase E — move codecs into the fork

Copy the settled loaders into `qodat-cache` (submodule commit on `Qodat/rs-cache`), depend on them from the app. Only after phases A–C so the fork does not inherit RL types.

### What can stay as `qodat-api`

`Cache`, `Encoder`, `EncodeResult`, all `qodat.cache.definition.*` viewer interfaces, `ClientScript1Instruction` (already a local copy; Abex/RL copyright header), `RS2Model` / `ModelDefinition`. These are the scene contract.

### Risks

| Risk | Why |
|---|---|
| Sprites | Shared palette, maxWidth/Height, frame index; `Texture.method2680` samples sprite pixels |
| Textures | Animation / average color / sprite lookup closure |
| Maps | Entirely RL in the future-map doc; do not start here |
| Frames | Dual OSRS + NR 317; leftover-byte check is strict |
| Maya | Index 22 vs animations; `jagex.MayaAnimation.load` takes Displee `Index`; sounds type leak |
| Interfaces IF1/IF3/237 | Three layouts; CS1 bytecode; parent-id packing; type 10 rev237 extra |
| `getIntefaceGroup` | RL typo only on Oldschool path; Displee already uses `getInterfaceGroup` |
| `qodat-api` `api()` | Removing the Gradle dep too early breaks every remaining `net.runelite.*` import |
| Submodule | Codec moves need a `qodat-cache` commit, not only this repo |
| `OldschoolCacheRuneLite` object | First access opens an RL `Store` on `osrsCachePath` |
| `ModelLoader.java` | Large RL port; `RSModelLoader` already has low-rev — easy to fork the wrong path |
| Encode | Almost unused (`AnimationExporter` save commented). Decode-first. |

---

## First implementation slice — **done** (imports swapped; compile/tests deferred)

**Scope: Phase A step 1–2 only.** Completed without compile/commit.

1. Added `qodat-api/src/main/kotlin/qodat/cache/io/OutputStream.java` matching the RL test surface (`writeByte/Short/Int/String`, `write24BitInt`, `flip` → `byte[]`) plus the rest of RL write helpers so later slices stay import-only.
2. Confirmed `qodat.cache.io.InputStream` covers what item/seq need (`readUnsignedByte/Short`, `readByte`, `readShort`, `readInt`, `read24BitInt`, `readString`). Kotlin `.offset` maps to `getOffset`/`setOffset`.
3. Changed imports only in:
   - `ItemLoader226.kt`
   - `SequenceStream.kt`
   - `SequenceLoader206.kt`
   - `SequenceLoader226.kt`
   - `ItemLoader226Test.kt`
   - `SequenceLoader206Test.kt`
   - `SequenceLoader226Test.kt`
   - `SequenceLoader226ExtraTest.kt`
   - `SequenceStreamTest.kt`
4. Same definition classes. No DispleeCache / submodule / RL deletion elsewhere.

**Out of first slice:** Interface/frame/legacy IO swap, `AnimationMayaDefinition` sound type, `ModelLoader.java`, NPC/object/sprite/texture, `OldschoolCacheRuneLite`, gradle coord removal.

**Done when:** those nine files have zero `net.runelite.cache.io` imports; existing synthesized tests are expected to pass in the later exclusive compile wave.

---

## Second implementation slice — **done** (frames / interfaces / legacy IO)

**Scope: Phase A step 3 without `io_extensions` / `model_exporter`.** Import-only swap onto `qodat.cache.io.*`. Compiled and tested.

Changed imports only in:

- `AnimationFrameCodec.kt`, `AnimationFrameCodecTest.kt`
- `InterfaceLoader237.kt`, `InterfaceLoader237Test.kt`
- `LegacyStream`, `LegacyNpcDecoder`, `LegacyObjectDecoder`, `LegacyItemDecoder`, `LegacyKitDecoder`, `LegacySequenceDecoder`, `LegacyFrameDecoder`, `LegacyAnimationSkeletonDecoder`
- `LegacyIndexDat`, `LegacyAnimationStorage`, `LegacyFrameStorage`, `LegacyBodyKitStorage`
- `LegacyNpcDecoderTest`, `LegacyObjectDecoderTest`

Same definition classes. No DispleeCache / submodule / RL deletion elsewhere. `DispleeMain.kt` left dirty.

**Out of slice:** `io_extensions.kt`, `model_exporter.kt`, `RSModelLoader*` tests, `AnimationMayaDefinition` sound type, `ModelLoader.java`, NPC/object/sprite/texture, `OldschoolCacheRuneLite`, gradle coord removal.

---

## Third implementation slice — **done** (remaining Phase A IO)

**Scope: Phase A step 4.** Import-only swap onto `qodat.cache.io.*`. Compiled and tested. No remaining `net.runelite.cache.io` in source.

Changed imports only in:

- `ModelLoader.java`
- `io_extensions.kt`, `model_exporter.kt`, `IoExtensionsTest.kt`
- `RSModelLoaderDecodeTest.kt`
- `NpcDefinitionLoaderTest.kt`, `ObjectDefinitionLoaderTest.kt`, `SpriteDefinitionLoaderTest.kt`, `TextureDefinitionLoaderTest.kt`

Same definition classes. No DispleeCache / submodule / RL deletion elsewhere. `DispleeMain.kt` left dirty.

**Out of slice:** Phase B Sound/Maya, `OldschoolCacheRuneLite`, gradle coord removal.

---

## Investigation constraints (this pass)

- No Gradle / compile / tests
- No git commit / checkout / merge / rebase / stash / pull
- No RuneLite usage deleted
- `DispleeMain.kt`, `.idea/`, `build/` not touched
