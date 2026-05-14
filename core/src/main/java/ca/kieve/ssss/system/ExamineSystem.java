package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.EventContext;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.GhostEntity;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.LogContext;
import ca.kieve.ssss.context.PositionContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.event.ExamineEvent;
import ca.kieve.ssss.util.DescriptionComposer;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_EXAMINE;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.EXAMINE;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.CEILING;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.FLOOR;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.MAIN;
import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

public class ExamineSystem extends System {
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

    private enum ExamineMode {
        VISIBLE,
        MEMORY,
        UNKNOWN
    }

    private final Dominion m_ecs;
    private final InputContext m_input;
    private final EventContext m_events;
    private final ExamineContext m_examine;
    private final VisionContext m_vision;
    private final PositionContext m_pos;
    private final LogContext m_log;

    public ExamineSystem(GameContext gameContext) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_input = gameContext.input();
        m_events = gameContext.events();
        m_examine = gameContext.examine();
        m_vision = gameContext.vision();
        m_pos = gameContext.pos();
        m_log = gameContext.log();
    }

    @Override
    public void awaitingUserInput() {
        if (m_input.consume(EXAMINE)) {
            if (m_input.isMode(MODE_EXAMINE)) {
                m_input.setMode(MODE_NORMAL);
                m_examine.exit();
                return;
            }

            if (!m_input.isMode(MODE_NORMAL)) {
                return;
            }

            var playerPos = getPlayerPosition();
            if (playerPos != null) {
                m_input.setMode(MODE_EXAMINE);
                m_examine.enter(playerPos);
            }
            return;
        }

        if (!m_input.isMode(MODE_EXAMINE)) {
            return;
        }

        if (m_input.consume(CANCEL)) {
            m_input.setMode(MODE_NORMAL);
            m_examine.exit();
            return;
        }

        if (m_examine.isSelectionMode()) {
            int itemCount = getItemCount();
            if (m_input.consume(UP)) {
                m_examine.decrementSelectedIndex(itemCount);
            }
            if (m_input.consume(DOWN)) {
                m_examine.incrementSelectedIndex(itemCount);
            }
            m_input.consume(LEFT);
            m_input.consume(RIGHT);
        } else {
            if (m_input.consume(UP)) {
                m_examine.moveCrosshair(NORTH);
            }
            if (m_input.consume(LEFT)) {
                m_examine.moveCrosshair(WEST);
            }
            if (m_input.consume(DOWN)) {
                m_examine.moveCrosshair(SOUTH);
            }
            if (m_input.consume(RIGHT)) {
                m_examine.moveCrosshair(EAST);
            }
        }

        if (m_input.consume(CONFIRM)) {
            handleEnterKey();
        }
    }

    @Override
    public void postTick() {
        var examineEvents = m_events.getEvents(ExamineEvent.class);
        for (var event : examineEvents) {
            var entity = event.target();
            var descriptor = entity.get(Descriptor.class);
            if (descriptor != null) {
                logDescription(entity);
            }
        }
    }

    private ExamineMode getMode() {
        var crosshair = m_examine.getCrosshairPos();
        if (m_vision.isVisible(crosshair.x, crosshair.y)) {
            return ExamineMode.VISIBLE;
        }
        if (m_vision.isExplored(crosshair.x, crosshair.y, crosshair.z)) {
            return ExamineMode.MEMORY;
        }
        return ExamineMode.UNKNOWN;
    }

    private int getItemCount() {
        return switch (getMode()) {
        case VISIBLE -> getExamineItems().size();
        case MEMORY -> getGhostItems().size();
        case UNKNOWN -> 0;
        };
    }

    private List<GhostEntity> getGhostItems() {
        var crosshair = m_examine.getCrosshairPos();
        var ghost = m_vision.getGhost(crosshair.x, crosshair.y, crosshair.z);
        if (ghost == null) {
            return List.of();
        }
        return ghost.entities;
    }

    private List<ExamineItem> getExamineItems() {
        var items = new ArrayList<ExamineItem>();

        var mainPos = m_examine.getCrosshairPos();
        var mainEntities = ExamineContext.sortEntitiesByZIndex(m_pos.getAt(mainPos));
        for (var entity : mainEntities) {
            items.add(new ExamineItem(entity, MAIN));
        }

        var ceilingPos = m_examine.getCeilingPos();
        var ceilingEntities = ExamineContext.sortEntitiesByZIndex(m_pos.getAt(ceilingPos));
        for (var entity : ceilingEntities) {
            items.add(new ExamineItem(entity, CEILING));
        }

        var floorPos = m_examine.getFloorPos();
        var floorEntities = ExamineContext.sortEntitiesByZIndex(m_pos.getAt(floorPos));
        for (var entity : floorEntities) {
            items.add(new ExamineItem(entity, FLOOR));
        }

        return items;
    }

    private void handleEnterKey() {
        switch (getMode()) {
        case VISIBLE -> handleVisibleEnterKey();
        case MEMORY -> handleGhostEnterKey();
        case UNKNOWN -> logNothingHere();
        }
    }

    private void handleVisibleEnterKey() {
        var items = getExamineItems();

        if (items.isEmpty()) {
            logNothingHere();
            return;
        }

        if (!m_examine.isSelectionMode()) {
            if (items.size() == 1) {
                logExamineItem(items.getFirst());
            } else {
                m_examine.enterSelectionMode();
            }
            return;
        }

        int selectedIndex = m_examine.getSelectedIndex();
        if (selectedIndex < items.size()) {
            logExamineItem(items.get(selectedIndex));
            m_examine.exitSelectionMode();
        }
    }

    private void handleGhostEnterKey() {
        var items = getGhostItems();

        if (items.isEmpty()) {
            logNothingHere();
            return;
        }

        if (!m_examine.isSelectionMode()) {
            if (items.size() == 1) {
                logGhostItem(items.getFirst());
            } else {
                m_examine.enterSelectionMode();
            }
            return;
        }

        int selectedIndex = m_examine.getSelectedIndex();
        if (selectedIndex < items.size()) {
            logGhostItem(items.get(selectedIndex));
            m_examine.exitSelectionMode();
        }
    }

    private void logNothingHere() {
        m_log.log("You don't see anything there.");
    }

    private void logExamineItem(ExamineItem item) {
        String prefix = switch (item.type()) {
        case MAIN -> "";
        case FLOOR -> "[Floor] ";
        case CEILING -> "[Ceiling] ";
        };
        m_log.log(prefix + DescriptionComposer.compose(item.entity()));
    }

    private void logGhostItem(GhostEntity ghost) {
        String prefix = switch (ghost.type()) {
        case MAIN -> "[Memory] ";
        case FLOOR -> "[Memory] [Floor] ";
        case CEILING -> "[Memory] [Ceiling] ";
        };
        m_log.log(prefix + ghost.composedDescription());
    }

    private void logDescription(Entity entity) {
        m_log.log(DescriptionComposer.compose(entity));
    }

    private Vec3i getPlayerPosition() {
        var playerResults = m_ecs.findEntitiesWith(Player.class, Position.class);
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
