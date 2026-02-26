package ca.kieve.ssss.editor.util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MapPathUtil {
    private static final String MAPS_REL_PATH =
            "core/src/main/resources/content/maps";

    private MapPathUtil() {
    }

    public static File resolveMapsDir() {
        Path userDir = Path.of(System.getProperty("user.dir"));
        Path fromUserDir = userDir.resolve(MAPS_REL_PATH);
        if (fromUserDir.toFile().isDirectory()) {
            return fromUserDir.toFile();
        }

        Path fromParent =
                userDir.getParent().resolve(MAPS_REL_PATH);
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

    private static void collectMaps(
            Path mapsRoot, File dir, List<String> out) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                collectMaps(mapsRoot, f, out);
            } else if (f.getName().endsWith(".yaml")) {
                out.add(mapsRoot.relativize(f.toPath())
                        .toString().replace('\\', '/'));
            }
        }
    }
}
