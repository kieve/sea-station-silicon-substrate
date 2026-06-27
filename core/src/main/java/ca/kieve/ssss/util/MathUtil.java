package ca.kieve.ssss.util;

public final class MathUtil {
    private MathUtil() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    public static float clamp(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }

    public static int clampWarn(int value, int min, int max, String name) {
        int clamped = Math.clamp(value, min, max);
        if (clamped != value) {
            IO.println(
                "[MathUtil] WARN: " + name + " " + value
                    + " out of range [" + min + ", " + max + "], clamped to " + clamped
            );
        }
        return clamped;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
