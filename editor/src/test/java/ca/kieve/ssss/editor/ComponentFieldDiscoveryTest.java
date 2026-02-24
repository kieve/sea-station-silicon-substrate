package ca.kieve.ssss.editor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.annotations.EditorRef;
import ca.kieve.ssss.content.ComponentTypeDeserializer;
import ca.kieve.ssss.editor.component.ComponentIntrospector;
import ca.kieve.ssss.editor.component.ComponentIntrospector.FieldInfo;

class ComponentFieldDiscoveryTest {

    @Test
    void allComponentTypesCanBeIntrospected() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        assertFalse(names.isEmpty(),
                "Should find component types");

        for (String name : names) {
            assertDoesNotThrow(
                    () -> ComponentIntrospector
                            .getEditableFields(name),
                    "Should introspect: " + name);
        }
    }

    @Test
    void allComponentFieldsCanBeDiscovered() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();

        var sb = new StringBuilder();
        sb.append("\n=== Component Field Discovery ===\n");

        for (String name : names) {
            Class<?> clazz =
                    ComponentTypeDeserializer
                            .resolveType(name);
            assertNotNull(clazz);

            List<FieldInfo> fields =
                    ComponentIntrospector
                            .getEditableFields(clazz);

            sb.append("\n").append(name);
            if (clazz.isEnum()) {
                sb.append(" (enum): ");
                sb.append(String.join(", ",
                        ComponentIntrospector
                                .getEnumConstants(
                                        clazz)));
            } else if (clazz.isRecord()) {
                sb.append(" (record)");
            }

            if (fields.isEmpty()
                    && !clazz.isEnum()) {
                sb.append(": (marker)");
            }
            sb.append("\n");

            for (FieldInfo field : fields) {
                sb.append("  - ")
                        .append(field.name())
                        .append(": ")
                        .append(field.type()
                                .getSimpleName());
                if (field.isRef()) {
                    sb.append(" (ref -> ")
                            .append(field.refSource())
                            .append(")");
                }
                sb.append("\n");
            }
        }

        sb.append("\n=== End ===\n");
        IO.println(sb);
    }

    // -- Direct value fields --

    @Test
    void healthHasMaxHpAndHp() {
        var fields = ComponentIntrospector
                .getEditableFields("Health");
        assertFieldExists(fields, "maxHp", long.class);
        assertFieldExists(fields, "hp", long.class);
        assertEquals(2, fields.size());
    }

    @Test
    void positionHasDecomposedXyz() {
        var fields = ComponentIntrospector
                .getEditableFields("Position");
        assertFieldExists(fields, "x", int.class);
        assertFieldExists(fields, "y", int.class);
        assertFieldExists(fields, "z", int.class);
        assertFieldNotPresent(fields, "m_position");
        assertEquals(3, fields.size());
    }

    @Test
    void velocityHasDecomposedXyz() {
        var fields = ComponentIntrospector
                .getEditableFields("Velocity");
        assertFieldExists(fields, "x", int.class);
        assertFieldExists(fields, "y", int.class);
        assertFieldExists(fields, "z", int.class);
        assertFieldNotPresent(fields, "instant");
        assertEquals(3, fields.size());
    }

    @Test
    void scurryConfigHasClockwiseAndXyz() {
        var fields = ComponentIntrospector
                .getEditableFields("ScurryConfig");
        assertFieldExists(fields, "clockwise",
                boolean.class);
        assertFieldExists(fields, "x", int.class);
        assertFieldExists(fields, "y", int.class);
        assertFieldExists(fields, "z", int.class);
        assertFieldNotPresent(fields,
                "initialDirection");
        assertEquals(4, fields.size());
    }

    @Test
    void scurryInitHasDecomposedXyz() {
        var fields = ComponentIntrospector
                .getEditableFields("ScurryInit");
        assertFieldExists(fields, "x", int.class);
        assertFieldExists(fields, "y", int.class);
        assertFieldExists(fields, "z", int.class);
        assertFieldNotPresent(fields,
                "initialDirection");
        assertEquals(3, fields.size());
    }

    @Test
    void speedHasAllFields() {
        var fields = ComponentIntrospector
                .getEditableFields("Speed");
        assertFieldExists(fields, "val", int.class);
        assertFieldExists(fields, "canActAt", long.class);
        assertFieldExists(fields, "canAct",
                boolean.class);
        assertEquals(3, fields.size());
    }

    @Test
    void colorCompHasColor() {
        var fields = ComponentIntrospector
                .getEditableFields("ColorComp");
        assertFieldExists(fields, "color");
        assertEquals(1, fields.size());
    }

    @Test
    void descriptorHasNameAndDescription() {
        var fields = ComponentIntrospector
                .getEditableFields("Descriptor");
        assertFieldExists(fields, "name",
                String.class);
        assertFieldExists(fields, "description",
                String.class);
        assertEquals(2, fields.size());
    }

    @Test
    void openableHasAllFields() {
        var fields = ComponentIntrospector
                .getEditableFields("Openable");
        assertFieldExists(fields, "isOpen",
                boolean.class);
        assertFieldExists(fields, "openGlyphId",
                String.class);
        assertFieldExists(fields, "closedGlyphId",
                String.class);
        assertFieldExists(fields, "openColorHex",
                String.class);
        assertFieldExists(fields, "closedColorHex",
                String.class);
        assertEquals(5, fields.size());
    }

    @Test
    void socketHasEditableFieldsOnly() {
        var fields = ComponentIntrospector
                .getEditableFields("Socket");
        assertFieldExists(fields, "socketedMaxHp",
                long.class);
        assertFieldExists(fields, "socketedHp",
                long.class);
        assertFieldExists(fields, "destroyed",
                boolean.class);
        assertFieldNotPresent(fields,
                "socketedEntity");
        assertEquals(3, fields.size());
    }

    @Test
    void renderingHintHasZIndex() {
        var fields = ComponentIntrospector
                .getEditableFields("RenderingHint");
        assertFieldExists(fields, "zIndex", int.class);
        assertEquals(1, fields.size());
    }

    @Test
    void lockableHasIsLocked() {
        var fields = ComponentIntrospector
                .getEditableFields("Lockable");
        assertFieldExists(fields, "isLocked",
                boolean.class);
        assertEquals(1, fields.size());
    }

    // -- @EditorRef fields --

    @Test
    void materialHasEntityRef() {
        var fields = ComponentIntrospector
                .getEditableFields("Material");
        assertEquals(1, fields.size());
        var field = fields.getFirst();
        assertEquals("id", field.name());
        assertEquals(String.class, field.type());
        assertTrue(field.isRef());
        assertEquals(EditorRef.Source.ENTITY,
                field.refSource());
    }

    @Test
    void equipmentHasWeaponRef() {
        var fields = ComponentIntrospector
                .getEditableFields("Equipment");
        assertEquals(1, fields.size());
        var field = fields.getFirst();
        assertEquals("weaponId", field.name());
        assertEquals(String.class, field.type());
        assertTrue(field.isRef());
        assertEquals(EditorRef.Source.ENTITY,
                field.refSource());
    }

    @Test
    void tileGlyphHasGlyphRef() {
        var fields = ComponentIntrospector
                .getEditableFields("TileGlyph");
        assertEquals(1, fields.size());
        var field = fields.getFirst();
        assertEquals("glyphId", field.name());
        assertEquals(String.class, field.type());
        assertTrue(field.isRef());
        assertEquals(EditorRef.Source.GLYPH,
                field.refSource());
    }

    @Test
    void aiControllerHasBehaviorRef() {
        var fields = ComponentIntrospector
                .getEditableFields("AiController");
        assertEquals(1, fields.size());
        var field = fields.getFirst();
        assertEquals("behavior", field.name());
        assertEquals(String.class, field.type());
        assertTrue(field.isRef());
        assertEquals(EditorRef.Source.BEHAVIOR,
                field.refSource());
    }

    // -- @EditorIgnore fields --

    @Test
    void cameraCompIsFullyIgnored() {
        var fields = ComponentIntrospector
                .getEditableFields("CameraComp");
        assertTrue(fields.isEmpty());
        assertTrue(ComponentIntrospector.isMarker(
                ComponentTypeDeserializer
                        .resolveType("CameraComp")));
    }

    @Test
    void socketPlugIsFullyIgnored() {
        var fields = ComponentIntrospector
                .getEditableFields("SocketPlug");
        assertTrue(fields.isEmpty());
    }

    @Test
    void lastAttackerIsFullyIgnored() {
        var fields = ComponentIntrospector
                .getEditableFields("LastAttacker");
        assertTrue(fields.isEmpty());
    }

    @Test
    void inventoryIsFullyIgnored() {
        var fields = ComponentIntrospector
                .getEditableFields("Inventory");
        assertTrue(fields.isEmpty());
    }

    // -- Marker components --

    @Test
    void markerComponentsHaveNoFields() {
        String[] markers = {
                "Player", "Solid", "Collider",
                "Attackable", "Examinable",
                "Socketable", "PlayerController",
                "Hidden", "Opaque", "Item"
        };
        for (String name : markers) {
            Class<?> clazz =
                    ComponentTypeDeserializer
                            .resolveType(name);
            assertNotNull(clazz,
                    "Should resolve: " + name);
            assertTrue(
                    ComponentIntrospector.isMarker(clazz),
                    name + " should be a marker");
            assertTrue(
                    ComponentIntrospector
                            .getEditableFields(clazz)
                            .isEmpty(),
                    name + " should have no fields");
        }
    }

    // -- Enum components --

    @Test
    void sizeEnumHasConstants() {
        Class<?> sizeClass =
                ComponentTypeDeserializer
                        .resolveType("Size");
        assertNotNull(sizeClass);
        assertFalse(
                ComponentIntrospector.isMarker(
                        sizeClass),
                "Enum should not be reported"
                        + " as marker");

        List<String> constants =
                ComponentIntrospector
                        .getEnumConstants(sizeClass);
        assertAll(
                () -> assertTrue(
                        constants.contains("TINY")),
                () -> assertTrue(
                        constants.contains("SMALL")),
                () -> assertTrue(
                        constants.contains("MEDIUM")),
                () -> assertTrue(
                        constants.contains("LARGE")),
                () -> assertTrue(
                        constants.contains("GIGANTIC"))
        );

        // Enum should have no editable fields
        assertTrue(ComponentIntrospector
                .getEditableFields(sizeClass).isEmpty());
    }

    @Test
    void enumConstantsByNameWorks() {
        List<String> constants =
                ComponentIntrospector
                        .getEnumConstants("Size");
        assertFalse(constants.isEmpty());
        assertTrue(constants.contains("TINY"));
    }

    @Test
    void enumConstantsForNonEnumIsEmpty() {
        List<String> constants =
                ComponentIntrospector
                        .getEnumConstants("Health");
        assertTrue(constants.isEmpty());
    }

    // -- Edge cases --

    @Test
    void unknownTypeNameReturnsEmpty() {
        var fields = ComponentIntrospector
                .getEditableFields("DoesNotExist");
        assertTrue(fields.isEmpty());
    }

    @Test
    void unknownTypeEnumConstantsReturnsEmpty() {
        var constants = ComponentIntrospector
                .getEnumConstants("DoesNotExist");
        assertTrue(constants.isEmpty());
    }

    @Test
    void refFieldsAreNotDirectlyEditable() {
        // All ref fields should report String.class
        // as their type, not the runtime field type
        for (String name
                : ComponentTypeDeserializer
                        .getAllTypeNames()) {
            var fields = ComponentIntrospector
                    .getEditableFields(name);
            for (FieldInfo field : fields) {
                if (field.isRef()) {
                    assertEquals(String.class,
                            field.type(),
                            name + "." + field.name()
                                    + " ref should"
                                    + " have String"
                                    + " type");
                }
            }
        }
    }

    // -- Helpers --

    private static void assertFieldExists(
            List<FieldInfo> fields, String name) {
        assertTrue(
                fields.stream().anyMatch(
                        f -> f.name().equals(name)),
                "Should have field: " + name);
    }

    private static void assertFieldExists(
            List<FieldInfo> fields,
            String name, Class<?> type) {
        assertTrue(
                fields.stream().anyMatch(
                        f -> f.name().equals(name)
                                && f.type() == type),
                "Should have field: " + name
                        + " of type "
                        + type.getSimpleName());
    }

    private static void assertFieldNotPresent(
            List<FieldInfo> fields, String name) {
        assertFalse(
                fields.stream().anyMatch(
                        f -> f.name().equals(name)),
                "Should NOT have field: " + name);
    }
}
