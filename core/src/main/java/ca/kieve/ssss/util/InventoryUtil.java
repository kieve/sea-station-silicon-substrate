package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static boolean pickUp(GameContext context, Entity player, Entity item) {
        var inventory = player.get(Inventory.class);
        if (inventory == null) {
            context.log().log("You have no inventory.");
            return false;
        }

        var position = item.get(Position.class);
        if (position != null) {
            position.cleanup(context, item);
            item.removeType(Position.class);
        }

        inventory.items().add(item);

        var descriptor = item.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        context.log().log("Picked up " + name);
        return true;
    }
}
