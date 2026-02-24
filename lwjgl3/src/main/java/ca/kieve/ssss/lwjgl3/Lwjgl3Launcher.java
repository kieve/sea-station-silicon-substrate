package ca.kieve.ssss.lwjgl3;

import ca.kieve.ssss.Main;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String ARG_WINDOW_X =
            "--window-x=";
    private static final String ARG_WINDOW_Y =
            "--window-y=";

    private Lwjgl3Launcher() {}

    public static void main(String[] args) {
        // Handles macOS support and helps on Windows.
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        createApplication(args);
    }

    private static Lwjgl3Application createApplication(
            String[] args) {
        return new Lwjgl3Application(
                new Main(),
                getDefaultConfiguration(args));
    }

    private static Lwjgl3ApplicationConfiguration
            getDefaultConfiguration(String[] args) {
        var configuration =
                new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(
                "Sea Station Silicon Substrate ");
        //// Vsync limits the frames per second to what
        //// your hardware can display, and helps
        //// eliminate screen tearing. This setting
        //// doesn't always work on Linux, so the line
        //// after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the
        //// currently active monitor, plus 1 to try to
        //// match fractional refresh rates. The Vsync
        //// setting above should limit the actual FPS to
        //// match the monitor.
        var refreshRate =
                Lwjgl3ApplicationConfiguration
                        .getDisplayMode().refreshRate;
        configuration.setForegroundFPS(refreshRate + 1);
        //// If you remove the above line and set Vsync
        //// to false, you can get unlimited FPS, which
        //// can be useful for testing performance, but
        //// can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers
        //// to fully disable Vsync; this can cause
        //// screen tearing.

        configuration.setWindowedMode(
                WINDOW_WIDTH, WINDOW_HEIGHT);
        //// You can change these files; they are in
        //// lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of
        //// assets/ .
        configuration.setWindowIcon(
                "libgdx128.png", "libgdx64.png",
                "libgdx32.png", "libgdx16.png");

        applyWindowPosition(configuration, args);

        return configuration;
    }

    private static void applyWindowPosition(
            Lwjgl3ApplicationConfiguration configuration,
            String[] args) {
        Integer x = null;
        Integer y = null;
        for (String arg : args) {
            if (arg.startsWith(ARG_WINDOW_X)) {
                x = parseIntArg(arg, ARG_WINDOW_X);
            } else if (arg.startsWith(ARG_WINDOW_Y)) {
                y = parseIntArg(arg, ARG_WINDOW_Y);
            }
        }
        if (x != null && y != null) {
            configuration.setWindowPosition(x, y);
        }
    }

    private static Integer parseIntArg(
            String arg, String prefix) {
        try {
            return Integer.parseInt(
                    arg.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
