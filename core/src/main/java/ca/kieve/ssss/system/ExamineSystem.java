package ca.kieve.ssss.system;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_EXAMINE;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.EXAMINE;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.*;
import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

import java.util.ArrayList;
import java.util.List;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.event.ExamineEvent;
import ca.kieve.ssss.util.DescriptionComposer;
import ca.kieve.ssss.util.Vec3i;

public class ExamineSystem extends System {
    private final InputContext m_input;

    /**
     * Represents an item in the examine selection list.
     * Can be an entity at main level, floor level, or ceiling level.
     */
    public record ExamineItem(Entity entity, ItemType type) {
        public enum ItemType {
            MAIN,
            FLOOR,
            CEILING
        }
    }

    public ExamineSystem(GameContext gameContext) {
        super(gameContext);
        m_input = gameContext.input();
    }

    @Override
    public void awaitingUserInput() {
        var examineContext = m_gameContext.examine();

        if (m_input.consume(EXAMINE)) {
            if (m_input.isMode(MODE_EXAMINE)) {
                m_input.setMode(MODE_NORMAL);
                examineContext.exit();
                return;
            }

            if (!m_input.isMode(MODE_NORMAL)) {
                return;
            }

            var playerPos = getPlayerPosition();
            if (playerPos != null) {
                m_input.setMode(MODE_EXAMINE);
                examineContext.enter(playerPos);
            }
            return;
        }

        if (!m_input.isMode(MODE_EXAMINE)) {
            return;
        }

        if (m_input.consume(CANCEL)) {
            m_input.setMode(MODE_NORMAL);
            examineContext.exit();
            return;
        }

        if (examineContext.isSelectionMode()) {
            int itemCount = getExamineItems(examineContext).size();
            if (m_input.consume(UP)) {
                examineContext.decrementSelectedIndex(itemCount);
            }
            if (m_input.consume(DOWN)) {
                examineContext.incrementSelectedIndex(itemCount);
            }
            m_input.consume(LEFT);
            m_input.consume(RIGHT);
        } else {
            if (m_input.consume(UP)) {
                examineContext.moveCrosshair(NORTH);
            }
            if (m_input.consume(LEFT)) {
                examineContext.moveCrosshair(WEST);
            }
            if (m_input.consume(DOWN)) {
                examineContext.moveCrosshair(SOUTH);
            }
            if (m_input.consume(RIGHT)) {
                examineContext.moveCrosshair(EAST);
            }
        }

        if (m_input.consume(CONFIRM)) {
            handleEnterKey(examineContext);
        }
    }

    @Override
    public void postTick() {
        var examineEvents = m_gameContext.events().getEvents(ExamineEvent.class);
        for (var event : examineEvents) {
            var entity = event.target();
            var descriptor = entity.get(Descriptor.class);
            if (descriptor != null) {
                logDescription(entity);
            }
        }
    }

    private List<ExamineItem> getExamineItems(ExamineContext examineContext) {
        var items = new ArrayList<ExamineItem>();

        var mainPos = examineContext.getCrosshairPos();
        var mainEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(mainPos)
        );
        for (var entity : mainEntities) {
            items.add(new ExamineItem(entity, MAIN));
        }

        var ceilingPos = examineContext.getCeilingPos();
        var ceilingEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(ceilingPos)
        );
        for (var entity : ceilingEntities) {
            items.add(new ExamineItem(entity, CEILING));
        }

        var floorPos = examineContext.getFloorPos();
        var floorEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(floorPos)
        );
        for (var entity : floorEntities) {
            items.add(new ExamineItem(entity, FLOOR));
        }

        return items;
    }

    private void handleEnterKey(ExamineContext examineContext) {
        var items = getExamineItems(examineContext);

        if (items.isEmpty()) {
            return;
        }

        if (!examineContext.isSelectionMode()) {
            if (items.size() == 1) {
                logExamineItem(items.getFirst());
            } else {
                examineContext.enterSelectionMode();
            }
            return;
        }

        int selectedIndex = examineContext.getSelectedIndex();
        if (selectedIndex < items.size()) {
            logExamineItem(items.get(selectedIndex));
            examineContext.exitSelectionMode();
        }
    }

    private void logExamineItem(ExamineItem item) {
        String prefix = switch (item.type()) {
            case MAIN -> "";
            case FLOOR -> "[Floor] ";
            case CEILING -> "[Ceiling] ";
        };
        m_gameContext.log().log(prefix + DescriptionComposer.compose(item.entity()));
    }

    private void logDescription(Entity entity) {
        m_gameContext.log().log(DescriptionComposer.compose(entity));
    }

    private Vec3i getPlayerPosition() {
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var playerResult = playerResults.stream().findFirst();
        if (playerResult.isEmpty()) {
            return null;
        }

        var playerWith = playerResult.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);

        // If socketed, return the body's position
        if (socketPlug != null && socketPlug.currentBody != null) {
            var bodyPos = socketPlug.currentBody.get(Position.class);
            if (bodyPos != null) {
                return bodyPos.getPosition().copy();
            }
        }

        // Return the player's position
        return playerWith.comp2().getPosition().copy();
    }
}
