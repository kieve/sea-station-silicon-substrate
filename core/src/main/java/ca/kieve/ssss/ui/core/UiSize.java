package ca.kieve.ssss.ui.core;

public record UiSize(int w, int h) {
    public static UiSize ZERO = new UiSize(0, 0);
}
