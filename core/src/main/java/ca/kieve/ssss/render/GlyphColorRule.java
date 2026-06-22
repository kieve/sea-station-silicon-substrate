package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import java.util.Optional;

/**
 * A single state-driven exception to an entity's base glyph color.
 *
 * <p>Rules are consulted in order by {@link GlyphColorResolver}; the first one
 * that returns a color wins. Returning {@link Optional#empty()} means "this
 * rule does not apply to this entity," so the next rule (or the base color) is
 * used. Each rule isolates one visual condition (dead, destroyed, ...) so the
 * render system never has to grow a chain of {@code if}s.
 */
public interface GlyphColorRule {
    Optional<Color> colorFor(Entity entity);
}
