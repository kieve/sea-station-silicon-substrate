# Test Harness

Goal: a lightweight harness for testing game logic — update systems, ECS state, clock progression, AI/pathing/vision outcomes, log contents — without standing up libGDX rendering. Render-correctness testing is explicitly out of scope.

## Why this is cheap

Game logic is already decoupled from rendering. `GameContext` is a plain record. Update systems (`ClockSystem`, `WasdSystem`, `AttackSystem`, `PathingSystem`, `AiControllerSystem`, …) only touch `GameContext`. The libGDX calls that block tests are concentrated in two places:

- `ContentLoader` and `StaticTestMapGenerator` — read YAML via `Gdx.files.internal(...)`.
- `Main`, `GameWindow`, render systems — only constructed during real rendering; the harness skips them.

`Gdx.files.internal` is satisfied by libGDX's `HeadlessApplication`. No GL context needed.

## Pieces to build

1. **Headless bootstrap.** A JUnit `@BeforeAll` (or shared base class) that initializes a `HeadlessApplication` once per test JVM so `Gdx.files` and `Gdx.app.log` work. No render systems, no `SpriteBatch`.

2. **`TestGameContext` factory.** Constructs a `GameContext`, runs the same cross-reference `init()` calls `Main.create()` does (`examine`, `eject`, `interact`, `pathing`, `gameEngine`), but never calls `initializeRenderSystems(...)`. Accepts a map YAML path so different scenarios load different maps via the existing `StaticTestMapGenerator`. Anything that today reads `Gdx.graphics.getWidth/Height` (e.g. `PlayScreen` sizing) is not constructed.

3. **`TestEngine` driver.** Thin helper around an existing `GameContext`:
   - `pressAction(InputAction ...)` — push actions into `InputContext` directly (bypass `InputActionController` / `Gdx.input`).
   - `tickTurn()` — mirror `GameWindow.update()`: run update systems once, then loop until `clock.getTickStage() == AWAIT_INPUT`. One call = one full player+AI turn.
   - `tickFrame()` — single pass through update systems, for tests that need to observe intermediate stages.
   - Accessors for common assertions: player entity, log contents, clock state, entities-at-position.

4. **A handful of representative tests** to exercise the harness API before writing more:
   - `WasdSystem` moves the player one tile per direction; respects collision.
   - `ClockSystem` advances `PLAYER → AI → AWAIT_INPUT` and AI entities act at the right cadence.
   - `AttackSystem` resolves a kill after the expected number of turns; `LogContext` records it.

## Out of scope

- Rendering, fonts, glyph layout, UI node positioning. Those need a `GlyphCanvas`-style abstraction over the render systems, which is a separate, larger investment and is deferred until there's a concrete need.
- Backfilling tests for existing behavior. The harness exists; what gets tested with it is a separate decision made later.

## Risks / notes

- `HeadlessApplication` is a real libGDX runtime — it spawns a thread. Initialize it once across the suite, not per test, to keep tests fast.
- `ContentLoader` reads from `assets/` via `Gdx.files.internal`. Tests will pick up real game content. That's fine for now; if test maps diverge, add a test-only content tree later.
- `GlyphFactory` calls `FreeTypeFontGenerator`, which needs GL. The harness must not invoke it — meaning `TestGameContext` should not exercise any code path that loads fonts. If `ContentLoader` eagerly loads fonts today, that path needs to be skippable in test mode.
