package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Attackable;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Socket;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Coverage for {@link GlyphColorResolver} and its rules: state-driven glyph
 * color overrides resolved without the render system knowing about any
 * individual condition.
 */
class GlyphColorResolverTest {
    private final Dominion m_ecs = Dominion.create();
    private final GlyphColorResolver m_resolver = new GlyphColorResolver(
        List.of(new DestroyedBodyTint(), new DeadTint())
    );

    @Test
    void deadEntityTintsMaroon() {
        Entity dead = m_ecs.createEntity(new Health(2, 0));
        assertEquals(
            Color.MAROON,
            m_resolver.resolve(dead, Color.WHITE),
            "any entity with depleted Health should tint maroon"
        );
    }

    @Test
    void aliveEntityKeepsBaseColor() {
        Entity alive = m_ecs.createEntity(new Health(2, 2));
        assertEquals(
            Color.WHITE,
            m_resolver.resolve(alive, Color.WHITE),
            "a living entity should keep its base color"
        );
    }

    @Test
    void entityWithoutHealthKeepsBaseColor() {
        Entity decoration = m_ecs.createEntity(new Attackable());
        assertEquals(
            Color.GOLD,
            m_resolver.resolve(decoration, Color.GOLD),
            "an entity with no relevant state should fall through to the base color"
        );
    }

    @Test
    void destroyedBodyTintsDarkGrayEvenWhenHealthAboveZero() {
        Socket socket = new Socket();
        socket.destroyed = true;
        // hp still above zero: socket damage is tracked separately, so the
        // destroyed rule must win over the dead rule.
        Entity wreck = m_ecs.createEntity(socket, new Health(100, 100));
        assertEquals(
            Color.DARK_GRAY,
            m_resolver.resolve(wreck, Color.WHITE),
            "a permanently destroyed body should tint dark gray"
        );
    }
}
