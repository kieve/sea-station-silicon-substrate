package ca.kieve.ssss.system.ai;

import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.ai.AiAttacker;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.system.System;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

/**
 * AI system for entities that attack the player when adjacent.
 * Entities with AiAttacker component will attempt to attack the player
 * each tick they can act (based on Speed component).
 */
public class AiAttackerSystem extends System {
    public AiAttackerSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        Entity player = findPlayer();
        if (player == null) {
            return;
        }

        var playerPosComp = player.get(Position.class);
        if (playerPosComp == null) {
            return;
        }
        Vec3i playerPos = playerPosComp.getPosition();

        var attackers = m_gameContext.ecs().findEntitiesWith(
            AiAttacker.class,
            Position.class,
            Speed.class,
            Equipment.class
        );

        attackers.forEach(result -> {
            var pos = result.comp2().getPosition();
            var speed = result.comp3();

            if (!speed.canAct) {
                return;
            }

            // Only attack on same Z level
            if (pos.z != playerPos.z) {
                return;
            }

            // Check if adjacent (Manhattan distance == 1)
            int dist = Math.abs(pos.x - playerPos.x) + Math.abs(pos.y - playerPos.y);
            if (dist != 1) {
                return;
            }

            // Create attack event
            m_gameContext.events().addSystemEvent(new AttackEvent(result.entity(), player));
        });
    }

    private Entity findPlayer() {
        var results = m_gameContext.ecs().findEntitiesWith(PlayerController.class);
        var it = results.iterator();
        if (it.hasNext()) {
            return it.next().entity();
        }
        return null;
    }
}
