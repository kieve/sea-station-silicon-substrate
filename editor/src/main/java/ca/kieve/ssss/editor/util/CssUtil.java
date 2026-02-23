package ca.kieve.ssss.editor.util;

public final class CssUtil {
    public static String inline(String css) {
        return "data:text/css," + css;
    }

    private CssUtil() {}
}
