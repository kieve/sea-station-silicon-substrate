package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InputContext.Mode;
import ca.kieve.ssss.event.ExamineEvent;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.DescriptionComposer;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.List;

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

        // Handle E key to toggle examine mode
        if (m_input.consume(InputAction.EXAMINE)) {
            if (m_input.isMode(Mode.EXAMINE)) {
                m_input.setMode(Mode.NORMAL);
                examineContext.exit();
                return;
            }

            // Only enter examine mode from normal mode
            if (!m_input.isMode(Mode.NORMAL)) {
                return;
            }

            // Enter examine mode - start at player position
            var playerPos = getPlayerPosition();
            if (playerPos != null) {
                m_input.setMode(Mode.EXAMINE);
                examineContext.enter(playerPos);
            }
            return;
        }

        if (!m_input.isMode(Mode.EXAMINE)) {
            return;
        }

        // Handle Escape to exit examine mode
        if (m_input.consume(InputAction.CANCEL)) {
            m_input.setMode(Mode.NORMAL);
            examineContext.exit();
            return;
        }

        // Handle WASD based on current mode
        if (examineContext.isSelectionMode()) {
            // In selection mode, W/S controls the selector
            int itemCount = getExamineItems(examineContext).size();
            if (m_input.consume(InputAction.UP)) {
                examineContext.decrementSelectedIndex(itemCount);
            }
            if (m_input.consume(InputAction.DOWN)) {
                examineContext.incrementSelectedIndex(itemCount);
            }
            // Consume LEFT and RIGHT to prevent them from doing anything
            m_input.consume(InputAction.LEFT);
            m_input.consume(InputAction.RIGHT);
        } else {
            // Normal mode: WASD moves crosshair
            if (m_input.consume(InputAction.UP)) {
                examineContext.moveCrosshair(Vec3i.NORTH);
            }
            if (m_input.consume(InputAction.LEFT)) {
                examineContext.moveCrosshair(Vec3i.WEST);
            }
            if (m_input.consume(InputAction.DOWN)) {
                examineContext.moveCrosshair(Vec3i.SOUTH);
            }
            if (m_input.consume(InputAction.RIGHT)) {
                examineContext.moveCrosshair(Vec3i.EAST);
            }
        }

        // Handle Enter key for selection
        if (m_input.consume(InputAction.CONFIRM)) {
            handleEnterKey(examineContext);
        }
    }

    @Override
    public void postTick() {
        // Consume EXAMINE events and log their descriptions
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

        // Get entities at main level (crosshair position)
        var mainPos = examineContext.getCrosshairPos();
        var mainEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(mainPos)
        );
        for (var entity : mainEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.MAIN));
        }

        // Get entities at ceiling level (z+1)
        var ceilingPos = examineContext.getCeilingPos();
        var ceilingEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(ceilingPos)
        );
        for (var entity : ceilingEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.CEILING));
        }

        // Get entities at floor level (z-1)
        var floorPos = examineContext.getFloorPos();
        var floorEntities = ExamineContext.sortEntitiesByZIndex(
            m_gameContext.pos().getAt(floorPos)
        );
        for (var entity : floorEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.FLOOR));
        }

        return items;
    }

    private void handleEnterKey(ExamineContext examineContext) {
        var items = getExamineItems(examineContext);

        if (items.isEmpty()) {
            return;
        }

        if (!examineContext.isSelectionMode()) {
            // Not in selection mode
            if (items.size() == 1) {
                // Single item - auto-select and log (stay in examine mode)
                logExamineItem(items.get(0));
            } else {
                // Multiple items - enter selection mode
                examineContext.enterSelectionMode();
            }
            return;
        }

        // In selection mode - confirm selection (exit selection mode, stay in examine)
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
        // Try to find player via SocketPlug
        var plugResults = m_gameContext.ecs().findEntitiesWith(
            SocketPlug.class,
            Position.class
        );

        var plugResult = plugResults.stream().findFirst();
        if (plugResult.isPresent()) {
            var socketPlug = plugResult.get().comp1();
            // If socketed into a body, use that body's position
            if (socketPlug.currentBody != null) {
                var bodyPos = socketPlug.currentBody.get(Position.class);
                if (bodyPos != null) {
                    return bodyPos.getPosition().copy();
                }
            }
            // Otherwise use player's own position
            return plugResult.get().comp2().getPosition().copy();
        }

        // Fallback: find entity with PlayerController
        var playerResults = m_gameContext.ecs().findEntitiesWith(
            PlayerController.class,
            Position.class
        );

        var playerResult = playerResults.stream().findFirst();
        if (playerResult.isPresent()) {
            return playerResult.get().comp2().getPosition().copy();
        }

        return null;
    }
}
