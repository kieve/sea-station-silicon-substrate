package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InteractContext;
import ca.kieve.ssss.context.InteractContext.Phase;
import ca.kieve.ssss.event.Interaction;
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

            var playerPos = getPlayerPosition();
            if (playerPos == null) {
                return;
            }

            m_input.setMode(MODE_INTERACT);
            m_interactContext.enter(playerPos);
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

        var playerPos = getPlayerPosition();
        if (playerPos == null) {
            return;
        }

        Vec3i targetPos = selfTile ? playerPos : playerPos.add(direction);
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

        if (m_input.consume(CONFIRM)) {
            var interactions = m_interactContext.getInteractions();
            int index = m_interactContext.getSelectedIndex();
            if (index < interactions.size()) {
                executeInteraction(interactions.get(index));
            }
        }
    }

    private void executeInteraction(Interaction interaction) {
        switch (interaction.verb()) {
        case "Pick up" -> pickUp(interaction.entity());
        }

        m_input.setMode(MODE_NORMAL);
        m_interactContext.exit();
    }

    private void pickUp(Entity entity) {
        var playerEntity = getPlayerEntity();
        if (playerEntity == null) {
            return;
        }

        var inventory = playerEntity.get(Inventory.class);
        if (inventory == null) {
            m_gameContext.log().log("You have no inventory.");
            return;
        }

        var position = entity.get(Position.class);
        if (position != null) {
            position.cleanup(m_gameContext, entity);
            entity.removeType(Position.class);
        }

        inventory.items().add(entity);

        var descriptor = entity.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        m_gameContext.log().log("Picked up " + name);

        IO.println("Inventory:");
        for (var item : inventory.items()) {
            var desc = item.get(Descriptor.class);
            String itemName = desc != null ? desc.name() : "???";
            IO.println("  - " + itemName);
        }

        var playerSpeed = getControlledEntity().get(Speed.class);
        int speedVal = playerSpeed != null ? playerSpeed.val : 100;
        m_clock.processPlayerActed(speedVal);
    }

    private Entity getPlayerEntity() {
        var playerResults =
            m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return null;
        }
        return optionalPlayer.get().entity();
    }

    private Entity getControlledEntity() {
        var playerEntity = getPlayerEntity();
        if (playerEntity == null) {
            return null;
        }
        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug != null && socketPlug.currentBody != null) {
            return socketPlug.currentBody;
        }
        return playerEntity;
    }

    private Vec3i getPlayerPosition() {
        var playerResults =
            m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return null;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);

        if (socketPlug != null && socketPlug.currentBody != null) {
            var bodyPos = socketPlug.currentBody.get(Position.class);
            if (bodyPos != null) {
                return bodyPos.getPosition().copy();
            }
        }

        return playerWith.comp2().getPosition().copy();
    }
}
