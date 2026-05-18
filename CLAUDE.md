# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sea Station Silicon Substrate is a roguelike game built with libGDX (Java game framework). It uses an Entity Component System (ECS) architecture via Dominion ECS, and implements a turn-based game loop with a custom clock system.

## Build Commands

**Run the application:**
```bash
./gradlew lwjgl3:run
```

**Build the project:**
```bash
./gradlew build
```

Building automatically runs the formatter and linter. The formatter auto-formats all Java sources
before compilation, and Checkstyle linting runs as part of the `check` lifecycle. To run them
independently:

```bash
./gradlew format        # auto-format all Java source files
./gradlew formatCheck   # verify formatting (fails if files need changes)
./gradlew lint          # lint main source files
./gradlew lintTest      # lint test source files
```

**Run tests:**
```bash
./gradlew test
```

**Run tests for a single class:**
```bash
./gradlew test --tests "ca.kieve.ssss.util.Vec3iTest"
```

**Generate test coverage report:**
```bash
./gradlew jacocoTestReport
```
Coverage reports are generated at `core/build/reports/jacoco/test/html/index.html`

**Clean build artifacts:**
```bash
./gradlew clean
```

**Build runnable JAR:**
```bash
./gradlew lwjgl3:jar
```
Output is at `lwjgl3/build/libs/`

## Architecture

### Game state lives in `GameContext`

All game data (world model, regions, pathing caches, vision, clock, ECS,
etc.) is held by `GameContext` — typically through dedicated mutable
`*Context` classes (`WorldContext`, `MapContext`, `PathingContext`, …).
`GameEngine` is for orchestration only: it wires systems together, drives
the tick loop, and populates contexts at init. Never add data fields to
`GameEngine` if other systems need to read or mutate them — add a context
(or extend an existing one) instead.

The same applies to map generation: `MapGenerator.generate` takes the
contexts it needs to populate (`MapContext`, `WorldContext`) and mutates
them as a side effect. It doesn't return data the caller has to thread
into context manually.

### Passability and blocking conditions

`SolidUtil.blocksMover(entity, moverSize)` is the single source of truth
for "does this entity at this cell block a mover?" — combining `Solid`
(with `Openable.isOpen` respected), `MaxPassableSize`, and any future
conditions. Both within-region pathing grid construction
(`PathingContext.rebuildGrid`) and cross-region portal traversability
(`PathingContext.crossRegionNextStep`) route through it.

When adding a new blocking condition (faction allies, key-bearing movers,
state-gated tiles), extend `blocksMover` — don't sprinkle parallel checks
through systems. If the new condition needs more than `Size` to evaluate
(e.g., the mover's inventory), widen the predicate to take an `Entity`
and adjust the pathing grid cache key accordingly.

### Game World Coordinate System

The game world uses a **Y-up coordinate system** where:
- Positive X → Right (East)
- Negative X → Left (West)
- Positive Y → Up (North)
- Negative Y → Down (South)

**WASD Key Mapping:**
- **W** → Y+ (North/Up on screen)
- **A** → X- (West/Left on screen)
- **S** → Y- (South/Down on screen)
- **D** → X+ (East/Right on screen)

This is consistent across all systems that handle directional input (WasdSystem, EjectSystem, ExamineSystem, etc.). Always use these mappings when implementing new directional features.

## Key Dependencies

- **libGDX** (`$gdxVersion`): Core game framework
- **Dominion ECS** (`$dominionEcsVersion`): Entity Component System
- **SquidSquad** (`$squidSquadVersion`): Roguelike utilities (squidcore, squidsmooth). See [SquidSquad.md](SquidSquad.md) for detailed API documentation and usage examples.
- **Jackson** (`$jacksonVersion`): YAML parsing for data-driven content
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for tests
- **JaCoCo**: Code coverage tool

## Editor (Map Editor)

The `editor` module is a standalone JavaFX application for editing map YAML files. It loads and saves the same map format used by the game's `YamlMapGenerator`.

**Run the editor:**
```bash
./gradlew editor:run
```

### Y-Axis Flip (Screen vs. Data Coordinates)

The game world uses Y-up coordinates (row increases upward), but the editor's screen/canvas uses Y-down (row increases downward). `MapRenderer` bridges this with a fixed-anchor flip:

```
screenRow = -dataRow
dataRow   = -screenRow
```

This mapping is anchored at row 0 and is **independent of the grid's bounding box**. Do not use a formula like `maxRow - row + minRow` — that creates a dependency on the live bounds, which causes a feedback loop when painting at grid edges (expanding bounds shifts the mapping, which shifts where the next paint lands, causing runaway expansion).

The flip affects:
- `MapRenderer.visualRowToDataRow()` — converts screen row to data row
- `MapRenderer.render()` — flips data rows for display
- `MapRenderer.getMapOriginY()` — uses `-maxRow * CELL_SIZE` (the topmost visual row under the flip)

### Java 25
The project uses Java 25. Use `IO.println()` instead of `System.out.println()` for console output.

`IO` is an implicitly declared class in Java 25 (part of `java.io`). It does **not** require an explicit import — do not add `import java.io.IO;`. It is available automatically in all source files.

### Windows-Specific Paths
This is a Windows development environment. Use backslash-escaped paths or forward slashes when working with file paths.

### No Deprecation
Never use `@Deprecated` annotations. This is not a library project. If a method should be deprecated, remove it entirely and update all usages to use the new method.

### Remove Dead Code
Always remove dead code immediately. When refactoring or replacing functionality, delete the old files, methods, and classes that are no longer used. Do not leave unused code in the codebase. This includes:
- Unused classes and interfaces
- Unused methods and fields
- Unused imports
- Commented-out code blocks
