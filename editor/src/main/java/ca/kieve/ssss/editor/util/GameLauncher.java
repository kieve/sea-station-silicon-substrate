package ca.kieve.ssss.editor.util;

import java.io.IOException;
import java.nio.file.Path;

import ca.kieve.ssss.editor.Globals;

public final class GameLauncher {
    private static final int GAME_WIDTH = 1280;
    private static final int GAME_HEIGHT = 720;

    public static void launch(
            double editorX, double editorY,
            double editorWidth, double editorHeight)
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        if (projectRoot == null) {
            throw new IOException(
                    "Could not find project root "
                            + "(no gradlew found).");
        }

        int windowX = (int) (editorX
                + (editorWidth - GAME_WIDTH) / 2);
        int windowY = (int) (editorY
                + (editorHeight - GAME_HEIGHT) / 2);

        String gradlew = Globals.IS_WIN
                ? "gradlew.bat" : "gradlew";
        String args = "--window-x=" + windowX
                + " --window-y=" + windowY;

        new ProcessBuilder(
                projectRoot.resolve(gradlew).toString(),
                "lwjgl3:run",
                "--args=" + args)
                .directory(projectRoot.toFile())
                .inheritIO()
                .start();
    }

    private static Path resolveProjectRoot() {
        Path userDir = Path.of(
                System.getProperty("user.dir"));

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

    private GameLauncher() {}
}
