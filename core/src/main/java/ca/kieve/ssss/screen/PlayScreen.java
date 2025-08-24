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
    private final HorizontalLayout m_layout;

    public PlayScreen(GameContext gameContext) {
        m_layout = new HorizontalLayout();

        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

        m_layout.setSize(new UiSize(w, h));

        var gameWindow = new GameWindow(gameContext);
        m_layout.add(gameWindow);

        // Another viewport...
        var rightUiWindow = new UiWindow(gameContext);
        m_layout.add(rightUiWindow, new LayoutParams(300));

        var stackLayout = new StackLayout();
        rightUiWindow.add(stackLayout);

        // Testing label?
        var text = new Text("This is a test.");
        stackLayout.add(text);
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
        m_layout.setSize(new UiSize(width, height));
    }
}
