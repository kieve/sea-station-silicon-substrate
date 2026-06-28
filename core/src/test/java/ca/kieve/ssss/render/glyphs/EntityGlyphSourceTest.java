package ca.kieve.ssss.render.glyphs;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Dominion;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.render.DeadTint;
import ca.kieve.ssss.render.GlyphColorResolver;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityGlyphSourceTest {
    private static final int CAMERA_Z = 1;

    private final Dominion m_ecs = Dominion.create();
    private final TileGlyph m_floor = new TileGlyph(null, '.', 0f, 0f);
    private final GlyphColorResolver m_resolver = new GlyphColorResolver(List.of(new DeadTint()));
    private final EntityGlyphSource m_source = new EntityGlyphSource(m_ecs, m_floor, m_resolver);

    private Map<Vec3i, CellGlyph> collect() {
        var composer = new CellGlyphComposer();
        m_source.collect(CAMERA_Z, composer);
        Map<Vec3i, CellGlyph> map = new HashMap<>();
        for (var cell : composer.cells()) {
            map.put(cell.cell(), cell);
        }
        return map;
    }

    @Test
    void entityOnCameraPlaneUsesItsGlyph() {
        var glyph = new TileGlyph(null, '@', 0f, 0f);
        m_ecs.createEntity(new Position(2, 3, CAMERA_Z), glyph);

        var cell = collect().get(new Vec3i(2, 3, 0));
        assertSame(
            glyph,
            cell.glyph(),
            "an entity on the camera plane should render its own glyph"
        );
        assertEquals(0, cell.priority(), "relativeZ 0 with default zIndex is priority 0");
    }

    @Test
    void entityOneLevelBelowRendersFloorGlyph() {
        var glyph = new TileGlyph(null, '@', 0f, 0f);
        m_ecs.createEntity(new Position(2, 3, CAMERA_Z - 1), glyph);

        var cell = collect().get(new Vec3i(2, 3, 0));
        assertSame(m_floor, cell.glyph(), "an entity one level below is shown as the floor glyph");
        assertEquals(-100, cell.priority(), "relativeZ -1 yields priority -100");
    }

    @Test
    void hiddenEntityIsSkipped() {
        m_ecs.createEntity(
            new Position(2, 3, CAMERA_Z),
            new TileGlyph(null, '@', 0f, 0f),
            new Hidden()
        );
        assertTrue(collect().isEmpty(), "a Hidden entity should not be collected");
    }

    @Test
    void entityAboveCameraIsSkipped() {
        m_ecs.createEntity(new Position(2, 3, CAMERA_Z + 1), new TileGlyph(null, '@', 0f, 0f));
        assertTrue(collect().isEmpty(), "entities above the camera plane are not collected");
    }

    @Test
    void entityTwoLevelsBelowIsSkipped() {
        m_ecs.createEntity(new Position(2, 3, CAMERA_Z - 2), new TileGlyph(null, '@', 0f, 0f));
        assertTrue(collect().isEmpty(), "entities more than one level below are not collected");
    }

    @Test
    void zIndexMinusOneIsSkipped() {
        m_ecs.createEntity(
            new Position(2, 3, CAMERA_Z),
            new TileGlyph(null, '@', 0f, 0f),
            new RenderingHint(-1)
        );
        assertTrue(collect().isEmpty(), "zIndex -1 means never draw");
    }

    @Test
    void priorityIncludesZIndex() {
        m_ecs.createEntity(
            new Position(2, 3, CAMERA_Z),
            new TileGlyph(null, '@', 0f, 0f),
            new RenderingHint(2)
        );
        var cell = collect().get(new Vec3i(2, 3, 0));
        assertEquals(2, cell.priority(), "priority is relativeZ * 100 + zIndex");
    }

    @Test
    void colorResolverOverridesBaseColor() {
        m_ecs.createEntity(
            new Position(2, 3, CAMERA_Z),
            new TileGlyph(null, '@', 0f, 0f),
            new Health(2, 0)
        );
        var cell = collect().get(new Vec3i(2, 3, 0));
        assertEquals(Color.MAROON, cell.color(), "a dead entity should resolve to the dead tint");
    }

    @Test
    void colorCompProvidesBaseColor() {
        m_ecs.createEntity(
            new Position(2, 3, CAMERA_Z),
            new TileGlyph(null, '@', 0f, 0f),
            new ColorComp(Color.GOLD)
        );
        var cell = collect().get(new Vec3i(2, 3, 0));
        assertEquals(Color.GOLD, cell.color(), "with no override the ColorComp color is used");
    }

    @Test
    void defaultsToWhiteWithoutColorComp() {
        m_ecs.createEntity(new Position(2, 3, CAMERA_Z), new TileGlyph(null, '@', 0f, 0f));
        var cell = collect().get(new Vec3i(2, 3, 0));
        assertEquals(Color.WHITE, cell.color(), "entities without a ColorComp default to white");
    }
}
