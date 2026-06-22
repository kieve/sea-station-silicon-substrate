package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Health;

import java.util.Optional;

/**
 * Tints any entity that has a {@link Health} component maroon once its HP is
 * depleted, signalling death regardless of whether the entity was authored
 * with a {@code ColorComp}.
 */
public final class DeadTint implements GlyphColorRule {
    @Override
    public Optional<Color> colorFor(Entity entity) {
        var health = entity.get(Health.class);
        if (health != null && health.hp <= 0) {
            return Optional.of(Color.MAROON);
        }
        return Optional.empty();
    }
}
