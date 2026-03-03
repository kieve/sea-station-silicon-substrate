package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.annotations.EditorDecompose;
import ca.kieve.ssss.annotations.EditorIgnore;
import ca.kieve.ssss.annotations.EditorRef;
import ca.kieve.ssss.content.ComponentTypeDeserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers editable properties for component classes. Uses reflection to find
 * fields, respecting {@link EditorIgnore} and {@link EditorRef} annotations.
 */
public final class ComponentIntrospector {
    public record FieldInfo(String name, Class<?> type, EditorRef.Source refSource) {
        public FieldInfo(String name, Class<?> type) {
            this(name, type, null);
        }

        public boolean isRef() {
            return refSource != null;
        }
    }

    /**
     * Discovers editable fields for a component by its simple type name
     * (e.g. "Health", "Position").
     *
     * @return editable fields, or empty list if the type cannot be resolved or is a marker / enum
     *         component
     */
    public static List<FieldInfo> getEditableFields(String typeName) {
        Class<?> clazz = ComponentTypeDeserializer.resolveType(typeName);
        if (clazz == null) {
            return List.of();
        }
        return getEditableFields(clazz);
    }

    /**
     * Discovers editable fields for a component class.
     * <ul>
     *   <li>Records: uses record components</li>
     *   <li>Enums: returns empty (use {@link #getEnumConstants})</li>
     *   <li>Classes: uses declared instance fields</li>
     * </ul>
     * Excludes static, synthetic, and {@link EditorIgnore} fields. Fields with
     * {@link EditorRef} use the declared YAML key as the property name and
     * {@code String.class} as the type.
     */
    public static List<FieldInfo> getEditableFields(Class<?> componentClass) {
        var result = new ArrayList<FieldInfo>();

        if (componentClass.isRecord()) {
            discoverRecordFields(componentClass, result);
        } else if (!componentClass.isEnum()) {
            discoverClassFields(componentClass, result);
        }

        return result;
    }

    /**
     * Returns the enum constant names for an enum component class.
     *
     * @return constant names, or empty list if the class is not an enum
     */
    public static List<String> getEnumConstants(Class<?> componentClass) {
        if (!componentClass.isEnum()) {
            return List.of();
        }
        var constants = componentClass.getEnumConstants();
        var result = new ArrayList<String>(constants.length);
        for (Object c : constants) {
            result.add(c.toString());
        }
        return result;
    }

    /**
     * Returns the enum constant names for an enum component by its simple
     * type name.
     *
     * @return constant names, or empty list if the type cannot be resolved or is not an enum
     */
    public static List<String> getEnumConstants(String typeName) {
        Class<?> clazz = ComponentTypeDeserializer.resolveType(typeName);
        if (clazz == null) {
            return List.of();
        }
        return getEnumConstants(clazz);
    }

    /**
     * Returns true if the component class is a marker component
     * (no editable fields and not an enum).
     */
    public static boolean isMarker(Class<?> componentClass) {
        if (componentClass.isEnum()) {
            return false;
        }
        return getEditableFields(componentClass).isEmpty();
    }

    private static void discoverRecordFields(Class<?> componentClass, List<FieldInfo> result) {
        for (RecordComponent rc : componentClass.getRecordComponents()) {
            if (rc.isAnnotationPresent(EditorIgnore.class)) {
                continue;
            }
            if (shouldDecompose(rc)) {
                decomposeFields(rc.getType(), result);
                continue;
            }
            EditorRef ref = rc.getAnnotation(EditorRef.class);
            if (ref != null) {
                result.add(new FieldInfo(ref.value(), String.class, ref.source()));
            } else {
                result.add(new FieldInfo(rc.getName(), rc.getType()));
            }
        }
    }

    private static void discoverClassFields(Class<?> componentClass, List<FieldInfo> result) {
        for (Field field : componentClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.isSynthetic()) {
                continue;
            }
            if (field.isAnnotationPresent(EditorIgnore.class)) {
                continue;
            }
            if (shouldDecompose(field)) {
                decomposeFields(field.getType(), result);
                continue;
            }
            EditorRef ref = field.getAnnotation(EditorRef.class);
            if (ref != null) {
                result.add(new FieldInfo(ref.value(), String.class, ref.source()));
            } else {
                result.add(new FieldInfo(field.getName(), field.getType()));
            }
        }
    }

    private static boolean shouldDecompose(RecordComponent rc) {
        return rc.isAnnotationPresent(EditorDecompose.class)
            || rc.getType().isAnnotationPresent(EditorDecompose.class);
    }

    private static boolean shouldDecompose(Field field) {
        return field.isAnnotationPresent(EditorDecompose.class)
            || field.getType().isAnnotationPresent(EditorDecompose.class);
    }

    private static void decomposeFields(Class<?> type, List<FieldInfo> result) {
        for (Field sub : type.getDeclaredFields()) {
            if (Modifier.isStatic(sub.getModifiers())) {
                continue;
            }
            if (sub.isSynthetic()) {
                continue;
            }
            result.add(new FieldInfo(sub.getName(), sub.getType()));
        }
    }

    private ComponentIntrospector() {
    }
}
