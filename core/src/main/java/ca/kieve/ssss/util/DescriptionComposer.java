package ca.kieve.ssss.util;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Lockable;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.Socket;

import dev.dominion.ecs.api.Entity;

public class DescriptionComposer {
    private DescriptionComposer() {
        // Do not instantiate
    }

    public static String compose(Entity entity) {
        var descriptor = entity.get(Descriptor.class);
        if (descriptor == null) {
            return "";
        }

        var builder = new StringBuilder();

        // Base descriptor info
        builder.append("It's a ").append(descriptor.name()).append(". ");
        builder.append(descriptor.description()).append(" ");

        // Health status
        var health = entity.get(Health.class);
        if (health != null) {
            builder.append(getHealthDescription(health)).append(" ");
        }

        // Openable / Lockable status
        var openable = entity.get(Openable.class);
        if (openable != null) {
            var lockable = entity.get(Lockable.class);
            if (lockable != null && lockable.isLocked) {
                builder.append("It's locked. ");
            } else if (openable.isOpen) {
                builder.append("It's open. ");
            } else {
                builder.append("It's closed. ");
            }
        }

        // Material
        var material = entity.get(Material.class);
        if (material != null) {
            var materialDescriptor = material.entity().get(Descriptor.class);
            if (materialDescriptor != null) {
                builder.append("It's made from ").append(materialDescriptor.name()).append(". ");
            }
        }

        // Socket status (only if entity has Socket component)
        var socket = entity.get(Socket.class);
        if (socket != null) {
            if (socket.socketedEntity != null) {
                builder.append("You're controlling it.");
            } else if (socket.destroyed) {
                builder.append("It's destroyed and unusable.");
            } else if (health != null) {
                if (health.hp > 0) {
                    builder.append("Could be hijacked, if defeated.");
                } else {
                    builder.append("Is prime to be hijacked.");
                }
            }
        }

        return builder.toString().trim();
    }

    private static String getHealthDescription(Health health) {
        if (health.hp <= 0) {
            return "It's dead.";
        }

        double percentage = (double) health.hp / health.maxHp;
        if (percentage >= 1.0) {
            return "It looks healthy.";
        } else if (percentage >= 0.75) {
            return "It looks slightly damaged.";
        } else if (percentage >= 0.25) {
            return "It looks nearly dead.";
        } else {
            return "It looks nearly dead.";
        }
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
