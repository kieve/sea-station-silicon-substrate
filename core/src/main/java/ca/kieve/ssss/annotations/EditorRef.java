package ca.kieve.ssss.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a field is editable in the map editor
 * via a string ID reference. The runtime field type
 * (Entity, BitmapFont, etc.) is constructed from the
 * ID by looking it up in the appropriate registry.
 *
 * <p>For example, {@code Material.entity} is an Entity
 * at runtime, but in YAML it appears as
 * {@code id: material_wood}. Annotating the field with
 * {@code @EditorRef(value = "id",
 *     source = Source.ENTITY)}
 * tells the editor to show an "id" property whose valid
 * values come from the entity registry.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface EditorRef {
    /**
     * The YAML property name for this field.
     */
    String value();

    /**
     * Which content registry provides the valid
     * values for this reference.
     */
    Source source();

    enum Source {
        ENTITY,
        GLYPH,
        BEHAVIOR
    }
}
