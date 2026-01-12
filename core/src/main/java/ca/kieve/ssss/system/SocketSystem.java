package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.PositionContext;
import ca.kieve.ssss.event.EjectEvent;
import ca.kieve.ssss.event.SocketEvent;
import ca.kieve.ssss.util.Vec3i;

/**
 * Handles the Socket mechanic where the player (a "microchip") can swap control
 * between different robotic bodies. Bodies must have a Socket component and be
 * dead (Health.hp == 0) to be entered.
 */
public class SocketSystem extends System {
    private static final Vec3i[] CARDINAL_DIRECTIONS = {
        Vec3i.NORTH, Vec3i.EAST, Vec3i.SOUTH, Vec3i.WEST
    };
    private static final Vec3i[] DIAGONAL_DIRECTIONS = {
        Vec3i.NORTHEAST, Vec3i.NORTHWEST, Vec3i.SOUTHEAST, Vec3i.SOUTHWEST
    };

    private final PositionContext m_positionContext;

    public SocketSystem(GameContext gameContext) {
        super(gameContext);
        m_positionContext = gameContext.pos();
    }

    @Override
    public void preTick() {
        // Eject events are processed in postTick() to ensure they're handled
        // in the same tick cycle as the attack that caused them.
    }

    @Override
    public void tick() {
        // Check for SOCKET events from InteractSystem
        var socketEvents = m_gameContext.events().getEvents(SocketEvent.class);
        if (socketEvents.isEmpty()) {
            return;
        }

        // Find the player entity
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug == null) {
            return;
        }

        // Process each socket event
        for (var event : socketEvents) {
            var entity = event.target();
            var socket = entity.get(Socket.class);
            if (socket == null) {
                continue;
            }

            var health = entity.get(Health.class);
            if (health == null) {
                continue;
            }

            if (health.hp != 0) {
                continue;
            }

            // Found a dead socketed robot - swap into it
            handleSocketSwap(playerEntity, socketPlug, entity, socket);

            // Log the swap
            m_gameContext.log().log("You jack into the robotic body!");
            return;
        }
    }

    @Override
    public void postTick() {
        // Process eject events first - must happen before position sync
        // This ensures forced ejects from AttackSystem are handled in the same tick
        var ejectEvents = m_gameContext.events().getEvents(EjectEvent.class);
        for (var event : ejectEvents) {
            ejectFromSocket(
                event.playerEntity(),
                event.socketPlug(),
                event.bodyEntity(),
                event.socket()
            );
        }

        // Update player position to follow their socketed body
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);

        // Check if player is socketed into a body
        if (socketPlug == null || socketPlug.currentBody == null) {
            return;
        }

        var bodyPos = socketPlug.currentBody.get(Position.class);
        if (bodyPos == null) {
            return;
        }

        var playerPos = playerWith.comp2();
        var oldPlayerPos = playerPos.getPosition().copy();
        var newPlayerPos = bodyPos.getPosition().copy();

        if (oldPlayerPos.equals(newPlayerPos)) {
            return;
        }

        playerPos.setPosition(m_gameContext, playerEntity, newPlayerPos);
    }

    /**
     * Handles swapping the player into a new socket, ejecting from the old one if needed.
     */
    private void handleSocketSwap(Entity playerEntity, SocketPlug socketPlug, Entity newBodyEntity, Socket newSocket) {
        // First, check if player is currently socketed into another body
        if (socketPlug.currentBody != null) {
            var oldSocket = socketPlug.currentBody.get(Socket.class);
            if (oldSocket != null) {
                ejectFromSocket(playerEntity, socketPlug, socketPlug.currentBody, oldSocket);
            }
        }

        // Socket player into the new body
        newSocket.socketedEntity = playerEntity;
        socketPlug.currentBody = newBodyEntity;

        // Transfer control components to the new body
        transferControlToBody(playerEntity, socketPlug, newBodyEntity);
    }

    /**
     * Ejects the player from a socketed body.
     * Restores visibility and PlayerController to the player.
     * Player's Speed component is never transferred, so no restoration needed.
     *
     * For forced ejects (when socket.destroyed is true), this method also handles
     * positioning the player in a valid adjacent tile.
     */
    public void ejectFromSocket(
            Entity playerEntity,
            SocketPlug socketPlug,
            Entity bodyEntity,
            Socket socket
    ) {
        // Handle forced eject positioning before clearing socket state
        if (socket.destroyed) {
            positionPlayerForForcedEject(playerEntity, bodyEntity);
        }

        socket.socketedEntity = null;
        socketPlug.currentBody = null;

        // Remove PlayerController from body and restore to player
        removeControlFromBody(bodyEntity);
        if (!playerEntity.has(PlayerController.class)) {
            playerEntity.add(new PlayerController());
        }

        // Show player sprite again by removing Hidden marker
        if (playerEntity.has(Hidden.class)) {
            playerEntity.removeType(Hidden.class);
        }

        // Reset body's zIndex back to non-player level (1)
        var bodyHint = bodyEntity.get(RenderingHint.class);
        if (bodyHint != null) {
            bodyHint.zIndex = 1;
        }

        m_gameContext.log().log("You disconnect from the robotic body.");
    }

    /**
     * Finds a valid position for the player during a forced eject.
     * Tries cardinal directions first, then diagonals, then allows overlap.
     */
    private void positionPlayerForForcedEject(Entity playerEntity, Entity bodyEntity) {
        var bodyPos = bodyEntity.get(Position.class);
        var playerPos = playerEntity.get(Position.class);
        if (bodyPos == null || playerPos == null) {
            return;
        }

        Vec3i bodyPosition = bodyPos.getPosition();

        // Try cardinal directions first
        for (Vec3i dir : CARDINAL_DIRECTIONS) {
            Vec3i targetPos = bodyPosition.add(dir);
            if (isPositionValidForEject(targetPos)) {
                playerPos.setPosition(m_gameContext, playerEntity, targetPos);
                return;
            }
        }

        // Try diagonal directions
        for (Vec3i dir : DIAGONAL_DIRECTIONS) {
            Vec3i targetPos = bodyPosition.add(dir);
            if (isPositionValidForEject(targetPos)) {
                playerPos.setPosition(m_gameContext, playerEntity, targetPos);
                return;
            }
        }

        // No valid position found - allow overlap with the dead mech
        // Player stays at the mech's position (already synced via postTick)
        m_gameContext.log().log("You're trapped in the wreckage!");
    }

    /**
     * Checks if a position is valid for ejecting to.
     * A position is invalid if it contains a solid entity or another Socket body.
     */
    private boolean isPositionValidForEject(Vec3i pos) {
        var entities = m_positionContext.getAt(pos);
        for (var entity : entities) {
            if (entity.has(Solid.class)) {
                return false;
            }
            if (entity.has(Socket.class)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transfers PlayerController from player to the body.
     * Hides the player sprite by adding Hidden marker.
     * Player's Speed is NOT transferred - systems query the controlled entity instead.
     */
    private void transferControlToBody(Entity playerEntity, SocketPlug socketPlug, Entity bodyEntity) {
        var playerController = playerEntity.get(PlayerController.class);
        if (playerController == null) {
            return;
        }

        // Hide player sprite by adding Hidden marker
        if (!playerEntity.has(Hidden.class)) {
            playerEntity.add(new Hidden());
        }

        // Remove PlayerController from player
        playerEntity.removeType(PlayerController.class);

        // Add PlayerController to body (needed for ECS queries like ClockSystem)
        if (!bodyEntity.has(PlayerController.class)) {
            bodyEntity.add(playerController);
        }

        // Ensure body has Velocity for movement
        if (!bodyEntity.has(Velocity.class)) {
            bodyEntity.add(new Velocity());
        }

        // Update body's zIndex to player level (2)
        var bodyHint = bodyEntity.get(RenderingHint.class);
        if (bodyHint != null) {
            bodyHint.zIndex = 2;
        }
    }

    /**
     * Removes control components from a body when the player ejects.
     */
    private void removeControlFromBody(Entity bodyEntity) {
        // Remove PlayerController if present
        if (bodyEntity.has(PlayerController.class)) {
            bodyEntity.removeType(PlayerController.class);
        }

        // Keep Speed component but mark as unable to act
        var speed = bodyEntity.get(Speed.class);
        if (speed == null) {
            return;
        }

        speed.canAct = false;
    }
}
