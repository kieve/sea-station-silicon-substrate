package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InputContext.Mode;
import ca.kieve.ssss.event.EventType;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.DescriptionComposer;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

import java.util.List;
import java.util.stream.Collectors;

public class ExamineSystem extends System {
    private final InputContext m_input;

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
            int entityCount = getDescriptorCount(examineContext.getCrosshairPos());
            if (m_input.consume(InputAction.UP)) {
                examineContext.decrementSelectedIndex(entityCount);
            }
            if (m_input.consume(InputAction.DOWN)) {
                examineContext.incrementSelectedIndex(entityCount);
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
        var examineEvents = m_gameContext.events().getEvents(EventType.EXAMINE);
        for (var entity : examineEvents) {
            var descriptor = entity.get(Descriptor.class);
            if (descriptor != null) {
                logDescription(entity);
            }
        }
    }

    private int getDescriptorCount(Vec3i pos) {
        var entities = m_gameContext.pos().getAt(pos);
        return (int) entities.stream()
            .filter(entity -> entity.get(Descriptor.class) != null)
            .count();
    }

    private void handleEnterKey(
        ca.kieve.ssss.context.ExamineContext examineContext
    ) {
        var crosshairPos = examineContext.getCrosshairPos();
        var entities = m_gameContext.pos().getAt(crosshairPos);

        List<Entity> entitiesWithDescriptor = entities.stream()
            .filter(entity -> entity.get(Descriptor.class) != null)
            .collect(Collectors.toList());

        if (entitiesWithDescriptor.isEmpty()) {
            return;
        }

        if (!examineContext.isSelectionMode()) {
            // Not in selection mode
            if (entitiesWithDescriptor.size() == 1) {
                // Single entity - auto-select and log (stay in examine mode)
                logDescription(entitiesWithDescriptor.get(0));
            } else {
                // Multiple entities - enter selection mode
                examineContext.enterSelectionMode();
            }
            return;
        }

        // In selection mode - confirm selection (exit selection mode, stay in examine)
        int selectedIndex = examineContext.getSelectedIndex();
        if (selectedIndex < entitiesWithDescriptor.size()) {
            logDescription(entitiesWithDescriptor.get(selectedIndex));
            examineContext.exitSelectionMode();
        }
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
