package ca.kieve.ssss.screen;

import com.badlogic.gdx.Gdx;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.core.UiScreen;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.ui.layout.HorizontalLayout;
import ca.kieve.ssss.ui.layout.HorizontalLayout.LayoutParams;
import ca.kieve.ssss.ui.layout.StackLayout;
import ca.kieve.ssss.ui.node.Text;
import ca.kieve.ssss.ui.widget.GameWindow;

public class PlayScreen implements UiScreen {
    private final UiWindow m_mainUiWindow;

    public PlayScreen(GameContext gameContext) {
        m_mainUiWindow = new UiWindow(gameContext);
        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();
        m_mainUiWindow.setSize(new UiSize(w, h));

        var layout = new HorizontalLayout();
        m_mainUiWindow.add(layout);

        // Have to explicitly set the parent so it can reapply the viewport
        // after it renders the GameWindow
        layout.setParentWindow(m_mainUiWindow);

        var gameWindow = new GameWindow(gameContext);
        layout.add(gameWindow);

        var rightLayout = new StackLayout();
        layout.add(rightLayout, new LayoutParams(300));

        // Testing label?
        var text = new Text("This is a test.");
        rightLayout.add(text);
    }

    @Override
    public void update(float delta) {
        m_mainUiWindow.update(null, delta);
    }

    @Override
    public void render(float delta) {
        m_mainUiWindow.render(null, delta);
    }

    @Override
    public void resize(int width, int height) {
        m_mainUiWindow.setSize(new UiSize(width, height));
    }
}
