# SquidSquad Library Documentation

SquidSquad is a modular Java library suite designed for procedural generation and roguelike game development. It is the successor to SquidLib, offering a more flexible, loosely-connected module design where you only depend on the modules you need.

**Current Version in Project:** 4.0.0-beta2

**Latest Stable Version:** 4.0.3 (available on Maven Central)

## Architecture Overview

All modules depend on `squidcore`, which in turn depends on:
- **jdkgdxds**: Data structures optimized for Java and GWT
- **digital**: Numeric utilities and digit manipulation
- **juniper**: Random number generation
- **regexodus**: Cross-platform regular expressions

Some modules (squidglyph, squidsmooth, squidpress, squidstore) depend on libGDX, but many modules can be used without libGDX for server-side code or testing.

## Detailed API Documentation

For comprehensive API docs with full method signatures and usage examples, see:

- **[squidgrid — FOV](docs/squidgrid-fov.md)**: Field of View calculation (`FOV.reuseFOV()`, resistance maps, Radius types)
- **[squidgrid — Coord & Region](docs/squidgrid-coord.md)**: Immutable pooled coordinates, region set operations
- **[squidgrid — Line of Sight](docs/squidgrid-los.md)**: BresenhamLine, OrthoLine, LineTools
- **[squidgrid — Lighting](docs/squidgrid-lighting.md)**: Radiance, LightSource, LightingManager
- **[squidgrid — Noise](docs/squidgrid-noise.md)**: Perlin, Simplex, Cellular, Foam noise generation

## Modules Currently Used

This project uses the following SquidSquad modules:
- **squidcore**: Core utilities
- **squidgrid**: 2D grid utilities (FOV, Coord, Region, line drawing, noise)
- **squidpath**: Pathfinding (DijkstraMap)
- **squidplace**: Map generation utilities
- **squidsmooth**: Animation and interpolation

## Module Reference

### squidcore (Foundation)
Required by all other modules. Provides:

- **String Utilities**: String handling and text conversions
- **Dice**: Dice rolling and probability tables (e.g., parsing "3d6+2")
- **Compression**: String and byte array compression utilities
- **Color Descriptions**: Support for Oklab (smooth blending) and RGB color formats
- **UniqueIdentifier**: UUID alternative that works with GWT

**Gradle:**
```gradle
implementation 'com.squidpony:squidcore:4.0.3'
```

### squidgrid (2D Grid Utilities)
Essential for 2D grid-based games. Provides:

#### Coord Class
Immutable 2D point with object pooling for efficiency.

```java
// Create coordinates using factory method (pooled)
Coord pos = Coord.get(x, y);
Coord adjacent = Coord.get(x + 1, y);

// Coordinates are immutable - operations return new instances
Coord moved = pos.translate(1, 0);
```

#### Region Class
Represents regions on a 2D grid for working with collections of positions.

#### Field of View (FOV)
Calculate visible areas from a position.

```java
// FOV calculation returns visibility map
float[][] visible = FOV.reuseFOV(resistance, light, x, y, radius);
```

#### Line of Sight
- **BresenhamLine**: Classic line drawing algorithm
- **OrthoLine**: Orthogonal (no diagonal) line drawing

#### Lighting
- **Radiance**: Light properties (color, intensity)
- **LightSource**: Individual light emitters
- **LightingManager**: Manages multiple light sources

#### Noise Generation
Configurable noise supporting Perlin, Simplex, and other variants.

```java
// Noise generation for terrain
Noise noise = new Noise(seed, frequency, noiseType);
float value = noise.getConfiguredNoise(x, y);
```

**Gradle:**
```gradle
implementation 'com.squidpony:squidgrid:4.0.3'
```

### squidplace (Map Generation)
Dungeon and map generation producing `char[][]` grids.

#### DungeonProcessor
Ensures connectivity and places environment features.

```java
// Generate dungeon as char[][]
// Uses x-then-y indexing (dungeon[x][y])
char[][] dungeon = generator.generate();

// DungeonProcessor adds features
DungeonProcessor processor = new DungeonProcessor(width, height, random);
dungeon = processor.generate(dungeon, environment);
// Can place doors, water, grass, boulders based on environment
```

**Key Features:**
- Maps represented as `char[][]` with x-then-y indexing
- Repeatable generation with seeded RNG
- Environment-aware decoration placement

**Gradle:**
```gradle
implementation 'com.squidpony:squidplace:4.0.3'
```

### squidpath (Pathfinding)
Graph-based pathfinding using modified simple-graphs library.

#### DijkstraMap
Powerful pathfinding beyond simple A*.

```java
DijkstraMap dijkstra = new DijkstraMap(dungeon);

// Basic pathfinding
ObjectList<Coord> path = dijkstra.findPath(maxLength, start, target);

// Advanced features:
// - Pathfind while maintaining minimum distance
// - Flee from target
// - Find closest of multiple targets
// - Reuse calculations for near-instantaneous subsequent paths
```

**Gradle:**
```gradle
implementation 'com.squidpony:squidpath:4.0.3'
```

### squidtext (Text Generation)
Procedural text and name generation.

#### Thesaurus
Generate readable, varied text output.

```java
Thesaurus thesaurus = new Thesaurus(seed);
String varied = thesaurus.process("The @hero@ defeated the @monster@.");
```

#### Language
Generate gibberish that looks like real languages.

```java
Language russian = Language.RUSSIAN_AUTHENTIC;
String fakeName = russian.word(seed, true);
```

#### NameGenerator
Generate names matching input corpus patterns.

**Gradle:**
```gradle
implementation 'com.squidpony:squidtext:4.0.3'
```

### squidsmooth (Animation)
Animation and interpolation for libGDX-based games.

#### Glider Classes
Objects that smoothly transition between values:

- **VectorGlider**: Interpolate x/y positions
- **IntColorGlider**: Smooth color transitions
- **AngleGlider**: Rotation interpolation
- **SequenceGlider**: Chain multiple animations

#### Director
Central manager for all Glider objects.

```java
Director director = new Director();

// Create a position glider
VectorGlider glider = new VectorGlider(startX, startY);
glider.setTarget(endX, endY, duration);
director.add(glider);

// Update in render loop
director.step(deltaTime);

// Check current interpolated position
float currentX = glider.getX();
float currentY = glider.getY();

// Director supports pause, resume, stop, restart
director.pause();
director.resume();
```

**Gradle:**
```gradle
implementation 'com.squidpony:squidsmooth:4.0.3'
```

### squidglyph (Text Display)
Text-based roguelike display using TextraTypist.

- Classic ASCII/Unicode roguelike rendering
- REXPaint .xp file import/export
- Color support with distance-based lighting

**Gradle:**
```gradle
implementation 'com.squidpony:squidglyph:4.0.3'
```

### squidworld (World Generation)
Large-scale world map generation building on squidplace.

**Gradle:**
```gradle
implementation 'com.squidpony:squidworld:4.0.3'
```

### Serialization Modules

#### squidstore (JSON)
JSON serialization using libGDX, GWT-compatible.

```gradle
implementation 'com.squidpony:squidstoretext:4.0.3'
```

#### squidfreeze (Kryo)
Binary serialization using Kryo for faster, smaller output.

```gradle
implementation 'com.squidpony:squidfreeze:4.0.3'
```

#### squidwrath (Apache Fory)
Binary serialization using Apache Fory with automatic type handling.

```gradle
implementation 'com.squidpony:squidwrath:4.0.3'
```

### squidpress (Input Handling)
Wraps libGDX input classes with key rebinding support.

```gradle
implementation 'com.squidpony:squidpress:4.0.3'
```

### squidold (Compatibility)
Migration support from older SquidLib versions.

```gradle
implementation 'com.squidpony:squidold:4.0.3'
```

## Common Patterns

### Coordinate System
SquidSquad uses **x-then-y indexing** for 2D arrays:
```java
char[][] map = new char[width][height];
char tile = map[x][y];  // NOT map[y][x]
```

### Seeded Random Generation
All procedural generation supports reproducible results:
```java
EnhancedRandom random = new WhiskerRandom(seed);
// Same seed = same results across platforms
```

### Immutable Coordinates
Coord instances are pooled and immutable:
```java
// Use factory method, not constructor
Coord c = Coord.get(5, 10);

// Operations return new instances
Coord moved = c.translate(1, 0);  // c unchanged
```

## Platform Support

- **Desktop**: Full support via LWJGL3
- **Android**: Full support
- **iOS**: Via RoboVM
- **Browser**: Via TeaVM or GWT
- **Server-side**: Modules without libGDX dependency

## Resources

- **GitHub Repository**: https://github.com/yellowstonegames/SquidSquad
- **Demo Projects**: https://github.com/yellowstonegames/SquidLib-Demos/tree/master/SquidSquad
- **Maven Central**: Search for `com.squidpony` artifacts
- **Issue Tracker**: https://github.com/yellowstonegames/SquidSquad/issues

## Notes for This Project

The project actively uses:
- **squidgrid**: `FOV.reuseFOV()` for fog of war, `Coord` for pathfinding coordinates
- **squidpath**: `DijkstraMap` for AI pathfinding (see `PathingContext`)

Potential future uses:
1. **squidsmooth**: Add smooth movement animations to entities using VectorGlider
2. **squidcore**: Use Dice for damage calculations, String utilities for text processing
3. **squidplace**: Procedural dungeon generation

## ProGuard Configuration (Desktop/iOS)

If using ProGuard optimization:
```
-optimizations !code/simplification/string
```

This prevents issues with string handling utilities.
