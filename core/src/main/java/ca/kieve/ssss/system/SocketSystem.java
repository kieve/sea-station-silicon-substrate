package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.EventType;
import dev.dominion.ecs.api.Entity;

/**
 * Handles the Socket mechanic where the player (a "microchip") can swap control
 * between different robotic bodies. Bodies must have a Socket component and be
 * dead (Health.hp == 0) to be entered.
 */
public class SocketSystem extends System {
    public SocketSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        // Check for SOCKET events from InteractSystem
        var socketEvents = m_gameContext.events().getEvents(EventType.SOCKET);
        if (socketEvents.isEmpty()) {
            return;
        }

        // Find the player entity
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

        // Process each socket event
        for (var entity : socketEvents) {
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
        // Update player position to follow their socketed body
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

        // Check if player is socketed into a body
        if (socketPlug.currentBody == null) {
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
     * Restores control components and sprite to the player.
     */
    private void ejectFromSocket(Entity playerEntity, SocketPlug socketPlug, Entity bodyEntity, Socket socket) {
        socket.socketedEntity = null;
        socketPlug.currentBody = null;

        // Remove control components from the old body and transfer back to player
        var wasdController = bodyEntity.get(WasdController.class);
        var bodySpeed = bodyEntity.get(Speed.class);

        removeControlFromBody(bodyEntity);

        // Restore control components to player
        if (wasdController != null && !playerEntity.has(WasdController.class)) {
            playerEntity.add(wasdController);
        }
        if (bodySpeed != null && !playerEntity.has(Speed.class)) {
            playerEntity.add(new Speed(bodySpeed.val));
        }

        // Restore TileGlyph to make player visible again
        var playerContext = m_gameContext.player();
        if (playerContext.tileGlyph != null) {
            playerEntity.add(playerContext.tileGlyph);
            playerContext.tileGlyph = null;
        }

        // Reset body's zIndex back to non-player level (1)
        var bodyHint = bodyEntity.get(RenderingHint.class);
        if (bodyHint != null) {
            bodyHint.zIndex = 1;
        }

        m_gameContext.log().log("You disconnect from the robotic body.");
    }

    /**
     * Transfers control components (WasdController, Speed) from player to the body.
     * Also hides the player sprite by removing TileGlyph.
     */
    private void transferControlToBody(Entity playerEntity, SocketPlug socketPlug, Entity bodyEntity) {
        var wasdController = playerEntity.get(WasdController.class);
        if (wasdController == null) {
            return;
        }

        var playerSpeed = playerEntity.get(Speed.class);

        // Cache and remove TileGlyph from player to hide the sprite
        // Keep DebugRect visible so we can see where the player entity is
        var playerGlyph = playerEntity.get(TileGlyph.class);
        if (playerGlyph != null) {
            m_gameContext.player().tileGlyph = playerGlyph;
            playerEntity.removeType(TileGlyph.class);
        }

        // Remove control components from player
        playerEntity.removeType(WasdController.class);
        if (playerSpeed != null) {
            playerEntity.removeType(Speed.class);
        }

        // Add control components to the body if it doesn't have them
        if (!bodyEntity.has(WasdController.class)) {
            bodyEntity.add(wasdController);
        }
        if (!bodyEntity.has(Speed.class) && playerSpeed != null) {
            bodyEntity.add(new Speed(playerSpeed.val));
        }
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
        // Remove WasdController if present
        if (bodyEntity.has(WasdController.class)) {
            bodyEntity.removeType(WasdController.class);
        }

        // Keep Speed component but mark as unable to act
        var speed = bodyEntity.get(Speed.class);
        if (speed == null) {
            return;
        }

        speed.canAct = false;
    }
}
