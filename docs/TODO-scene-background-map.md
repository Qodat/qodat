# Scene background maps

Design / TODO for rendering a **background environment** behind the entity currently shown in the 3D viewer: either a real Old School RuneScape map region loaded from the cache, or a user-authored chunk assembled from tiles and objects (Construction-style).

This is a plan, not an implementation. Every claim below is grounded in files that exist in this repo or on the current dependency classpath (`net.runelite:cache:1.12.24` via `qodat-api/build.gradle.kts`, Displee `CacheLibrary` via the `qodat-cache` included build).

---

## 1. Problem and user-facing feature

Qodat today renders one entity at a time (NPC, object, item, spotanim, or raw model) in an otherwise empty JavaFX `SubScene`. There is no world, no floor, and no scenery. The unused `GridView` (`src/main/kotlin/stan/qodat/scene/shape/GridView.kt`) is a flat subdivided plane and is never attached.

The feature:

1. **World-map background.** The user picks a place on the map (region id, region X/Z, or absolute tile X/Z). Qodat decodes that region’s terrain (and, if XTEA keys are available, its object spawns) and draws it *behind* the entity being viewed.
2. **Authored chunk.** The user starts from a blank 8×8 (Construction room) or 64×64 (full region) grid, paints underlay/overlay tiles, sets heights, and drops objects with type + rotation. The same renderer draws it. Persistence follows `QodatCache`’s existing JSON-per-asset layout.

Both paths share one intermediate “chunk” model and one mesh builder.

---

## 2. Where it plugs into the scene graph

### Current graph

```
SubScene3D.root (Group)
├── CameraHandler.cameraTransformGroup   // PerspectiveCamera, near=1, far=10000
├── SubScene3D.scalingGroup              // AutoScalingGroup (Scale + Translate, currently unused)
│   ├── AxisView                         // optional, Properties.showAxis
│   └── SceneContext.group               // swapped when the viewer/editor tab changes
│       └── Entity.getSceneNode()        // Model / NPC / Object / …
└── AmbientLight
```

Concrete classes:

| Piece | File |
|---|---|
| SubScene owner | `src/main/kotlin/stan/qodat/scene/SubScene3D.kt` |
| Per-tab contents | `src/main/kotlin/stan/qodat/scene/SceneContext.kt` |
| Tab wiring | `src/main/kotlin/stan/qodat/scene/controller/SceneController.kt` (`sceneContext` anonymous subclass) |
| Entity attach/detach | `EntityViewController` `onSelectedEvent` / `onUnselectedEvent` call `sceneContext.addNode` / `removeNode` (around lines 680–716) |
| Camera | `src/main/kotlin/stan/qodat/scene/control/CameraHandler.kt` |
| Clip / zoom | `Properties.cameraFarClip` = 10000, `cameraMaxZoom` = −3000 (`src/main/kotlin/stan/qodat/Properties.kt`) |

### Hook point (do **not** put terrain inside `SceneContext`)

`SceneContext` recenters its group on every child change:

```59:59:src/main/kotlin/stan/qodat/scene/SceneContext.kt
                t.y = -group.boundsInParent.centerY
```

A 64×64 region is 8192 scene units across. Putting it in that group would yank the viewed entity off-camera. Terrain must be a **sibling** of `sceneContents` on `SubScene3D.scalingGroup`, added *before* the context node so it draws behind, with `mouseTransparent = true`.

A marker comment lives at that listener in `SubScene3D.kt`. Proposed shape:

```
SubScene3D.scalingGroup
├── backgroundGroup          // NEW: one Group, swapped when the user picks a region/chunk
│   ├── terrainMeshView      // one (or few) AtlasMaterial MeshViews
│   └── sceneryGroup         // batched or per-loc Object models
├── AxisView
└── SceneContext.group       // unchanged: the entity being viewed
```

Keep the background independent of tab switches unless the user clears it. `SceneContext.clear()` / entity unselect must not destroy it.

`fxyz3d/` in this repo is only `Point3F`, `TextureMode`, and a colour palette. It is not a terrain stack. Ignore it for this feature.

---

## 3. Data flow

```
cache index 5  ──m{rx}_{rz}──►  MapLoader          ──► Region (heights, under/overlay, settings)
               ──l{rx}_{rz}──►  LocationsLoader    ──► Region.locations   [needs XTEA]
cache index 2  ──archive 1──►   UnderlayLoader     ──► UnderlayDefinition (rgb → HSL)
               ──archive 4──►   OverlayLoader      ──► OverlayDefinition  (rgb, texture, hideUnderlay)
               ──archive 6──►   ObjectLoader       ──► ObjectDefinition   (already loaded)

Region + floor defs
        │
        ▼
SceneChunk  (Qodat-owned; shared by “load from cache” and “load from JSON”)
        │
        ├── TerrainMeshBuilder  →  TriangleMesh + AtlasMaterial  →  MeshView
        └── LocPlacer           →  existing Object / Model, translated & rotated
        │
        ▼
backgroundGroup on SubScene3D.scalingGroup
```

Decode and mesh build belong on `BackgroundTasks` (`src/main/kotlin/stan/qodat/task/BackgroundTasks.kt`, `Dispatchers.Default`). Attach the `MeshView` on the JavaFX thread.

Authored chunks skip the cache read and hydrate the same `SceneChunk`.

---

## 4. Coordinate and addressing model

All of the following is already implemented in `net.runelite.cache.region.Region` / `RegionLoader` / `Location` / `Position` (RuneLite cache 1.12.24 sources). Displee’s README (`qodat-cache/README.md`) uses the same region-id split.

### Units

| Quantity | Value | Source |
|---|---|---|
| Tiles per region | 64 × 64 | `Region.X`, `Region.Y` |
| Height planes | 4 (0 = ground) | `Region.Z` |
| Scene units per tile | **128** | OSRS client `Scene` (not in this repo; do not use 8 here) |
| Height scale | stored byte × **8**, negated on plane 0 | `Region.loadTerrain` |
| Missing plane-0 height | `HeightCalc.calculate(baseX + x + 0xe3b7b, baseY + y + 0x87cce) * 8` | `Region.loadTerrain` + `HeightCalc` |
| Construction / build chunk | 8 × 8 tiles (8 chunks per region axis) | client map-square convention |

A full region is `64 * 128 = 8192` units on a side — already close to `Properties.cameraFarClip` (10000). Multi-region views need a larger far clip.

The “8-unit tile” figure is the **height multiplier**, not the ground stride. Use 128 for X/Z.

### Bit math

```
regionId     = (tileX >> 6) << 8 | (tileZ >> 6)
regionX      = (regionId >> 8) & 0xFF
regionZ      =  regionId       & 0xFF
baseTileX    =  regionX << 6
baseTileZ    =  regionZ << 6
localTileX   =  tileX & 63
localTileZ   =  tileZ & 63

chunkX       = (tileX >> 3) & 7          // 8×8 build chunk inside the region
chunkZ       = (tileZ >> 3) & 7

sceneX       =  localTileX * 128
sceneY       =  region.getTileHeight(plane, localTileX, localTileZ)   // already world units
sceneZ       =  localTileZ * 128
```

`Region` constructors (`Region(int id)` / `Region(int x, int y)`):

```
baseX = ((id >> 8) & 0xFF) << 6
baseY = (id  & 0xFF) << 6
regionID = x << 8 | y
```

`RegionLoader.findRegionForWorldCoordinates` does `x >>>= 6; y >>>= 6; return regions.get((x << 8) | y)`.

Archive names (named map index, which OSRS uses):

```
terrain   = "m" + regionX + "_" + regionZ     // unencrypted
locations = "l" + regionX + "_" + regionZ     // XTEA-encrypted
```

Worked example: Lumbridge = region **12850** → `regionX = 50`, `regionZ = 50` → archives `m50_50` / `l50_50`. Absolute tiles around (3222, 3218) fall in that region. Displee’s README uses the same 12850 example.

### Location packing (object spawns)

`LocationsLoader.loadLocations` (RuneLite cache):

```
id         += unsignedIntSmart          // until 0
position   += unsignedShortSmart - 1    // until 0
localZ      = position       & 0x3F
localX      = position >>  6 & 0x3F
plane       = position >> 12 & 0x3
type        = attributes >> 2           // loc type 0–22
orientation = attributes &  0x3         // 0–3, 90° steps
```

`Region.loadLocations` then rewrites the position to **absolute tiles** (`baseX + localX`, `baseY + localY`). `Position.fromPacked` is the 30-bit world form (`z << 28 | x << 14 | y`) used elsewhere; landscape archives use the local form above.

### UI coordinate entry

Accept any one of:

1. **Region id** — integer, e.g. `12850`.
2. **Region X, Z** — two integers 0–255, e.g. `50, 50`.
3. **Absolute tile X, Z** — converted with the `>> 6` formula. Optional plane 0–3 (default 0).
4. **Chunk** (phase 5) — region + chunk X/Z 0–7, or an authored-chunk name.

Show the derived `m{X}_{Z}` / `l{X}_{Z}` names and whether the `l` archive decrypted. Do not require the user to know archive names.

Suggested placement: a small “Background” strip on the viewer (controller work — out of scope for the first cache/mesh slice). Persist last-used coords in `Properties` the same way `osrsCachePath` is session-bound.

---

## 5. What the current dependencies already give you

### Reachable today (no new libraries)

**Raw map bytes + XTEA (Displee, primary backend after `0ac39d4`)**

- `com.displee.cache.CacheLibrary.data(5, "m${x}_${y}")` — terrain.
- `CacheLibrary.data(5, "l${x}_${y}", xtea)` — locations, four `Int` keys.
- Archive-level encrypt/decrypt: `qodat-cache/src/main/kotlin/com/displee/compress/CompressionExt.kt` (`encryptXTEA` / `decryptXTEA`). BZIP2 + XTEA is rejected.
- `DispleeCache.store` is already a live `CacheLibrary` on `Properties.osrsCachePath`.

**Full decode (RuneLite cache 1.12.24, already on the classpath via `qodat-api`)**

| Role | Class |
|---|---|
| Index 5 | `net.runelite.cache.IndexType.MAPS` |
| Config archives | `ConfigType.UNDERLAY(1)`, `OVERLAY(4)`, `OBJECT(6)` |
| Terrain decode | `definitions.loaders.MapLoader` → `MapDefinition` / `MapDefinition.Tile` |
| Loc decode | `definitions.loaders.LocationsLoader` → `LocationsDefinition` |
| Region assemble | `region.Region`, `region.RegionLoader` |
| Spawn record | `region.Location` (`id`, `type`, `orientation`, `Position`) |
| Procedural height | `region.HeightCalc` |
| Floor defs | `UnderlayManager` / `OverlayManager` + `UnderlayLoader` / `OverlayLoader` |
| 2D colour / shapes | `MapImageDumper` (`BLEND = 5`, `TILE_SHAPE_2D`, `packHslFull`, `adjustHSLListness0`) |
| XTEA file | `util.XteaKeyManager` + `util.XteaKey` (`{region, keys[]}` JSON list) |

`OldschoolCacheRuneLite` already constructs `net.runelite.cache.fs.Store` and uses `IndexType` / `NpcManager` / `ObjectManager`. It never calls `RegionLoader`.

**Objects already in Qodat**

- `DispleeCache.objectManager` / `OldschoolCacheRuneLite.objectManager` load config archive 6.
- Viewer entity: `stan.qodat.scene.runescape.entity.Object`.
- Lighting constants for scenery: `SCENERY_AMBIENT/CONTRAST/LIGHT_*` in `RuneScapeRendering.kt`.

**Colour**

- `stan.qodat.util.HslPalette` (in progress, already used) — 65536-entry client palette, gamma `0.8`, cited against `jagex.Rasterizer3D.Rasterizer3D_buildPalette`.
- `ModelUtil.hsbToColor` / `HslPalette.rgb` — the path `ModelAtlasMesh` already uses for face tints.
- `HslPalette.encode` — RGB → packed HSL, needed when blending neighbour underlays back into a 16-bit colour.

### Not reachable / must be written

| Gap | Detail |
|---|---|
| `qodat.cache.Cache` has no map API | `qodat-api/src/main/kotlin/qodat/cache/Cache.kt` ends at sprites/textures. No region, underlay, overlay, or loc methods. |
| No Qodat map types | Nothing like `MapDefinition` exists under `qodat-api` or `stan.qodat.cache`. |
| Displee has no map *decoders* | `qodat-cache` is index/archive/XTEA I/O only. Feed its bytes into RuneLite `MapLoader` / `LocationsLoader`, or reimplement those two small classes. |
| No 3D tile mesher | RuneLite cache draws a **2D** map (`MapImageDumper`). Client `SceneTilePaint` / `SceneTileModel` (two triangles vs shaped overlay fans, 128-unit tiles) are **not** on the classpath and are **not** vendored under `src/main/kotlin/jagex/` (that package is Maya + `Rasterizer3D` only). Port from a deob client. |
| Loc fields dropped on convert | `DispleeCache` `ObjectManager.convert` and `OldschoolCacheRuneLite.getObjects` keep only `name`, `modelIds`, `animationIds`, `findColor`, `replaceColor`. RuneLite `ObjectDefinition` also has `objectTypes`, `sizeX`/`sizeY`, `ambient`/`contrast`, `offsetX`/`offsetY`/`offsetHeight`, `modelSize*`, `contouredGround`, `isRotated` — all required to pick the right model for a loc type and sit it on the tile. |
| No XTEA store in Qodat | Nothing reads a key file or talks to a key service. |
| `QodatCache.add` is Model/NPC only | `src/main/kotlin/stan/qodat/cache/impl/qodat/QodatCache.kt`. Objects load from `objects/*.json` but are never written. No `chunks/` directory. |
| `LegacyCache` | 667-style `obj.dat` only. No map index. Out of scope unless someone later ports 317 `m`/`l` files. |
| World-map indices 18/19/20 | `IndexType.WORLDMAP_*` are the 2D world-map overlay, not 3D terrain. Do not use them for this feature. |

### Backend differences

| | `DispleeCache` (default “Displee”) | `OldschoolCacheRuneLite` (“LIVE”) |
|---|---|---|
| FS | `CacheLibrary` (included `qodat-cache` 7.3.0, substitutes `com.displee:rs-cache-library`) | `net.runelite.cache.fs.Store` |
| Maps | raw `data(5, name, xtea?)` | `RegionLoader(store, keyProvider)` |
| Floor defs | not loaded; `index(2).archive(1/4)` is unused | `UnderlayManager` / `OverlayManager` ready to call |
| Objects | RuneLite `ObjectLoader` through Displee bytes; loc-placement fields discarded | same discard in `getObjects()` |
| Revisions | object/anim loaders already revision-aware (`ObjectLoader.configureForRevision`, `SequenceLoader226`) | older `SequenceLoader` plus local 206/226 forks |

Implement the chunk renderer **once**. Give it a `SceneChunk`. Provide two loaders (`DispleeRegionLoader`, `RuneLiteRegionLoader`) that both produce that type. Prefer calling RuneLite’s `MapLoader`/`LocationsLoader` on Displee-fetched bytes so decode stays in one place.

---

## 6. Terrain colour, shapes, and lighting

### Underlay blend (neighbour average)

`MapImageDumper` (`BLEND = 5`) is the 2D form of the client’s ground blend. For each tile it accumulates underlay HSL over a 5-tile window, then:

```
avgHue   = runningHues / count
avgSat   = runningSat  / count
avgLight = runningLight / count
underlayHsl = packHslFull(avgHue, avgSat, avgLight)
```

`UnderlayDefinition.calculateHsl` also exposes `hueMultiplier`; the dumper weights hue as `hue * 256 / hueMultiplier`. Port that weighting. Convert the packed result through `HslPalette.rgb` (not a generic HSB conversion — see the comment on `HslPalette`).

Overlays use `OverlayDefinition.rgbColor` / `texture` / `secondaryRgbColor` / `hideUnderlay`. Magenta `0xFF00FF` means “no colour”. `adjustHSLListness0` darkens overlay lightness by `0.898` before draw. Textured overlays sample the texture’s average RGB (`MapImageDumper` via `rsTextureProvider.getAverageTextureRGB`); Qodat already loads textures through `DispleeCache.getTexture` + `TextureMaterial`.

### Overlay shape / rotation

From `MapLoader`:

```
overlayPath     = (attribute - 2) / 4     // 0–11
overlayRotation = (attribute - 2) & 3     // 0–3
```

`MapImageDumper` then does `shape = overlayPath + 1`, remaps 9/10 → 1 and 11 → 8 (`convertTileShape`), and uses `TILE_SHAPE_2D[8][4][]` masks. The **3D** client uses a different table (`SceneTileModel` vertex/face indices, typically 12 shapes × 4 rotations). Copy that table from a deob `Scene` / `SceneTileModel`. Shape 0 = two triangles covering the tile (`SceneTilePaint`).

### Lighting

Bake terrain faces with `ModelDefinition.light(..., SCENERY_AMBIENT, SCENERY_CONTRAST, SCENERY_LIGHT_*)` from `RuneScapeRendering.kt` (shallower key light than actors). Feed the three corner HSL values into `AtlasMaterial.setFaceTints` exactly as `ModelAtlasMesh.createAtlas` does.

`HslPalette` is the right colour path. Keep using it; do not invent a second palette.

---

## 7. Shared `SceneChunk` and authored-chunk persistence

### Intermediate model (new, Qodat-owned)

Enough to render, and enough to round-trip to JSON:

```
SceneChunk
  name, source: CACHE | AUTHORED
  regionX, regionZ, originTileX, originTileZ
  sizeX, sizeZ                    // 64 or 8 (or any rectangle)
  planes: 1..4
  tiles[plane][x][z]:
    height            // world units, same as Region.getTileHeight
    underlayId, overlayId
    overlayPath, overlayRotation
    settings
  locs[]:
    objectId, type, orientation
    localX, localZ, plane
```

Cache path: `Region` + floor defs → `SceneChunk`.  
Authored path: JSON → `SceneChunk`.  
One `TerrainMeshBuilder` + one `LocPlacer` consume it.

### Persistence (mirror `QodatCache`)

`QodatCache` already:

- Root: `Properties.qodatCachePath` → `~/.qodat/caches/qodat`
- Loads `npcs/`, `items/`, `objects/`, `models/`, `animations/`, `animation_frames/`, `animation_skeletons/` as pretty-printed kotlinx.serialization JSON (`ignoreUnknownKeys = true`)
- `add(Model)` writes `models/{name}.json`; `add(NPC)` writes `npcs/{name}.json`

Add:

```
~/.qodat/caches/qodat/chunks/{name}.json
```

`@Serializable data class QodatChunkDefinition(...)` next to `QodatNpcDefinition` in `QodatDefinitions.kt`. Extend `QodatCache.add` / `remove` / `reloadFromSource` the same way NPCs work. Do **not** invent a second store.

A Construction-style editor (phase 5):

- Blank chunk, default flat height, underlay 0.
- Palette of underlay/overlay ids (from the loaded cache’s `UnderlayManager` / `OverlayManager`).
- Click-to-paint tiles; drag to set height; drop an object from the existing object list with type + orientation.
- Optional “import 8×8 from region 12850 chunk (2,3)” so authored work can start from a real room.
- Save → `chunks/{name}.json`. Reload on next launch via `QodatCache.reloadFromSource`.

---

## 8. Materials and mesh strategy

Reuse the existing atlas path. Do not spawn a `MeshView` per tile.

| Existing piece | Role for terrain |
|---|---|
| `AtlasMaterial` | 2×2 texel tiles, unique `FaceTint`s share a tile, bilinear filter = Gouraud |
| `ModelAtlasMesh` | pattern: one `TriangleMesh`, UV per corner from the atlas |
| `ModelMesh.addVertex` / `addUV` | vertex cache; copy or extract for the terrain builder |
| `TextureMaterial` | overlay/underlay *textures* (floor defs with `texture != -1`) |
| `ModelMeshBuildType.ATLAS` | default; `MESH_PER_FACE` is for editing and is far too heavy here |
| `RS2ModelBuilder` | merge several loc models into one mesh when batching scenery |

Phase 1 can be untextured (HSL colours only). Textured floors are a later pass: either a second atlas of floor sprites, or a small set of `TextureMaterial` meshes beside the colour atlas.

Loc models: start by reusing `Object` + `Model` (one group per spawn). A busy region will stutter; phase 6 batches static locs (type 10/11/22, no animation) through `RS2ModelBuilder` into per-region atlas meshes, and leaves animated/wall-decor locs as individual nodes.

Apply `Translate(sceneX, sceneY, sceneZ)` and `Rotate(orientation * 90, Y_AXIS)` per loc. Honour `objectTypes` (which model for this loc type) and `sizeX`/`sizeY` once those fields are plumbed through `qodat.cache.definition.ObjectDefinition`.

---

## 9. Phased plan

Each phase is independently shippable and testable. Do not start phase *n+1* until phase *n* shows something in the viewer.

### Phase 1 — Smallest visible background

Goal: *something* from a real region appears behind the current model.

- [ ] Add `backgroundGroup: Group` on `SubScene3D.scalingGroup` (sibling of `SceneContext`, see hook comment). `mouseTransparent = true`.
- [ ] Hard-code Lumbridge (`12850` / `m50_50`) or read a single region id from a temporary property.
- [ ] Fetch terrain bytes: `DispleeCache.store.data(5, "m${x}_${y}")`.
- [ ] Decode with `MapLoader` → `Region.loadTerrain`.
- [ ] Build **one** `TriangleMesh` for plane 0: two triangles per tile, vertices at `(x*128, height, z*128)`. Skip empty tiles (`underlayId == 0 && overlayId == 0`).
- [ ] Colour each face with the raw underlay RGB (no neighbour blend). `UnderlayLoader` on `index(2).archive(1)`, or a single flat grey if floor defs are not ready.
- [ ] Push colours through `AtlasMaterial` + `HslPalette` the same way `ModelAtlasMesh.createAtlas` does.
- [ ] Offset the mesh so the viewed entity sits near the chunk centre (do not use `SceneContext`’s Y-recenter).
- [ ] Load via `BackgroundTasks.submit`; attach the `MeshView` on the FX thread.

**Test:** open any NPC, set the region, see a heightfield behind it. No keys required (`m` is not encrypted).

### Phase 2 — Overlays, blend, planes

- [ ] Port the `BLEND = 5` underlay average from `MapImageDumper` (lines ~426–517) into 3D vertex colours.
- [ ] Draw overlay path/rotation. Shape 0 = full overlay tile; other shapes = `SceneTileModel` tables from a deob client (2D masks in `MapImageDumper` are a fallback for a first cut).
- [ ] Honour `hideUnderlay` and magenta “no colour”.
- [ ] Optional planes 1–3, toggled (bridge/upper floors).
- [ ] Overlay textures via existing `TextureMaterial` where `OverlayDefinition.texture >= 0`.

**Test:** Lumbridge castle courtyard and a shoreline (tutorial island) show roads/water, not a single grass colour.

### Phase 3 — Object spawns (XTEA)

- [ ] XTEA provider: user-selected JSON file in the RuneLite `XteaKeyManager` shape (`[{region, keys:[k0,k1,k2,k3]}, …]`). Optional “keys missing → terrain only” (this is already how `RegionLoader.loadLocDef` behaves when `getKey` is null).
- [ ] `CacheLibrary.data(5, "l${x}_${y}", keys)` → `LocationsLoader`.
- [ ] Extend `ObjectDefinition` (qodat-api) with `objectTypes`, `sizeX`/`sizeY`, offsets, `ambient`/`contrast`. Stop dropping them in both cache backends.
- [ ] Place each `Location` with the existing `Object` entity, scenery lighting, Y-rotation from `orientation`.
- [ ] Skip loc types you cannot yet pose (roofs 12–21 can wait). Prioritise type 10 (centrepiece) and 22 (floor deco), then walls 0–3.

**Test:** Lumbridge castle walls and trees appear when keys for 12850 are supplied; without keys, terrain still shows and the UI says locations were skipped.

### Phase 4 — Coordinate UI and multi-region

- [ ] Background panel: region id **or** region X/Z **or** absolute tile X/Z; plane; “load” / “clear”.
- [ ] Show derived archive names and key status.
- [ ] Load a 3×3 of neighbouring regions around the selection (lazy, still one mesh per region).
- [ ] Raise `cameraFarClip` when a background is active (8192+ per region).
- [ ] Persist last coords in `Properties` / session.

**Test:** type `3222, 3218`, get Lumbridge; type `50, 50`, same; clear returns to empty backdrop.

### Phase 5 — Authored chunks (Construction-style)

- [ ] `SceneChunk` + `QodatChunkDefinition` JSON under `caches/qodat/chunks/`.
- [ ] New-chunk dialog: size 8×8 or 64×64, optional import from a real region/chunk.
- [ ] Tile paint + height + loc place, using the object list that already exists.
- [ ] Save/load through `QodatCache.add` / `reloadFromSource`.
- [ ] Same `TerrainMeshBuilder` / `LocPlacer` as the cache path.

**Test:** paint a 8×8 room, drop a crate (type 10), restart Qodat, the chunk is still there and renders behind a model.

### Phase 6 — Performance

- [ ] Never one `MeshView` per tile. Cap: one colour-atlas mesh per region per plane, plus a small number of textured meshes.
- [ ] Batch static locs per region via `RS2ModelBuilder` + one `AtlasMaterial`.
- [ ] Build on `BackgroundTasks`; do not block the FX thread on `MapLoader` or mesh fill.
- [ ] Frustum / distance: drop planes 2–3 first, then locs beyond N tiles, then far regions.
- [ ] JavaFX has weak mesh culling — prefer fewer, larger `MeshView`s over hundreds of nodes (`Entity.getSceneNode` is already one `Group` per entity; hundreds of locs will hitch).
- [ ] Keep `mouseTransparent` on the background so picking still hits the viewed entity.

---

## 10. Performance notes (this codebase)

- `ModelAtlasMesh` exists specifically because JavaFX cannot colour triangles without a texture. Terrain has thousands of unique blended colours; the atlas’s “same `FaceTint` shares a tile” (`AtlasMaterial.setFaceTints`) is what keeps the image small.
- `MESH_PER_FACE` (`ModelMeshBuildType`) is the edit path and creates one mesh per triangle. A 64×64×2-triangle ground would be ~8k nodes. Do not use it for terrain.
- `BackgroundTasks.submit(addProgressIndicator, task)` already runs `Task`s on `Dispatchers.Default` and hops back to JavaFX for UI. Use that; do not invent a second executor.
- `Properties.cameraFarClip = 10000` and `cameraMaxZoom = -3000` are framed for a single model (~hundreds of units). One region is 8192 units wide. Adjust clip/zoom when a background is loaded or the far edge vanishes.
- `SubScene.cacheHint = SPEED` is already set in `SubScene3D.createSubScene`.
- Ambient light is a single `AmbientLight` on the root; terrain shading must be **baked** (as models already do), not left to JavaFX lights.

---

## 11. Open questions and blockers

1. **XTEA keys.** `l` archives are encrypted. Realistic options, in order:
   - User-supplied key file (`XteaKeyManager` JSON). Best fit; no network, revision-agnostic.
   - Keys bundled per cache revision (check them in next to `Properties.osrsCachePath`, or ship a file under `~/.qodat/caches/OS/…`).
   - Terrain-only when keys are missing — **required fallback**, already the RuneLite `RegionLoader` behaviour.
   - Fetching keys from a third-party API is a product/legal decision; do not bake a URL in without an explicit choice.
2. **No 3D tile-shape tables in-repo.** Phase 2 needs a client `SceneTileModel` port. `MapImageDumper.TILE_SHAPE_2D` is only a 4×4 pixel mask.
3. **`ObjectDefinition` is too thin** for loc placement. Expanding it is an API change in `qodat-api` (and both cache backends). Until then, type-10 models can be placed but walls/decor will use the wrong mesh.
4. **Revision drift.** Displee is the live backend and already special-cases anim/item revisions (`SequenceLoader226`, `ItemLoader226`, `ObjectLoader.configureForRevision`). `MapLoader` still uses `readUnsignedShort` for every terrain attribute (post-2017 OSRS). If a future revision changes the `m`/`l` format, only `MapLoader` / `LocationsLoader` need a fork — same pattern as the sequence loaders. RuneLite `cache:1.12.24` may lag the Displee-opened cache.
5. **`SceneContext` Y-recenter** will fight any attempt to parent terrain under the entity group. Keep the sibling-on-`scalingGroup` rule.
6. **Legacy / 317 maps** are a different file layout (`LegacyCache` has no index 5). Out of scope.
7. **World-map indices 18–20** are not 3D terrain.
8. **Which cache is selected.** Viewer uses `Properties.viewerCache` (Displee or RuneLite). The background loader must use the same instance; do not assume `DispleeCache` if the user picked LIVE.
9. **Entity vs background scale.** Models are in the same 128-unit system as the world, but the camera starts at `Translate(0, 0, -1000)`. A region-sized mesh will dwarf a single NPC unless you either (a) translate the background so the picked tile is under the origin, or (b) offer a “fit background to model” scale. Prefer (a): pin the selected tile (or region centre) to `(0, 0, 0)` and leave the entity where it is.
10. **Concurrent work.** Cache backends, `qodat-api`, export, and controllers are owned by other workstreams. Phase 1 can be done with only `SubScene3D` + new files under e.g. `stan.qodat.scene.runescape.map` and a temporary property, calling `DispleeCache.store` and RuneLite loaders directly. API/controller polish waits until those trees are free.

---

## 12. File / class index

### This repo — scene

- `src/main/kotlin/stan/qodat/scene/SubScene3D.kt` — **hook** (`scalingGroup` sibling)
- `src/main/kotlin/stan/qodat/scene/SceneContext.kt` — do not parent terrain here (`centerY` recenter)
- `src/main/kotlin/stan/qodat/scene/AutoScalingGroup.kt`
- `src/main/kotlin/stan/qodat/scene/AbstractSubScene.kt` — `fill` = `Properties.subSceneBackgroundColor`
- `src/main/kotlin/stan/qodat/scene/control/CameraHandler.kt`
- `src/main/kotlin/stan/qodat/scene/runescape/RuneScapeRendering.kt` — `light()`, scenery vs actor constants
- `src/main/kotlin/stan/qodat/scene/runescape/model/Model.kt`
- `src/main/kotlin/stan/qodat/scene/runescape/model/ModelAtlasMesh.kt`
- `src/main/kotlin/stan/qodat/scene/runescape/model/ModelMesh.kt`
- `src/main/kotlin/stan/qodat/scene/runescape/model/ModelMeshBuildType.kt`
- `src/main/kotlin/stan/qodat/scene/paint/AtlasMaterial.kt`
- `src/main/kotlin/stan/qodat/scene/paint/TextureMaterial.kt`
- `src/main/kotlin/stan/qodat/scene/runescape/entity/Entity.kt` / `Object.kt`
- `src/main/kotlin/stan/qodat/scene/shape/GridView.kt` — unused flat plane; not RS terrain
- `src/main/kotlin/stan/qodat/scene/provider/SceneNodeProvider.kt`
- `src/main/kotlin/stan/qodat/task/BackgroundTasks.kt`
- `src/main/kotlin/stan/qodat/util/HslPalette.kt`
- `src/main/kotlin/stan/qodat/util/ModelUtil.kt`
- `src/main/kotlin/jagex/Rasterizer3D.java` — `Rasterizer3D_buildPalette` (palette reference)

### This repo — cache

- `qodat-api/src/main/kotlin/qodat/cache/Cache.kt` — no map API
- `qodat-api/src/main/kotlin/qodat/cache/definition/ObjectDefinition.kt` — too thin for locs
- `qodat-api/src/main/kotlin/qodat/cache/models/RS2ModelBuilder.kt`
- `src/main/kotlin/stan/qodat/cache/impl/displee/DispleeCache.kt` — `store: CacheLibrary`, objects via `ObjectManager`
- `src/main/kotlin/stan/qodat/cache/impl/displee/types/ObjectManager.kt` — drops loc-placement fields
- `src/main/kotlin/stan/qodat/cache/impl/oldschool/OldschoolCacheRuneLite.kt` — `Store` + `IndexType`, no `RegionLoader`
- `src/main/kotlin/stan/qodat/cache/impl/qodat/QodatCache.kt` — JSON persistence pattern
- `src/main/kotlin/stan/qodat/cache/impl/qodat/QodatDefinitions.kt`
- `src/main/kotlin/stan/qodat/cache/impl/legacy/LegacyCache.kt` — no maps
- `qodat-cache/src/main/kotlin/com/displee/cache/CacheLibrary.kt` — `data(..., xtea)`
- `qodat-cache/README.md` — `m`/`l` + XTEA examples

### Classpath — RuneLite cache 1.12.24 (not vendored; read from the Gradle sources jar)

- `net.runelite.cache.IndexType` / `ConfigType`
- `net.runelite.cache.region.{Region, RegionLoader, Location, Position, HeightCalc}`
- `net.runelite.cache.definitions.{MapDefinition, LocationsDefinition, UnderlayDefinition, OverlayDefinition, ObjectDefinition}`
- `net.runelite.cache.definitions.loaders.{MapLoader, LocationsLoader, UnderlayLoader, OverlayLoader}`
- `net.runelite.cache.{UnderlayManager, OverlayManager, MapImageDumper}`
- `net.runelite.cache.util.{XteaKeyManager, XteaKey}`

### Gradle

- Root `build.gradle.kts`: `implementation(project("qodat-api"))`, `com.displee:rs-cache-library:7.1.3` (substituted).
- `settings.gradle.kts`: `includeBuild("qodat-cache")` substitutes that module.
- `qodat-api/build.gradle.kts`: `api("net.runelite:cache:1.12.24")`.
- `qodat-cache/build.gradle.kts`: library version `7.3.0`, XTEA in the compress path.

### Suggested new files (when implementing)

```
src/main/kotlin/stan/qodat/scene/runescape/map/
  SceneChunk.kt
  TerrainMeshBuilder.kt
  LocPlacer.kt
  RegionCoordinate.kt          // bit math + UI parsing
  SceneBackground.kt           // owns backgroundGroup, load/clear
src/main/kotlin/stan/qodat/cache/impl/map/   // when the cache tree is free
  DispleeRegionSource.kt
  XteaKeyStore.kt
```

---

## 13. Out of scope for v1

- Walkable collision / clip flags (tile `settings` is decoded but unused).
- Projectiles, roofs, lighting volumes, water shaders.
- Editing and writing back to index 5.
- 317 / `LegacyCache` maps.
- 2D world-map image (`MapImageDumper` / indices 18–20) as a skybox.
- Multiplayer / live-world streaming.
