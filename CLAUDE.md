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

### ECS (Entity Component System)

The project uses Dominion ECS (`dev.dominion.ecs`) for entity management. All components implement the `Component` interface (core/src/main/java/ca/kieve/ssss/component/Component.java).

- **GameContext** (`ca.kieve.ssss.context.GameContext`): Central record holding the ECS instance (`Dominion`), along with specialized contexts (Clock, Position, Log, Player), input multiplexer, Random, and system lists.
- **Components**: Interfaces/classes in `ca.kieve.ssss.component.*` that represent data attached to entities (Position, Velocity, Health, Speed, etc.)
- **Systems**: Classes extending `ca.kieve.ssss.system.System` that contain game logic

#### Dominion ECS API

**Adding components to entities:**
```java
entity.add(new Position(x, y, z));  // Add component instance
```

**Removing components from entities:**
```java
entity.removeType(TileGlyph.class);  // Remove by type (preferred)
entity.remove(componentInstance);     // Remove specific instance
```

**Important:** Use `removeType(Class)` not `remove(Class)` when removing by type. The `remove()` method expects the actual component instance, not the class.

**Querying entities:**
```java
var results = dominion.findEntitiesWith(Position.class, Velocity.class);
results.forEach(with -> {
    var pos = with.comp1();      // First component
    var vel = with.comp2();      // Second component
    var entity = with.entity();  // The entity itself
});
```

**Checking for components:**
```java
entity.has(TileGlyph.class);  // Returns boolean
entity.get(Speed.class);      // Returns component or null
```

### Turn-Based Clock System

The game uses a custom turn-based clock with four tick stages defined in `TickStage`:

1. **AWAIT_INPUT**: Game waits for player input; rendering only occurs in this stage
2. **PRE_TICK**: Entities signal their intent (e.g., "I want to move left")
3. **TICK**: Processing based on preTick events (e.g., collision detection, mining)
4. **POST_TICK**: State changes that tick logic depends on are finalized

The `ClockSystem` (core/src/main/java/ca/kieve/ssss/system/ClockSystem.java) manages this flow:
- Time advances only when entities act
- Speed-based turn scheduling: entities with `Speed` component act every `(100 * 100) / speed` ticks
- Player actions (WasdController) pause time until input is received
- Non-player entities act based on their speed values

### System Lifecycle

Systems extend the abstract `System` class and override lifecycle methods:
- `awaitingUserInput()`: Called when waiting for player input
- `preTick()`: Pre-processing phase
- `tick()`: Main processing phase
- `postTick()`: Post-processing phase

All systems are registered in `GameWindow.createSystems()` (core/src/main/java/ca/kieve/ssss/ui/widget/GameWindow.java) as either update systems or render systems.

### UI System

Custom UI framework built on libGDX with:
- **UiWindow**: Root container with viewport and camera
- **UiNode**: Base class for UI elements with layout support
- **Layout Types**: StackLayout, HorizontalLayout, VerticalLayout (in `ca.kieve.ssss.ui.layout.*`)
- **GameWindow**: The main game viewport (extends UiWindow), renders at TILE_SIZE=32 pixels per tile

The main screen is `PlayScreen` which uses a vertical layout with:
- Top section: HorizontalLayout containing game window and right sidebar
- Bottom section: Log panel (150px fixed height)

**Important:** The UI system uses a **Y-down coordinate system** (y=0 at top, increases downward), which differs from libGDX's default Y-up coordinate system. This is configured in `UiWindow` by flipping the camera's up vector. When implementing layouts, position (0, 0) is the top-left corner.

### Map Generation

Map generation uses `MapModelBuilder` (core/src/main/java/ca/kieve/ssss/REPLACE/MapModelBuilder.java) to create cave-like structures with rooms and corridors.

### Blueprints

Entity creation is handled via blueprint classes in `ca.kieve.ssss.blueprint.*`:
- `PlayerBlueprint`: Creates the player entity
- `ActorBlueprint`: Creates AI-controlled entities
- `TileBlueprints`: Creates wall/floor tiles
- `MaterialBlueprint`: Creates material definitions

## Key Dependencies

- **libGDX** (`$gdxVersion`): Core game framework
- **Dominion ECS** (`$dominionEcsVersion`): Entity Component System
- **SquidSquad** (`$squidSquadVersion`): Roguelike utilities (squidcore, squidsmooth)
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for tests
- **JaCoCo**: Code coverage tool

## Project Structure

- `core/`: Main game logic (platform-agnostic)
  - `src/main/java/ca/kieve/ssss/`: Root package
    - `component/`: ECS components
    - `system/`: ECS systems
    - `context/`: Context objects (GameContext, ClockContext, etc.)
    - `blueprint/`: Entity factory classes
    - `ui/`: Custom UI framework
    - `screen/`: Game screens
    - `util/`: Utility classes
- `lwjgl3/`: Desktop launcher (LWJGL3 backend)
- `assets/`: Game assets (automatically indexed via `generateAssetList` task)

## Development Notes

### Fixed Frame Rate
The game runs at a fixed 60 FPS (`TARGET_FPS = 60f` in MainEngine). Delta time accumulates until a full frame is ready.

### Rendering Only During AWAIT_INPUT
The `MainEngine.render()` method only renders when `TickStage == AWAIT_INPUT`, preventing visual updates during turn processing.

### Java 21
The project uses Java 21 (`sourceCompatibility = 21`).

### Windows-Specific Paths
This is a Windows development environment. Use backslash-escaped paths or forward slashes when working with file paths.

## Coding Style Rules

### Line Length
Maximum line length is 100 characters. Break long lines at logical points.

### Early Exit Pattern
Prefer to invert and early exit `if` statements to avoid unnecessary nesting and simplify reading code.

**Bad:**
```java
public void process() {
    if (condition1) {
        if (condition2) {
            if (condition3) {
                // Do work
            }
        }
    }
}
```

**Good:**
```java
public void process() {
    if (!condition1) {
        return;
    }
    if (!condition2) {
        return;
    }
    if (!condition3) {
        return;
    }
    // Do work
}
```
