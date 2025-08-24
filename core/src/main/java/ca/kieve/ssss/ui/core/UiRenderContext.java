package ca.kieve.ssss.ui.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import ca.kieve.ssss.context.GameContext;

public record UiRenderContext(
    GameContext gameContext,
    Camera camera,
    SpriteBatch spriteBatch,
    ShapeRenderer shapeRenderer
) {
}
