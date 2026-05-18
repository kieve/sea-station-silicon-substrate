package ca.kieve.ssss.util;

import java.lang.reflect.RecordComponent;

/**
 * Argument-check helpers that produce a richer message than
 * {@link java.util.Objects#requireNonNull(Object, String)}.
 *
 * <p>{@link #nonNull(Object, String)} is the general-purpose form; on
 * failure it derives the caller's simple class name via
 * {@link StackWalker} so the exception names both the parameter and the
 * place that rejected it.
 *
 * <p>{@link #componentsNonNull(Object...)} is the record-aware form. From
 * a record's canonical constructor, the parameter names are pulled
 * automatically from {@link Class#getRecordComponents()} — pass only the
 * values in declaration order.
 */
public final class Require {
    private Require() {
    }

    /**
     * Returns {@code value} if non-null; otherwise throws
     * {@link NullPointerException} with a message naming the caller and the
     * offending parameter.
     */
    public static <T> T nonNull(T value, String paramName) {
        if (value == null) {
            throw new NullPointerException(formatNullMessage(callerClass(), paramName));
        }
        return value;
    }

    /**
     * Validates that each of {@code values} is non-null. Must be called from
     * a record's canonical constructor: the values are matched to the
     * record's components in declaration order, so the thrown
     * {@link NullPointerException} names the actual record component that
     * was null without the caller repeating the field name as a string.
     *
     * <p>{@code values} may be shorter than the record's component list —
     * only the <em>leading</em> components are validated. This is the
     * idiom for records whose trailing components are intentionally
     * nullable (e.g. an optional field): list the required leading
     * fields, leave the optional ones out.
     */
    public static void componentsNonNull(Object... values) {
        Class<?> caller = callerClass();
        if (!caller.isRecord()) {
            throw new IllegalStateException(
                "Require.componentsNonNull must be called from a record canonical "
                    + "constructor; caller was " + caller.getName()
            );
        }
        RecordComponent[] components = caller.getRecordComponents();
        if (values.length > components.length) {
            throw new IllegalArgumentException(
                caller.getSimpleName() + ": passed " + values.length
                    + " values but record has only " + components.length + " components"
            );
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                throw new NullPointerException(formatNullMessage(caller, components[i].getName()));
            }
        }
    }

    private static Class<?> callerClass() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(
                frames -> frames
                    .skip(2)
                    .findFirst()
                    .map(StackWalker.StackFrame::getDeclaringClass)
                    .orElse(Require.class)
            );
    }

    private static String formatNullMessage(Class<?> caller, String paramName) {
        return caller.getSimpleName() + ": required '" + paramName + "' was null";
    }
}
