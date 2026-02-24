package ca.kieve.ssss.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Classpath scanning utilities that handle both
 * filesystem directories and JAR entries.
 */
public final class ClasspathUtil {
    /**
     * Lists simple class names (no inner classes) in
     * the package that the given URL points to.
     * Works whether the classes live in a filesystem
     * directory or inside a JAR.
     *
     * @param url     the URL from
     *                ClassLoader.getResource(pkgPath)
     * @param pkgPath the package path using forward
     *                slashes (e.g. "com/example/pkg")
     * @return simple names without ".class" suffix
     */
    public static List<String> listClassNames(
            URL url, String pkgPath) {
        if ("file".equals(url.getProtocol())) {
            return listFromDirectory(url);
        }
        if ("jar".equals(url.getProtocol())) {
            return listFromJar(url, pkgPath);
        }
        return List.of();
    }

    private static List<String> listFromDirectory(
            URL url) {
        var result = new ArrayList<String>();
        File dir;
        try {
            dir = new File(url.toURI());
        } catch (URISyntaxException e) {
            return result;
        }
        if (!dir.isDirectory()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            extractClassName(file.getName(), result);
        }
        return result;
    }

    private static List<String> listFromJar(
            URL url, String pkgPath) {
        var result = new ArrayList<String>();
        try {
            var conn = (JarURLConnection)
                    url.openConnection();
            JarFile jar = conn.getJarFile();
            String prefix = pkgPath.endsWith("/")
                    ? pkgPath
                    : pkgPath + "/";
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName =
                        entries.nextElement().getName();
                if (!entryName.startsWith(prefix)) {
                    continue;
                }
                String relative = entryName
                        .substring(prefix.length());
                if (relative.contains("/")) {
                    continue;
                }
                extractClassName(relative, result);
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static void extractClassName(
            String fileName, List<String> out) {
        if (!fileName.endsWith(".class")) {
            return;
        }
        String simpleName = fileName.substring(
                0, fileName.length() - 6);
        if (simpleName.contains("$")) {
            return;
        }
        out.add(simpleName);
    }

    private ClasspathUtil() {}
}
