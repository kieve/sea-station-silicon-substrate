package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InventoryContext;
import ca.kieve.ssss.context.InventoryContext.PaneLocation;
import ca.kieve.ssss.context.InventoryContext.PaneSide;
import ca.kieve.ssss.context.LogContext;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.InventoryUtil;
import ca.kieve.ssss.util.PlayerUtil;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_INVENTORY;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.INVENTORY;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.SWITCH_PANE;
import static ca.kieve.ssss.input.InputAction.UP;

public class InventorySystem extends System {
    private static final InputAction[] LOCATION_ACTIONS = {
        InputAction.N,
        InputAction.S,
        InputAction.E,
        InputAction.W,
        InputAction.NE,
        InputAction.NW,
        InputAction.SE,
        InputAction.SW,
        InputAction.ORIGIN,
        InputAction.SELF
    };

    private final Dominion m_ecs;
    private final InputContext m_input;
    private final InventoryContext m_inventory;
    private final LogContext m_log;

    public InventorySystem(GameContext gameContext) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_input = gameContext.input();
        m_inventory = gameContext.inventory();
        m_log = gameContext.log();
    }

    @Override
    public void awaitingUserInput() {
        if (m_input.consume(INVENTORY)) {
            if (m_input.isMode(MODE_NORMAL)) {
                m_input.setMode(MODE_INVENTORY);
                m_inventory.enter();
            }
            return;
        }

        if (!m_input.isMode(MODE_INVENTORY)) {
            return;
        }

        if (m_input.consume(CANCEL)) {
            closeInventory();
            return;
        }

        if (m_input.consume(SWITCH_PANE)) {
            m_inventory.swapActiveSide();
        }

        for (InputAction locationAction : LOCATION_ACTIONS) {
            if (!m_input.consume(locationAction)) {
                continue;
            }
            var location = InventoryContext.locationFor(locationAction);
            if (location != null) {
                m_inventory.setLocation(m_inventory.getActiveSide(), location);
            }
            break;
        }

        var activeSide = m_inventory.getActiveSide();
        int itemCount = m_inventory
            .getItemsAt(m_gameContext, m_inventory.getLocation(activeSide))
            .size();

        if (m_input.consume(UP)) {
            m_inventory.decrementCursor(activeSide, itemCount);
        }
        if (m_input.consume(DOWN)) {
            m_inventory.incrementCursor(activeSide, itemCount);
        }
        m_input.consume(LEFT);
        m_input.consume(RIGHT);

        if (m_input.consume(CONFIRM)) {
            tryMove();
        }
    }

    private void closeInventory() {
        m_input.setMode(MODE_NORMAL);
        m_inventory.exit();
    }

    private void tryMove() {
        var sourceSide = m_inventory.getActiveSide();
        var targetSide = (sourceSide == PaneSide.LEFT) ? PaneSide.RIGHT : PaneSide.LEFT;
        var sourceLoc = m_inventory.getLocation(sourceSide);
        var targetLoc = m_inventory.getLocation(targetSide);

        if (sourceLoc == targetLoc) {
            m_log.log("Source and destination are the same.");
            return;
        }

        var sourceItems = m_inventory.getItemsAt(m_gameContext, sourceLoc);
        if (sourceItems.isEmpty()) {
            return;
        }
        int cursor = m_inventory.getCursor(sourceSide);
        if (cursor < 0 || cursor >= sourceItems.size()) {
            return;
        }
        var item = sourceItems.get(cursor);
        var playerEntity = PlayerUtil.getPlayerEntity(m_ecs);
        if (playerEntity == null) {
            return;
        }

        boolean success = executeMove(playerEntity, item, sourceLoc, targetLoc);
        if (!success) {
            return;
        }

        var controlled = PlayerUtil.getControlledEntity(m_ecs);
        var speed = controlled != null ? controlled.get(Speed.class) : null;
        int speedVal = speed != null ? speed.val : 100;
        m_clock.processPlayerActed(speedVal);

        int newSourceCount = m_inventory.getItemsAt(m_gameContext, sourceLoc).size();
        m_inventory.clampCursor(sourceSide, newSourceCount);
    }

    private boolean executeMove(
        Entity player,
        Entity item,
        PaneLocation sourceLoc,
        PaneLocation targetLoc
    ) {
        if (sourceLoc == PaneLocation.PLAYER_INVENTORY) {
            var dropPos = m_inventory.resolveTilePos(m_gameContext, targetLoc);
            if (dropPos == null) {
                return false;
            }
            return InventoryUtil.drop(m_gameContext, player, item, dropPos);
        }
        if (targetLoc == PaneLocation.PLAYER_INVENTORY) {
            return InventoryUtil.pickUp(m_gameContext, player, item);
        }
        var toPos = m_inventory.resolveTilePos(m_gameContext, targetLoc);
        if (toPos == null) {
            return false;
        }
        return InventoryUtil.moveOnGround(m_gameContext, item, toPos);
    }
}
