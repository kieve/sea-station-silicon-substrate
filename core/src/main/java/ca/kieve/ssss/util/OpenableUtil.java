package ca.kieve.ssss.util;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;

public final class OpenableUtil {
    private OpenableUtil() {}

    public static void open(GameContext context, Entity entity) {
        var openable = entity.get(Openable.class);
        if (openable == null || openable.isOpen) {
            return;
        }

        openable.isOpen = true;
        swapGlyph(context, entity, openable.openGlyphId);
        swapColor(entity, openable.openColorHex);

        var descriptor = entity.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "something";
        context.log().log("The " + name + " opens.");
    }

    public static void close(GameContext context, Entity entity) {
        var openable = entity.get(Openable.class);
        if (openable == null || !openable.isOpen) {
            return;
        }

        openable.isOpen = false;
        swapGlyph(context, entity, openable.closedGlyphId);
        swapColor(entity, openable.closedColorHex);

        var descriptor = entity.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "something";
        context.log().log("The " + name + " closes.");
    }

    private static void swapGlyph(
            GameContext context, Entity entity, String glyphId) {
        var glyphFactory = context.entityFactory().getGlyphFactory();
        entity.removeType(TileGlyph.class);
        entity.add(glyphFactory.getGlyph(glyphId));
    }

    private static void swapColor(Entity entity, String colorHex) {
        if (colorHex == null) {
            return;
        }
        var colorComp = entity.get(ColorComp.class);
        if (colorComp != null) {
            colorComp.color = Color.valueOf(colorHex);
        } else {
            entity.add(new ColorComp(Color.valueOf(colorHex)));
        }
    }
}
