package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.EjectContext;
import ca.kieve.ssss.context.EventContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InputContext.Mode;
import ca.kieve.ssss.event.EjectEvent;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

public class EjectSystem extends System {
    private final InputContext m_input;
    private final EjectContext m_ejectContext;
    private final EventContext m_eventContext;

    public EjectSystem(GameContext gameContext) {
        super(gameContext);
        m_input = gameContext.input();
        m_ejectContext = gameContext.eject();
        m_eventContext = gameContext.events();
    }

    @Override
    public void awaitingUserInput() {
        // Handle Q key to toggle eject mode
        if (m_input.consume(InputAction.EJECT)) {
            if (m_input.isMode(Mode.EJECT)) {
                // Cancel eject mode
                m_input.setMode(Mode.NORMAL);
                m_ejectContext.exit();
                return;
            }

            // Only enter eject mode from normal mode
            if (!m_input.isMode(Mode.NORMAL)) {
                return;
            }

            // Try to enter eject mode - only if socketed
            if (!tryEnterEjectMode()) {
                return;
            }
            return;
        }

        if (!m_input.isMode(Mode.EJECT)) {
            return;
        }

        // Handle direction selection
        Vec3i selectedDirection = null;
        if (m_input.consume(InputAction.UP)) {
            selectedDirection = Vec3i.NORTH;
        } else if (m_input.consume(InputAction.LEFT)) {
            selectedDirection = Vec3i.WEST;
        } else if (m_input.consume(InputAction.DOWN)) {
            selectedDirection = Vec3i.SOUTH;
        } else if (m_input.consume(InputAction.RIGHT)) {
            selectedDirection = Vec3i.EAST;
        }

        if (selectedDirection == null) {
            return;
        }

        // Attempt to eject in the selected direction
        if (!m_ejectContext.isDirectionValid(selectedDirection)) {
            m_gameContext.log().log("There's no room for you there.");
            return;
        }

        // Perform the eject
        performEject(selectedDirection);
    }

    private boolean tryEnterEjectMode() {
        // Find the player entity
        var playerResults = m_gameContext.ecs().findEntitiesWith(
            SocketPlug.class,
            Position.class
        );

        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return false;
        }

        var playerWith = optionalPlayer.get();
        var socketPlug = playerWith.comp1();

        // Check if player is socketed
        if (socketPlug.currentBody == null) {
            return false;
        }

        // Get the body's position
        var bodyPos = socketPlug.currentBody.get(Position.class);
        if (bodyPos == null) {
            return false;
        }

        // Enter eject mode
        m_input.setMode(Mode.EJECT);
        m_ejectContext.enter(bodyPos.getPosition());
        return true;
    }

    private void performEject(Vec3i direction) {
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
        var playerPos = playerWith.comp2();

        Entity bodyEntity = socketPlug.currentBody;
        if (bodyEntity == null) {
            return;
        }

        var socket = bodyEntity.get(Socket.class);
        if (socket == null) {
            return;
        }

        // Get speed before ejecting (from the body)
        var bodySpeed = bodyEntity.get(Speed.class);
        int speedVal = bodySpeed != null ? bodySpeed.val : 100;

        // Create eject event for SocketSystem to process
        m_eventContext.addEvent(new EjectEvent(playerEntity, socketPlug, bodyEntity, socket));

        // Restore player's TileGlyph immediately so they appear without delay
        var playerContext = m_gameContext.player();
        if (playerContext.tileGlyph != null) {
            playerEntity.add(playerContext.tileGlyph);
            playerContext.tileGlyph = null;
        }

        // Move player to the eject position
        Vec3i newPos = m_ejectContext.getCurrentPos().add(direction);
        playerPos.setPosition(m_gameContext, playerEntity, newPos);

        // Exit eject mode
        m_input.setMode(Mode.NORMAL);
        m_ejectContext.exit();

        // Advance time
        m_clock.processPlayerActed(speedVal);
    }
}
