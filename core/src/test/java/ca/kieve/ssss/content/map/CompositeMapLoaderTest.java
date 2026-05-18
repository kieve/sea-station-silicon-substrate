package ca.kieve.ssss.content.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.testharness.HeadlessGdxBootstrap;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeMapLoaderTest {
    private CompositeMapLoader loader;

    @BeforeEach
    void setUp() {
        HeadlessGdxBootstrap.ensureInitialized();
        loader = new CompositeMapLoader();
    }

    @Test
    void offsetModeProducesExpectedPlacements() {
        List<CompositeMapLoader.Placement> placements = loader
            .load("test/composite/two_rooms_offset.yaml");

        // root + 2 children
        assertEquals(3, placements.size());

        // Submap entity ids in the YAML are "a" and "b".
        CompositeMapLoader.Placement leafA = findById(placements, "a");
        CompositeMapLoader.Placement leafB = findById(placements, "b");
        assertEquals(new Vec3i(0, 0, 0), leafA.worldOffset());
        assertEquals(new Vec3i(3, 0, 0), leafB.worldOffset());
        assertEquals(new Vec3i(3, 3, 1), leafA.localBounds());
        assertEquals(new Vec3i(3, 3, 1), leafB.localBounds());
    }

    @Test
    void connectorModeComputesOffsetFromConnectorPair() {
        List<CompositeMapLoader.Placement> placements = loader
            .load("test/composite/two_rooms_connector.yaml");

        CompositeMapLoader.Placement leafB = findById(placements, "b");
        // parent.dock @ (5,1,0) + east @ (1,0,0) - child.west_edge @ (0,1,0) = (6,0,0)
        assertEquals(new Vec3i(6, 0, 0), leafB.worldOffset());
    }

    @Test
    void nestedThreeDeepComposesOffsets() {
        List<CompositeMapLoader.Placement> placements = loader
            .load("test/composite/nested_three_deep.yaml");

        // Submap entity at the leaf level is id "leaf" (set in nested_mid.yaml).
        CompositeMapLoader.Placement leaf = findById(placements, "leaf");
        // root (0,0,0) -> mid (0,0,0) -> leaf_a (1,1,0) = (1,1,0)
        assertEquals(new Vec3i(1, 1, 0), leaf.worldOffset());
    }

    @Test
    void localConnectorWithoutDirectionThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loader.load("test/composite/connector_missing_direction.yaml")
        );

        assertTrue(
            ex.getMessage().contains("direction"),
            "expected 'direction' in error, was: " + ex.getMessage()
        );
    }

    @Test
    void missingConnectorThrowsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loader.load("test/composite/missing_connector.yaml")
        );

        assertTrue(
            ex.getMessage().contains("nonexistent"),
            "expected connector name in error, was: " + ex.getMessage()
        );
    }

    @Test
    void cycleIsDetected() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> loader.load("test/composite/cycle_a.yaml")
        );

        assertTrue(
            ex.getMessage().toLowerCase().contains("cycle"),
            "expected cycle in error, was: " + ex.getMessage()
        );
    }

    @Test
    void rootPlacementHasZeroBoundsForPureComposition() {
        // two_rooms_offset.yaml has no layers of its own
        List<CompositeMapLoader.Placement> placements = loader
            .load("test/composite/two_rooms_offset.yaml");

        CompositeMapLoader.Placement root = placements.get(0);
        assertEquals(new Vec3i(0, 0, 0), root.localBounds());
    }

    private static CompositeMapLoader.Placement findById(
        List<CompositeMapLoader.Placement> placements,
        String id
    ) {
        return placements.stream()
            .filter(p -> p.id().equals(id))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("no placement with id " + id + " in " + placements)
            );
    }
}
