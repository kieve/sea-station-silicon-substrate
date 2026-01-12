package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Damage;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.LastAttacker;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.event.EjectEvent;

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

        var attackerName = getEntityName(attackerEntity);

        m_gameContext.log().log(
            attackerName + " hit You for " + damage + " damage with " + weaponName + "!"
        );

        if (socket.socketedHp == 0) {
            handleSocketedDeath(targetEntity, socket);
        }
    }

    private void handleSocketedDeath(Entity bodyEntity, Socket socket) {
        var playerEntity = socket.socketedEntity;
        if (playerEntity == null) {
            return;
        }

        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug == null) {
            return;
        }

        m_gameContext.log().log("Your robotic body is destroyed! You are forcibly ejected!");

        // Mark mech as permanently destroyed
        socket.destroyed = true;

        // Remove Socketable so it can't be re-entered
        if (bodyEntity.has(Socketable.class)) {
            bodyEntity.removeType(Socketable.class);
        }

        // Post eject event to be processed by SocketSystem
        m_gameContext.events().addEvent(
            new EjectEvent(playerEntity, socketPlug, bodyEntity, socket)
        );

        // Change color to indicate permanent destruction
        var colorComp = bodyEntity.get(ColorComp.class);
        if (colorComp != null) {
            colorComp.color = Color.DARK_GRAY;
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

    private String getEntityName(Entity entity) {
        // Check if this is the player (either directly or via socketed body)
        if (entity.has(PlayerController.class) || entity.has(Player.class)) {
            return "You";
        }

        // Check if this entity is currently being controlled by the player
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class);
        for (var result : playerResults) {
            var playerEntity = result.entity();
            var socketPlug = playerEntity.get(SocketPlug.class);
            if (socketPlug != null && socketPlug.currentBody == entity) {
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
