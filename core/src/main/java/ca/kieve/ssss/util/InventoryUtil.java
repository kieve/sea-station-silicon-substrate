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

    public static boolean drop(GameContext context, Entity player, Entity item, Vec3i dropPos) {
        var inventory = player.get(Inventory.class);
        if (inventory == null) {
            return false;
        }
        if (!inventory.items().remove(item)) {
            return false;
        }

        item.add(new Position(dropPos));
        context.pos().add(item, dropPos);

        var descriptor = item.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        context.log().log("Dropped " + name);
        return true;
    }

    public static boolean moveOnGround(GameContext context, Entity item, Vec3i toPos) {
        var position = item.get(Position.class);
        if (position == null) {
            return false;
        }
        position.setPosition(context, item, toPos);
        var descriptor = item.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        context.log().log("Moved " + name);
        return true;
    }
}
