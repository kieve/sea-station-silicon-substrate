# SquidGrid FOV (Field of View)

**Package:** `com.github.yellowstonegames.grid`
**Class:** `FOV`
**Module:** `squidgrid`
**Version:** 4.0.0-beta2 (project) / 4.0.3+ (latest)

## Overview

`FOV` is a utility class that provides static methods for calculating Field of View on 2D grids. It determines which cells are visible from a given source position, accounting for obstacles that block sight. FOV calculations are fundamental to roguelike games for determining what the player can see, and are also used for light propagation.

FOV uses **recursive shadowcasting**, which is fast, accurate, and produces aesthetically pleasing results. The algorithm works with a resistance map (how much each cell blocks light/vision) and produces a light/visibility map (how much light/visibility reaches each cell).

All methods use **x-then-y indexing** for 2D arrays: `array[x][y]`.

## Key Concepts

### Resistance Maps

A resistance map is a `float[][]` where each cell contains a value from `0.0f` to `1.0f`:
- `0.0f` = fully transparent (open floor)
- `1.0f` = fully opaque (solid wall)
- Values between represent partial transparency (e.g., glass, fog)

### Light/Visibility Maps

The output is a `float[][]` where each cell contains a value from `0.0f` to `1.0f`:
- `0.0f` = not visible / no light
- `1.0f` = fully visible / full light (typically the source cell)
- Values between represent partial visibility that decreases with distance

### Radius Types

FOV calculations use the `Radius` enum to determine the shape of the visible area:
- `Radius.CIRCLE` / `Radius.SPHERE` = Euclidean distance (round shape)
- `Radius.SQUARE` / `Radius.CUBE` = Chebyshev distance (square shape)
- `Radius.DIAMOND` / `Radius.OCTAHEDRON` = Manhattan distance (diamond shape)

## Public Methods

### `reuseFOV`

The primary method for FOV calculation. Computes the field of view from a source point and writes results into an existing `float[][]`, avoiding allocation.

```java
public static float[][] reuseFOV(
    float[][] resistanceMap,
    float[][] light,
    int startX,
    int startY,
    float radius,
    Radius radiusStrategy)
```

**Parameters:**
- `resistanceMap` -- A `float[width][height]` grid of resistance values (0.0 = transparent, 1.0 = opaque). This is NOT modified.
- `light` -- A `float[width][height]` grid that will be filled with the FOV result. Must be the same dimensions as `resistanceMap`. This IS modified in place.
- `startX` -- The x-coordinate of the FOV source (viewer/light position).
- `startY` -- The y-coordinate of the FOV source.
- `radius` -- The maximum radius of the FOV, in cells. Cells beyond this distance will be 0.0.
- `radiusStrategy` -- The `Radius` enum value determining the shape of the FOV area.

**Returns:** The `light` array, now filled with visibility values. The source cell `light[startX][startY]` will be `1.0f`.

**Example:**
```java
int width = 40, height = 30;
float[][] resistance = new float[width][height];
float[][] lightMap = new float[width][height];

// Set up walls (1.0 = opaque)
resistance[5][5] = 1.0f;
resistance[5][6] = 1.0f;
resistance[5][7] = 1.0f;

// Calculate FOV from position (10, 10) with radius 8
FOV.reuseFOV(resistance, lightMap, 10, 10, 8.0f, Radius.CIRCLE);

// Check visibility
if (lightMap[12][14] > 0.0f) {
    // Cell (12, 14) is visible
}
```

### `reuseFOV` (Angle-Limited)

Calculates FOV limited to a cone/arc defined by an angle and span.

```java
public static float[][] reuseFOV(
    float[][] resistanceMap,
    float[][] light,
    int startX,
    int startY,
    float radius,
    Radius radiusStrategy,
    float angle,
    float span)
```

**Parameters:**
- `resistanceMap` -- Resistance grid (same as above).
- `light` -- Output light grid (same as above).
- `startX`, `startY` -- Source position.
- `radius` -- Maximum FOV radius.
- `radiusStrategy` -- Shape of the FOV area.
- `angle` -- The center angle of the cone, in degrees. 0 = right/east, 90 = up/north, 180 = left/west, 270 = down/south.
- `span` -- The total angular width of the cone, in degrees. A span of 90 creates a quarter-circle cone.

**Returns:** The `light` array filled with visibility values, restricted to the specified cone.

**Example:**
```java
// FOV cone facing north (90 degrees) with 60-degree width
FOV.reuseFOV(resistance, lightMap, 10, 10, 8.0f, Radius.CIRCLE, 90f, 60f);
```

### `generateResistances`

Generates a resistance map from a `char[][]` dungeon map, where wall characters produce high resistance and floor characters produce zero resistance.

```java
public static float[][] generateResistances(char[][] map)
```

**Parameters:**
- `map` -- A `char[width][height]` dungeon map using standard dungeon characters (`'#'` for walls, `'.'` for floors, `'+'` for doors, etc.).

**Returns:** A new `float[width][height]` resistance map suitable for use with `reuseFOV`.

**Character Resistance Mapping:**
- `'.'` (floor) -> `0.0f`
- `'#'` (wall) -> `1.0f`
- `'+'` (closed door) -> `1.0f`
- `'/'` (open door) -> `0.0f`
- Most other floor-like characters -> `0.0f`

**Example:**
```java
char[][] dungeon = new char[40][30];
// ... populate dungeon ...
float[][] resistance = FOV.generateResistances(dungeon);
```

### `generateSimpleResistances`

A simpler version that only considers wall (`'#'`) characters as opaque.

```java
public static float[][] generateSimpleResistances(char[][] map)
```

**Parameters:**
- `map` -- A `char[width][height]` dungeon map.

**Returns:** A `float[width][height]` where `'#'` cells are `1.0f` and all others are `0.0f`.

### `addFOVsInto`

Merges multiple FOV maps into one combined map. Useful for combining the player's FOV with light source FOVs, or combining multiple light sources.

```java
public static float[][] addFOVsInto(
    float[][] addInto,
    float[][]... fovMaps)
```

**Parameters:**
- `addInto` -- The target `float[][]` that will receive the combined values. Modified in place.
- `fovMaps` -- One or more FOV maps to combine into `addInto`.

**Returns:** The `addInto` array with combined values. Values are clamped to `[0.0, 1.0]`.

**Example:**
```java
float[][] playerFOV = new float[width][height];
float[][] torchFOV = new float[width][height];
float[][] combined = new float[width][height];

FOV.reuseFOV(resistance, playerFOV, playerX, playerY, 8.0f, Radius.CIRCLE);
FOV.reuseFOV(resistance, torchFOV, torchX, torchY, 5.0f, Radius.CIRCLE);

FOV.addFOVsInto(combined, playerFOV, torchFOV);
```

### `mixVisibleFOVs`

Combines an FOV map (like a light source) with a "LOS map" (a very-high-radius FOV from the viewer) so that a distant light is only visible where the viewer can actually see.

```java
public static float[][] mixVisibleFOVs(
    float[][] losMap,
    float[][]... fovMaps)
```

**Parameters:**
- `losMap` -- A FOV map from the viewer's perspective, typically with a very large radius. Used to mask what the viewer can actually see.
- `fovMaps` -- One or more FOV maps (e.g., from light sources) to combine, but only where `losMap` has visibility.

**Returns:** A new `float[][]` with the combined visible FOV values.

**Example:**
```java
// Large LOS map for the player
float[][] losMap = new float[width][height];
FOV.reuseFOV(resistance, losMap, playerX, playerY, 50.0f, Radius.CIRCLE);

// Distant torch
float[][] torchFOV = new float[width][height];
FOV.reuseFOV(resistance, torchFOV, torchX, torchY, 6.0f, Radius.CIRCLE);

// Player can only see the torch's light where their LOS reaches
float[][] visibleLight = FOV.mixVisibleFOVs(losMap, torchFOV);
```

## Common Usage Patterns

### Basic Player Visibility

```java
// Setup (once)
float[][] resistance = FOV.generateResistances(dungeon);
float[][] visible = new float[width][height];

// Each turn (reuses the visible array)
FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0f, Radius.CIRCLE);

// Rendering: use visible[x][y] to determine tile brightness
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        if (visible[x][y] > 0.0f) {
            renderTile(x, y, visible[x][y]); // brightness from 0-1
        }
    }
}
```

### Custom Resistance Map

If your game does not use `char[][]` dungeon maps, build resistance manually:

```java
float[][] resistance = new float[width][height];
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        if (isWall(x, y)) {
            resistance[x][y] = 1.0f;
        } else if (isGlass(x, y)) {
            resistance[x][y] = 0.15f; // partially transparent
        } else {
            resistance[x][y] = 0.0f;
        }
    }
}
```

### Fog of War (Explored vs. Visible)

```java
boolean[][] explored = new boolean[width][height];
float[][] visible = new float[width][height];

// Each turn
FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0f, Radius.CIRCLE);

// Mark newly seen cells as explored
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        if (visible[x][y] > 0.0f) {
            explored[x][y] = true;
        }
    }
}
// Render: explored but not visible = dimmed, visible = bright, unexplored = hidden
```

## Important Caveats

1. **Array dimensions:** All `float[][]` arrays used together must have the same dimensions. The first index is X, the second is Y (`array[x][y]`).

2. **Thread safety:** FOV methods are static and do not use shared mutable state, but they modify the `light` array parameter. Do not share a `light` array between threads without synchronization.

3. **Performance:** `reuseFOV` is designed to reuse an existing array to avoid garbage collection pressure. Prefer it over methods that allocate new arrays.

4. **Radius values:** A radius of `0` will only light the source cell. Typical values are `4.0` to `12.0` for player vision. Very large radii are used for LOS maps.

5. **Source cell:** The source cell (`light[startX][startY]`) is always set to `1.0f` regardless of radius.

## See Also

- [Lighting](squidgrid-lighting.md) -- For managing multiple dynamic light sources with `LightingManager`
- [Line of Sight](squidgrid-los.md) -- For checking if a direct line exists between two points
- [Coord and Region](squidgrid-coord.md) -- For working with grid positions and areas

## Source

- [GitHub: SquidSquad squidgrid](https://github.com/yellowstonegames/SquidSquad/tree/main/squidgrid)
- [GitHub: SquidSquad](https://github.com/yellowstonegames/SquidSquad)
