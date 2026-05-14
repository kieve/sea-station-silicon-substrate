# SquidGrid Line of Sight

**Package:** `com.github.yellowstonegames.grid`
**Module:** `squidgrid`
**Version:** 4.0.0-beta2 (project) / 4.0.3+ (latest)

## Overview

The squidgrid module provides line-drawing algorithms for determining lines of sight and drawing lines between two grid positions. The two primary classes are:

- **`BresenhamLine`** -- Classic Bresenham line algorithm that allows diagonal movement. Preferred for most line-of-sight uses.
- **`OrthoLine`** -- Orthogonal-only line algorithm that restricts movement to cardinal directions (no diagonals). Produces staircase-pattern lines.

Both classes produce ordered sequences of `Coord` representing the cells along the line from start to end.

---

## BresenhamLine

**Full Class Name:** `com.github.yellowstonegames.grid.BresenhamLine`

### Overview

`BresenhamLine` implements the classic Bresenham line algorithm adapted for grid-based games. It traces a line between two points, selecting the cells that best approximate the ideal line. The resulting line can include diagonal steps.

This is the standard choice for line-of-sight calculations in roguelike games because it closely approximates a straight line through grid cells.

### Public Methods

#### `line` (static, Coord to Coord)

Draws a line between two Coords.

```java
public static ObjectList<Coord> line(Coord start, Coord end)
```

**Parameters:**
- `start` -- The starting Coord.
- `end` -- The ending Coord.

**Returns:** An `ObjectList<Coord>` containing all cells along the line, from `start` to `end` inclusive.

**Example:**
```java
Coord from = Coord.get(2, 3);
Coord to = Coord.get(10, 7);
ObjectList<Coord> line = BresenhamLine.line(from, to);

for (Coord point : line) {
    // Process each cell along the line
}
```

#### `line` (static, int coordinates)

Draws a line between two points specified as integer coordinates.

```java
public static ObjectList<Coord> line(int startX, int startY, int endX, int endY)
```

**Parameters:**
- `startX`, `startY` -- Starting position.
- `endX`, `endY` -- Ending position.

**Returns:** An `ObjectList<Coord>` containing all cells along the line.

**Example:**
```java
ObjectList<Coord> line = BresenhamLine.line(2, 3, 10, 7);
```

#### `line` (static, into existing array)

Draws a line and writes results into a provided `Coord[]` buffer to avoid allocation.

```java
public static Coord[] line(
    int startX, int startY,
    int endX, int endY,
    Coord[] buffer)
```

**Parameters:**
- `startX`, `startY` -- Starting position.
- `endX`, `endY` -- Ending position.
- `buffer` -- A pre-allocated `Coord[]` to write results into. Must be large enough to hold the line.

**Returns:** The `buffer` array (may be truncated or padded). Check for `null` entries if the buffer is larger than needed.

#### `reachable` (static)

Checks whether a target position is reachable (has clear line of sight) from a source position, given a resistance map and maximum distance.

```java
public static boolean reachable(
    float[][] resistanceMap,
    int startX, int startY,
    int endX, int endY,
    Radius radiusStrategy,
    float maxDistance)
```

**Parameters:**
- `resistanceMap` -- A `float[width][height]` resistance grid (same format as used by FOV: 0.0 = transparent, 1.0 = opaque).
- `startX`, `startY` -- The source position.
- `endX`, `endY` -- The target position.
- `radiusStrategy` -- The `Radius` type used for distance calculation (e.g., `Radius.CIRCLE`).
- `maxDistance` -- Maximum distance to check. If the Euclidean/Manhattan/Chebyshev distance (depending on strategy) exceeds this, returns `false` immediately.

**Returns:** `true` if a clear line of sight exists from start to end within the given distance.

**Example:**
```java
float[][] resistance = FOV.generateResistances(dungeon);

// Can the player see the enemy?
boolean canSee = BresenhamLine.reachable(
    resistance,
    player.x, player.y,
    enemy.x, enemy.y,
    Radius.CIRCLE,
    12.0f);

if (canSee) {
    // Enemy is visible
}
```

#### `reachable` (static, Coord version)

```java
public static boolean reachable(
    float[][] resistanceMap,
    Coord start,
    Coord end,
    Radius radiusStrategy,
    float maxDistance)
```

Same as above but using Coord parameters.

### Line of Sight Pattern

A common pattern for checking LOS by walking the Bresenham line and testing each cell:

```java
// Manual LOS check
ObjectList<Coord> line = BresenhamLine.line(startX, startY, endX, endY);
boolean blocked = false;
for (Coord c : line) {
    if (resistance[c.x][c.y] >= 1.0f) {
        blocked = true;
        break;
    }
}
```

However, prefer `reachable()` which handles this internally.

---

## OrthoLine

**Full Class Name:** `com.github.yellowstonegames.grid.OrthoLine`

### Overview

`OrthoLine` draws lines between two grid positions using only cardinal movement (up, down, left, right -- no diagonals). The resulting line has a staircase pattern. This is useful for games that restrict movement to 4-directional, or for creating orthogonal corridors and hallways.

### Public Methods

#### `line` (static, Coord to Coord)

Draws an orthogonal line between two Coords.

```java
public static ObjectList<Coord> line(Coord start, Coord end)
```

**Parameters:**
- `start` -- The starting Coord.
- `end` -- The ending Coord.

**Returns:** An `ObjectList<Coord>` containing all cells along the orthogonal line, from `start` to `end` inclusive.

**Example:**
```java
Coord from = Coord.get(2, 3);
Coord to = Coord.get(6, 7);
ObjectList<Coord> path = OrthoLine.line(from, to);
// Might produce: (2,3), (3,3), (3,4), (4,4), (4,5), (5,5), (5,6), (6,6), (6,7)
```

#### `line` (static, int coordinates)

```java
public static ObjectList<Coord> line(int startX, int startY, int endX, int endY)
```

Same as above but with integer coordinates.

#### `line` (static, into existing array)

```java
public static Coord[] line(
    int startX, int startY,
    int endX, int endY,
    Coord[] buffer)
```

Writes results into a pre-allocated buffer.

#### `reachable` (static)

Checks whether a target is reachable via orthogonal line of sight.

```java
public static boolean reachable(
    float[][] resistanceMap,
    int startX, int startY,
    int endX, int endY,
    Radius radiusStrategy,
    float maxDistance)
```

**Parameters:** Same as `BresenhamLine.reachable()`.

**Returns:** `true` if a clear orthogonal line of sight exists.

**Caveat:** Because orthogonal lines take longer paths (no diagonals), targets that are reachable via `BresenhamLine` may not be reachable via `OrthoLine` within the same `maxDistance`.

---

## LineTools

**Full Class Name:** `com.github.yellowstonegames.grid.LineTools`

### Overview

`LineTools` provides utilities for working with box-drawing characters to represent walls, doors, and other structural features. It can determine the appropriate Unicode box-drawing character based on which sides of a cell connect to adjacent walls.

### Key Methods

#### `lineCharAt`

Determines the appropriate line/box-drawing character for a position based on its neighbors.

```java
public static char lineCharAt(char[][] map, int x, int y)
```

**Parameters:**
- `map` -- A `char[][]` dungeon map.
- `x`, `y` -- The position to check.

**Returns:** A Unicode box-drawing character that visually connects to adjacent wall cells.

## Choosing Between BresenhamLine and OrthoLine

| Feature | BresenhamLine | OrthoLine |
|---------|--------------|-----------|
| Diagonal movement | Yes | No |
| Path length | Shorter | Longer (staircase) |
| Typical use | LOS, projectiles | 4-dir movement, corridors |
| Visual accuracy | Better line approximation | Axis-aligned only |

For most roguelike games with 8-directional movement, **BresenhamLine** is the standard choice for line-of-sight checks. Use **OrthoLine** when your game restricts movement to 4 directions or when generating orthogonal dungeon corridors.

## Important Caveats

1. **Line includes endpoints.** Both `start` and `end` are included in the returned line.

2. **Order matters.** The line is ordered from `start` to `end`. The first element is always `start`, the last is always `end`.

3. **Resistance map consistency.** Use the same resistance map format as `FOV.reuseFOV()` for `reachable()` checks: `0.0f` = transparent, `1.0f` = opaque.

4. **Buffer sizing.** When using the buffer variant, the buffer should be at least `max(|endX - startX|, |endY - startY|) + 1` elements for BresenhamLine, and `|endX - startX| + |endY - startY| + 1` for OrthoLine.

## See Also

- [FOV](squidgrid-fov.md) -- FOV uses similar resistance maps and provides area-based visibility
- [Coord and Region](squidgrid-coord.md) -- Lines produce sequences of Coords
- [Lighting](squidgrid-lighting.md) -- LOS checks are used internally by the lighting system

## Source

- [GitHub: SquidSquad squidgrid](https://github.com/yellowstonegames/SquidSquad/tree/main/squidgrid)
- [GitHub: SquidSquad](https://github.com/yellowstonegames/SquidSquad)
