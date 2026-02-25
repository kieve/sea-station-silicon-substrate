package ca.kieve.ssss.screen;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.core.UiScreen;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.ui.layout.HorizontalLayout;
import ca.kieve.ssss.ui.layout.StackLayout;
import ca.kieve.ssss.ui.layout.VerticalLayout;
import ca.kieve.ssss.ui.node.ExaminePanel;
import ca.kieve.ssss.ui.node.InteractPanel;
import ca.kieve.ssss.ui.node.LogPanel;
import ca.kieve.ssss.ui.node.Text;
import ca.kieve.ssss.ui.widget.GameWindow;

import com.badlogic.gdx.Gdx;

public class PlayScreen implements UiScreen {
    private final UiWindow m_mainUiWindow;

    public PlayScreen(GameContext gameContext) {
        m_mainUiWindow = new UiWindow(gameContext);
        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();
        m_mainUiWindow.setSize(new UiSize(w, h));

        // Root vertical layout: top section + bottom log panel
        var rootLayout = new VerticalLayout();
        m_mainUiWindow.add(rootLayout);
        rootLayout.setParentWindow(m_mainUiWindow);

        // Top section: game window + right panel (horizontal layout)
        var topLayout = new HorizontalLayout();
        rootLayout.add(topLayout);

        // Have to explicitly set the parent so it can reapply the viewport
        // after it renders the GameWindow
        topLayout.setParentWindow(m_mainUiWindow);

        // Wrap GameWindow in StackLayout to allow ExaminePanel overlay
        var gameStackLayout = new StackLayout();
        topLayout.add(gameStackLayout);
        gameStackLayout.setParentWindow(m_mainUiWindow);

        var gameWindow = new GameWindow(gameContext);
        gameStackLayout.add(gameWindow);

        // ExaminePanel overlays on top of GameWindow
        var examinePanel = new ExaminePanel();
        gameStackLayout.add(examinePanel);

        // InteractPanel overlays for multi-item interaction selection
        var interactPanel = new InteractPanel();
        gameStackLayout.add(interactPanel);

        var rightLayout = new StackLayout();
        topLayout.add(rightLayout, new HorizontalLayout.LayoutParams(300));

        // Testing label in right panel
        var text = new Text("Right Panel");
        rightLayout.add(text);

        // Bottom panel for log output
        var bottomLayout = new StackLayout();
        rootLayout.add(bottomLayout, new VerticalLayout.LayoutParams(150));

        var logPanel = new LogPanel();
        bottomLayout.add(logPanel);
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
