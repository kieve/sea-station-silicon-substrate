# Composable Map System

## Context

Currently, the game loads a single map via `StaticTestMapGenerator` into a single `WorldModel`. The goal is to allow multiple maps to be composed into a single world by connecting them at named connection points. Maps define exits (a position + facing direction), and a world definition file specifies which exits connect. Connected maps are placed so their exit tiles are adjacent (1 tile apart). This is a bounded first step — maps merge into a single `WorldModel` — designed so the API can later support infinite/chunked worlds.

## YAML Format Changes

### Map files gain a `connections` section

```yaml
# In static_test_map.yaml
connections:
  east_door:
    position: { x: 26, y: 7, z: 1 }
    facing: EAST

# In damaged_sub.yaml
connections:
  west_entry:
    position: { x: 0, y: 3, z: 1 }
    facing: WEST
```

Each connection point has a local position within the map and a `facing` direction (outward, toward the connected neighbor).

### New world definition file

`core/src/main/resources/content/worlds/test_world.yaml`

```yaml
maps:
  - id: main_hall
    file: static_test_map
  - id: submarine
    file: damaged_sub

connections:
  - from: { map: main_hall, exit: east_door }
    to: { map: submarine, exit: west_entry }

playerSpawn:
  map: main_hall
  position: { x: 6, y: 6, z: 1 }

floorGlyph: interpunct
```

### Map entity spawns (migrated from hard-coded Java)

Maps gain an `entities` section so entity spawns are data-driven:

```yaml
# In static_test_map.yaml
entities:
  - entityId: deadMech
    position: { x: 5, y: 5, z: 1 }
    color: "#FFD700FF"
  - entityId: trainingDummy
    position: { x: 23, y: 7, z: 1 }
    color: "#FF69B4FF"
```

## Implementation Steps

### Step 1: Create `Direction` enum

**New file:** `core/src/main/java/ca/kieve/ssss/util/Direction.java`

- Values: `NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`
- Each has a `Vec3i offset()` method (reuse existing `Vec3i.NORTH` etc., plus new Z offsets)
- `opposite()` method for validation

### Step 2: Create content records for YAML deserialization

**New files:**
- `content/ConnectionPointDefinition.java` — record with `Vec3i position`, `Direction facing`
- `content/WorldDefinition.java` — record with `List<WorldMapEntry> maps`, `List<WorldConnection> connections`, `WorldPlayerSpawn playerSpawn`, `String floorGlyph`
- `content/WorldMapEntry.java` — record with `String id`, `String file`
- `content/WorldConnection.java` — record with `ConnectionRef from`, `ConnectionRef to`
- `content/ConnectionRef.java` — record with `String map`, `String exit`
- `content/WorldPlayerSpawn.java` — record with `String map`, `Vec3i position`
- `content/MapEntitySpawn.java` — record with `String entityId`, `Vec3i position`, `String color` (nullable)

### Step 3: Modify `MapDefinition`

**File:** `core/src/main/java/ca/kieve/ssss/content/MapDefinition.java`

Add two fields:
- `Map<String, ConnectionPointDefinition> connections` (default empty map)
- `List<MapEntitySpawn> entities` (default empty list)

Existing maps without these fields will parse fine due to null-to-empty normalization.

### Step 4: Extract map parsing into `MapParser`

**New file:** `core/src/main/java/ca/kieve/ssss/world/MapParser.java`

Extract the YAML loading + character mapping + layer parsing logic from `StaticTestMapGenerator` into a reusable static utility. Returns a `ParsedLocalMap` record containing `String[][][] blocks`, `int width`, `int height`, `int depth`.

Both `StaticTestMapGenerator` (if kept) and `WorldComposer` will use this.

### Step 5: Create `WorldComposition` result record

**New file:** `core/src/main/java/ca/kieve/ssss/world/WorldComposition.java`

```java
public record WorldComposition(
    WorldModel worldModel,
    Vec3i playerSpawn,
    String floorGlyphId,
    List<PlacedMapInfo> placedMaps
) {
    public record PlacedMapInfo(
        String mapId,
        MapDefinition definition,
        Vec3i offset
    ) {}
}
```

### Step 6: Create `WorldComposer`

**New file:** `core/src/main/java/ca/kieve/ssss/world/WorldComposer.java`

Core logic:

1. **Load** the `WorldDefinition` YAML and each referenced `MapDefinition`
2. **Parse** each map into a local block grid via `MapParser`
3. **Compute offsets** via BFS from the first map (placed at origin):
    - For connection `A.exitX <-> B.exitY`:
    - `exitX_world = offsetA + exitX.position`
    - `exitY_world = exitX_world + exitX.facing.offset()`
    - `offsetB = exitY_world - exitY.position` (i.e. `exitY_world + exitY.position.product(-1)`)
    - Validate `exitX.facing == exitY.facing.opposite()`
4. **Normalize** offsets so minimum corner is at (0,0,0) — shift all offsets to eliminate negative coords
5. **Merge** into a single `WorldModel` sized to the bounding box
    - For each map, write non-air blocks at `localPos + offset`
    - If a non-air block would overwrite another non-air block, throw `IllegalStateException`
6. **Adjust** player spawn: `worldDef.playerSpawn.position + mapOffset`
7. **Validate** merged dimensions fit within `PositionContext` limits (200x200x20)

### Step 7: Update map YAML files

- Add `connections` to `static_test_map.yaml` and `damaged_sub.yaml`
- Add `entities` to `static_test_map.yaml` (migrate spawns from `StaticTestMapGenerator.createEntities()`)
- Create `core/src/main/resources/content/worlds/test_world.yaml`

Note: The hard-coded `createDebugMover` and `createRoboMouse` calls in `StaticTestMapGenerator` use special factory methods. These will need either equivalent YAML entity spawn support or a temporary bridge in `GameEngine`. Since `createDebugMover` takes a speed parameter and `createRoboMouse` takes clockwise/direction, these may need to stay as code for now (called from `GameEngine` with offset adjustment) until the entity YAML format supports those parameters.

### Step 8: Update `GameEngine`

**File:** `core/src/main/java/ca/kieve/ssss/GameEngine.java`

- Replace `MapGenerator` field with `WorldComposer` + `WorldComposition`
- `init()`: Call `worldComposer.compose("test_world", blockTypes)` instead of `mapGenerator.generate()`
- `createEntities()`: Iterate `composition.placedMaps()`, create offset-adjusted entities from each map's entity list
- `initializeRenderSystems()`: Use `composition.floorGlyphId()` instead of `mapGenerator.getFloorGlyphId()`
- Temporarily keep special entity creation (debug movers, robo mouse) as code with offset applied

### Step 9: Clean up

- Refactor `StaticTestMapGenerator` to use `MapParser`, or remove if fully replaced
- Remove `MapGenerator` interface if no longer needed (or keep for future procedural generation)
- Remove `getMapGenerator()` from `GameEngine` if unused

## Key Files

| File | Action |
|------|--------|
| `core/.../util/Direction.java` | **New** |
| `core/.../content/ConnectionPointDefinition.java` | **New** |
| `core/.../content/WorldDefinition.java` | **New** |
| `core/.../content/WorldMapEntry.java` | **New** |
| `core/.../content/WorldConnection.java` | **New** |
| `core/.../content/ConnectionRef.java` | **New** |
| `core/.../content/WorldPlayerSpawn.java` | **New** |
| `core/.../content/MapEntitySpawn.java` | **New** |
| `core/.../content/MapDefinition.java` | **Modify** — add connections + entities |
| `core/.../world/MapParser.java` | **New** |
| `core/.../world/WorldComposition.java` | **New** |
| `core/.../world/WorldComposer.java` | **New** |
| `core/.../world/StaticTestMapGenerator.java` | **Refactor/Remove** |
| `core/.../GameEngine.java` | **Modify** |
| `content/maps/static_test_map.yaml` | **Modify** — add connections + entities |
| `content/maps/damaged_sub.yaml` | **Modify** — add connections |
| `content/worlds/test_world.yaml` | **New** |

## Reusable Existing Code

- `Vec3i.add()`, `Vec3i.product(-1)` for offset arithmetic
- `Vec3i.NORTH/SOUTH/EAST/WEST` constants (Direction enum wraps these)
- `WorldModel.setBlock()` for merging
- `WorldEntityFactory.createEntities()` unchanged — works on the merged WorldModel
- `EntityFactory.createEntity(context, id, pos, color)` for per-map entity spawns
- Jackson YAML deserialization patterns from `StaticTestMapGenerator` and `ContentLoader`

## Verification

1. **Build:** `./gradlew build` — no compilation errors
2. **Run:** `./gradlew lwjgl3:run` — game loads with the composed world
3. **Visual check:** Both maps render correctly, player spawns in main_hall, can walk to the connection point and cross into the submarine map
4. **Overlap test:** Temporarily modify a map so non-air blocks overlap — verify the error is thrown
5. **Single-map test:** Create a world definition with just one map and no connections — verify it works identically to before
