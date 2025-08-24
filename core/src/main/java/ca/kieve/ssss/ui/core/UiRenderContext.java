package ca.kieve.ssss.ui.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public record UiRenderContext(
    Camera camera,
    SpriteBatch spriteBatch,
    ShapeRenderer shapeRenderer
) {
}
