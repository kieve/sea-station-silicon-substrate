package ca.kieve.ssss.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a composite type whose instance fields should
 * be shown individually in the map editor. When placed
 * on a class, any component field of that type is
 * automatically decomposed. Can also be placed on an
 * individual field or record component for one-off
 * decomposition.
 *
 * <p>For example, {@code Vec3i} is annotated with
 * {@code @EditorDecompose}, so any component field of
 * type {@code Vec3i} will appear as {@code x},
 * {@code y}, {@code z} instead of the single composite
 * field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT})
public @interface EditorDecompose {}
