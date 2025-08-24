package ca.kieve.ssss.ui.core;

public record UiPosition(int x, int y) {
    public static UiPosition ZERO =  new UiPosition(0, 0);
}

