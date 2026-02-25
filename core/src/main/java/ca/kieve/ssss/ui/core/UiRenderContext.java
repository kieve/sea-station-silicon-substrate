package ca.kieve.ssss.ui.core;

import ca.kieve.ssss.context.GameContext;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public record UiRenderContext(
    GameContext gameContext,
    Camera camera,
    SpriteBatch spriteBatch,
    ShapeRenderer shapeRenderer
) {
}
