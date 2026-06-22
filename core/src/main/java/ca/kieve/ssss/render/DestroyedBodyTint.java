package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Socket;

import java.util.Optional;

/**
 * Tints a socketed body dark gray once it has been permanently destroyed
 * (cannot be re-entered). Checked before {@link DeadTint} because a destroyed
 * body takes damage to its socket HP pool, so its own {@link
 * ca.kieve.ssss.component.Health} may still read above zero.
 */
public final class DestroyedBodyTint implements GlyphColorRule {
    @Override
    public Optional<Color> colorFor(Entity entity) {
        var socket = entity.get(Socket.class);
        if (socket != null && socket.destroyed) {
            return Optional.of(Color.DARK_GRAY);
        }
        return Optional.empty();
    }
}
