package ca.kieve.ssss;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.screen.PlayScreen;
import ca.kieve.ssss.ui.core.UiScreen;
import ca.kieve.ssss.util.TickStage;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainEngine extends ApplicationAdapter {
    public static final boolean DEBUG_GRID = false;

    private static final float TARGET_FPS = 60f;
    private static final float TARGET_FRAME_TIME_MS = 1000f / TARGET_FPS;

    private GameContext m_gameContext;
    private float m_frameDeltaAccumulator = 0f;

    private UiScreen m_currentScreen = null;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);

        m_gameContext = new GameContext();
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
        if (m_gameContext.clock().getTickStage() != TickStage.AWAIT_INPUT) {
            return;
        }
        ScreenUtils.clear(Color.BLACK);
        m_currentScreen.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        m_currentScreen.resize(width, height);
    }
}
