package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Size;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.LogContext;
import ca.kieve.ssss.util.NameUtil;
import ca.kieve.ssss.util.SocketUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WaterDamageSystem extends System {
    private static final long DAMAGE_PERIOD = 10;
    private static final int DAMAGE_PER_PERIOD = 1;
    private static final double TINY_THRESHOLD = 0.05;

    private final FluidContext m_fluid;
    private final Dominion m_ecs;
    private final LogContext m_log;
    private final Map<Entity, Integer> m_tally = new HashMap<>();

    private long m_nextDamageTime = DAMAGE_PERIOD;

    public WaterDamageSystem(GameContext gameContext) {
        super(gameContext);
        m_fluid = gameContext.fluid();
        m_ecs = gameContext.ecs();
        m_log = gameContext.log();
    }

    @Override
    public void tick() {
        accrue();
    }

    @Override
    public void reportResults() {
        accrue();
        flush();
    }

    private void accrue() {
        long now = m_clock.getCurrentTime();
        int steps = 0;
        while (now >= m_nextDamageTime) {
            steps++;
            m_nextDamageTime += DAMAGE_PERIOD;
        }
        if (steps == 0) {
            return;
        }

        int damage = steps * DAMAGE_PER_PERIOD;
        for (Entity entity : roboticEntities()) {
            applyWaterDamage(entity, damage);
        }
    }

    private Set<Entity> roboticEntities() {
        Set<Entity> entities = new LinkedHashSet<>();
        for (var result : m_ecs.findEntitiesWith(Socket.class)) {
            entities.add(result.entity());
        }
        for (var result : m_ecs.findEntitiesWith(Socketable.class)) {
            entities.add(result.entity());
        }
        for (var result : m_ecs.findEntitiesWith(SocketPlug.class)) {
            entities.add(result.entity());
        }
        return entities;
    }

    private void applyWaterDamage(Entity entity, int damage) {
        var socketPlug = entity.get(SocketPlug.class);
        if (socketPlug != null && socketPlug.currentBody != null) {
            return;
        }

        var position = entity.get(Position.class);
        if (position == null) {
            return;
        }

        double mass = m_fluid.getMass(position.getPosition());
        if (!submerged(entity.get(Size.class), mass)) {
            return;
        }

        dealDamage(entity, damage);
    }

    private static boolean submerged(Size size, double mass) {
        Size effective = size == null ? Size.MEDIUM : size;
        return switch (effective) {
        case TINY -> mass >= TINY_THRESHOLD;
        case SMALL, MEDIUM -> mass >= FluidContext.LEVEL_1_CEIL;
        case LARGE, GIGANTIC -> mass >= FluidContext.LEVEL_2_CEIL;
        };
    }

    private void dealDamage(Entity entity, int damage) {
        var socket = entity.get(Socket.class);
        if (socket != null && socket.socketedEntity != null) {
            if (socket.socketedHp <= 0) {
                return;
            }
            socket.socketedHp -= damage;
            if (socket.socketedHp < 0) {
                socket.socketedHp = 0;
            }
            m_tally.merge(entity, damage, Integer::sum);
            if (socket.socketedHp == 0) {
                logDamage(entity);
                SocketUtil.handleSocketedDeath(m_gameContext, entity, socket);
            }
            return;
        }

        var health = entity.get(Health.class);
        if (health == null || health.hp <= 0) {
            return;
        }
        health.hp -= damage;
        if (health.hp < 0) {
            health.hp = 0;
        }
        m_tally.merge(entity, damage, Integer::sum);
        if (health.hp == 0) {
            logDamage(entity);
            m_log.log(NameUtil.nameOf(m_ecs, entity) + " is destroyed!");
        }
    }

    private void logDamage(Entity entity) {
        Integer total = m_tally.remove(entity);
        if (total == null) {
            return;
        }
        m_log.log(NameUtil.nameOf(m_ecs, entity) + " took " + total + " water damage.");
    }

    private void flush() {
        for (Entity entity : new ArrayList<>(m_tally.keySet())) {
            logDamage(entity);
        }
    }
}
