package ca.kieve.ssss.editor;

public final class Globals {
    public static final boolean IS_WIN;

    static {
        IS_WIN = System.getProperty("os.name", "")
                .toLowerCase()
                .startsWith("win");
    }

    private Globals() {}
}
