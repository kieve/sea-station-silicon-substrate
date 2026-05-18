package ca.kieve.ssss.editor.util;

import ca.kieve.ssss.content.ContentRef;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MapPathUtil {
    private static final String MAPS_REL_PATH = "core/src/main/resources/content/maps";
    private static final String CONTENT_MAPS_PREFIX = "maps/";

    private MapPathUtil() {
    }

    public static File resolveMapsDir() {
        Path userDir = Path.of(System.getProperty("user.dir"));
        Path fromUserDir = userDir.resolve(MAPS_REL_PATH);
        if (fromUserDir.toFile().isDirectory()) {
            return fromUserDir.toFile();
        }

        Path fromParent = userDir.getParent().resolve(MAPS_REL_PATH);
        if (fromParent.toFile().isDirectory()) {
            return fromParent.toFile();
        }

        return null;
    }

    public static List<String> listAvailableMaps() {
        File mapsDir = resolveMapsDir();
        if (mapsDir == null || !mapsDir.isDirectory()) {
            return List.of();
        }
        var names = new ArrayList<String>();
        collectMaps(mapsDir.toPath(), mapsDir, names);
        names.sort(String::compareTo);
        return names;
    }

    public static String getRelativePath(File mapFile) {
        if (mapFile == null) {
            return "";
        }
        File mapsDir = resolveMapsDir();
        if (mapsDir == null) {
            return mapFile.getName();
        }
        Path mapsPath = mapsDir.toPath().normalize();
        Path filePath = mapFile.toPath().normalize();
        if (!filePath.startsWith(mapsPath)) {
            return mapFile.getName();
        }
        return mapsPath.relativize(filePath)
            .toString().replace('\\', '/');
    }

    /**
     * Resolves a submap {@code ref} (e.g. {@code ./damaged_sub.yaml} or
     * {@code /maps/home_base/damaged_sub.yaml}) against {@code referringFile}.
     * Returns {@code null} if the maps root cannot be located or the
     * referring file is outside it.
     */
    public static File resolveSubmapRef(File referringFile, String ref) {
        if (referringFile == null || ref == null) {
            return null;
        }
        File mapsDir = resolveMapsDir();
        if (mapsDir == null) {
            return null;
        }
        Path mapsRoot = mapsDir.toPath().normalize();
        Path filePath = referringFile.toPath().normalize();
        if (!filePath.startsWith(mapsRoot)) {
            return null;
        }
        String relToMaps = mapsRoot.relativize(filePath).toString().replace('\\', '/');
        try {
            String resolved = ContentRef.resolve(CONTENT_MAPS_PREFIX + relToMaps, ref);
            if (!resolved.startsWith(CONTENT_MAPS_PREFIX)) {
                return null;
            }
            String mapPath = resolved.substring(CONTENT_MAPS_PREFIX.length());
            return new File(mapsDir, mapPath.replace('/', File.separatorChar));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void collectMaps(Path mapsRoot, File dir, List<String> out) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                collectMaps(mapsRoot, f, out);
            } else if (f.getName().endsWith(".yaml")) {
                out.add(mapsRoot.relativize(f.toPath()).toString().replace('\\', '/'));
            }
        }
    }
}
