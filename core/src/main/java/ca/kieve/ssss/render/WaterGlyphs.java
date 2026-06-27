package ca.kieve.ssss.render;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.context.FluidContext;

import static ca.kieve.ssss.util.MathUtil.clamp;
import static ca.kieve.ssss.util.MathUtil.lerp;

public final class WaterGlyphs {
    private WaterGlyphs() {
    }

    public static String glyphIdForLevel(int level) {
        if (level <= 0 || level >= FluidContext.MAX_LEVEL) {
            throw new IllegalArgumentException("no surface glyph for water level " + level);
        }
        return switch (level) {
        case 1 -> "water_1";
        case 2 -> "water_3";
        default -> "water_5";
        };
    }

    public static Color colorForLevel(int level) {
        float t = clamp((float) level / FluidContext.MAX_LEVEL, 0f, 1f);
        float r = lerp(0.45f, 0.05f, t);
        float g = lerp(0.75f, 0.25f, t);
        float b = lerp(0.95f, 0.55f, t);
        float a = lerp(0.55f, 1.00f, t);
        return new Color(r, g, b, a);
    }
}
