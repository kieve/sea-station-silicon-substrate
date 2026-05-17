package ca.kieve.ssss.testharness;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * One-shot libGDX headless runtime for tests.
 *
 * <p>Initializes {@link HeadlessApplication} a single time per test JVM so
 * {@code Gdx.files}, {@code Gdx.app}, and other non-GL globals are available.
 * Render-system construction is still off-limits — those require a real GL
 * context.
 */
public final class HeadlessGdxBootstrap {
    private static boolean s_initialized = false;

    private HeadlessGdxBootstrap() {
    }

    public static synchronized void ensureInitialized() {
        if (s_initialized) {
            return;
        }
        var config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {
        }, config);
        s_initialized = true;
    }
}
