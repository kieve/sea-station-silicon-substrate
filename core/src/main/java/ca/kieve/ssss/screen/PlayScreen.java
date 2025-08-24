package ca.kieve.ssss.screen;

import com.badlogic.gdx.Gdx;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.core.UiScreen;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.ui.layout.HorizontalLayout;
import ca.kieve.ssss.ui.node.UiPanel;
import ca.kieve.ssss.ui.widget.GameWindow;

public class PlayScreen implements UiScreen {
    private final HorizontalLayout m_layout;

    public PlayScreen(GameContext gameContext) {
        m_layout = new HorizontalLayout();

        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

        m_layout.setUiSize(new UiSize(w, h));

        var window = new UiWindow();
        var gameWindow = new GameWindow(gameContext, window);
        window.addChild(gameWindow);

        m_layout.addChild(window);
        m_layout.addChild(new UiPanel());
    }

    @Override
    public void update(float delta) {
        m_layout.update(null, delta);
    }

    @Override
    public void render(float delta) {
        m_layout.render(null, delta);
    }

    @Override
    public void resize(int width, int height) {
        m_layout.setUiSize(new UiSize(width, height));
    }
}
