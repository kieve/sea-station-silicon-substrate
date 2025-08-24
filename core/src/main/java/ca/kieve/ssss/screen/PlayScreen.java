package ca.kieve.ssss.screen;

import com.badlogic.gdx.Gdx;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.core.UiScreen;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.ui.layout.NodeLayout;
import ca.kieve.ssss.ui.widget.GameWindow;

public class PlayScreen implements UiScreen {
    private final NodeLayout m_nodeLayout;

    public PlayScreen(GameContext gameContext) {
        m_nodeLayout = new NodeLayout();

        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

        m_nodeLayout.setUiSize(new UiSize(w, h));

        var window = new UiWindow();
        var gameWindow = new GameWindow(gameContext, window);
        window.addChild(gameWindow);

        m_nodeLayout.addChild(window);
    }

    @Override
    public void update(float delta) {
        m_nodeLayout.update(null, delta);
    }

    @Override
    public void render(float delta) {
        m_nodeLayout.render(null, delta);
    }

    @Override
    public void resize(int width, int height) {
        m_nodeLayout.setUiSize(new UiSize(width, height));
    }
}
