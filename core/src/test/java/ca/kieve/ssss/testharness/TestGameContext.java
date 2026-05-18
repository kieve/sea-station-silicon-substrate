package ca.kieve.ssss.testharness;

import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.map.MapGenerator;
import ca.kieve.ssss.content.map.YamlMapGenerator;
import ca.kieve.ssss.context.GameContext;

/**
 * Constructs a {@link GameContext} for tests.
 *
 * <p>Mirrors the wiring in {@code Main.create()} — content load, context
 * cross-references, engine init, map init systems — but skips render system
 * construction and never touches {@code Gdx.input}. Tests drive the engine via
 * {@link TestEngine}.
 *
 * <p>Map fixture model (two tiers):
 * <ol>
 * <li><b>Default empty map</b> — {@link #createEmpty()} loads
 *     {@code content/maps/test/empty.yaml}: a 9×9 walled room with a single
 *     player spawn at the center. Use this when the test does not care about
 *     world geometry and just needs a valid context with a controllable
 *     player.</li>
 * <li><b>Purpose-built map</b> — {@link #create(String)} loads any map by
 *     path. For geometry-dependent tests, drop a dedicated YAML under
 *     {@code core/src/test/resources/content/maps/test/} and load it
 *     here.</li>
 * </ol>
 *
 * <p>Do not reuse {@code debug/static_test_map.yaml} or production maps in
 * tests — they drift, and tests pinned to them break for unrelated reasons.
 */
public final class TestGameContext {
    private TestGameContext() {
    }

    /**
     * Loads all content and builds a headless {@link GameContext} from the
     * given map YAML (path relative to {@code content/maps/}).
     */
    public static GameContext create(String mapFilename) {
        HeadlessGdxBootstrap.ensureInitialized();

        ContentRegistry content = new ContentLoader().loadAll();
        MapGenerator mapGenerator = new YamlMapGenerator(mapFilename);
        GameContext context = new GameContext(content, mapGenerator, true);

        context.gameEngine().init(context);
        context.examine().init(context);
        context.eject().init(context);
        context.interact().init(context);
        context.pathing().init(context);

        return context;
    }

    /**
     * Default empty test map: 9×9 walled room, player at (4, 4, 1). The
     * preferred starting point for any test that does not depend on specific
     * world geometry.
     */
    public static GameContext createEmpty() {
        return create("test/empty.yaml");
    }
}
