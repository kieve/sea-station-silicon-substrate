package ca.kieve.ssss.editor.util;

import ca.kieve.ssss.editor.Globals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GameLauncher {
    private static final int GAME_WIDTH = 1280;
    private static final int GAME_HEIGHT = 720;

    private static final String MAIN_CLASS = "ca.kieve.ssss.lwjgl3.Lwjgl3Launcher";
    private static final String CLASSPATH_FILE = "lwjgl3/build/run-classpath.txt";
    private static final String CORE_RESOURCES = "core/src/main/resources";

    public static void launchDirect(
        double editorX,
        double editorY,
        double editorWidth,
        double editorHeight
    ) throws IOException {
        Path projectRoot = resolveProjectRoot();
        if (projectRoot == null) {
            throw new IOException("Could not find project root " + "(no gradlew found).");
        }

        Path cpFile = projectRoot.resolve(CLASSPATH_FILE);
        if (!Files.exists(cpFile)) {
            throw new IOException(
                "Classpath file not found.\n"
                    + "Run: ./gradlew "
                    + "lwjgl3:exportRunClasspath"
            );
        }

        // Prepend source resources so freshly saved
        // maps are picked up without a rebuild.
        String buildCp = Files.readString(cpFile).strip();
        String srcResources = projectRoot
            .resolve(CORE_RESOURCES)
            .toAbsolutePath().toString();
        String classpath = srcResources
            + File.pathSeparator + buildCp;

        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java")
            .toString();

        int[] pos = windowCenter(editorX, editorY, editorWidth, editorHeight);

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("--enable-native-access=ALL-UNNAMED");
        cmd.add("--add-opens=" + "java.base/java.lang=ALL-UNNAMED");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(MAIN_CLASS);
        cmd.add("--window-x=" + pos[0]);
        cmd.add("--window-y=" + pos[1]);

        new ProcessBuilder(cmd)
            .directory(projectRoot.resolve("assets").toFile())
            .inheritIO()
            .start();
    }

    public static void launchGradle(
        double editorX,
        double editorY,
        double editorWidth,
        double editorHeight
    ) throws IOException {
        Path projectRoot = resolveProjectRoot();
        if (projectRoot == null) {
            throw new IOException("Could not find project root " + "(no gradlew found).");
        }

        int[] pos = windowCenter(editorX, editorY, editorWidth, editorHeight);
        String windowArgs = "--window-x=" + pos[0]
            + " --window-y=" + pos[1];

        String gradlew = Globals.IS_WIN
            ? "gradlew.bat"
            : "gradlew";

        new ProcessBuilder(
            projectRoot.resolve(gradlew).toString(),
            "lwjgl3:run",
            "--args=" + windowArgs
        )
            .directory(projectRoot.toFile())
            .inheritIO()
            .start();
    }

    private static int[] windowCenter(
        double editorX,
        double editorY,
        double editorWidth,
        double editorHeight
    ) {
        int windowX = (int) (editorX
            + (editorWidth - GAME_WIDTH) / 2);
        int windowY = (int) (editorY
            + (editorHeight - GAME_HEIGHT) / 2);
        return new int[] { windowX, windowY };
    }

    private static Path resolveProjectRoot() {
        Path userDir = Path.of(System.getProperty("user.dir"));

        if (hasGradlew(userDir)) {
            return userDir;
        }

        Path parent = userDir.getParent();
        if (parent != null && hasGradlew(parent)) {
            return parent;
        }

        return null;
    }

    private static boolean hasGradlew(Path dir) {
        return dir.resolve("gradlew").toFile().exists()
            || dir.resolve("gradlew.bat")
                .toFile().exists();
    }

    private GameLauncher() {
    }
}
