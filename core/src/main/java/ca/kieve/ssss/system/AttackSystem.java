package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Damage;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.LastAttacker;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.util.NameUtil;
import ca.kieve.ssss.util.SocketUtil;

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

        // Check if target is a socketed body - damage goes to socketedHp
        var socket = targetEntity.get(Socket.class);
        if (socket != null && socket.socketedEntity != null) {
            processSocketedDamage(attackerEntity, targetEntity, socket, weaponEntity, weaponDamage);
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

        // Track who attacked this entity for use in AI behaviors
        trackLastAttacker(targetEntity, attackerEntity);

        var attackerName = NameUtil.nameOf(m_gameContext.ecs(), attackerEntity);
        var targetName = NameUtil.nameOf(m_gameContext.ecs(), targetEntity);

        m_gameContext.log().log(
            attackerName + " hit " + targetName + " for " + damage
                + " damage with " + weaponName + "!"
        );

        if (targetHealth.hp != 0) {
            return;
        }
        m_gameContext.log().log(targetName + " is destroyed!");
    }

    private void processSocketedDamage(
        Entity attackerEntity,
        Entity targetEntity,
        Socket socket,
        Entity weaponEntity,
        Damage weaponDamage
    ) {
        var weaponDescriptor = weaponEntity.get(Descriptor.class);
        var weaponName = weaponDescriptor != null
            ? weaponDescriptor.name()
            : "unknown weapon";
        var damage = weaponDamage.value;

        socket.socketedHp -= damage;
        if (socket.socketedHp < 0) {
            socket.socketedHp = 0;
        }

        var attackerName = NameUtil.nameOf(m_gameContext.ecs(), attackerEntity);

        m_gameContext.log().log(
            attackerName + " hit You for " + damage + " damage with " + weaponName + "!"
        );

        if (socket.socketedHp == 0) {
            SocketUtil.handleSocketedDeath(m_gameContext, targetEntity, socket);
        }
    }

    private void trackLastAttacker(Entity targetEntity, Entity attackerEntity) {
        var lastAttacker = targetEntity.get(LastAttacker.class);
        if (lastAttacker == null) {
            targetEntity.add(new LastAttacker(attackerEntity));
        } else {
            lastAttacker.attacker = attackerEntity;
        }
    }
}
