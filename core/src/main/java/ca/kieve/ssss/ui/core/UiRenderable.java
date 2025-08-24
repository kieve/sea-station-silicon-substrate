package ca.kieve.ssss.ui.core;

public interface UiRenderable {
    void update(UiRenderContext renderContext, float delta);
    void render(UiRenderContext renderContext, float delta);
}
