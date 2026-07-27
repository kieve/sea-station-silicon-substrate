package ca.kieve.ssss.ui.node;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.context.DebugContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.testharness.TestGameContext;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExaminePanelTest {
    private static final Vec3i UNSEEN_CELL = new Vec3i(20, 15, 1);

    @Test
    void fullVisionExaminesUnseenCellsLive() {
        GameContext gc = TestGameContext.createEmpty();
        var beacon = gc.ecs().createEntity(new Descriptor("Beacon", "A blinking marker."));
        gc.pos().add(beacon, UNSEEN_CELL);
        gc.examine().enter(UNSEEN_CELL);
        setFullVision(gc, true);

        var panel = new ExaminePanel();
        panel.update(new UiRenderContext(gc, null, null, null), 0f);

        assertEquals(1, panel.getEntityNames().size());
        assertTrue(panel.getEntityNames().contains("Beacon"));
    }

    @Test
    void withoutFullVisionUnseenCellsShowNothing() {
        GameContext gc = TestGameContext.createEmpty();
        var beacon = gc.ecs().createEntity(new Descriptor("Beacon", "A blinking marker."));
        gc.pos().add(beacon, UNSEEN_CELL);
        gc.examine().enter(UNSEEN_CELL);
        setFullVision(gc, false);

        var panel = new ExaminePanel();
        panel.update(new UiRenderContext(gc, null, null, null), 0f);

        assertTrue(panel.getEntityNames().isEmpty());
    }

    private static void setFullVision(GameContext gc, boolean on) {
        for (DebugContext.Toggle toggle : gc.debug().getToggles()) {
            if (toggle.label().equals("Full Vision")) {
                toggle.setter().accept(on);
                return;
            }
        }
    }
}
