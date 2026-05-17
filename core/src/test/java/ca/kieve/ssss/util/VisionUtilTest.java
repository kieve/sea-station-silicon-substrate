package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Opaque;
import ca.kieve.ssss.component.Openable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionUtilTest {
    private Dominion ecs;

    @BeforeEach
    void setUp() {
        ecs = Dominion.create();
    }

    @Test
    void entityWithoutOpaque_doesNotBlockVision() {
        Entity entity = ecs.createEntity();

        assertFalse(VisionUtil.blocksVision(entity));
    }

    @Test
    void opaqueEntityWithNoOpenable_blocksVision() {
        Entity entity = ecs.createEntity(new Opaque());

        assertTrue(VisionUtil.blocksVision(entity));
    }

    @Test
    void opaqueClosedOpenable_blocksVision() {
        Entity entity = ecs.createEntity(new Opaque(), new Openable("open", "closed"));

        assertTrue(VisionUtil.blocksVision(entity));
    }

    @Test
    void opaqueOpenOpenable_doesNotBlockVision() {
        Openable openable = new Openable("open", "closed");
        openable.isOpen = true;
        Entity entity = ecs.createEntity(new Opaque(), openable);

        assertFalse(VisionUtil.blocksVision(entity));
    }
}
