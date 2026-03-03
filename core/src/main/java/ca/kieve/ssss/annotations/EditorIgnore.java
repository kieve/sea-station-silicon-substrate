package ca.kieve.ssss.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as not editable from the map editor.
 * Fields annotated with this are runtime engine state
 * (e.g. Camera, Entity references) that cannot be
 * serialized to/from YAML.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
public @interface EditorIgnore {
}
