package ca.kieve.ssss.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import ca.kieve.ssss.component.Component;
import ca.kieve.ssss.util.ClasspathUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class ComponentTypeDeserializer
    extends
    JsonDeserializer<Class<?>> {
    private static final List<String> PACKAGES = List.of(
        "ca.kieve.ssss.component.",
        "ca.kieve.ssss.ai.behavior."
    );

    public static Class<?> resolveType(String typeName) {
        for (String pkg : PACKAGES) {
            Class<?> clazz = tryLoadClass(pkg, typeName);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }

    public static List<String> getAllTypeNames() {
        var names = new TreeSet<String>();
        var loader = Component.class.getClassLoader();
        for (String pkg : PACKAGES) {
            String pkgPath = pkg.replace('.', '/');
            if (pkgPath.endsWith("/")) {
                pkgPath = pkgPath.substring(0, pkgPath.length() - 1);
            }
            URL url = loader.getResource(pkgPath);
            if (url == null) {
                continue;
            }
            List<String> classNames = ClasspathUtil.listClassNames(url, pkgPath);
            for (String simpleName : classNames) {
                Class<?> clazz = tryLoadClass(pkg, simpleName);
                if (clazz == null) {
                    continue;
                }
                if (clazz.isInterface()
                    || !Component.class
                        .isAssignableFrom(clazz)) {
                    continue;
                }
                names.add(simpleName);
            }
        }
        return new ArrayList<>(names);
    }

    private static Class<?> tryLoadClass(String packagePrefix, String typeName) {
        try {
            return Class.forName(packagePrefix + typeName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Class<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String typeName = p.getText();
        Class<?> clazz = resolveType(typeName);
        if (clazz != null) {
            return clazz;
        }
        throw new IOException("Failed to load component class: " + typeName);
    }
}
