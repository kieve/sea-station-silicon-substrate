package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.TestUiNode;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UiLayoutTest {
    private static class RenderCountingUiNode extends UiNode {
        private int renderCount = 0;
        private UiRenderContext lastRenderContext;
        private float lastDelta;

        @Override
        public void update(UiRenderContext renderContext, float delta) {
            // No-op for testing.
        }

        @Override
        public void render(UiRenderContext renderContext, float delta) {
            renderCount++;
            lastRenderContext = renderContext;
            lastDelta = delta;
        }

        int getRenderCount() {
            return renderCount;
        }

        UiRenderContext getLastRenderContext() {
            return lastRenderContext;
        }

        float getLastDelta() {
            return lastDelta;
        }
    }

    private static class TestUiLayout extends UiLayout {
        private int layoutCallCount = 0;

        @Override
        public void layout() {
            layoutCallCount++;
        }

        void resetLayoutCallCount() {
            layoutCallCount = 0;
        }

        int getLayoutCallCount() {
            return layoutCallCount;
        }

        int getChildCount() {
            return m_children.size();
        }

        UiNode getChildAt(int index) {
            return m_children.get(index);
        }
    }

    @Test
    void addChildWithoutLayoutParamsSetsOriginAndRelayouts() {
        TestUiLayout layout = new TestUiLayout();
        TestUiNode child = new TestUiNode();

        layout.setOrigin(new UiPosition(2, 3));
        layout.setPosition(new UiPosition(5, 7));
        layout.resetLayoutCallCount();

        layout.add(child);

        assertEquals(new UiPosition(7, 10), child.getOrigin());
        assertEquals(1, layout.getLayoutCallCount());
        assertEquals(1, layout.getChildCount());
        assertSame(child, layout.getChildAt(0));
    }

    @Test
    void addChildWithLayoutParamsAppliesParamsBeforeLayout() {
        TestUiLayout layout = new TestUiLayout();
        TestUiNode child = new TestUiNode();
        UiLayoutParams layoutParams = new UiLayoutParams() { };

        layout.resetLayoutCallCount();

        layout.add(child, layoutParams);

        assertSame(layoutParams, child.getLayoutParams());
        assertEquals(1, layout.getLayoutCallCount());
        assertSame(child, layout.getChildAt(0));
    }

    @Test
    void setPositionUpdatesChildOriginsAndRelayouts() {
        TestUiLayout layout = new TestUiLayout();
        TestUiNode child = new TestUiNode();

        layout.setOrigin(new UiPosition(1, 1));
        layout.add(child);
        layout.resetLayoutCallCount();

        layout.setPosition(new UiPosition(4, 6));

        assertEquals(new UiPosition(5, 7), child.getOrigin());
        assertEquals(1, layout.getLayoutCallCount());
    }

    @Test
    void setOriginUpdatesChildOriginsAndRelayouts() {
        TestUiLayout layout = new TestUiLayout();
        TestUiNode child = new TestUiNode();

        layout.setPosition(new UiPosition(3, 4));
        layout.add(child);
        layout.resetLayoutCallCount();

        layout.setOrigin(new UiPosition(2, 2));

        assertEquals(new UiPosition(5, 6), child.getOrigin());
        assertEquals(1, layout.getLayoutCallCount());
    }

    @Test
    void setSizeRelayouts() {
        TestUiLayout layout = new TestUiLayout();

        layout.resetLayoutCallCount();

        layout.setSize(new UiSize(100, 50));

        assertEquals(1, layout.getLayoutCallCount());
    }

    @Test
    void renderWithParentWindowResetsViewportAfterChildWindowRender() {
        TestUiLayout layout = new TestUiLayout();
        UiWindow parentWindow = mock(UiWindow.class);
        UiWindow childWindow = mock(UiWindow.class);
        float delta = 0.25f;

        layout.setParentWindow(parentWindow);
        layout.add(childWindow);

        layout.render(null, delta);

        verify(childWindow).render(isNull(), eq(delta));
        verify(parentWindow).applyViewport();
    }

    @Test
    void renderWithoutParentWindowStillRendersChildWindow() {
        TestUiLayout layout = new TestUiLayout();
        UiWindow childWindow = mock(UiWindow.class);
        float delta = 0.33f;

        layout.add(childWindow);

        layout.render(null, delta);

        verify(childWindow).render(isNull(), eq(delta));
    }

    @Test
    void renderWithParentWindowAndNonWindowChildDoesNotResetViewport() {
        TestUiLayout layout = new TestUiLayout();
        UiWindow parentWindow = mock(UiWindow.class);
        RenderCountingUiNode childNode = new RenderCountingUiNode();
        float delta = 0.5f;

        layout.setParentWindow(parentWindow);
        layout.add(childNode);

        layout.render(null, delta);

        assertEquals(1, childNode.getRenderCount());
        assertNull(childNode.getLastRenderContext());
        assertEquals(delta, childNode.getLastDelta());
        verify(parentWindow, never()).applyViewport();
    }

    @Test
    void renderWithoutParentWindowAndNonWindowChildDoesNotResetViewport() {
        TestUiLayout layout = new TestUiLayout();
        RenderCountingUiNode childNode = new RenderCountingUiNode();
        float delta = 0.75f;

        layout.add(childNode);

        layout.render(null, delta);

        assertEquals(1, childNode.getRenderCount());
        assertNull(childNode.getLastRenderContext());
        assertEquals(delta, childNode.getLastDelta());
    }
}
