# Test Backfill Checklist

Now that the Layer 1 harness (`TestEngine` / `TestGameContext`) exists, this is
the running list of systems, utilities, and AI pieces to backfill tests for.
Check off as we go.

Vocabulary:
- **Harness** — full headless `GameContext` driven by `TestEngine`. Best for
  anything that mutates ECS state via the update loop.
- **Unit** — plain JUnit + Mockito with hand-rolled fixtures. Best for pure
  functions and small evaluators.
- **Skip** — render-only, GL-coupled, or trivial data-only. Not worth a test.

Risk legend: 🔴 high (core loop, behaviour-driving) · 🟡 medium · 🟢 low

---

## Test fixtures: per-test maps

Two-tier approach:

1. **Default empty map** — a single shared fixture, just open floor and a
   player spawn, for tests that don't care about world geometry (clock
   stage transitions, input wiring, log output, anything entity-spawned
   programmatically in the test). Most tests should land here.
2. **Purpose-built per-test maps** — for tests where geometry *is* the
   subject (movement blocked by a wall, FOV around a pillar, locked door
   + key). Define a tight, minimum-viable map that names its scenario.

**Never reuse `debug/static_test_map.yaml` or production maps in tests.**
They drift as the game grows; a test pinned to a generic map asserts
against incidental geometry and breaks for unrelated reasons.

Conventions:
- Location: `core/src/test/resources/content/maps/test/<scenario>.yaml`.
  Test classpath is merged with main at test runtime, so
  `StaticTestMapGenerator("test/<scenario>.yaml")` Just Works without
  changing the loader.
- The default empty map lives at
  `core/src/test/resources/content/maps/test/empty.yaml` and is exposed
  as a convenience factory: `TestGameContext.createEmpty()`.
- Naming for purpose-built maps: `<system_or_scenario>_<variant>.yaml`.
  Examples: `wasd_blocked_by_wall.yaml`,
  `attack_player_vs_one_enemy.yaml`, `vision_pillar.yaml`.
- Keep maps **minimum viable** — only the tiles and entities the test
  needs. No decorative content.
- Player start should be unambiguous (a single `Player`-controlled entity
  at a known position).
- Duplication beats coupling: if two scenarios happen to look similar but
  represent different intents, copy rather than share.

---

## Update systems

Listed in execution order (see `GameEngine.create`).

- [ ] 🔴 **ClockSystem** — harness. Stage transitions across PLAYER ↔ AI phases,
      time advancement, queued waits. (Smoke test already covers the happy path.)
- [ ] 🟡 **InteractSystem** — harness. Trigger interact action on adjacent
      interactables; assert intent resolves and the correct downstream system
      consumes it.
- [ ] 🟡 **OpenSystem** — harness. Open/close doors, locked-door rejection,
      `Openable` state transitions.
- [ ] 🟡 **SocketSystem** — harness. Plug/unplug `SocketPlug` into `Socket`;
      lock/unlock side effects via `LockId`.
- [ ] 🟡 **ExamineSystem** — harness. Crosshair movement (WASD mapping), camera
      follow, examine target resolution, log output.
- [ ] 🟡 **EjectSystem** — harness. Direction inputs eject items in the right
      Y-up direction; inventory side effects.
- [ ] 🟡 **InteractMenuSystem** — harness. Menu open/close, option selection,
      action dispatch.
- [~] 🔴 **WasdSystem** — harness. ✅ all 4 directions, diagonal (UP+RIGHT in
      one turn), blocked-by-wall (single-axis), wait-advances-clock-only (smoke).
      Still TODO: blocked-by-`MaxPassableSize`, multi-axis blocked (e.g. UP+RIGHT
      with a wall to the north — does east still resolve?).
- [~] 🔴 **PathingSystem** — harness + unit. Per-tick driver wiring `Velocity`
      from cached paths is still bare; the planning side is covered indirectly
      through `PathingContext` (cross-region routes, locked-door rejection,
      door-state invalidation). Still TODO: multi-step path consumption across
      ticks, target re-pathing when the goal moves.
- [ ] 🔴 **AiControllerSystem** — harness. State machine ticks, target acquisition,
      idle entities not consuming clock budget incorrectly.
- [ ] 🔴 **AttackSystem** — harness. Damage application, `Health` decrement,
      death (entity removal / corpse), `LastAttacker` write, multi-attack-per-turn
      sanity.
- [ ] 🟡 **VelocitySystem** — harness. Velocity → Position application, collisions
      with Solid, multi-tick movement consumption.
- [ ] 🟢 **CameraSystem** — skip (render-coupled; null-camera guard already covered
      by harness existing without crash).
- [ ] 🔴 **VisionSystem** — harness + unit. FOV computation, `Hidden`/visibility
      flags, opaque blocker interactions, examine-mode camera override. Cross-
      check against `VisionUtil` / `OpaqueGrid` unit tests below.
- [ ] 🟡 **SanityCheckSystem** — harness. Verify it doesn't throw on valid state;
      maybe a couple of intentionally-broken states asserting it *does* throw.
- [ ] 🟡 **EventSystem** — harness. Event enqueue → handler dispatch → log/state
      effects.

## Init systems

- [~] 🟡 **MapInitSystem** — harness. Map load + spawn pipeline is exercised
      indirectly by `TestEngineSmokeTest`, `SubComplexCompositionTest`, and the
      pathing-context tests that load real fixtures. No direct spawn-count
      assertion yet.
- [ ] 🟡 **ScurryInitSystem** — harness. `ScurryInit` consumed, `ScurryConfig`
      applied, init component removed.

## Render systems

All **Skip** — out of scope per the harness plan. Cover via manual playtest only.
- DebugRectRenderSystem
- TileGlyphRenderSystem
- TileHighlightRenderSystem

---

## Utilities (pure unit tests)

- [x] Vec3i
- [x] ListUtil
- [x] DescriptionComposer
- [x] 🟢 **BoundingBox3i** — `contains` edges, defensive copy of origin/size,
      `forEach` visits every cell, zero/negative-size rejection. Covered by
      `BoundingBox3iTest`.
- [x] 🔴 **VisionUtil** — `blocksVision(Entity)` branches: no Opaque,
      Opaque+no Openable, Opaque+closed, Opaque+open.
- [~] 🟢 **OpaqueGrid** — single-method functional interface with no logic
      of its own. Real behaviour lives in `VisionContext.recalculate` (consumer)
      and the lambdas in `BlockTypeFactory.isOpaque` (producer); both get
      covered under their own entries.
- [ ] 🟡 **PlayerUtil** — controlled-entity lookup, controlled-position lookup,
      empty-ECS edge case.
- [ ] 🟡 **SolidUtil** — solid-at-position queries, multi-entity stacking.
- [ ] 🟡 **LockableUtil** — locked/unlocked transitions, key match logic.
- [ ] 🟡 **OpenableUtil** — open/close transitions, blocked-by-lock case.
- [ ] 🟡 **InventoryUtil** — add/remove/contains, capacity behaviour if any.
- [ ] 🟡 **InteractionResolver** — pick-the-right-interaction across multiple
      candidates at a tile.
- [ ] 🟢 **TickStage / TurnPhase** — enums, skip unless they grow logic.
- [ ] 🟢 **PerfClock** — perf instrumentation, skip.
- [ ] 🟢 **ClasspathUtil** — IO glue, skip unless it grows logic.

## Components

Mostly pure data carriers — **Skip** unless they have non-trivial logic.

- [ ] 🟢 **CameraComp** — unit. `setPosition` null-guard, `gdx()` accessor.
- [ ] 🟢 **TileGlyph** — null-font headless construction (already exercised by
      harness smoke). Optional dedicated test.

## Contexts

Mostly storage. Test only the ones with behavior beyond getters/setters.

- [ ] 🟡 **LogContext** — message append, ordering, message cap if any.
- [ ] 🟡 **ClockContext** — time/stage/phase invariants, advance logic.
- [ ] 🟡 **PositionContext** — entity↔position bookkeeping under move/spawn/
      despawn.
- [ ] 🟡 **VisionContext** — visibility flag set/clear, "ever-seen" memory.
- [x] 🟡 **PathingContext** — same-region planning, cross-region portal
      selection, per-mover passability (locked door at portal, closed door
      within region, opened door allowing the route), no-path branches.
      Covered by `PathingContextTest`.
- [ ] 🟢 InputContext, RenderContext, EventContext, ExamineContext,
      EjectContext, InteractContext, AiControllerContext — skip unless logic
      grows.

---

## Map composition (modular-maps branch)

Added by the modular-maps work — multiple YAML files compose into one
world via offsets or connector pairs, regions track per-cell ownership,
and pathing routes across them via portals.

- [x] 🟢 **MapRegion** — id/box invariants, `contains` via box, null
      rejection. Covered by `MapRegionTest`.
- [x] 🟡 **MapContext** — cell-stamp ownership beats bounding-box overlap
      lookups, before-init defensive behaviour, out-of-bounds → null,
      unmodifiable region view. Covered by `MapContextTest`.
- [x] 🔴 **CompositeMapLoader** — offset-mode placements, connector-mode
      offset math, nested composition, missing-connector and cycle
      diagnostics. Covered by `CompositeMapLoaderTest`.
- [x] 🟡 **YamlMapGenerator** — leaf load (`YamlMapGeneratorTest`) and
      composite load (`YamlMapGeneratorCompositeTest`): world sizing to
      the union, region-per-leaf registration, cell-stamp consultation,
      overlap fail-fast vs `allowOverlap: true`, child entity position
      translation.
- [x] 🟡 **RegionGraph** — portal discovery between adjacent regions,
      `portalsFor` symmetry, `findPortalTowards` closest-portal choice on
      multi-portal edges, unreachable-region returns null. Covered by
      `RegionGraphTest`.
- [x] 🟢 **WorldModel** — get/set round-trip, out-of-bounds → AIR,
      `isInBounds` matches box, `isSolid` via `BlockTypeFactory`,
      `isPassable` inversion, `hasFloor`/`hasCeiling` neighbour reads.
      Covered by `WorldModelTest`.
- [x] 🟢 **ContentRef** — `./` resolves against the referring file's
      directory, `/` rooted at content, parent-segment escape rejection.
      Covered by `ContentRefTest`.
- [ ] 🟢 **Portal** — `otherRegion`/`cellIn` lookups + the not-connected
      `IllegalArgumentException` branch. Implicitly exercised by
      `RegionGraphTest` but no direct test.
- [x] 🟡 **sub_complex composition (end-to-end)** — boundary cells stamp
      the right regions, wall cells on both sides of the join still spawn
      block entities, player walks east through `damaged_sub` up to the
      locked door at the portal. Covered by `SubComplexCompositionTest`.

### Editor (modular-maps branch)

Per `test-backfill.md`'s "out of scope" list the JavaFX editor module
relies on manual playtest. Modular-maps added a "Submaps" tab,
connector-aware rendering, and ghost-region overlays. No unit coverage
added; treat as manual playtest territory unless a pure-model regression
shows up.

---

## AI (unit-test heavy, harness for integration)

### Controller
- [ ] 🔴 **AiController** — state transitions on tick, reset propagation.
- [ ] 🔴 **StateEvaluator** — branch selection precedence, reset handling,
      `MaxTimes` interaction.

### States (each: unit test for `tick` + transitions)
- [ ] 🟡 IdleState
- [ ] 🟡 WanderState
- [ ] 🔴 ChaseState — pathing target update, lose-target case.
- [ ] 🟡 FleeState
- [ ] 🔴 AttackState — adjacency check, damage flow via AttackSystem.
- [ ] 🟡 ScurryState
- [ ] 🟡 RandomBranchState — branch weighting / determinism with seeded RNG.
- [ ] 🟢 AnnounceDeathState — log message only.

### Conditions
- [ ] 🟡 HasWeaponCondition
- [ ] 🟡 IsDeadCondition
- [ ] 🟡 WasAttackedCondition
- [ ] 🟡 DistanceToEntityCondition — boundary distances, missing-target case.
- [ ] 🟡 MaxTimesCondition — counter increment, reset interaction.

### Resets
- [ ] 🟡 AttackerChangeReset — fires when `LastAttacker` flips, no-op otherwise.

### Behaviors (data layer)
- [ ] 🟢 BehaviorFactory / BehaviorDefinition / BranchDefinition /
      ConditionDefinition / StateDefinition / TargetType — YAML round-trip.
      Optional; mostly covered transitively when state/condition tests run.

---

## Harness-driven scenario tests (cross-system)

Once individual systems have coverage, layer in a few end-to-end scenarios in
`testharness/`. Each gets its own purpose-built map fixture (see "Test
fixtures" above).

- [ ] 🔴 **Player kills enemy** — move adjacent → attack → enemy dies → log.
- [ ] 🔴 **Enemy chases player** — AI sees player, paths, attacks.
- [ ] 🟡 **Locked door + key** — pick up key → use on door → door opens.
- [ ] 🟡 **Eject from inventory** — inventory → ground tile via direction key.
- [ ] 🟡 **Examine flow** — toggle examine → move crosshair → log target →
      cancel.
- [ ] 🟡 **Vision regression** — entity behind opaque tile not visible; step
      around it → becomes visible; step back → marked as remembered.

---

## Out of scope

- Render correctness (TileGlyph rendering, font glyphs, shape rendering).
- libGDX input layer (`Gdx.input`) — bypassed entirely by `TestEngine.pressAction`.
- UI rendering — UI layout/node logic is already covered by existing tests.
- JavaFX `editor` module — separate concern, manual testing.
