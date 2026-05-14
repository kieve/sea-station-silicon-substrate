# SquidGrid Coord and Region

**Package:** `com.github.yellowstonegames.grid`
**Module:** `squidgrid`
**Version:** 4.0.0-beta2 (project) / 4.0.3+ (latest)

---

## Coord

**Full Class Name:** `com.github.yellowstonegames.grid.Coord`

### Overview

`Coord` is an **immutable** 2D integer point class with **object pooling** for efficiency. It is the standard way to represent grid positions throughout SquidSquad. Coord extends `Point2<Coord>` from the Crux library, providing a consistent interface for 2D point operations.

Because Coord instances are pooled, you should always use the factory method `Coord.get(x, y)` rather than calling a constructor. This ensures that identical coordinates share the same object instance (within the pool range), enabling fast equality checks and reducing memory usage.

### Fields

```java
public final int x;  // The x-coordinate (column)
public final int y;  // The y-coordinate (row)
```

Fields are `final` because Coord is immutable. All operations that would change coordinates return a new Coord instance instead.

### Factory Methods

#### `get`

The primary way to create Coord instances. Returns a pooled instance when possible.

```java
public static Coord get(int x, int y)
```

**Parameters:**
- `x` -- The x-coordinate.
- `y` -- The y-coordinate.

**Returns:** A Coord with the given coordinates. If within the pool range, returns a cached instance.

**Example:**
```java
Coord pos = Coord.get(5, 10);
Coord origin = Coord.get(0, 0);

// Pooled instances are identical objects (within default pool range)
Coord a = Coord.get(3, 4);
Coord b = Coord.get(3, 4);
// a == b is true (same pooled object)
```

#### `get` (from float)

Creates a Coord by rounding float coordinates.

```java
public static Coord get(float x, float y)
```

### Pool Management

#### `expandPoolTo`

Expands the coordinate pool to cover a larger range. The pool caches Coord instances for coordinates from `(-3, -3)` up to `(width-4, height-4)` by default.

```java
public static void expandPoolTo(int width, int height)
```

**Parameters:**
- `width` -- The desired pool width (x range).
- `height` -- The desired pool height (y range).

**Caveat:** The pool can only be expanded, never shrunk. Call this early (e.g., at game startup) if your map dimensions exceed the default pool size.

**Example:**
```java
// Expand pool to cover a 100x100 map
Coord.expandPoolTo(100, 100);
```

### Movement / Translation Methods

All movement methods return a **new** Coord instance. The original is never modified.

#### `translate`

Returns a new Coord shifted by the given amounts.

```java
public Coord translate(int x, int y)
```

**Parameters:**
- `x` -- Amount to add to the x-coordinate.
- `y` -- Amount to add to the y-coordinate.

**Returns:** A new Coord at `(this.x + x, this.y + y)`.

**Example:**
```java
Coord pos = Coord.get(5, 10);
Coord moved = pos.translate(1, -1); // (6, 9)
// pos is still (5, 10) -- immutable
```

#### `add`

Adds another Coord's coordinates to this one.

```java
public Coord add(Coord other)
```

**Returns:** A new Coord at `(this.x + other.x, this.y + other.y)`.

#### `subtract`

Subtracts another Coord's coordinates from this one.

```java
public Coord subtract(Coord other)
```

**Returns:** A new Coord at `(this.x - other.x, this.y - other.y)`.

### Distance Methods

#### `distance`

Calculates Euclidean distance to another Coord.

```java
public float distance(Coord other)
```

**Returns:** The Euclidean distance as a float.

#### `distanceSq`

Calculates squared Euclidean distance (avoids the square root, faster for comparisons).

```java
public float distanceSq(Coord other)
```

**Returns:** The squared Euclidean distance.

#### `manhattanDistance`

Calculates Manhattan (taxicab) distance.

```java
public int manhattanDistance(Coord other)
```

**Returns:** `|this.x - other.x| + |this.y - other.y|`

**Example:**
```java
Coord a = Coord.get(1, 1);
Coord b = Coord.get(4, 5);
int dist = a.manhattanDistance(b); // 7
```

#### `chebyshevDistance`

Calculates Chebyshev (chessboard king) distance.

```java
public int chebyshevDistance(Coord other)
```

**Returns:** `max(|this.x - other.x|, |this.y - other.y|)`

### Direction Methods

#### `toGoTo`

Returns the Direction needed to move from this Coord toward another.

```java
public Direction toGoTo(Coord target)
```

**Returns:** A `Direction` enum value (UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, or NONE).

#### `neighbors`

Returns an array of all adjacent Coords in the cardinal and diagonal directions.

```java
public Coord[] neighbors()
```

**Returns:** An array of 8 adjacent Coords (UP, DOWN, LEFT, RIGHT, and diagonals).

### Utility Methods

#### `isWithin`

Checks if this Coord falls within the given bounds.

```java
public boolean isWithin(int width, int height)
```

**Returns:** `true` if `0 <= x < width` and `0 <= y < height`.

#### `isAdjacentTo`

Checks if this Coord is adjacent (including diagonals) to another.

```java
public boolean isAdjacentTo(Coord other)
```

**Returns:** `true` if the Chebyshev distance is exactly 1.

### Equality and Hashing

Coord uses value-based equality: two Coords are equal if they have the same `x` and `y`.

```java
Coord a = Coord.get(3, 4);
Coord b = Coord.get(3, 4);
a.equals(b); // true
a == b;      // true (if within pool range, same object)
```

### Usage in This Project

The project uses Coord primarily for pathfinding with `DijkstraMap`:

```java
// Converting from project's Vec3i to Coord
Coord goalCoord = Coord.get(goal.x, goal.y);
Coord startCoord = Coord.get(start.x, start.y);

// DijkstraMap returns path as list of Coords
var path = dijkstra.findPath(1, null, null, startCoord, goalCoord);
Coord nextStep = path.first();

// Reading Coord fields for movement
int dx = nextStep.x - pos.x;
int dy = nextStep.y - pos.y;
```

---

## Region

**Full Class Name:** `com.github.yellowstonegames.grid.Region`

### Overview

`Region` represents a set of positions on a 2D grid, stored as a packed bitset for memory efficiency and fast set operations. It is the successor to SquidLib's `GreasedRegion` class. Region supports boolean operations (union, intersection, difference), morphological operations (expand, retract), flood fill, and conversion to/from other representations.

Regions are **mutable** -- operations modify the Region in place and return `this` for method chaining.

### Constructors

#### From dimensions

```java
public Region(int width, int height)
```

Creates an empty Region with the given dimensions.

#### From float array (FOV result)

```java
public Region(float[][] fovMap, float threshold)
```

Creates a Region containing all cells where the FOV value meets or exceeds the threshold.

**Parameters:**
- `fovMap` -- A `float[width][height]` array, typically from `FOV.reuseFOV()`.
- `threshold` -- Minimum value for a cell to be included (e.g., `0.01f` for "any visibility").

**Example:**
```java
float[][] visible = new float[width][height];
FOV.reuseFOV(resistance, visible, px, py, 9.0f, Radius.CIRCLE);

// Create a Region of all visible cells
Region visibleRegion = new Region(visible, 0.01f);
```

#### From char array

```java
public Region(char[][] map, char matching)
```

Creates a Region containing all cells that match the given character.

**Example:**
```java
// Region of all floor tiles
Region floors = new Region(dungeon, '.');
```

### Insertion and Removal

#### `insert`

Adds a single position to this Region.

```java
public Region insert(int x, int y)
```

```java
public Region insert(Coord point)
```

**Returns:** `this` for chaining.

#### `remove`

Removes a single position from this Region.

```java
public Region remove(int x, int y)
```

```java
public Region remove(Coord point)
```

**Returns:** `this` for chaining.

### Query Methods

#### `contains`

Checks if a position is in this Region.

```java
public boolean contains(int x, int y)
```

```java
public boolean contains(Coord point)
```

**Returns:** `true` if the position is part of this Region.

#### `isEmpty`

```java
public boolean isEmpty()
```

**Returns:** `true` if the Region contains no positions.

#### `size`

```java
public int size()
```

**Returns:** The number of positions in this Region.

#### `first`

```java
public Coord first()
```

**Returns:** The first Coord in the Region (lowest x, then lowest y), or `null` if empty.

#### `singleRandom`

```java
public Coord singleRandom(EnhancedRandom rng)
```

**Returns:** A single random Coord from this Region.

### Set Operations

All set operations modify this Region in place and return `this` for chaining.

#### `or` (Union)

```java
public Region or(Region other)
```

Adds all positions from `other` to this Region.

#### `and` (Intersection)

```java
public Region and(Region other)
```

Keeps only positions that are in both this Region and `other`.

#### `andNot` (Difference)

```java
public Region andNot(Region other)
```

Removes all positions that are in `other` from this Region.

#### `xor` (Symmetric Difference)

```java
public Region xor(Region other)
```

Keeps only positions that are in exactly one of the two Regions.

#### `not` (Complement)

```java
public Region not()
```

Inverts all positions: included become excluded and vice versa.

### Morphological Operations

#### `expand`

Grows the Region by one cell in all directions (8-way).

```java
public Region expand()
```

```java
public Region expand(int amount)
```

**Parameters:**
- `amount` -- Number of times to expand (each step grows by 1 cell).

**Returns:** `this` for chaining.

#### `retract`

Shrinks the Region by one cell from all edges.

```java
public Region retract()
```

```java
public Region retract(int amount)
```

**Returns:** `this` for chaining.

#### `fringe`

Returns only the outer edge of the Region (cells that would be added by `expand` but are not currently in the Region).

```java
public Region fringe()
```

**Returns:** `this`, now containing only the fringe cells.

#### `surface`

Returns only the inner edge of the Region (cells that would be removed by `retract`).

```java
public Region surface()
```

**Returns:** `this`, now containing only the surface cells.

### Flood Fill

#### `flood`

Performs flood fill from a starting position, limited to cells within this Region.

```java
public Region flood(Region bounds, int startX, int startY)
```

```java
public Region flood(Region bounds, int volume)
```

**Parameters:**
- `bounds` -- The Region that constrains the flood fill.
- `startX`, `startY` -- Starting position for the fill.
- `volume` -- Maximum number of cells to fill.

**Returns:** `this` for chaining.

### Matching / Filtering

#### `allMatch`

Checks if all positions in this Region satisfy a predicate based on a `float[][]` array.

#### `anyMatch`

Checks if any position in this Region satisfies a predicate based on a `float[][]` array.

### Conversion Methods

#### `asCoords`

Converts this Region to an array of Coord instances.

```java
public Coord[] asCoords()
```

**Returns:** A new `Coord[]` containing all positions in this Region.

#### `asList`

Converts this Region to a list of Coord instances.

```java
public ObjectList<Coord> asList()
```

### Copying

#### `copy`

```java
public Region copy()
```

**Returns:** A new Region that is an independent copy of this one.

### Common Usage Patterns

#### FOV to Region

```java
float[][] visible = new float[width][height];
FOV.reuseFOV(resistance, visible, px, py, 9.0f, Radius.CIRCLE);

// All visible cells as a Region
Region visibleRegion = new Region(visible, 0.01f);

// Get all visible floor tiles
Region visibleFloors = new Region(dungeon, '.').and(visibleRegion);

// Get all visible enemy positions
Coord[] visibleCoords = visibleFloors.asCoords();
```

#### Expanding an Area

```java
// Find all cells within 2 steps of a position
Region area = new Region(width, height);
area.insert(centerX, centerY);
area.expand(2);
```

#### Combining Regions

```java
Region room1 = new Region(dungeon, '.');
Region room2 = new Region(dungeon, ',');
Region allFloors = room1.or(room2); // Union of both
```

## Important Caveats

1. **Coord is immutable, Region is mutable.** Coord operations return new instances; Region operations modify in place.

2. **Pool range.** By default, the Coord pool covers a moderate range. Call `Coord.expandPoolTo()` early if your map is large to ensure pooling benefits.

3. **Region dimensions.** Regions have fixed dimensions set at construction. Positions outside these dimensions cannot be represented.

4. **x-then-y indexing.** Both Coord and Region follow SquidSquad's convention of `array[x][y]` indexing.

5. **Region method chaining.** Because Region methods return `this`, you can chain operations: `region.expand(2).and(bounds).retract()`.

## See Also

- [FOV](squidgrid-fov.md) -- FOV results can be converted to Regions
- [Line of Sight](squidgrid-los.md) -- Line algorithms produce sequences of Coords
- [Lighting](squidgrid-lighting.md) -- Lighting system works with Coords and Regions

## Source

- [GitHub: SquidSquad squidgrid](https://github.com/yellowstonegames/SquidSquad/tree/main/squidgrid)
- [GitHub: SquidSquad](https://github.com/yellowstonegames/SquidSquad)
