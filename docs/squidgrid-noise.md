# SquidGrid Noise Generation

**Package:** `com.github.yellowstonegames.grid`
**Module:** `squidgrid`
**Version:** 4.0.0-beta2 (project) / 4.0.3+ (latest)

## Overview

The squidgrid module provides the `Noise` class, a large and highly-configurable noise generator for producing continuous noise values. It supports multiple noise algorithms (Perlin, Simplex, Cellular, Foam, Honey, Value, and more), fractal layering (FBM, Ridged, Turbulent), and 2D through 6D evaluation. Noise is commonly used for terrain generation, texture synthesis, weather patterns, and other procedural content.

Additionally, squidgrid provides several single-purpose noise classes that implement the `INoise` interface for specific use cases.

---

## Noise

**Full Class Name:** `com.github.yellowstonegames.grid.Noise`

### Overview

`Noise` is the primary noise generator class. It wraps multiple noise algorithms behind a unified interface with configurable fractal options. Once configured, you call `getConfiguredNoise()` to sample noise values at any coordinate.

### Constructors

```java
public Noise()
```

Creates a Noise with default settings (seed 1337, Simplex fractal noise).

```java
public Noise(int seed)
```

Creates a Noise with the given seed and default noise type.

```java
public Noise(int seed, float frequency)
```

Creates a Noise with the given seed and frequency.

**Parameters:**
- `seed` -- The random seed. Same seed produces the same noise pattern.
- `frequency` -- Controls the "zoom level" of the noise. Lower values produce smoother, more spread-out noise. Higher values produce more detailed, compressed noise. Default is `0.03125f` (1/32).

```java
public Noise(int seed, float frequency, int noiseType)
```

Creates a Noise with full configuration.

**Parameters:**
- `seed` -- Random seed.
- `frequency` -- Noise frequency/zoom.
- `noiseType` -- One of the noise type constants (see below).

**Example:**
```java
// Simplex fractal noise (default, good general-purpose)
Noise noise = new Noise(12345, 0.03f, Noise.SIMPLEX_FRACTAL);

// Perlin noise, no fractal
Noise perlin = new Noise(12345, 0.05f, Noise.PERLIN);

// Cellular (Worley) noise for cave-like patterns
Noise cellular = new Noise(12345, 0.08f, Noise.CELLULAR);
```

### Noise Type Constants

These constants are passed to `setNoiseType()` or the constructor.

#### Base Noise Types (No Fractal)

| Constant | Description |
|----------|-------------|
| `Noise.VALUE` | Value noise -- smooth, blocky interpolation |
| `Noise.PERLIN` | Perlin noise -- classic gradient noise |
| `Noise.SIMPLEX` | Simplex noise -- faster, fewer artifacts than Perlin |
| `Noise.CELLULAR` | Cellular (Worley/Voronoi) noise -- organic cell patterns |
| `Noise.WHITE_NOISE` | White noise -- pure random, no continuity |
| `Noise.CUBIC` | Cubic noise -- smoother interpolation than Value |
| `Noise.FOAM` | Foam noise -- smooth, organic, good for terrain |
| `Noise.HONEY` | Honey noise -- similar to Foam, slightly different character |
| `Noise.MUTANT` | Mutant noise -- unusual, experimental |

#### Fractal Noise Types

Use these when you want layered noise with octaves:

| Constant | Description |
|----------|-------------|
| `Noise.VALUE_FRACTAL` | Value noise with fractal layering |
| `Noise.PERLIN_FRACTAL` | Perlin noise with fractal layering |
| `Noise.SIMPLEX_FRACTAL` | Simplex noise with fractal layering (default) |
| `Noise.CELLULAR_FRACTAL` | Cellular noise with fractal layering |
| `Noise.CUBIC_FRACTAL` | Cubic noise with fractal layering |
| `Noise.FOAM_FRACTAL` | Foam noise with fractal layering |
| `Noise.HONEY_FRACTAL` | Honey noise with fractal layering |
| `Noise.MUTANT_FRACTAL` | Mutant noise with fractal layering |

### Configuration Methods

#### `setNoiseType`

Sets the noise algorithm.

```java
public void setNoiseType(int noiseType)
```

**Parameters:**
- `noiseType` -- One of the noise type constants.

**Caveat:** Choose a `_FRACTAL` type if you want to use fractal options (octaves, lacunarity, gain). Non-fractal types ignore fractal settings.

#### `setFrequency`

Sets the noise frequency (zoom level).

```java
public void setFrequency(float frequency)
```

**Parameters:**
- `frequency` -- Lower = smoother/larger features, higher = more detailed/smaller features. Typical range: `0.01f` to `0.1f`.

#### `setSeed`

Sets the random seed.

```java
public void setSeed(int seed)
```

#### `setFractalOctaves`

Sets the number of fractal octaves (layers of noise at different scales).

```java
public void setFractalOctaves(int octaves)
```

**Parameters:**
- `octaves` -- Number of layers. More octaves = more detail but slower. Default is `1`. Typical range: `1` to `8`.

**Example:**
```java
noise.setFractalOctaves(4); // 4 layers of detail
```

#### `setFractalLacunarity`

Sets the frequency multiplier between octaves.

```java
public void setFractalLacunarity(float lacunarity)
```

**Parameters:**
- `lacunarity` -- How much the frequency increases per octave. Default is `2.0f`. Higher values add finer detail per octave.

#### `setFractalGain`

Sets the amplitude multiplier between octaves.

```java
public void setFractalGain(float gain)
```

**Parameters:**
- `gain` -- How much the amplitude decreases per octave. Default is `0.5f`. Lower values make higher octaves contribute less.

#### `setFractalType`

Sets the fractal combination method.

```java
public void setFractalType(int fractalType)
```

**Constants:**
- `Noise.FBM` -- Fractional Brownian Motion (default). Smooth, natural-looking.
- `Noise.BILLOW` -- Absolute value of FBM, creating ridge-like bumps.
- `Noise.RIDGED_MULTI` -- Inverted billow, creating sharp ridges (good for mountains).

#### `setCellularDistanceFunction`

For cellular noise, sets how distance is measured between cells.

```java
public void setCellularDistanceFunction(int distanceFunction)
```

**Constants:**
- `Noise.EUCLIDEAN` -- Standard Euclidean distance (round cells).
- `Noise.MANHATTAN` -- Manhattan distance (diamond-shaped cells).
- `Noise.NATURAL` -- A blend that often looks more organic.

#### `setCellularReturnType`

For cellular noise, sets what value is returned.

```java
public void setCellularReturnType(int returnType)
```

**Constants:**
- `Noise.CELL_VALUE` -- Returns a value assigned to the nearest cell.
- `Noise.DISTANCE` -- Returns the distance to the nearest cell center.
- `Noise.DISTANCE_2` -- Returns the distance to the second-nearest cell.
- `Noise.DISTANCE_2_ADD` -- Sum of nearest and second-nearest distances.
- `Noise.DISTANCE_2_SUB` -- Difference of nearest and second-nearest distances.
- `Noise.DISTANCE_2_MUL` -- Product of nearest and second-nearest distances.
- `Noise.DISTANCE_2_DIV` -- Ratio of nearest and second-nearest distances.

### Sampling Methods

#### `getConfiguredNoise` (2D)

Samples the configured noise at a 2D position.

```java
public float getConfiguredNoise(float x, float y)
```

**Parameters:**
- `x`, `y` -- The coordinates to sample. These are in noise space (affected by frequency).

**Returns:** A float value, typically in the range `[-1.0, 1.0]`. The exact range depends on the noise type and fractal settings.

**Example:**
```java
Noise noise = new Noise(42, 0.04f, Noise.SIMPLEX_FRACTAL);
noise.setFractalOctaves(4);

for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float value = noise.getConfiguredNoise(x, y);
        // value is roughly in [-1, 1]
        // Map to [0, 1] if needed:
        float normalized = (value + 1f) * 0.5f;
    }
}
```

#### `getConfiguredNoise` (3D)

```java
public float getConfiguredNoise(float x, float y, float z)
```

Samples 3D noise. The z-axis can be used for animation (varying z over time creates smoothly changing 2D noise).

#### `getConfiguredNoise` (4D, 5D, 6D)

```java
public float getConfiguredNoise(float x, float y, float z, float w)
public float getConfiguredNoise(float x, float y, float z, float w, float u)
public float getConfiguredNoise(float x, float y, float z, float w, float u, float v)
```

Higher-dimensional noise. Useful for seamless tiling (wrap coordinates through higher dimensions) or multi-parameter procedural generation.

### Direct Sampling Methods

These bypass the configured frequency and fractal settings, sampling raw noise at exact coordinates:

```java
public float getNoise(float x, float y)
public float getNoise(float x, float y, float z)
// ... up to 6D
```

### Static Evaluation

For one-off noise samples without creating a Noise object:

```java
public static float noise(float x, float y, int seed)
public static float noise(float x, float y, float z, int seed)
```

## INoise Interface

**Full Interface Name:** `com.github.yellowstonegames.grid.INoise`

### Overview

`INoise` is the interface implemented by all noise generators in squidgrid. It defines the contract for noise evaluation and configuration. The `Noise` class implements `INoise`, as do several single-purpose noise classes.

### Key Methods

```java
float getNoise(float x, float y);
float getNoise(float x, float y, float z);
int getSeed();
void setSeed(int seed);
float getMinValue();  // Minimum possible return value
float getMaxValue();  // Maximum possible return value
```

## Single-Purpose Noise Classes

These classes implement `INoise` and provide specific noise algorithms without the full configurability of the `Noise` class:

| Class | Description |
|-------|-------------|
| `SimplexNoise` | Pure simplex noise implementation |
| `PerlinNoise` | Pure Perlin noise implementation |
| `ValueNoise` | Pure value noise implementation |
| `FoamNoise` | Foam noise -- smooth, organic character |
| `HoneyNoise` | Honey noise -- variant of foam |
| `CyclicNoise` | Noise that tiles/repeats seamlessly |
| `PhantomNoise` | High-quality but expensive noise |

## Common Usage Patterns

### Terrain Height Map

```java
Noise heightNoise = new Noise(42, 0.02f, Noise.SIMPLEX_FRACTAL);
heightNoise.setFractalOctaves(5);
heightNoise.setFractalLacunarity(2.0f);
heightNoise.setFractalGain(0.5f);

float[][] heightMap = new float[width][height];
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float h = heightNoise.getConfiguredNoise(x, y);
        heightMap[x][y] = (h + 1f) * 0.5f; // normalize to [0, 1]
    }
}
```

### Cave Generation (Cellular Noise)

```java
Noise caveNoise = new Noise(42, 0.08f, Noise.CELLULAR);
caveNoise.setCellularDistanceFunction(Noise.NATURAL);
caveNoise.setCellularReturnType(Noise.DISTANCE_2_SUB);

for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float value = caveNoise.getConfiguredNoise(x, y);
        boolean isWall = value < 0.0f;
        dungeon[x][y] = isWall ? '#' : '.';
    }
}
```

### Animated Noise (using 3D)

```java
Noise animNoise = new Noise(42, 0.05f, Noise.FOAM_FRACTAL);
animNoise.setFractalOctaves(3);

// Each frame, advance z slightly for smooth animation
float time = frameCount * 0.01f;
for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float value = animNoise.getConfiguredNoise(x, y, time);
        // Use value for animated effects (water, clouds, fire)
    }
}
```

### Mountain Ridges (Ridged Multi-Fractal)

```java
Noise ridgeNoise = new Noise(42, 0.03f, Noise.SIMPLEX_FRACTAL);
ridgeNoise.setFractalOctaves(4);
ridgeNoise.setFractalType(Noise.RIDGED_MULTI);

for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float ridge = ridgeNoise.getConfiguredNoise(x, y);
        // High values = ridge peaks
    }
}
```

### Combining Multiple Noise Layers

```java
Noise baseNoise = new Noise(42, 0.02f, Noise.SIMPLEX_FRACTAL);
baseNoise.setFractalOctaves(4);

Noise detailNoise = new Noise(123, 0.1f, Noise.PERLIN_FRACTAL);
detailNoise.setFractalOctaves(2);

for (int x = 0; x < width; x++) {
    for (int y = 0; y < height; y++) {
        float base = baseNoise.getConfiguredNoise(x, y);
        float detail = detailNoise.getConfiguredNoise(x, y);
        float combined = base * 0.7f + detail * 0.3f;
    }
}
```

## Noise Type Selection Guide

| Use Case | Recommended Type | Notes |
|----------|-----------------|-------|
| General terrain | `SIMPLEX_FRACTAL` | Good default, fast, few artifacts |
| Mountains | `SIMPLEX_FRACTAL` + `RIDGED_MULTI` | Sharp ridges |
| Caves | `CELLULAR` | Organic cell patterns |
| Smooth terrain | `FOAM_FRACTAL` | Very smooth, organic |
| Clouds | `SIMPLEX_FRACTAL` or `FOAM_FRACTAL` | Low frequency, few octaves |
| Texture detail | `PERLIN_FRACTAL` | Classic look |
| Random scatter | `WHITE_NOISE` | No continuity |

## Important Caveats

1. **Frequency matters.** The most common mistake is using a frequency that is too high or too low. Start with `0.03f` and adjust. The frequency is multiplied with the input coordinates.

2. **Output range.** Most noise types return values roughly in `[-1, 1]`, but fractal types and some cellular configurations may vary. Use `getMinValue()` and `getMaxValue()` to check.

3. **Noise lookup warning.** When using cellular noise with a noise lookup (e.g., `setCellularNoiseLookup()`), ensure the lookup noise type is appropriate. Value, Foam, Honey, Perlin, and Simplex are suggested. White Noise and Cellular are not recommended as lookup types.

4. **Fractal types only work with _FRACTAL noise types.** Setting `setFractalOctaves()` on a non-fractal noise type (e.g., `Noise.SIMPLEX` instead of `Noise.SIMPLEX_FRACTAL`) has no effect.

5. **Seed consistency.** The same seed, frequency, and noise type will always produce the same output for the same coordinates, across all platforms (including GWT/TeaVM).

6. **Performance.** Higher dimensions are slower. 2D is fastest; 6D is about 6x slower. More fractal octaves also multiply cost linearly.

## See Also

- [FOV](squidgrid-fov.md) -- FOV resistance maps can be derived from noise-generated terrain
- [Coord and Region](squidgrid-coord.md) -- Regions can be created from noise thresholds

## Source

- [GitHub: SquidSquad squidgrid](https://github.com/yellowstonegames/SquidSquad/tree/main/squidgrid)
- [GitHub: SquidSquad](https://github.com/yellowstonegames/SquidSquad)
- [FastNoise Javadoc (SquidLib)](https://yellowstonegames.github.io/SquidLib/squidlib-util/apidocs/squidpony/squidmath/FastNoise.html) -- Predecessor class documentation
