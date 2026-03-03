package ca.kieve.ssss.component;

public class Openable implements Component {
    public final String openGlyphId;
    public final String closedGlyphId;
    public final String openColorHex;
    public final String closedColorHex;

    public boolean isOpen;

    public Openable(String openGlyphId, String closedGlyphId) {
        this(openGlyphId, closedGlyphId, null, null);
    }

    public Openable(
        String openGlyphId,
        String closedGlyphId,
        String openColorHex,
        String closedColorHex
    ) {
        isOpen = false;
        this.openGlyphId = openGlyphId;
        this.closedGlyphId = closedGlyphId;
        this.openColorHex = openColorHex;
        this.closedColorHex = closedColorHex;
    }
}
