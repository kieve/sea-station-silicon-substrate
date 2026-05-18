package ca.kieve.ssss;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.map.MapGenerator;
import ca.kieve.ssss.content.map.YamlMapGenerator;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.screen.PlayScreen;
import ca.kieve.ssss.ui.core.UiScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private static final float TARGET_FPS = 60f;
    private static final float TARGET_FRAME_TIME_MS = 1000f / TARGET_FPS;

    private GameContext m_gameContext;
    private float m_frameDeltaAccumulator = 0f;

    private UiScreen m_currentScreen = null;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);

        ContentLoader contentLoader = new ContentLoader();
        ContentRegistry content = contentLoader.loadAll();

        MapGenerator mapGenerator = new YamlMapGenerator(content.getSystemConfig().launchMap());
        m_gameContext = new GameContext(content, mapGenerator);
        // Initialize contexts that need cross-references
        m_gameContext.gameEngine().init(m_gameContext);
        m_gameContext.examine().init(m_gameContext);
        m_gameContext.eject().init(m_gameContext);
        m_gameContext.interact().init(m_gameContext);
        m_gameContext.pathing().init(m_gameContext);

        Gdx.input.setInputProcessor(m_gameContext.inputMux());
        m_currentScreen = new PlayScreen(m_gameContext);
    }

    @Override
    public void render() {
        float deltaSeconds = Gdx.graphics.getDeltaTime();
        float deltaMs = deltaSeconds * 1000.0f;
        m_frameDeltaAccumulator += deltaMs;
        if (m_frameDeltaAccumulator < TARGET_FRAME_TIME_MS) {
            return;
        }
        m_frameDeltaAccumulator -= TARGET_FRAME_TIME_MS;

        update(TARGET_FRAME_TIME_MS);
        render(TARGET_FRAME_TIME_MS);
    }

    private void update(float delta) {
        m_currentScreen.update(delta);
    }

    private void render(float delta) {
        // Always clear and render - GameWindow handles caching internally
        ScreenUtils.clear(Color.BLACK);
        m_currentScreen.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        m_currentScreen.resize(width, height);
    }
}
