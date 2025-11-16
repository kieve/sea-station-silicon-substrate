package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Damage;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.EventType;

public class AttackSystem extends System {
    public AttackSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        var attackEvents = m_gameContext.events().getEvents(EventType.ATTACK);
        if (attackEvents.isEmpty()) {
            return;
        }

        var playerResults = m_gameContext.ecs().findEntitiesWith(
            SocketPlug.class,
            Position.class
        );

        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerWith.comp1();

        var attackerEntity = playerEntity;
        if (socketPlug.currentBody != null) {
            attackerEntity = socketPlug.currentBody;
        }

        var equipment = attackerEntity.get(Equipment.class);
        if (equipment == null || equipment.weapon == null) {
            return;
        }

        var weaponEntity = equipment.weapon;
        var weaponDamage = weaponEntity.get(Damage.class);
        if (weaponDamage == null) {
            return;
        }

        var weaponDescriptor = weaponEntity.get(Descriptor.class);
        var weaponName = weaponDescriptor != null
            ? weaponDescriptor.name()
            : "unknown weapon";
        var damage = weaponDamage.value;

        for (var targetEntity : attackEvents) {
            var targetHealth = targetEntity.get(Health.class);
            if (targetHealth == null) {
                continue;
            }
            if (targetHealth.hp <= 0) {
                continue;
            }

            targetHealth.hp -= damage;
            if (targetHealth.hp < 0) {
                targetHealth.hp = 0;
            }

            var targetDescriptor = targetEntity.get(Descriptor.class);
            var targetName = targetDescriptor != null
                ? targetDescriptor.name()
                : "something";

            m_gameContext.log().log(
                "You hit " + targetName + " for " + damage + " damage with " + weaponName + "!"
            );

            if (targetHealth.hp == 0) {
                m_gameContext.log().log(targetName + " is destroyed!");
                var colorComp = targetEntity.get(ColorComp.class);
                if (colorComp != null) {
                    colorComp.color = Color.MAROON;
                }
            }
        }
    }
}
