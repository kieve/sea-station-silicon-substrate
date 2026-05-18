package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.MaxPassableSize;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.Size;
import ca.kieve.ssss.component.Solid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for {@link SolidUtil#blocksMover} — the central
 * per-entity blocking predicate.
 */
class SolidUtilTest {
    private Dominion ecs;

    @BeforeEach
    void setUp() {
        ecs = Dominion.create();
    }

    @Test
    void plainSolidBlocksAnyone() {
        Entity wall = ecs.createEntity(new Solid());

        assertTrue(SolidUtil.blocksMover(wall, null));
        assertTrue(SolidUtil.blocksMover(wall, Size.TINY));
        assertTrue(SolidUtil.blocksMover(wall, Size.GIGANTIC));
    }

    @Test
    void openOpenableDoesNotBlock() {
        Openable openable = new Openable("open", "closed");
        openable.isOpen = true;
        Entity door = ecs.createEntity(new Solid(), openable);

        assertFalse(SolidUtil.blocksMover(door, null));
        assertFalse(SolidUtil.blocksMover(door, Size.MEDIUM));
    }

    @Test
    void closedOpenableStillBlocks() {
        Entity door = ecs.createEntity(new Solid(), new Openable("open", "closed"));

        assertTrue(SolidUtil.blocksMover(door, Size.MEDIUM));
    }

    @Test
    void maxPassableSizeBlocksOversizedMovers() {
        Entity mouseHole = ecs.createEntity(new MaxPassableSize(Size.TINY));

        // SMALL (1) > TINY (0) → blocked
        assertTrue(SolidUtil.blocksMover(mouseHole, Size.SMALL));
        assertTrue(SolidUtil.blocksMover(mouseHole, Size.MEDIUM));
    }

    @Test
    void maxPassableSizeAllowsFittingMovers() {
        Entity mouseHole = ecs.createEntity(new MaxPassableSize(Size.TINY));

        assertFalse(SolidUtil.blocksMover(mouseHole, Size.TINY));
    }

    @Test
    void maxPassableSizeIgnoredWhenMoverSizeIsNull() {
        // Grid built without a mover (the unsized variant) can't reason
        // about size restrictions — fall back to "not blocking."
        Entity mouseHole = ecs.createEntity(new MaxPassableSize(Size.TINY));

        assertFalse(SolidUtil.blocksMover(mouseHole, null));
    }

    @Test
    void plainEntityWithoutSolidOrSizeRestrictionDoesNotBlock() {
        // E.g., a dropped item or a player corpse with no Solid.
        Entity loose = ecs.createEntity();

        assertFalse(SolidUtil.blocksMover(loose, Size.MEDIUM));
        assertFalse(SolidUtil.blocksMover(loose, null));
    }
}
