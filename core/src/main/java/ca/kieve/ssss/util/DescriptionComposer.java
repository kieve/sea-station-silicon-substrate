package ca.kieve.ssss.util;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Material;
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

        // Material
        var material = entity.get(Material.class);
        if (material != null) {
            builder.append("It's made from ").append(material.description()).append(". ");
        }

        // Socket status (only if entity has Socket component)
        var socket = entity.get(Socket.class);
        if (socket != null) {
            if (socket.socketedEntity != null) {
                builder.append("You're controlling it.");
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
}
