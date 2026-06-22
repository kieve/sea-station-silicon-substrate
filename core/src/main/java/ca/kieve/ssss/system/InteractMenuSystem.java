package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InteractContext;
import ca.kieve.ssss.context.InteractContext.Phase;
import ca.kieve.ssss.event.Interaction;
import ca.kieve.ssss.util.InventoryUtil;
import ca.kieve.ssss.util.LockableUtil;
import ca.kieve.ssss.util.OpenableUtil;
import ca.kieve.ssss.util.PlayerUtil;
import ca.kieve.ssss.util.Vec3i;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_INTERACT;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.INTERACT;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.input.InputAction.WAIT;
import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

public class InteractMenuSystem extends System {
    private final InputContext m_input;
    private final InteractContext m_interactContext;

    public InteractMenuSystem(GameContext gameContext) {
        super(gameContext);
        m_input = gameContext.input();
        m_interactContext = gameContext.interact();
    }

    @Override
    public void awaitingUserInput() {
        if (m_input.consume(INTERACT)) {
            if (m_input.isMode(MODE_INTERACT)) {
                m_input.setMode(MODE_NORMAL);
                m_interactContext.exit();
                return;
            }

            if (!m_input.isMode(MODE_NORMAL)) {
                return;
            }

            var playerPos = PlayerUtil.getControlledPosition(m_gameContext.ecs());
            if (playerPos == null) {
                return;
            }

            m_input.setMode(MODE_INTERACT);
            m_interactContext.enter(playerPos);

            if (!m_interactContext.hasAnyInteractable()) {
                m_gameContext.log().log("Nothing to interact with here.");
                m_input.setMode(MODE_NORMAL);
                m_interactContext.exit();
                return;
            }

            var singleDirection = m_interactContext.getSingleValidDirection();
            if (singleDirection != null) {
                selectAndProcess(playerPos.add(singleDirection));
            }
            return;
        }

        if (!m_input.isMode(MODE_INTERACT)) {
            return;
        }

        if (m_input.consume(CANCEL)) {
            m_input.setMode(MODE_NORMAL);
            m_interactContext.exit();
            return;
        }

        if (m_interactContext.getPhase() == Phase.DIRECTION_SELECT) {
            handleDirectionSelect();
        } else {
            handleItemSelect();
        }
    }

    private void handleDirectionSelect() {
        Vec3i direction = null;
        boolean selfTile = false;

        if (m_input.consume(UP)) {
            direction = NORTH;
        } else if (m_input.consume(DOWN)) {
            direction = SOUTH;
        } else if (m_input.consume(LEFT)) {
            direction = WEST;
        } else if (m_input.consume(RIGHT)) {
            direction = EAST;
        } else if (m_input.consume(WAIT)) {
            selfTile = true;
        }

        if (direction == null && !selfTile) {
            return;
        }

        var playerPos = PlayerUtil.getControlledPosition(m_gameContext.ecs());
        if (playerPos == null) {
            return;
        }

        Vec3i targetPos = selfTile ? playerPos : playerPos.add(direction);
        selectAndProcess(targetPos);
    }

    private void selectAndProcess(Vec3i targetPos) {
        m_interactContext.selectTarget(targetPos);

        var interactions = m_interactContext.getInteractions();
        if (interactions.isEmpty()) {
            m_gameContext.log().log("Nothing to interact with here.");
            m_input.setMode(MODE_NORMAL);
            m_interactContext.exit();
            return;
        }

        if (interactions.size() == 1) {
            executeInteraction(interactions.getFirst());
            return;
        }

        m_interactContext.enterItemSelect();
    }

    private void handleItemSelect() {
        if (m_input.consume(UP)) {
            m_interactContext.decrementSelectedIndex();
        }
        if (m_input.consume(DOWN)) {
            m_interactContext.incrementSelectedIndex();
        }
        m_input.consume(LEFT);
        m_input.consume(RIGHT);

        if (!m_input.consume(CONFIRM)) {
            return;
        }
        var interactions = m_interactContext.getInteractions();
        int index = m_interactContext.getSelectedIndex();
        if (index < interactions.size()) {
            executeInteraction(interactions.get(index));
        }
    }

    private void executeInteraction(Interaction interaction) {
        var ecs = m_gameContext.ecs();
        var playerEntity = PlayerUtil.getPlayerEntity(ecs);

        boolean success = switch (interaction.verb()) {
        case PICK_UP ->
            InventoryUtil.pickUp(m_gameContext, playerEntity, interaction.entity());
        case OPEN -> {
            OpenableUtil.open(m_gameContext, interaction.entity());
            yield true;
        }
        case CLOSE ->
            OpenableUtil.tryClose(m_gameContext, interaction.entity());
        case UNLOCK ->
            LockableUtil.unlock(m_gameContext, playerEntity, interaction.entity());
        case LOCK ->
            LockableUtil.lock(m_gameContext, interaction.entity());
        };

        if (success) {
            var controlled = PlayerUtil.getControlledEntity(ecs);
            var playerSpeed = controlled != null
                ? controlled.get(Speed.class)
                : null;
            int speedVal = playerSpeed != null ? playerSpeed.val : 100;
            m_clock.processPlayerActed(speedVal);
        }

        m_input.setMode(MODE_NORMAL);
        m_interactContext.exit();
    }
}
