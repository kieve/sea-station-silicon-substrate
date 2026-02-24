package ca.kieve.ssss.editor;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.component.Component;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.content.ComponentTypeDeserializer;
import ca.kieve.ssss.util.ClasspathUtil;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTypeDeserializerTest {
    @Test
    void resolveTypeFindsComponentPackageClasses() {
        Class<?> clazz =
                ComponentTypeDeserializer
                        .resolveType("Position");
        assertEquals(Position.class, clazz);
    }

    @Test
    void resolveTypeFindsAiBehaviorPackageClasses() {
        Class<?> clazz =
                ComponentTypeDeserializer
                        .resolveType("AiController");
        assertEquals(AiController.class, clazz);
    }

    @Test
    void resolveTypeReturnsNullForUnknown() {
        Class<?> clazz =
                ComponentTypeDeserializer
                        .resolveType("NonExistentType");
        assertNull(clazz);
    }

    @Test
    void getAllTypeNamesReturnsNonEmptyList() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        assertFalse(
                names.isEmpty(),
                "getAllTypeNames should find component"
                        + " classes on the classpath");
    }

    @Test
    void getAllTypeNamesContainsKnownComponents() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        assertAll(
                () -> assertTrue(
                        names.contains("Position"),
                        "Should contain Position"),
                () -> assertTrue(
                        names.contains("Health"),
                        "Should contain Health"),
                () -> assertTrue(
                        names.contains("Speed"),
                        "Should contain Speed"),
                () -> assertTrue(
                        names.contains("AiController"),
                        "Should contain AiController")
        );
    }

    @Test
    void getAllTypeNamesExcludesNonComponents() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        assertFalse(
                names.contains("Component"),
                "Should exclude the Component"
                        + " interface");
    }

    @Test
    void getAllTypeNamesIsSorted() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        for (int i = 1; i < names.size(); i++) {
            assertTrue(
                    names.get(i - 1).compareTo(
                            names.get(i)) <= 0,
                    "List should be sorted: "
                            + names.get(i - 1)
                            + " should come before "
                            + names.get(i));
        }
    }

    @Test
    void getAllTypeNamesResultsAreAllResolvable() {
        List<String> names =
                ComponentTypeDeserializer
                        .getAllTypeNames();
        for (String name : names) {
            Class<?> clazz =
                    ComponentTypeDeserializer
                            .resolveType(name);
            assertNotNull(
                    clazz,
                    "resolveType should find: " + name);
            assertTrue(
                    Component.class.isAssignableFrom(
                            clazz),
                    name + " should implement Component");
        }
    }

    @Test
    void allComponentPackageClassesImplementComponent() {
        String pkg = "ca.kieve.ssss.component";
        String path = pkg.replace('.', '/');
        URL url = Component.class.getClassLoader()
                .getResource(path);
        assertNotNull(url,
                "Should find component package");

        List<String> classNames =
                ClasspathUtil.listClassNames(url, path);
        assertFalse(classNames.isEmpty(),
                "Should find classes in component"
                        + " package");

        var violations = new ArrayList<String>();
        for (String simpleName : classNames) {
            Class<?> clazz;
            try {
                clazz = Class.forName(
                        pkg + "." + simpleName);
            } catch (ClassNotFoundException e) {
                fail("Could not load class: "
                        + simpleName);
                return;
            }

            if (clazz.isInterface()
                    || clazz.isEnum()) {
                continue;
            }

            if (!Component.class
                    .isAssignableFrom(clazz)) {
                violations.add(simpleName);
            }
        }

        assertTrue(
                violations.isEmpty(),
                "All classes in ca.kieve.ssss.component"
                        + " should implement Component,"
                        + " but these do not: "
                        + violations);
    }
}
