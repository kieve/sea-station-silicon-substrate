package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Damage;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.AttackEvent;

public class AttackSystem extends System {
    public AttackSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        var attackEvents = m_gameContext.events().getEvents(AttackEvent.class);
        for (var event : attackEvents) {
            processAttack(event.attacker(), event.target());
        }
    }

    private void processAttack(Entity attackerEntity, Entity targetEntity) {
        var equipment = attackerEntity.get(Equipment.class);
        if (equipment == null || equipment.weapon == null) {
            return;
        }

        var weaponEntity = equipment.weapon;
        var weaponDamage = weaponEntity.get(Damage.class);
        if (weaponDamage == null) {
            return;
        }

        var targetHealth = targetEntity.get(Health.class);
        if (targetHealth == null || targetHealth.hp <= 0) {
            return;
        }

        var weaponDescriptor = weaponEntity.get(Descriptor.class);
        var weaponName = weaponDescriptor != null
            ? weaponDescriptor.name()
            : "unknown weapon";
        var damage = weaponDamage.value;

        targetHealth.hp -= damage;
        if (targetHealth.hp < 0) {
            targetHealth.hp = 0;
        }

        var attackerName = getEntityName(attackerEntity);
        var targetName = getEntityName(targetEntity);

        m_gameContext.log().log(
            attackerName + " hit " + targetName + " for " + damage
                + " damage with " + weaponName + "!"
        );

        if (targetHealth.hp == 0) {
            m_gameContext.log().log(targetName + " is destroyed!");
            var colorComp = targetEntity.get(ColorComp.class);
            if (colorComp != null) {
                colorComp.color = Color.MAROON;
            }
        }
    }

    private String getEntityName(Entity entity) {
        // Check if this is the player (either directly or via socketed body)
        if (entity.has(PlayerController.class) || entity.has(SocketPlug.class)) {
            return "You";
        }

        // Check if this entity is currently being controlled by the player
        var playerResults = m_gameContext.ecs().findEntitiesWith(SocketPlug.class);
        for (var result : playerResults) {
            var socketPlug = result.comp();
            if (socketPlug.currentBody == entity) {
                return "You";
            }
        }

        // Otherwise use the entity's descriptor name
        var descriptor = entity.get(Descriptor.class);
        if (descriptor != null) {
            return descriptor.name();
        }

        return "something";
    }
}
