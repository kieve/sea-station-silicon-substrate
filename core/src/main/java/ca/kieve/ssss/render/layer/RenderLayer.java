package ca.kieve.ssss.render.layer;

public interface RenderLayer {
    RenderSurface surface();

    boolean shouldRender(RenderFrame frame);

    void draw(RenderFrame frame);
}
