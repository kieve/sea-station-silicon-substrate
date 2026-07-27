# Incompressible Water, Compressible Air, One Pressure Field

## Goal

Stop treating water as compressible. Split the single per-cell `mass`
scalar into **water fill** (incompressible, `[0, 1]`) and **pressure**
(its own field), then add **air** as a second occupant of the same cell
volume driven by that same pressure field.

The end state is one cell-state grid and one solver over a two-phase
mixture — not `FluidContext` plus a sibling `AirContext` with a second
simulator.

---

## Why

### Water does not meaningfully compress

Water's bulk modulus is ~2.2 GPa. At 10,000 m the ambient pressure is
roughly 100 MPa, giving ΔV/V ≈ 4.5% — under 0.05 L difference for a
litre at sea level, across the entire depth range the game will ever
use. Air over that same range compresses by a factor of ~1000. The two
phases are four orders of magnitude apart in compressibility, so the
correct simplification is **water is rigid, air is the compressible
phase**.

### But `MAX_COMPRESS` is load-bearing — it is not modeling compressibility

`FluidSimulator.MAX_COMPRESS = 0.02` is a numerical stand-in for
hydrostatic pressure propagation in a local cellular automaton. It is
currently the **only** mechanism that makes water rise, and deleting it
without a replacement silently breaks the core of the game (flooding
climbing a stairwell to the next deck).

The proof is in `flowUp` (`FluidSimulator.java:129-148`). Upward flow is
`remaining - stableState(remaining + massAbove)`, and `stableState`
returns `FULL` for any total ≤ `FULL` (`FluidSimulator.java:173-174`):

- Water capped at 1.0, air above → `flow = 1.0 - 1.0 = 0`, clamped to
  zero by `clampFlow`. Water never rises.
- Compression on, cell at 1.02 → `stableState(1.02) ≈ 1.0004`, so
  ~0.0196 flows up per step. That trickle is the multi-deck flood.

### The over-mass field is already doing pressure's job

`FluidSimulator.java:34` pins source cells to `sourceDepth * FULL`, so
`waterDepth: 3` means mass `3.0` — three full cells of water inside one
cell. As *mass* that is nonsense. As *pressure* it is exactly right.

Pressure already exists in the model; it is just wearing mass's clothes.
Splitting it out is a rename of something real, not a new invention.

### One field serves both phases

An explicit pressure field is also precisely what the air system needs,
so this change consolidates rather than adding a second axis:

```
freeVolume(cell)  = 1 - waterFill(cell)
airPressure(cell) = airAmount(cell) / max(freeVolume(cell), EPSILON)
```

That yields the signature mechanics of an underwater station directly,
with no special cases:

- **Diving bell.** Room floods, air cannot escape, free volume shrinks,
  air pressure climbs until it balances the incoming ocean head. The
  flood stalls partway.
- **Venting.** Open a door, air escapes, pressure drops, the room floods
  the rest of the way immediately.
- **Ceiling pockets.** Air rises through water on the same head
  gradient, so pockets emerge instead of being authored.

None of this is reachable while water is also compressible, because
"this cell is full" stays fuzzy and water's over-mass is
indistinguishable from air's.

---

## State model

Per cell, replacing today's single `mass` plus `source`:

| Field | Range | Notes |
| --- | --- | --- |
| `water` | `[0, 1]` | Incompressible volume fraction. Conserved. |
| `air` | `[0, ∞)` | Compressible quantity (amount, not fraction). |
| `pressure` | `[0, ∞)` | Propagated. Ocean sources impose it as a boundary condition. |

Flow is driven by **head difference** rather than mass difference: a
cell pushes water toward lower total head, where head combines cell
height, the water column above, and whatever gas pressure rests on the
surface. Upward flow stops being a compression artifact and becomes
ordinary pressure-driven flow.

`waterDepth` in map YAML becomes a **pressure** boundary condition
rather than a mass pin. This also finally separates it cleanly from
`waterFill`, which stays a mass/fill value in `[0, 1]` — today the two
are presented as independent optional knobs but setting both silently
discards the fill, because `FluidSimulator.java:33-35` overwrites source
cells on the first tick before anything reads it.

---

## Suggested phasing

Phase 1 is independently valuable and should land first; phase 2 is the
reason phase 1 is shaped this way.

### Phase 1 — split mass into water + pressure

1. `FluidContext`: `water` (clamped to `[0, 1]`) and `pressure` as
   separate fields. Keep the existing `levelForMass` quantization — it
   already operates on `[0, 1]`.
2. `FluidSimulator.stableState`: equilibrate head instead of mass.
   `MAX_COMPRESS` goes away.
3. `flowUp`: drive from pressure gradient rather than excess mass.
4. Source pinning (`FluidSimulator.java:34`): set pressure, not mass.
5. `YamlMapGenerator` (`:156-161`): `waterDepth` → `setSourcePressure`.

Keep the flux/conductance skeleton. It is sound, and `FluidBarrier`
gating in `FluidUtil` carries over unchanged.

### Phase 2 — add air to the same grid

1. Add the `air` field to the same context. **Do not** create an
   `AirContext` sibling with its own simulator — water and air are
   coupled through shared cell volume, and two independent solvers
   cannot produce the diving bell. Splitting them would also mean
   cross-checks sprinkled between systems, exactly what `CLAUDE.md`
   warns against for `blocksMover`.
2. Derive gas pressure from `air / freeVolume` and feed it into the same
   head calculation phase 1 introduced.
3. Buoyancy: air rises through water on the head gradient.

---

## Blast radius

Smaller than it looks. Everything downstream of mass already lives in
`[0, 1]` and clamps, so capping water at 1.0 changes nothing for it:

- `FluidContext.levelForMass` returns `MAX_LEVEL` for anything `>= FULL`
  (`:95-98`) — over-mass is already invisible to rendering.
- `WaterDamageSystem.submerged` thresholds are all ≤ `LEVEL_2_CEIL`
  (`:104-111`).
- `WaterDepthLayer.java:69` reads mass for display only.
- `FluidUtil.overflowMass` is a fraction of `FULL` (`:63-68`).

The real sites are `flowUp`, `stableState`, and the source pinning at
`FluidSimulator.java:34`, plus the debug overlay if it shows raw mass.

---

## Details to settle during implementation

**Division blow-up near full.** As `water` approaches 1.0 with gas still
present, `air / (1 - water)` explodes. Clamp the divisor and let the
pressure feedback be self-limiting — water naturally halts at the fill
where gas pressure balances incoming head. That is the correct
behaviour, not a guard against a bug.

**`FluidBarrier` is water-shaped.** `barrierHeight` models a lip you
pour over (`FluidUtil.overflowMass`). Air has no lip; it cares about
aperture. The component will need to split or generalize in phase 2.

**Per-step allocation.** `FluidSimulator.step` calls
`fluid.waterCells()` every step, allocating a fresh `ArrayList<Vec3i>`
over the whole wet volume (`FluidContext.java:131-142`), plus a
`HashMap<Vec3i, Double>` of boxed deltas (`FluidSimulator.java:37`).
For a station designed to flood across many decks that is per-tick
garbage proportional to flooded volume. Flattening the
`double[x][y][z]` grids to 1D with int-indexed delta buffers fixes it
and makes adding the `air` / `pressure` fields trivial
(struct-of-arrays). Worth doing during phase 1, since every accessor is
being touched anyway.

---

## Rejected alternative: zone-based atmosphere

Space Station 13 stores one gas mixture per flood-filled room and
equalizes instantly within it. Cheap, and instant equalization is
usually right for air.

Not adopted as the primary model, because a zone cannot represent "this
cell is 60% water" and partial flooding is the point of the game.

Worth keeping in reserve as an **optimization**: per-cell storage with
zone-accelerated solving — detect connected fully-air regions and
equalize them in one pass instead of iterating diffusion. Reach for it
only if air takes too long to settle.

---

## Related

Phase 2 is the trigger for the `MapBlockDefinition` refactor noted in
`TODO.txt` (dropping `waterFill` / `waterDepth` for a generic
`components:` list). Air is the second per-cell system, and at that
point `Water` and `Air` become sibling cell initializers — the generic
list shape earns itself, and `MapGenerator.generate` stops taking
`FluidContext` as an explicit parameter. Doing that refactor before air
exists means building the framework around a single special case.
