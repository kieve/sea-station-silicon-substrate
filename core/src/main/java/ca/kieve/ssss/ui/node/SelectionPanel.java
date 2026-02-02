package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

public abstract class SelectionPanel extends UiNode {
    private static final int PADDING = 5;
    private static final int ARROW_WIDTH = 8;
    private static final int ARROW_HEIGHT = 10;
    private static final int ARROW_MARGIN = 6;

    private final GlyphLayout m_glyphLayout = new GlyphLayout();

    private List<String> m_items = new ArrayList<>();
    private int m_selectedIndex = 0;
    private boolean m_active = false;
    private boolean m_showSelectionIndicator = false;

    protected void setItems(List<String> items) {
        m_items = items;
    }

    protected void setSelectedIndex(int index) {
        m_selectedIndex = index;
    }

    protected void setActive(boolean active) {
        m_active = active;
    }

    protected void setShowSelectionIndicator(boolean show) {
        m_showSelectionIndicator = show;
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        if (!m_active || m_items.isEmpty()) {
            return;
        }

        var pos = getScreenPosition();
        var size = getSize();

        // Build item text without the selection indicator
        var textBuilder = new StringBuilder();
        for (int i = 0; i < m_items.size(); i++) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append("\n");
            }
            textBuilder.append(m_items.get(i));
        }
        String text = textBuilder.toString();

        // Reserve space for the arrow indicator
        int indicatorWidth = 0;
        if (m_showSelectionIndicator) {
            indicatorWidth = ARROW_WIDTH + ARROW_MARGIN;
        }

        m_glyphLayout.setText(UI_UBUNTU_24, text);
        int textWidth = (int) Math.ceil(m_glyphLayout.width + indicatorWidth);
        int textHeight = (int) Math.ceil(m_glyphLayout.height);

        int panelWidth = textWidth + PADDING * 2;
        int panelHeight = textHeight + PADDING * 2;

        panelWidth = Math.min(panelWidth, size.w());
        panelHeight = Math.min(panelHeight, size.h());

        int panelX = pos.x() + (size.w() - panelWidth) / 2;
        int panelY = pos.y();

        var sr = renderContext.shapeRenderer();
        sr.begin(ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(panelX, panelY, panelWidth, panelHeight);
        sr.end();
        sr.begin(ShapeType.Line);
        sr.setColor(Color.CYAN);
        sr.rect(panelX + 1, panelY + 1, panelWidth - 1, panelHeight - 2);
        sr.end();

        // Draw the arrow indicator for the selected line
        if (m_showSelectionIndicator) {
            float lineHeight = UI_UBUNTU_24.getLineHeight();
            float capHeight = UI_UBUNTU_24.getCapHeight();
            float arrowX = panelX + PADDING;
            float arrowCenterY = panelY + PADDING
                    + m_selectedIndex * lineHeight
                    + capHeight / 2;

            sr.begin(ShapeType.Filled);
            sr.setColor(Color.WHITE);
            sr.triangle(
                    arrowX, arrowCenterY - ARROW_HEIGHT / 2f,
                    arrowX, arrowCenterY + ARROW_HEIGHT / 2f,
                    arrowX + ARROW_WIDTH, arrowCenterY);
            sr.end();
        }

        float textX = panelX + PADDING + indicatorWidth;

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, Color.WHITE);
        FontRepo.draw(
                UI_UBUNTU_24,
                batch,
                text,
                textX,
                panelY + PADDING);
        batch.end();
    }
}
