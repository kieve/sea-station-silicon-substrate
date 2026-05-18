package ca.kieve.ssss.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentRefTest {
    @Test
    void relativeRefResolvedAgainstReferringFilesDirectory() {
        String result = ContentRef.resolve("maps/home_base/sub_complex.yaml", "./damaged_sub.yaml");

        assertEquals("maps/home_base/damaged_sub.yaml", result);
    }

    @Test
    void relativeRefWithParentDirSegment() {
        String result = ContentRef.resolve(
            "maps/home_base/sub_complex.yaml",
            "./../shared/corridor.yaml"
        );

        assertEquals("maps/shared/corridor.yaml", result);
    }

    @Test
    void absoluteRefRootedAtContent() {
        String result = ContentRef.resolve(
            "maps/home_base/sub_complex.yaml",
            "/maps/elsewhere/x.yaml"
        );

        assertEquals("maps/elsewhere/x.yaml", result);
    }

    @Test
    void absoluteRefIgnoresReferringFile() {
        String fromA = ContentRef.resolve("a/b/c.yaml", "/x/y.yaml");
        String fromB = ContentRef.resolve("totally/different.yaml", "/x/y.yaml");

        assertEquals(fromA, fromB);
        assertEquals("x/y.yaml", fromA);
    }

    @Test
    void refWithoutLeadingDotOrSlashRejected() {
        assertThrows(IllegalArgumentException.class, () -> ContentRef.resolve("a.yaml", "b.yaml"));
    }

    @Test
    void parentSegmentsThatLandAtRootAreAllowed() {
        // maps/home_base goes up twice to land at the bare content root.
        String result = ContentRef.resolve("maps/home_base/x.yaml", "./../../top.yaml");

        assertEquals("top.yaml", result);
    }

    @Test
    void parentSegmentsThatEscapeRootRejected() {
        // One step further than parentSegmentsThatLandAtRootAreAllowed.
        assertThrows(
            IllegalArgumentException.class,
            () -> ContentRef.resolve("maps/home_base/x.yaml", "./../../../escape.yaml")
        );
    }

    @Test
    void nullRefRejected() {
        assertThrows(IllegalArgumentException.class, () -> ContentRef.resolve("a.yaml", null));
    }
}
