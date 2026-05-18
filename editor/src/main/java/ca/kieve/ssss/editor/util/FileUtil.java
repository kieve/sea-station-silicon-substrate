package ca.kieve.ssss.editor.util;

import java.io.File;
import java.io.IOException;

/**
 * Small {@link File} helpers for editor code that walks disk paths
 * (e.g. submap ref resolution and cycle detection in
 * {@code ComposedWorld}).
 */
public final class FileUtil {
    private FileUtil() {
    }

    /**
     * Returns {@link File#getCanonicalFile()}, falling back to
     * {@link File#getAbsoluteFile()} when canonical resolution fails
     * (e.g. on a missing path or a permission error). Used as a stable
     * identity key when detecting submap cycles where the same file
     * may show up under different relative paths.
     */
    public static File canonicalOrAbsolute(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }
}
