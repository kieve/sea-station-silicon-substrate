# SquidGrid Lighting

**Package:** `com.github.yellowstonegames.grid`
**Module:** `squidgrid`
**Version:** 4.0.0-beta2 (project) / 4.0.3+ (latest)

## Overview

The squidgrid lighting system provides a complete framework for managing dynamic lighting in grid-based games. It consists of three main classes:

- **`Radiance`** -- Describes the visual properties of a light (range, color, flicker, strobe).
- **`LightSource`** -- Combines a position with a `Radiance` to represent a light emitter.
- **`LightingManager`** -- Manages multiple light sources, calculates combined lighting, and produces the final color/brightness map for rendering.

These classes work on top of the FOV system to calculate how light propagates through the environment, accounting for walls and other obstacles.

---

## Radiance

**Full Class Name:** `com.github.yellowstonegames.grid.Radiance`

### Overview

`Radiance` encapsulates the visual characteristics of a light source: how far it reaches, what color it is, and how it changes over time (flicker, strobe effects). Radiance objects are mutable and can be updated each frame to produce animated lighting effects.

### Constructors

```java
public Radiance()
```

Creates a default Radiance with neutral settings.

```java
public Radiance(float range, int color, float flicker, float strobe)
```

**Parameters:**
- `range` -- The maximum distance the light reaches, in cells. Higher values create larger lit areas.
- `color` -- The light color as a packed int (Oklab or RGBA format, depending on configuration). Use SquidSquad's color utilities or `DescriptiveColor` to create color values.
- `flicker` -- The amount of random range variation per frame, producing a flickering effect. `0.0f` = steady light, `0.3f` = moderate flicker, `1.0f` = highly erratic.
- `strobe` -- The rate at which the light pulses on and off. `0.0f` = no strobe, positive values create a pulsing effect.

**Example:**
```java
// Steady white torch light, range 5
Radiance torchLight = new Radiance(5.0f, 0xFFFFFFFF, 0.2f, 0.0f);

// Flickering orange campfire, range 4
Radiance campfire = new Radiance(4.0f, 0xFFA500FF, 0.5f, 0.0f);

// Pulsing blue magical light, range 6
Radiance magicGlow = new Radiance(6.0f, 0x4488FFFF, 0.0f, 0.4f);
```

### Fields

```java
public float range;     // Maximum light distance in cells
public int color;       // Packed int color
public float flicker;   // Random range variation (0.0-1.0)
public float strobe;    // Pulsing rate (0.0 = none)
```

### Key Methods

#### `update`

Advances the Radiance animation state by one step. Call this each frame or turn to animate flickering and strobing.

```java
public float update(long tick)
```

**Parameters:**
- `tick` -- The current game tick or frame counter. Used to drive the strobe animation.

**Returns:** The current effective range after applying flicker and strobe effects.

#### `copy`

Creates an independent copy of this Radiance.

```java
public Radiance copy()
```

**Returns:** A new Radiance with the same settings.

---

## LightSource

**Full Class Name:** `com.github.yellowstonegames.grid.LightSource`

### Overview

`LightSource` pairs a grid position (`Coord`) with a `Radiance` to create a positioned light emitter. LightSource objects are what you add to a `LightingManager` to create the lighting scene.

### Constructors

```java
public LightSource(Coord position, Radiance radiance)
```

**Parameters:**
- `position` -- The grid position of this light source.
- `radiance` -- The visual characteristics of this light.

**Example:**
```java
Coord torchPos = Coord.get(10, 5);
Radiance torchRadiance = new Radiance(5.0f, 0xFFA500FF, 0.3f, 0.0f);
LightSource torch = new LightSource(torchPos, torchRadiance);
```

### Fields

```java
public Coord position;     // Grid position of the light
public Radiance radiance;  // Visual properties
```

### Key Methods

#### `getPosition` / `setPosition`

```java
public Coord getPosition()
public void setPosition(Coord position)
```

#### `getRadiance` / `setRadiance`

```java
public Radiance getRadiance()
public void setRadiance(Radiance radiance)
```

---

## LightingManager

**Full Class Name:** `com.github.yellowstonegames.grid.LightingManager`

### Overview

`LightingManager` is the central class for managing multiple light sources in a scene. It handles:

1. Adding and removing light sources
2. Calculating FOV for each light source
3. Combining all light FOVs into a single lighting map
4. Mixing light colors based on overlap
5. Optionally mixing with the viewer's own FOV for "visible light" rendering

LightingManager uses `FOV.reuseFOV()` internally, so it requires a resistance map (same format as FOV).

### Constructors

```java
public LightingManager(
    float[][] resistanceMap,
    int backgroundColor,
    Radius radiusStrategy,
    float viewerRange)
```

**Parameters:**
- `resistanceMap` -- A `float[width][height]` resistance grid (0.0 = transparent, 1.0 = opaque). Shared with FOV.
- `backgroundColor` -- The ambient/background color in packed int format. This is the color of cells with no light.
- `radiusStrategy` -- The `Radius` type for FOV calculations (typically `Radius.CIRCLE`).
- `viewerRange` -- The maximum range for the viewer's own FOV. Set to `0` if you do not want viewer FOV mixed in.

**Example:**
```java
float[][] resistance = FOV.generateResistances(dungeon);

// Dark background, circular light falloff, viewer sees 9 cells
LightingManager lighting = new LightingManager(
    resistance,
    0x000000FF,   // black background
    Radius.CIRCLE,
    9.0f);
```

### Light Source Management

#### `addLight` (with Coord and Radiance)

Adds a light source at a specific position.

```java
public void addLight(Coord position, Radiance radiance)
```

**Parameters:**
- `position` -- Grid position of the light.
- `radiance` -- Visual properties of the light.

**Example:**
```java
lighting.addLight(Coord.get(10, 5), new Radiance(5.0f, 0xFFA500FF, 0.3f, 0.0f));
```

#### `addLight` (with LightSource)

Adds a pre-built LightSource.

```java
public void addLight(LightSource light)
```

#### `removeLight`

Removes a light source at a specific position.

```java
public void removeLight(Coord position)
```

#### `moveLight`

Moves an existing light source to a new position.

```java
public void moveLight(Coord oldPosition, Coord newPosition)
```

**Parameters:**
- `oldPosition` -- The current position of the light to move.
- `newPosition` -- The new position for the light.

#### `hasLight`

Checks if a light source exists at a position.

```java
public boolean hasLight(Coord position)
```

### Calculation Methods

#### `calculateFOV` (no viewer)

Calculates the combined lighting from all light sources without considering a viewer.

```java
public void calculateFOV()
```

After calling this, the lighting manager's internal color map is updated with the combined light colors.

#### `calculateFOV` (with viewer position)

Calculates lighting and mixes it with the viewer's FOV, so only light the viewer can actually see is included.

```java
public void calculateFOV(Coord viewerPosition)
```

**Parameters:**
- `viewerPosition` -- The position of the viewer. A separate FOV is calculated from this position using the `viewerRange` specified in the constructor.

**Example:**
```java
// Add lights
lighting.addLight(Coord.get(10, 5), new Radiance(5.0f, 0xFFA500FF, 0.3f, 0.0f));
lighting.addLight(Coord.get(20, 15), new Radiance(3.0f, 0x4488FFFF, 0.0f, 0.2f));

// Calculate with viewer at player position
lighting.calculateFOV(Coord.get(playerX, playerY));
```

#### `updateAll`

Updates all light source animations (flicker, strobe) and recalculates FOV. Call this each frame or turn.

```java
public void updateAll()
```

#### `update`

Updates animations and recalculates with a viewer position.

```java
public void update(Coord viewerPosition)
```

### Accessing Results

#### `getColorMap`

Gets the combined color map after FOV calculation.

```java
public int[][] getColorMap()
```

**Returns:** A `int[width][height]` array where each cell contains the packed color of the combined lighting at that position. Cells with no light have the background color.

#### `getLightMap`

Gets the combined light intensity map.

```java
public float[][] getLightMap()
```

**Returns:** A `float[width][height]` array with light intensity values from `0.0f` (no light) to `1.0f` (full light).

#### `getFovResult`

Gets the FOV result for the viewer (if `calculateFOV(Coord)` was used).

```java
public float[][] getFovResult()
```

**Returns:** A `float[width][height]` FOV map for the viewer.

### Resistance Map

#### `setResistanceMap`

Replaces the resistance map (e.g., after the dungeon layout changes).

```java
public void setResistanceMap(float[][] resistanceMap)
```

### Common Usage Patterns

#### Basic Lighting Setup

```java
// Initialize
float[][] resistance = FOV.generateResistances(dungeon);
LightingManager lighting = new LightingManager(
    resistance, 0x000000FF, Radius.CIRCLE, 9.0f);

// Add static lights (torches on walls)
lighting.addLight(Coord.get(5, 3), new Radiance(4.0f, 0xFFA500FF, 0.3f, 0.0f));
lighting.addLight(Coord.get(15, 8), new Radiance(4.0f, 0xFFA500FF, 0.3f, 0.0f));

// Game loop
void onTurn() {
    // Recalculate with player position
    lighting.update(Coord.get(playerX, playerY));

    // Render using the color map
    int[][] colors = lighting.getColorMap();
    float[][] brightness = lighting.getLightMap();

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            if (brightness[x][y] > 0.0f) {
                renderTileWithColor(x, y, colors[x][y], brightness[x][y]);
            }
        }
    }
}
```

#### Moving Light Sources

```java
// Player carries a torch
Coord oldPlayerPos = Coord.get(playerX, playerY);
// ... player moves ...
Coord newPlayerPos = Coord.get(newPlayerX, newPlayerY);
lighting.moveLight(oldPlayerPos, newPlayerPos);
```

#### Dynamic Lights

```java
// Add a temporary explosion flash
Coord explosionPos = Coord.get(ex, ey);
lighting.addLight(explosionPos, new Radiance(8.0f, 0xFF4400FF, 0.0f, 0.0f));

// ... after a few turns ...
lighting.removeLight(explosionPos);
```

## VisionFramework

**Full Class Name:** `com.github.yellowstonegames.grid.VisionFramework`

### Overview

`VisionFramework` is a higher-level class that wraps `FOV`, `LightingManager`, and related functionality into a single cohesive system. It handles the common pattern of combining player FOV with environmental lighting, managing seen/explored tiles, and producing the final rendering data.

If you find yourself manually wiring together FOV, LightingManager, and explored-tile tracking, consider using `VisionFramework` instead, which handles all of this boilerplate.

### Key Responsibilities

- Manages the player's FOV calculation
- Manages a `LightingManager` for environmental lights
- Tracks which tiles have been explored (fog of war)
- Produces combined visibility and color data for rendering
- Handles the common update loop (recalculate FOV, update lights, combine results)

### Basic Usage

```java
VisionFramework vision = new VisionFramework();
vision.restart(resistance, Coord.get(playerX, playerY), viewRange);

// Add lights
vision.lighting.addLight(torchPos, torchRadiance);

// Each turn
vision.update(Coord.get(playerX, playerY));

// Access results through the VisionFramework's fields
```

## Important Caveats

1. **Color format.** Light colors are packed integers. The exact format (RGBA, Oklab) depends on configuration. Use SquidSquad's `DescriptiveColor` class for color creation.

2. **Resistance map sharing.** The `LightingManager` references the resistance map, not copies it. If you modify the resistance map externally, you do not need to call `setResistanceMap` again (but you do need to recalculate FOV).

3. **Performance.** Each light source requires a separate FOV calculation. Many lights (50+) may impact performance. Consider limiting the number of lights or only calculating lights near the viewer.

4. **Flicker seed.** Flicker is pseudo-random but deterministic for a given tick value, so flickering lights produce consistent visuals across frames.

5. **Array dimensions.** All arrays (resistance, color, light) share the same `[width][height]` dimensions with x-then-y indexing.

## See Also

- [FOV](squidgrid-fov.md) -- The underlying FOV calculations used by the lighting system
- [Line of Sight](squidgrid-los.md) -- For checking direct visibility between points
- [Coord and Region](squidgrid-coord.md) -- Coord is used for light positions

## Source

- [GitHub: SquidSquad squidgrid](https://github.com/yellowstonegames/SquidSquad/tree/main/squidgrid)
- [GitHub: SquidSquad](https://github.com/yellowstonegames/SquidSquad)
