package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.EjectContext;
import ca.kieve.ssss.context.EventContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.event.EjectEvent;
import ca.kieve.ssss.util.Vec3i;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_EJECT;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.EJECT;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

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
        if (m_input.consume(EJECT)) {
            if (m_input.isMode(MODE_EJECT)) {
                // Cancel eject mode
                m_input.setMode(MODE_NORMAL);
                m_ejectContext.exit();
                return;
            }

            // Only enter eject mode from normal mode
            if (!m_input.isMode(MODE_NORMAL)) {
                return;
            }

            // Try to enter eject mode - only if socketed
            if (!tryEnterEjectMode()) {
                return;
            }
            return;
        }

        if (!m_input.isMode(MODE_EJECT)) {
            return;
        }

        // Handle direction selection
        Vec3i selectedDirection = null;
        if (m_input.consume(UP)) {
            selectedDirection = NORTH;
        } else if (m_input.consume(LEFT)) {
            selectedDirection = WEST;
        } else if (m_input.consume(DOWN)) {
            selectedDirection = SOUTH;
        } else if (m_input.consume(RIGHT)) {
            selectedDirection = EAST;
        }

        if (selectedDirection == null) {
            return;
        }

        if (!m_ejectContext.isDirectionValid(selectedDirection)) {
            m_gameContext.log().log("There's no room for you there.");
            return;
        }

        performEject(selectedDirection);
    }

    private boolean tryEnterEjectMode() {
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

        if (socketPlug.currentBody == null) {
            return false;
        }

        var bodyPos = socketPlug.currentBody.get(Position.class);
        if (bodyPos == null) {
            return false;
        }

        m_input.setMode(MODE_EJECT);
        m_ejectContext.enter(bodyPos.getPosition());
        return true;
    }

    private void performEject(Vec3i direction) {
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

        var bodySpeed = bodyEntity.get(Speed.class);
        int speedVal = bodySpeed != null ? bodySpeed.val : 100;

        m_eventContext.addEvent(new EjectEvent(playerEntity, socketPlug, bodyEntity, socket));

        // Restore player's TileGlyph immediately so they appear without delay
        var playerContext = m_gameContext.player();
        if (playerContext.tileGlyph != null) {
            playerEntity.add(playerContext.tileGlyph);
            playerContext.tileGlyph = null;
        }

        Vec3i newPos = m_ejectContext.getCurrentPos().add(direction);
        playerPos.setPosition(m_gameContext, playerEntity, newPos);

        m_input.setMode(MODE_NORMAL);
        m_ejectContext.exit();

        m_clock.processPlayerActed(speedVal);
    }
}
