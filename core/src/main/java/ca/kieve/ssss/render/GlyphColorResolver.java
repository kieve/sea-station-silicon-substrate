package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import java.util.List;

/**
 * Resolves the render color for an entity's glyph by consulting an ordered
 * list of {@link GlyphColorRule}s. The first rule to return a color wins; if
 * none apply, the supplied base color is used.
 *
 * <p>This keeps state-driven color exceptions out of the render system: each
 * exception is its own rule, and adding one is a new class plus a single entry
 * in the rule list. List order is priority order — put more specific rules
 * first.
 */
public final class GlyphColorResolver {
    private final List<GlyphColorRule> m_rules;

    public GlyphColorResolver(List<GlyphColorRule> rules) {
        m_rules = rules;
    }

    public Color resolve(Entity entity, Color base) {
        for (var rule : m_rules) {
            var override = rule.colorFor(entity);
            if (override.isPresent()) {
                return override.get();
            }
        }
        return base;
    }
}
