package ca.kieve.ssss.ui.core;

public interface UiScreen {
    void update(float delta);
    void render(float delta);
    void resize(int width, int height);
}
