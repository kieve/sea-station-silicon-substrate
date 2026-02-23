# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Important:** If you notice any discrepancies between this documentation and the actual code, offer to update CLAUDE.md to reflect the current state of the codebase.

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
- **Player Component**: The `Player` marker component identifies the player entity. Query with `dominion.findEntitiesWith(Player.class)` to find the player.

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

All systems are registered in `GameEngine` (core/src/main/java/ca/kieve/ssss/GameEngine.java):
- Update systems: `createUpdateSystems()` - logic systems (ClockSystem, WasdSystem, VelocitySystem, etc.)
- Render systems: `initializeRenderSystems()` - visual systems (TileGlyphRenderSystem, ExamineCrosshairRenderSystem, etc.)

### Input Modes and Modal Systems

The game uses `InputContext.Mode` to manage mutually exclusive input modes. Each mode corresponds to a system that handles input for that mode:

- **NORMAL**: Default mode for standard gameplay (WasdSystem handles movement)
- **EXAMINE**: Examine mode for inspecting tiles (ExamineSystem)
- **EJECT**: Eject mode for leaving a socketed body (EjectSystem)

**Pattern for Modal Systems:**

Systems that manage input modes must follow this pattern in `awaitingUserInput()`:

1. **Handle toggle key**: Check if the mode's activation key was pressed
2. **Exit if already in mode**: If currently in this mode, return to NORMAL
3. **Guard against wrong mode**: Only enter the mode from NORMAL mode
4. **Process mode-specific input**: After guards, process input only when in the correct mode

```java
@Override
public void awaitingUserInput() {
    // Handle activation key to toggle mode
    if (m_input.consume(InputAction.MY_MODE_KEY)) {
        if (m_input.isMode(Mode.MY_MODE)) {
            // Already in this mode - exit to normal
            m_input.setMode(Mode.NORMAL);
            m_myContext.exit();
            return;
        }

        // Only enter this mode from NORMAL mode
        if (!m_input.isMode(Mode.NORMAL)) {
            return;
        }

        // Enter the mode
        m_input.setMode(Mode.MY_MODE);
        m_myContext.enter(startPos);
        return;
    }

    // Early exit if not in this mode
    if (!m_input.isMode(Mode.MY_MODE)) {
        return;
    }

    // Handle mode-specific input (only reaches here when in MY_MODE)
    // ...
}
```

This pattern ensures:
- Modes can only be entered from NORMAL mode (prevents mode stacking)
- Each mode can always be exited back to NORMAL
- Systems don't interfere with each other's input handling

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

### Map Generation

Map generation is handled by the `world` package (`ca.kieve.ssss.world`):

- **MapGenerator**: Interface defining map generation contract
- **StaticTestMapGenerator**: Implementation that loads maps from YAML files
- **WorldModel**: 3D voxel representation using x-then-y-then-z indexing
- **WorldEntityFactory**: Creates block entities from WorldModel

**YAML Map Format:**
Maps are defined in `core/src/main/resources/content/maps/` using character-based layer definitions. Entity spawning (including the player) is data-driven via the `entities` list:

```yaml
blocks:
  floor:
    type: wood
    layoutChar: '+'
  wall:
    type: stone
    layoutChar: '#'
  air:
    type: air
    layoutChar: '.'

floorGlyph: interpunct

entities:
  - id: player
    components:
      - type: Position
        x: 6
        y: 6
        z: 1

  - id: aiAttacker
    components:
      - type: Position
        x: 9
        y: 6
        z: 1
      - type: ColorComp
        color: SCARLET

  - id: roboMouse
    components:
      - type: Position
        x: 8
        y: 8
        z: 1
      - type: ScurryInit
      - type: ColorComp
        color: GRAY

layers:
  '0': |
    #################
    #+++++++++++++++#
    ...
  '1': |
    #################
    #...............#
    ...
```

Each entity entry has:
- `id`: Matches the entity's `Identifier` key in ContentRegistry
- `components` (optional): Uses `ComponentDefinition` format to override/add components from the base entity definition. Position is just another component — entities without Position are supported.

**Map Init Systems:**
After all map entities are spawned, `MapInitSystem` implementations run for post-spawn processing. For example, `ScurryInitSystem` finds entities with the `ScurryInit` marker component, creates a randomized `ScurryConfig`, and removes the marker. New init systems implement the `MapInitSystem` interface and are registered in `GameEngine.createEntities()`.

**Integration:**
In `GameEngine.init()`:
1. MapGenerator creates WorldModel: `m_worldModel = m_mapGenerator.generate(blockTypes)`
2. Block entities created: `WorldEntityFactory.createEntities(context, worldModel)`
3. Map entities spawned via `factory.createEntityWithOverrides()` for each entry in `m_mapGenerator.getEntities()`
4. Map init systems run for post-spawn processing (e.g., `ScurryInitSystem`)

### AI System

The AI system uses a data-driven behavior tree architecture defined in `behaviors.yaml`.

**Core Components:**
- `AiController` component: Attaches behavior to entities via `behavior` ID
- `AiControllerSystem`: Evaluates conditions and executes state logic each tick
- `BehaviorFactory`: Loads and instantiates behaviors from YAML

**AI States** (in `ca.kieve.ssss.ai.state`):
- `IdleState`: Do nothing
- `WanderState`: Move randomly within a range
- `ChaseState`: Pathfind toward a target entity
- `AttackState`: Attack an adjacent target
- `ScurryState`: Move along walls
- `AnnounceDeathState`: Log a message on death

**Conditions** (in `ca.kieve.ssss.ai.condition`):
- `IsDeadCondition`: Check if entity's HP <= 0
- `HasWeaponCondition`: Check if entity has equipped weapon
- `DistanceToEntityCondition`: Check distance to target (PLAYER, LAST_ATTACKER)
- `WasAttackedCondition`: Check if entity was recently attacked
- `MaxTimesCondition`: Limit state execution count

**Behavior YAML Format:**
```yaml
---
id: player_hunter
states:
  - state: AnnounceDeathState
    priority: 0
    conditions:
      - type: IsDead
      - type: MaxTimes
        maxTimes: 1
    message: "The attacker curses you as it falls."
  - state: AttackState
    priority: 1
    conditions:
      - type: DistanceToEntity
        target: PLAYER
        distance: 1
      - type: HasWeapon
  - state: ChaseState
    priority: 2
    conditions:
      - type: DistanceToEntity
        target: PLAYER
        distance: 20
  - state: WanderState
    priority: 3
    range: 5
  - state: IdleState
    priority: 4
```

States are evaluated by priority (lowest first). The first state whose conditions all pass is executed.

**Adding AI to Entities:**
```yaml
- type: AiController
  behavior: player_hunter
```

### Data-Driven Content System

Entity and content definitions are loaded from YAML files using Jackson, enabling data-driven game content without code changes.

**Content Loading Architecture:**
- `ContentLoader`: Loads YAML files from `core/src/main/resources/content/`
- `ContentRegistry`: Central registry for all loaded definitions
- `EntityFactory`: Creates entities from YAML definitions using reflection
- `ComponentFactory`: Instantiates components via reflection based on YAML specs

**Content Index Files:**
The `file_list.txt` and `dir_list.txt` files in content directories are **auto-generated** by the `generateContentIndex` gradle task (runs during `processResources`). Do not edit them directly. They enable content discovery at runtime since libGDX's internal file system doesn't support directory listing on all platforms. To add new content, just add the YAML file to the appropriate directory and rebuild — the index files will be regenerated automatically.

**YAML Content Files:**
- `entities_base.yaml`: Base entity templates (base_entity, physics, solid, combatant, socketable)
- `entities.yaml`: Entity definitions (player, enemies, etc.) with component lists
- `behaviors.yaml`: AI behavior definitions with state machines and conditions
- `weapons.yaml`: Weapon definitions (name, description, damage)
- `materials.yaml`: Material entity definitions with component composition
- `glyphs.yaml`: Visual representations (fonts, characters, offsets)
- `fonts.yaml`: Font definitions for rendering
- `blocks.yaml`: Block entity definitions with parent inheritance and component composition
- `maps/`: Directory containing static map YAML files (static_test_map.yaml, damaged_sub.yaml)

**Creating Entities from YAML:**
```java
// Access factory from GameContext
EntityFactory factory = context.entityFactory();

// Create entity by ID (position always required)
Entity player = factory.createEntity(context, "player", spawnPos);
Entity dummy = factory.createEntity(context, "trainingDummy", pos, Color.PINK);
Entity block = factory.createEntity(context, "block_stone", pos);

// Create entity with component overrides (used by map YAML loading)
// Position is optional — only registered if included in overrides
Entity entity = factory.createEntityWithOverrides(context, "entityId", overrides);
```

**Adding New Content:**
1. Define entity in `entities.yaml`:
```yaml
---
parents: [base_entity, physics, solid, combatant]
components:
  - type: Identifier
    key: newEnemy
  - type: TileGlyph
    glyphId: M
  - type: Descriptor
    name: Enemy Name
    description: A dangerous foe
  - type: Health
    maxHp: 50
  - type: Speed
    val: 100
  - type: Equipment
    weaponId: power_fist
```

2. Spawn in a map YAML file (preferred for map-specific entities):
```yaml
entities:
  - id: newEnemy
    components:
      - type: Position
        x: 10
        y: 5
        z: 1
      - type: ColorComp
        color: RED
```

3. Or reference in code for programmatic spawning:
```java
factory.createEntity(context, "newEnemy", pos, Color.RED);
```

**Component Reflection:**
- Components are instantiated by class name (e.g., `type: Speed` → `ca.kieve.ssss.component.Speed`)
- Constructor arguments match by type and count
- Marker components (no args) use default constructors
- Enum components are supported via `valueOf()`
- Color properties support hex (`"#RRGGBBAA"`), decimal RGB/RGBA (`"255,0,0"`), and named colors (`BLUE`, `GOLD`, `SCARLET`, etc.) via libGDX `Color` class fields

**Component Composition with Parent Inheritance:**

Entity definitions support a `parents` field (list of strings) that enables component inheritance and composition from multiple parents. Components are merged in order: first parent → second parent → ... → explicit components. Later items override earlier ones when they have the same component type.

**YAML Format:**
- Entities are defined as separate YAML documents using `---` separators
- No root object - each document is a standalone entity definition
- Entity ID is determined by the `Identifier` component's `key` field
- Component properties are specified directly (not nested under `properties:`)

**Base Entity Definitions:**

`entities_base.yaml` defines reusable base entities that can be composed together:
- `base_entity`: RenderingHint (zIndex: 1), Examinable
- `physics`: Velocity, Collider
- `solid`: Solid
- `combatant`: Health (maxHp: 100), Attackable
- `socketable`: Socket (socketedMaxHp, socketedHp for HP pool when possessing), Socketable

Example with multiple parents:
```yaml
---
parents: [base_entity, physics, solid, combatant]
components:
  - type: Identifier
    key: aiAttacker
  - type: TileGlyph
    glyphId: A
  - type: Descriptor
    name: Attacker
    description: "A hostile attacker"
  - type: Health
    maxHp: 20  # Override default 100 from combatant
  - type: Speed
    val: 100
  - type: Equipment
    weaponId: chip_claws
  - type: AiController
    behavior: player_hunter
```

In this example, `aiAttacker` inherits from four base definitions and overrides `Health` with a lower `maxHp`. Component replacement is determined by component type - if multiple parents or the child define a component with the same `type`, the last one wins.

## Key Dependencies

- **libGDX** (`$gdxVersion`): Core game framework
- **Dominion ECS** (`$dominionEcsVersion`): Entity Component System
- **SquidSquad** (`$squidSquadVersion`): Roguelike utilities (squidcore, squidsmooth). See [SquidSquad.md](SquidSquad.md) for detailed API documentation and usage examples.
- **Jackson** (`$jacksonVersion`): YAML parsing for data-driven content
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for tests
- **JaCoCo**: Code coverage tool

## Project Structure

- `core/`: Main game logic (platform-agnostic)
  - `src/main/java/ca/kieve/ssss/`: Root package
    - `ai/`: AI behavior system
      - `behavior/`: Core behavior classes (AiController, BehaviorFactory)
      - `condition/`: Condition evaluators (IsDeadCondition, DistanceToEntityCondition, etc.)
      - `state/`: AI states (IdleState, ChaseState, AttackState, ScurryState, WanderState)
    - `component/`: ECS components
    - `content/`: Data-driven content loading (EntityFactory, ContentRegistry, etc.)
    - `context/`: Context objects (GameContext, ClockContext, etc.)
    - `event/`: Event system (AttackEvent, SocketEvent, ExamineEvent, etc.)
    - `input/`: Input handling (InputAction, InputActionController)
    - `repository/`: Shared repositories (FontRepo)
    - `screen/`: Game screens
    - `system/`: ECS systems
    - `ui/`: Custom UI framework
    - `util/`: Utility classes
    - `world/`: World/map management (MapGenerator, WorldModel, WorldEntityFactory)
  - `src/main/resources/content/`: YAML content definitions
    - `entities_base.yaml`: Base entity templates
    - `entities.yaml`: Entity definitions
    - `behaviors.yaml`: AI behavior definitions
    - `weapons.yaml`: Weapon definitions
    - `materials.yaml`: Material entity definitions
    - `glyphs.yaml`: Glyph definitions
    - `fonts.yaml`: Font definitions
    - `blocks.yaml`: Block entity definitions
    - `maps/`: Static map YAML files
- `editor/`: JavaFX map editor (see Editor section below)
  - `src/main/java/ca/kieve/ssss/editor/`: Root package
    - `component/`: UI panels and rendering (MapViewPanel, MapRenderer, BlockPanel, etc.)
    - `model/`: Data models (EditorMapModel, SparseGrid)
    - `ui/`: Reusable UI widgets (PanCanvas, CompactTreeTable, etc.)
    - `util/`: Utilities (DialogUtil, CssUtil)
- `lwjgl3/`: Desktop launcher (LWJGL3 backend)
- `assets/`: Game assets (automatically indexed via `generateAssetList` task)

## Editor (Map Editor)

The `editor` module is a standalone JavaFX application for editing map YAML files. It loads and saves the same map format used by the game's `StaticTestMapGenerator`.

**Run the editor:**
```bash
./gradlew editor:run
```

### Editor Architecture

- **EditorApp / EditorFxApp**: Entry point; launches the JavaFX application
- **MapViewPanel**: Main editing view — contains the canvas, tool bar, block/entity panels, and overlays
- **MapRenderer**: Renders a `SparseGrid` layer to a JavaFX `Canvas`, including block cells, entity markers, and selection highlights
- **PanCanvas**: Reusable pannable/zoomable canvas viewport with middle-click drag and WASD/arrow key panning
- **EditorMapModel**: In-memory model of the map being edited; wraps layers as `SparseGrid` instances
- **SparseGrid**: Sparse row/col grid that tracks its own bounding box (minRow, maxRow, minCol, maxCol)

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

## Development Notes

### Fixed Frame Rate
The game runs at a fixed 60 FPS (`TARGET_FPS = 60f` in Main). Delta time accumulates until a full frame is ready.

### Render Caching and Dirty Marking
The game uses a FrameBuffer caching system in `GameWindow` to efficiently render only when necessary:
- The game world is rendered to a FrameBuffer (texture) and cached
- The cached frame is always drawn to screen, even during tick processing
- Re-rendering only occurs when in `AWAIT_INPUT` stage AND the render is marked dirty
- Contexts that change visual state (ExamineContext, EjectContext) automatically call `RenderContext.markDirty()` when their state changes
- `ClockSystem.postTick()` marks dirty when transitioning back to `AWAIT_INPUT`

This prevents intermediate game states from being rendered while ensuring the final state is always displayed.

### Java 25
The project uses Java 25. Use `IO.println()` instead of `System.out.println()` for console output.

`IO` is an implicitly declared class in Java 25 (part of `java.io`). It does **not** require an explicit import — do not add `import java.io.IO;`. It is available automatically in all source files.

Note that `System.nanoTime()` and similar calls from `java.lang.System` must be fully qualified as `java.lang.System.nanoTime()` in any file that imports `ca.kieve.ssss.system.System`, since the simple name `System` resolves to the project's class.

### Windows-Specific Paths
This is a Windows development environment. Use backslash-escaped paths or forward slashes when working with file paths.

## Coding Style Rules

### Line Length
Maximum line length is 100 characters. Break long lines at logical points.

### Member Variable Naming Convention
All non-constant, non-static member variables must be prefixed with `m_`. Constants (static final fields) should use SCREAMING_SNAKE_CASE without the prefix.

**In constructors and methods, never use `this.property = property`**. Instead, use the `m_` prefix to distinguish member variables from parameters.

**Bad:**
```java
public class MyClass {
    private String name;
    private int value;

    public MyClass(String name, int value) {
        this.name = name;      // Using 'this' to distinguish
        this.value = value;
    }
}
```

**Good:**
```java
public class MyClass {
    private static final int MAX_VALUE = 100;  // Constants don't use m_ prefix
    private String m_name;
    private int m_value;

    public MyClass(String name, int value) {
        m_name = name;         // No 'this' needed
        m_value = value;
    }
}
```

This pattern:
- Makes member variables immediately distinguishable from local variables and parameters
- Eliminates the need for `this` keyword in most cases
- Improves code readability by making scope explicit

### Pass GameContext, Not Individual Contexts
When a system or component needs access to context objects, pass `GameContext` rather than individual context objects. The receiving class should locally cache references to the specific contexts it needs.

**Bad:**
```java
public class MyInputController extends InputAdapter {
    private final InputContext m_inputContext;

    public MyInputController(InputContext inputContext) {
        m_inputContext = inputContext;  // Passing individual context
    }
}
```

**Good:**
```java
public class MyInputController extends InputAdapter {
    private final InputContext m_inputContext;

    public MyInputController(GameContext gameContext) {
        m_inputContext = gameContext.input();  // Cache locally from GameContext
    }
}
```

This pattern:
- Keeps constructors consistent (always expect GameContext)
- Allows components to access additional contexts later without signature changes
- Makes dependencies on GameContext explicit and centralized

### Collections Are Never Null
Collections should never be null. Always use an empty collection instead of null. This eliminates null checks and makes code cleaner.

**Bad:**
```java
public record EntityDefinition(List<String> parents) {
    public void process() {
        if (parents != null) {  // Unnecessary null check
            for (String parent : parents) {
                // ...
            }
        }
    }
}
```

**Good:**
```java
public record EntityDefinition(List<String> parents) {
    public EntityDefinition {
        parents = parents != null ? parents : List.of();  // Normalize in constructor
    }

    public void process() {
        for (String parent : parents) {  // No null check needed
            // ...
        }
    }
}
```

### Context Initialization with init(GameContext)
Since `GameContext` is a record that creates all context objects in a single constructor call, contexts that need references to other contexts cannot receive them during construction. Instead, use an `init(GameContext)` method that is called after `GameContext` is created.

**Example:**
```java
public class MyContext {
    private RenderContext m_renderContext;
    private PositionContext m_positionContext;

    public void init(GameContext gameContext) {
        m_renderContext = gameContext.render();
        m_positionContext = gameContext.pos();
    }

    public void doSomething() {
        // Can now use m_renderContext.markDirty() etc.
    }
}
```

**In Main.create():**
```java
m_gameContext = new GameContext();
// Initialize contexts that need cross-references
m_gameContext.examine().init(m_gameContext);
m_gameContext.eject().init(m_gameContext);
```

This pattern:
- Provides a single initialization point for each context
- Allows contexts to cache references to other contexts they depend on
- Keeps the initialization logic centralized in `Main.create()`
- Follows the same "pass GameContext" philosophy used elsewhere

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

### No Deprecation
Never use `@Deprecated` annotations. This is not a library project. If a method should be deprecated, remove it entirely and update all usages to use the new method.

### Remove Dead Code
Always remove dead code immediately. When refactoring or replacing functionality, delete the old files, methods, and classes that are no longer used. Do not leave unused code in the codebase. This includes:
- Unused classes and interfaces
- Unused methods and fields
- Unused imports
- Commented-out code blocks

### No Fully Qualified Class Names
Never use fully qualified class names (e.g., `ca.kieve.ssss.context.ClockContext.method()`) unless there is a name collision. Always import the class and use the simple name.

**Bad:**
```java
var result = ca.kieve.ssss.context.ClockContext.getTicksToAct(speed);
```

**Good:**
```java
import ca.kieve.ssss.context.ClockContext;
// ...
var result = ClockContext.getTicksToAct(speed);
```

### Method Ordering
Within a class, methods should be ordered as follows:
1. Fields (static fields first, then instance fields)
2. Constructors (if any exist)
3. Static methods
4. Instance methods

Constructors are optional. If a class uses the implicit default constructor, omit it. Only include explicit constructors when they perform initialization logic.

**Example:**
```java
public class MyClass {
    private static final int CONSTANT = 100;
    private int m_value = 0;

    public static int calculateSomething(int input) {
        return input * CONSTANT;
    }

    public int getValue() {
        return m_value;
    }

    public void setValue(int value) {
        m_value = value;
    }
}
```
