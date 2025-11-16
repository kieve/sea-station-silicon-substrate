package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext.Mode;
import ca.kieve.ssss.input.ExamineInputController;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;
import java.util.stream.Collectors;

public class ExamineSystem extends System {
    private final ExamineInputController m_inputController;

    public ExamineSystem(
        GameContext gameContext,
        ExamineInputController inputController
    ) {
        super(gameContext);
        m_inputController = inputController;
    }

    @Override
    public void awaitingUserInput() {
        var inputContext = m_gameContext.input();
        var examineContext = m_gameContext.examine();

        // Handle E key to toggle examine mode
        if (m_inputController.consumeExamineKey()) {
            if (inputContext.isMode(Mode.EXAMINE)) {
                inputContext.setMode(Mode.NORMAL);
                examineContext.exit();
                return;
            }

            // Enter examine mode - start at player position
            var playerPos = getPlayerPosition();
            if (playerPos != null) {
                inputContext.setMode(Mode.EXAMINE);
                examineContext.enter(playerPos);
            }
            return;
        }

        if (!inputContext.isMode(Mode.EXAMINE)) {
            return;
        }

        // Handle Escape to exit examine mode
        if (m_inputController.consumeEscapeKey()) {
            inputContext.setMode(Mode.NORMAL);
            examineContext.exit();
            return;
        }

        // Handle WASD based on current mode
        if (examineContext.isSelectionMode()) {
            // In selection mode, W/S controls the selector
            int entityCount = getDescriptorCount(examineContext.getCrosshairPos());
            if (m_inputController.consumeW()) {
                examineContext.decrementSelectedIndex(entityCount);
            }
            if (m_inputController.consumeS()) {
                examineContext.incrementSelectedIndex(entityCount);
            }
            // Consume A and D to prevent them from doing anything
            m_inputController.consumeA();
            m_inputController.consumeD();
        } else {
            // Normal mode: WASD moves crosshair
            if (m_inputController.consumeW()) {
                examineContext.moveCrosshair(new Vec3i(0, 1, 0));
            }
            if (m_inputController.consumeA()) {
                examineContext.moveCrosshair(new Vec3i(-1, 0, 0));
            }
            if (m_inputController.consumeS()) {
                examineContext.moveCrosshair(new Vec3i(0, -1, 0));
            }
            if (m_inputController.consumeD()) {
                examineContext.moveCrosshair(new Vec3i(1, 0, 0));
            }
        }

        // Handle Enter key for selection
        if (m_inputController.consumeEnterKey()) {
            handleEnterKey(examineContext);
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

        List<Descriptor> descriptors = entities.stream()
            .map(entity -> entity.get(Descriptor.class))
            .filter(descriptor -> descriptor != null)
            .collect(Collectors.toList());

        if (descriptors.isEmpty()) {
            return;
        }

        if (!examineContext.isSelectionMode()) {
            // Not in selection mode
            if (descriptors.size() == 1) {
                // Single entity - auto-select and log (stay in examine mode)
                logDescription(descriptors.get(0));
            } else {
                // Multiple entities - enter selection mode
                examineContext.enterSelectionMode();
            }
            return;
        }

        // In selection mode - confirm selection (exit selection mode, stay in examine)
        int selectedIndex = examineContext.getSelectedIndex();
        if (selectedIndex < descriptors.size()) {
            logDescription(descriptors.get(selectedIndex));
            examineContext.exitSelectionMode();
        }
    }

    private void logDescription(Descriptor descriptor) {
        m_gameContext.log().log(descriptor.description());
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

        // Fallback: find entity with WasdController
        var wasdResults = m_gameContext.ecs().findEntitiesWith(
            WasdController.class,
            Position.class
        );

        var wasdResult = wasdResults.stream().findFirst();
        if (wasdResult.isPresent()) {
            return wasdResult.get().comp2().getPosition().copy();
        }

        return null;
    }
}
