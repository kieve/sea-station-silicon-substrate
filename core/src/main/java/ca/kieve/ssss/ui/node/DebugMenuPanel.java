package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

public class DebugMenuPanel extends UiNode {
    private static final int PADDING = 8;
    private static final int ARROW_WIDTH = 8;
    private static final int ARROW_HEIGHT = 10;
    private static final int ARROW_MARGIN = 6;
    private static final String TITLE = "Debug Menu";

    private final GlyphLayout m_glyphLayout = new GlyphLayout();

    @Override
    public void update(UiRenderContext renderContext, float delta) {
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        if (!gc.input().isMode(InputContext.Mode.MODE_DEBUG_MENU)) {
            return;
        }

        var debug = gc.debug();
        var toggles = debug.getToggles();
        var pos = getScreenPosition();
        var size = getSize();

        var lines = new String[toggles.size() + 1];
        lines[0] = TITLE;
        for (int i = 0; i < toggles.size(); i++) {
            var t = toggles.get(i);
            lines[i + 1] = (t.getter().getAsBoolean() ? "[x] " : "[ ] ") + t.label();
        }

        float maxLineWidth = 0;
        for (String line : lines) {
            m_glyphLayout.setText(UI_UBUNTU_24, line);
            if (m_glyphLayout.width > maxLineWidth) {
                maxLineWidth = m_glyphLayout.width;
            }
        }

        int indicatorWidth = ARROW_WIDTH + ARROW_MARGIN;
        int contentWidth = (int) Math.ceil(maxLineWidth) + indicatorWidth;

        float lineHeight = UI_UBUNTU_24.getLineHeight();
        int contentHeight = (int) Math.ceil(lineHeight * lines.length);

        int panelWidth = Math.min(contentWidth + PADDING * 2, size.w());
        int panelHeight = Math.min(contentHeight + PADDING * 2, size.h());

        int panelX = pos.x() + (size.w() - panelWidth) / 2;
        int panelY = pos.y() + (size.h() - panelHeight) / 2;

        var sr = renderContext.shapeRenderer();
        sr.begin(ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(panelX, panelY, panelWidth, panelHeight);
        sr.end();
        sr.begin(ShapeType.Line);
        sr.setColor(Color.YELLOW);
        sr.rect(panelX + 1, panelY + 1, panelWidth - 1, panelHeight - 2);
        sr.end();

        int selectedIndex = debug.getSelectedIndex();
        float capHeight = UI_UBUNTU_24.getCapHeight();
        float arrowX = panelX + PADDING;
        float arrowCenterY = panelY + PADDING
            + (selectedIndex + 1) * lineHeight
            + capHeight / 2;

        sr.begin(ShapeType.Filled);
        sr.setColor(Color.WHITE);
        sr.triangle(
            arrowX,
            arrowCenterY - ARROW_HEIGHT / 2f,
            arrowX,
            arrowCenterY + ARROW_HEIGHT / 2f,
            arrowX + ARROW_WIDTH,
            arrowCenterY
        );
        sr.end();

        float textX = panelX + PADDING + indicatorWidth;
        float textY = panelY + PADDING;

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, Color.WHITE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(lines[i]);
        }
        FontRepo.draw(UI_UBUNTU_24, batch, sb.toString(), textX, textY);
        batch.end();
    }
}
