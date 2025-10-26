package ca.kieve.ssss.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Vec3iTest {
    @Test
    void constructorStoresValues() {
        Vec3i vec = new Vec3i(1, 2, 3);

        assertEquals(1, vec.x);
        assertEquals(2, vec.y);
        assertEquals(3, vec.z);
    }

    @Test
    void copyProducesEqualButIndependentInstance() {
        Vec3i original = new Vec3i(4, 5, 6);
        Vec3i copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);

        copy.x = 7;
        assertNotEquals(original.x, copy.x);
    }

    @Test
    void setCopiesValuesFromArgument() {
        Vec3i target = new Vec3i(0, 0, 0);
        Vec3i source = new Vec3i(8, 9, 10);

        target.set(source);

        assertEquals(8, target.x);
        assertEquals(9, target.y);
        assertEquals(10, target.z);
    }

    @Test
    void addMutAddsComponentsInPlace() {
        Vec3i vec = new Vec3i(1, 2, 3);
        vec.addMut(new Vec3i(4, 5, 6));

        assertEquals(5, vec.x);
        assertEquals(7, vec.y);
        assertEquals(9, vec.z);
    }

    @Test
    void addReturnsNewInstanceLeavingOriginalUntouched() {
        Vec3i vec = new Vec3i(1, 2, 3);
        Vec3i result = vec.add(new Vec3i(4, 5, 6));

        assertEquals(new Vec3i(1, 2, 3), vec);
        assertEquals(new Vec3i(5, 7, 9), result);
        assertNotSame(vec, result);
    }

    @Test
    void productMutMultipliesComponentsInPlace() {
        Vec3i vec = new Vec3i(2, -3, 4);
        vec.productMut(3);

        assertEquals(6, vec.x);
        assertEquals(-9, vec.y);
        assertEquals(12, vec.z);
    }

    @Test
    void productReturnsNewInstanceLeavingOriginalUntouched() {
        Vec3i vec = new Vec3i(2, -3, 4);
        Vec3i result = vec.product(3);

        assertEquals(new Vec3i(2, -3, 4), vec);
        assertEquals(new Vec3i(6, -9, 12), result);
        assertNotSame(vec, result);
    }

    @Test
    void equalsAndHashCodeDependOnComponents() {
        Vec3i a = new Vec3i(1, 2, 3);
        Vec3i b = new Vec3i(1, 2, 3);
        Vec3i c = new Vec3i(3, 2, 1);
        Vec3i mismatchY = new Vec3i(1, 9, 3);
        Vec3i mismatchZ = new Vec3i(1, 2, 7);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, mismatchY);
        assertNotEquals(a, mismatchZ);
    }

    @Test
    void equalsReturnsFalseForNonVec3iAndNull() {
        Vec3i vec = new Vec3i(1, 2, 3);

        assertFalse(vec.equals("not a vec"));
        assertFalse(vec.equals(null));
    }

    @Test
    void toStringIncludesComponentValues() {
        Vec3i vec = new Vec3i(7, 8, 9);

        String text = vec.toString();

        assertTrue(text.contains("x=7"));
        assertTrue(text.contains("y=8"));
        assertTrue(text.contains("z=9"));
    }

    @Test
    void constantsExposeExpectedValues() {
        assertEquals(new Vec3i(0, 0, 0), Vec3i.ZERO);
        assertEquals(new Vec3i(1, 0, 0), Vec3i.X);
        assertEquals(new Vec3i(0, 1, 0), Vec3i.Y);
    }
}
