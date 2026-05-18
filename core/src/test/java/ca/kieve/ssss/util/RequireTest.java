package ca.kieve.ssss.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequireTest {
    /** Small record used to exercise {@link Require#componentsNonNull}. */
    record Sample(String first, Integer second) {
        Sample {
            Require.componentsNonNull(first, second);
        }
    }

    /** Record with three components, used to check the third one fires too. */
    record Triple(String a, String b, String c) {
        Triple {
            Require.componentsNonNull(a, b, c);
        }
    }

    /** Record with a trailing nullable component. */
    record PartialSample(String required, Integer alsoRequired, String optional) {
        PartialSample {
            // Only validate the two leading required fields; `optional` may
            // be null without tripping the check.
            Require.componentsNonNull(required, alsoRequired);
        }
    }

    /** Caller passes more values than the record has components. */
    record TooManyValuesRecord(String first, String second) {
        TooManyValuesRecord {
            Require.componentsNonNull(first, second, "extra");
        }
    }

    @Test
    void nonNullReturnsValueWhenNotNull() {
        String value = "hello";

        assertSame(value, Require.nonNull(value, "value"));
    }

    @Test
    void nonNullThrowsNpeOnNull() {
        assertThrows(NullPointerException.class, () -> Require.nonNull(null, "thing"));
    }

    @Test
    void nonNullMessageNamesTheParameter() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> Require.nonNull(null, "id")
        );

        assertTrue(
            npe.getMessage().contains("'id'"),
            "expected message to mention the param name, was: " + npe.getMessage()
        );
    }

    @Test
    void nonNullMessageNamesTheCallerClass() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> Require.nonNull(null, "x")
        );

        assertTrue(
            npe.getMessage().contains("RequireTest"),
            "expected message to mention the caller class, was: " + npe.getMessage()
        );
    }

    @Test
    void nonNullMessageOmitsPackagePrefix() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> Require.nonNull(null, "x")
        );

        assertEquals(
            -1,
            npe.getMessage().indexOf("ca.kieve.ssss"),
            "expected message to use simple class name, was: " + npe.getMessage()
        );
    }

    @Test
    void componentsNonNullSucceedsWhenAllNonNull() {
        Sample sample = new Sample("hi", 1);

        assertEquals("hi", sample.first());
        assertEquals(1, sample.second());
    }

    @Test
    void componentsNonNullFirstNullNamesFirstComponent() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> new Sample(null, 1)
        );

        assertTrue(
            npe.getMessage().contains("'first'"),
            "expected 'first', was: " + npe.getMessage()
        );
        assertTrue(
            npe.getMessage().contains("Sample"),
            "expected record name, was: " + npe.getMessage()
        );
    }

    @Test
    void componentsNonNullSecondNullNamesSecondComponent() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> new Sample("hi", null)
        );

        assertTrue(
            npe.getMessage().contains("'second'"),
            "expected 'second', was: " + npe.getMessage()
        );
    }

    @Test
    void componentsNonNullThirdNullNamesThirdComponent() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> new Triple("a", "b", null)
        );

        assertTrue(npe.getMessage().contains("'c'"), "expected 'c', was: " + npe.getMessage());
    }

    @Test
    void componentsNonNullFromNonRecordRejected() {
        assertThrows(IllegalStateException.class, () -> Require.componentsNonNull("x"));
    }

    @Test
    void componentsNonNullValidatesOnlyLeadingComponents() {
        // The trailing `optional` is null and should not trigger.
        PartialSample sample = new PartialSample("hi", 1, null);

        assertEquals("hi", sample.required());
        assertEquals(1, sample.alsoRequired());
    }

    @Test
    void componentsNonNullStillNamesLeadingNulls() {
        NullPointerException npe = assertThrows(
            NullPointerException.class,
            () -> new PartialSample(null, 1, "x")
        );

        assertTrue(
            npe.getMessage().contains("'required'"),
            "expected 'required', was: " + npe.getMessage()
        );
    }

    @Test
    void componentsNonNullRejectsMoreValuesThanComponents() {
        // Caller bug: passing 3 values into a 2-component record's check
        // is meaningless. Surface it loudly.
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new TooManyValuesRecord("a", "b")
        );

        assertTrue(ex.getMessage().contains("3"), ex.getMessage());
    }
}
