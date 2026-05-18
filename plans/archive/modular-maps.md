# Modular & Nestable Maps

Goal: support loading **multiple maps at once** (e.g. the home base plus a
second map connected to its east edge), with composition described in YAML.
Maps can nest — a YAML file can be either a leaf grid (today's shape) or a
container that references other map files.

This is a multi-phase plan. Phases 1–3 land the data model and a working
two-map scenario; phases 4–5 deal with the cross-cutting consequences
(pathfinding, editor); phase 6 mops up tests.

---

## Background — what we have today

(Survey notes; not a design proposal, just the lay of the land.)

- **YAML schema** lives in `core/src/main/java/ca/kieve/ssss/content/MapDefinition.java`
  (`MapDefinition`, `MapBlockDefinition`, `MapEntityDefinition`). Today: a
  `blocks` table, a string-keyed `layers` map (z-index → ASCII grid), a
  `floorGlyph`, and an entity list.
- **Loader**: `StaticTestMapGenerator` (in `core/src/main/java/ca/kieve/ssss/world/`)
  reads one YAML via Jackson and writes blocks into a single global
  `WorldModel` (`m_blocks[x][y][z]`, sized at load time).
- **Coordinate space is global**. `Position` is just `Vec3i`. `PositionContext`
  is a single pre-sized 200×200×20 cache. There is no map-id, no region-id.
- **Pathfinding** (`PathingSystem` + `PathingContext`) builds one DijkstraMap
  per z-level (and per `Size` for size-aware routing), sized to the whole map.
  Dirty-marks all grids each tick.
- **Editor** is a separate JavaFX module. Top-level `EditorFxApp` has
  "Map" / "System Config" tabs; inside the map view, `MapViewPanel`
  (`editor/src/main/java/ca/kieve/ssss/editor/component/MapViewPanel.java:207-213`)
  hosts an inner `TabPane` with "Blocks" and "Entities" tabs. `MapLoader` /
  `MapSaver` round-trip the same YAML schema as core.
- **Content index**: `core/build.gradle`'s `generateContentIndex` task writes
  `file_list.txt` per directory (flat, not recursive).
- **Tests** load maps through `TestGameContext.create("test/<scenario>.yaml")`.
  Test-only YAML lives at `core/src/test/resources/content/maps/test/`.
- **Existing tutorial map** lives at
  `core/src/main/resources/content/maps/tutorial/damaged_sub.yaml` and is
  referenced by `core/src/main/resources/content/system.yaml`'s `launchMap`.

---

## Phase 1 — Introduce `MapRegion` (no functional change)

Stand up the runtime concept of a "region of the world that came from one
YAML file" without changing what the player sees. After this phase the
existing map still loads exactly as before, but the engine knows it as
*region* `<map-path>` rather than "the whole world."

Work:
- New `MapRegion` (likely `core/src/main/java/ca/kieve/ssss/world/MapRegion.java`):
  `id` (e.g. the source YAML path), `originOffset` (`Vec3i`, the region's
  `(0,0,0)` in world coordinates), and `bounds` (width/height/depth in
  region-local coordinates).
- Hold the active set of regions in a new `MapContext` (or extend
  `GameContext`) — `List<MapRegion>` + a `regionAt(Vec3i worldPos)` lookup.
- Refactor `StaticTestMapGenerator` to:
  1. Compute the region's bounds from the YAML.
  2. Register a `MapRegion` with `originOffset = (0,0,0)` for the
     single-map case.
  3. Stamp each `WorldModel.setBlock(...)` it does with the owning region
     (cheap parallel array keyed by region index; used only for diagnostics
     and overlap checks in Phase 2).
- Keep `WorldModel` sized to the union of all regions (just the one for now).
- Entity `Position` coordinates from YAML are still treated as
  world-absolute — same behaviour as today.

**Future-streaming nudge** (no extra work, just don't preclude it): the
`MapRegion` shape — id + bounds + offset + per-region cell ownership — is
already the unit you'd stream in/out later. As long as Phase 4 keeps path
caches keyed by region (it will), loading and unloading regions at runtime
becomes a matter of `MapContext.add(region)` / `remove(region)` plus
pathing-cache eviction for that key. Don't design streaming now, but the
data model leaves the door open.

Tests (backfill for files we touched, per project policy):
- `StaticTestMapGenerator` — unit, golden-load of a fixture YAML, assert
  region registered with correct id/bounds/offset.
- `WorldModel` — unit, bounds + ownership stamping.
- `MapContext` — `regionAt()` correctness; empty world → null/empty.

Exit criteria: existing harness tests still pass unchanged.

---

## Phase 2 — Nested map references in YAML

Extend the schema so a YAML can compose other YAMLs. Two placement modes:
**explicit offset** (simple) and **connector pair** (relational).

Schema additions on `MapDefinition`:

```yaml
# Map-level named anchors. Not ECS entities — just (id, position) pairs
# used at load time to align submaps.
connectors:
  - id: east_door
    position: { x: 11, y: 5, z: 1 }
  - id: maintenance_hatch
    position: { x: 0, y: 5, z: 1 }

submaps:
  # Mode 1: explicit offset (existing rooms with known coords)
  - ref: ./maintenance_sub.yaml
    offset: { x: 12, y: 0, z: 0 }
    id: maintenance_east

  # Mode 2: connector pair (loader computes offset so the child's
  # remoteConnector lands on this map's localConnector)
  - ref: ./maintenance_sub.yaml
    localConnector: east_door
    remoteConnector: west_door
    id: maintenance_east
```

Either `offset` OR (`localConnector` + `remoteConnector`) must be present,
not both. With connectors, the loader resolves the child's offset as
`localConnector.position - remoteConnector.position` (component-wise).

Why connectors matter: maps don't have to be rectangular at the layer
they're being joined on — a parent might be concave with a void in the
middle that a submap fits into. Hardcoded offsets become brittle as maps
get edited; connectors stay correct as long as the named anchor still
exists in both files.

Loader changes (`StaticTestMapGenerator`, plus likely a new
`CompositeMapLoader`):
- Parse `connectors` and `submaps` from `MapDefinition` (extend the record).
- Two-pass load:
  1. Recursively walk the YAML tree, resolving each leaf region's
     world-space offset. For connector-mode submaps, look up the named
     connectors in parent and child; error clearly if a name is missing.
  2. Size `WorldModel` to the union of region footprints.
  3. Stamp each cell per region.
- Path resolution: see "Content ref paths" below — `./...` is relative to
  the current file, `/...` is absolute under `content/`.
- Cycle detection: error if the same resolved path appears twice on an
  ancestor chain.
- **Overlap policy**: because submaps can be placed inside concave parents,
  overlap can happen anywhere — not just at the bounding box. The loader
  must check the **placed footprint cell-by-cell**, not the bounding box.
  - **Blocks**: a non-air cell collision between two regions → fail load
    with a precise diagnostic (`region A and region B both claim block at
    (x,y,z)`). Per Phase 6 of the open-questions, last-writer-wins (in
    submap-list order) for cells where the parent explicitly declared
    `allowOverlap: true`. Default is fail-fast.
  - **Entities**: multiple entities sharing a tile is already legal in
    this engine — no special handling required. Entities from different
    regions just land in `PositionContext` like any other entities.
- Entities from each child are loaded with `Position` translated by the
  child's resolved offset.

Tests:
- New fixtures under `core/src/test/resources/content/maps/test/composite/`:
  `two_rooms_offset.yaml`, `two_rooms_connector.yaml`,
  `concave_parent_submap_in_void.yaml`, `overlap_conflict_blocks.yaml`,
  `overlap_allowed_last_wins.yaml`, `nested_three_deep.yaml`,
  `missing_connector.yaml`, `cycle.yaml`.
- Unit tests on the loader for each mode + each failure path.

---

## Phase 3 — Rename `tutorial` → `home_base`, add a second map

The first concrete payoff. The directory was originally named `tutorial`
but the design has shifted — it's actually the home base the player
returns to between expeditions. Rename first so the new composition uses
the right name from day one.

Rename:
- `core/src/main/resources/content/maps/tutorial/` →
  `core/src/main/resources/content/maps/home_base/`
- Update `core/src/main/resources/content/system.yaml`:
  `launchMap: home_base/damaged_sub.yaml`
- Grep for any remaining `tutorial` references in code/yaml (currently
  just `system.yaml` and this plan; the README mention is an unrelated
  external URL). Update them.
- Regenerate `file_list.txt` via the build (it'll move with the directory).

Author a second map:
- New leaf map `home_base/maintenance_sub.yaml`. Use a deliberately
  different layout style from `damaged_sub` (different block palette,
  different layer conventions) so we exercise the schema's flexibility.
- Add `connectors` blocks to both `damaged_sub.yaml` and
  `maintenance_sub.yaml` — at the door tiles where the two are meant to
  meet.

Author the composition (using **connector mode**, per Phase 2):
- New `home_base/sub_complex.yaml` — references `damaged_sub` (no offset:
  it's the root region, offset implicit `(0,0,0)`) and
  `maintenance_sub` linked via connectors. The composition file may
  contribute its own glue tiles (e.g. a shared door tile) or rely entirely
  on the children meeting at the connector.
- Update `system.yaml`'s `launchMap` to `home_base/sub_complex.yaml`.
- Manual playtest: launch with `./gradlew lwjgl3:run`, walk east from the
  damaged sub through the connector tile into the maintenance sub.

Tests:
- Harness test for the composition: load `sub_complex.yaml`, assert both
  regions present at expected offsets, walk the player from one region
  into the other and assert `Position` updates cross the region boundary.
- Backfill `MapInitSystem` tests (already on `test-backfill.md` as 🟡).

Exit criteria: two real maps loadable simultaneously, player crosses
boundary in-game.

---

## Phase 4 — Layered pathfinding

Today's pathfinder builds one DijkstraMap per z-level sized to the whole
world. With two side-by-side maps that's wasteful (you scan empty space
between them) and gets worse as the world grows. Move to a layered scheme.

Work:
- Refactor `PathingContext`: key DijkstraMaps by `(regionId, zLevel)` (and
  `(regionId, zLevel, Size)` for size-aware paths). Each grid is sized to
  its region's bounds, not the world's.
- Dirty-marking becomes region-scoped: when a `Solid` entity moves, only
  the region containing its old/new position is dirtied. Use
  `MapContext.regionAt()`.
- Introduce a **region connectivity graph**: nodes are regions, edges are
  *portals* — a passable cell in region A adjacent to a passable cell in
  region B. Each edge tracks which portal tiles back it; multiple portal
  tiles between the same pair of regions are valid (the edge is "open" if
  at least one is currently traversable).
- **Door/lockable awareness**: portal tiles often *are* doors. The portal
  edge's "is this currently passable for this mover" check has to consult
  the same per-entity passability logic that the within-region pathing
  uses — locked doors, closed doors with `Openable`, `MaxPassableSize`
  restrictions, etc. The naive "is the cell solid" check isn't enough.
  Concretely:
  - The connectivity graph stores portal *tiles*, not pre-computed
    booleans.
  - At query time, the planner evaluates each portal tile against the
    moving entity's capabilities (same predicate as the within-region
    DijkstraMap uses).
  - When a door's `Openable`/`Lockable` state changes, the affected
    region's path cache is dirtied (same hook as a `Solid` change). The
    *graph topology* doesn't change — the graph just lists candidate
    portals; passability is re-evaluated on the next query.
- New `PathingPlanner` (or fold into `PathingSystem`): when start and goal
  are in the same region, use the existing DijkstraMap. When they're in
  different regions, do a coarse A* over the region graph to pick the next
  reachable portal, then a fine DijkstraMap path within the current region
  toward that portal. Hand back one next-step, same shape as today's API.
- No-path handling: same as today (return null / signal AI to give up).
  Includes the "portal exists but is locked/blocked for this mover" case —
  the planner sees no traversable edge and gives up gracefully.

Tests (backfill for `PathingSystem` and `PathingContext`, both already 🔴
on `test-backfill.md`):
- Per-region path: AI in region A pathing to a target in region A.
- Cross-region path: AI in region A pathing to player in region B via a
  single portal — assert next step heads toward the portal, then through.
- Locked door at portal: AI without the key can't path through; AI with
  the key can. Same map, two different movers.
- Closed-then-opened door: planner returns no-path while closed, then
  routes through after the door opens (and cache was invalidated).
- No portal → no path.
- Dirty isolation: changing a `Solid` in region A doesn't invalidate
  region B's cached DijkstraMap.

---

## Phase 5 — Editor support

Bring the JavaFX editor in line with the new schema.

Work:
- `EditorMapModel`: add `connectors` and `submaps` fields mirroring the
  runtime schema. Serialize/deserialize via `MapLoader` / `MapSaver`.
- **New "Submaps" tab** added to the inner `TabPane` in
  `MapViewPanel.java:207-213`, beside the existing "Blocks" and
  "Entities" tabs. Contents:
  - List of child refs with id, ref path, and placement (offset, or
    local/remote connector names).
  - Add / remove / edit a submap entry. Connector dropdowns pull from
    the current file's `connectors` and the referenced file's
    `connectors`.
  - "Open referenced file" action.
- A "Connectors" affordance — could be its own minor tab or folded into
  the "Entities" view, since connectors are basically map-level named
  positions. Pick whichever fits the existing UX best; if in doubt,
  start as a section inside the new Submaps tab.
- `MapRenderer`:
  - "Ghost region" rendering path for submaps in single-map mode
    (outline + label, not editable from this view).
  - Composed-world view (read-only): recursively pull child layers,
    translate, draw.
  - Render connector tiles distinctively.
  - Render overlap cells in a warning colour with a status-bar message.
- Toggle between single-map mode and composed-world mode (probably a
  button on the map view's toolbar).
- File browser side: when opening a composition YAML, show its children
  in a collapsible tree.

Tests:
- Editor remains primarily covered by manual playtest (per
  `test-backfill.md`'s "out of scope" list). Add unit tests for pure model
  logic only: `EditorMapModel` round-trip of `connectors` + `submaps`,
  connector resolution math, overlap detection in the flatten step.

---

## Phase 6 — Test backfill sweep

After phases 1–5 land, walk `test-backfill.md` and tick off entries for
files we actually touched, plus add new entries:

- `StaticTestMapGenerator` (add)
- `MapInitSystem` (already listed)
- `PathingSystem`, `PathingContext` (already listed, 🔴)
- `WorldModel` (add)
- New files: `MapRegion`, `MapContext`, `CompositeMapLoader`,
  `PathingPlanner` — add entries and write tests.

Per project testing policy: tests for *new code in this branch* land in
this branch; backfill for unchanged files stays separate — but every file
we modify in phases 1–5 counts as "changed" and gets coverage now.

---

## Resolved design decisions

### Content ref paths

Generalise the content loader to support both forms across all ref-like
fields (maps, future entity refs, etc.):

- `./<path>` — **relative**, resolved against the directory of the
  referring file. E.g. inside `home_base/sub_complex.yaml`, a ref of
  `./damaged_sub.yaml` resolves to `home_base/damaged_sub.yaml`.
- `/<path>` — **absolute**, rooted at the `content/` directory. E.g.
  `/maps/home_base/damaged_sub.yaml` regardless of the referring file's
  location.

(Note: your feedback labelled these the other way around; I've gone with
the conventional symbol mapping since the symbols are unambiguous —
`.` = here, `/` = root. If you actually meant the reverse, flag it and
we'll swap.)

Implementation: a small `ContentRef` resolver utility plus updates to
whatever currently consumes content paths. Out of strict necessity for
this phase, but worth doing in Phase 2 since the submap refs need *some*
resolver and we'd rather have the right one from day one.

### Overlap policy

- **Blocks**: default is fail-fast with a precise diagnostic; opt-in
  `allowOverlap: true` on a submap enables last-writer-wins (in submap-
  list order) for that placement.
- **Entities**: no conflict — multiple entities per tile is already legal.

### Rotation / reflection of submaps

Out of scope. Submaps only translate. If a use case appears later, we'll
revisit — it's a significant change to coordinate handling and portal
identification.

### Entity ownership across regions

Out of scope here. Likely a much bigger change — Dominion's entity model
isn't obviously partitioned, and we may need a creative solution
(per-region entity tags, or an entirely separate entity store per region).
Only needed when we add map streaming and unload regions outside the
player's vicinity.

### Save/load of dynamic state

Out of scope. Separate feature.
