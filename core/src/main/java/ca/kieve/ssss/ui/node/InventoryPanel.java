package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InventoryContext;
import ca.kieve.ssss.context.InventoryContext.PaneSide;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.util.DescriptionComposer;

import java.util.List;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

public class InventoryPanel extends UiNode {
    private static final int PADDING = 8;
    private static final int PANE_GAP = 8;
    private static final int ARROW_WIDTH = 8;
    private static final int ARROW_HEIGHT = 10;
    private static final int ARROW_MARGIN = 6;
    private static final int GLYPH_MARGIN = 6;
    private static final int DESC_LINES = 5;
    private static final String FOOTER = "Tab: switch  0-9: location  Enter: move  Q: close";

    private final GlyphLayout m_glyphLayout = new GlyphLayout();

    @Override
    public void update(UiRenderContext renderContext, float delta) {
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var inventory = gc.inventory();
        if (!inventory.isActive()) {
            return;
        }

        var pos = getScreenPosition();
        var size = getSize();

        int panelX = pos.x() + PADDING;
        int panelY = pos.y() + PADDING;
        int panelWidth = size.w() - PADDING * 2;
        int panelHeight = size.h() - PADDING * 2;

        var sr = renderContext.shapeRenderer();
        sr.begin(ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(panelX, panelY, panelWidth, panelHeight);
        sr.end();
        sr.begin(ShapeType.Line);
        sr.setColor(Color.CYAN);
        sr.rect(panelX + 1, panelY + 1, panelWidth - 1, panelHeight - 2);
        sr.end();

        float lineHeight = UI_UBUNTU_24.getLineHeight();
        int footerReserved = (int) Math.ceil(lineHeight) + PADDING;
        int descReserved = (int) Math.ceil(lineHeight * DESC_LINES) + PADDING;

        int paneWidth = (panelWidth - PADDING * 2 - PANE_GAP) / 2;
        int paneHeight = panelHeight - PADDING * 2 - footerReserved - descReserved;
        int leftPaneX = panelX + PADDING;
        int rightPaneX = leftPaneX + paneWidth + PANE_GAP;
        int paneY = panelY + PADDING;

        var activeSide = inventory.getActiveSide();
        renderPane(
            gc,
            inventory,
            PaneSide.LEFT,
            leftPaneX,
            paneY,
            paneWidth,
            paneHeight,
            activeSide == PaneSide.LEFT,
            renderContext
        );
        renderPane(
            gc,
            inventory,
            PaneSide.RIGHT,
            rightPaneX,
            paneY,
            paneWidth,
            paneHeight,
            activeSide == PaneSide.RIGHT,
            renderContext
        );

        int descX = panelX + PADDING;
        int descY = paneY + paneHeight + PADDING;
        int descWidth = panelWidth - PADDING * 2;
        int descHeight = descReserved - PADDING;
        renderDescription(gc, inventory, renderContext, descX, descY, descWidth, descHeight);

        renderFooter(renderContext, panelX, panelY, panelWidth, panelHeight);
    }

    private void renderDescription(
        GameContext gc,
        InventoryContext inventory,
        UiRenderContext renderContext,
        int x,
        int y,
        int width,
        int height
    ) {
        var sr = renderContext.shapeRenderer();
        sr.begin(ShapeType.Line);
        sr.setColor(Color.GRAY);
        sr.rect(x, y, width, height);
        sr.end();

        var activeSide = inventory.getActiveSide();
        var items = inventory.getItemsAt(gc, inventory.getLocation(activeSide));
        int cursor = inventory.getCursor(activeSide);

        String text;
        if (items.isEmpty()) {
            text = "(nothing selected)";
        } else {
            int safeCursor = Math.max(0, Math.min(cursor, items.size() - 1));
            text = DescriptionComposer.compose(items.get(safeCursor));
        }

        float lineHeight = UI_UBUNTU_24.getLineHeight();
        List<String> lines = FontRepo.wrapText(UI_UBUNTU_24, text, width - PADDING * 2, "");
        int maxLines = Math.max(1, (int) ((height - PADDING) / lineHeight));

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, items.isEmpty() ? Color.GRAY : Color.WHITE);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            FontRepo
                .draw(UI_UBUNTU_24, batch, lines.get(i), x + PADDING, y + PADDING + i * lineHeight);
        }
        batch.end();
    }

    private void renderPane(
        GameContext gc,
        InventoryContext inventory,
        PaneSide side,
        int x,
        int y,
        int width,
        int height,
        boolean isActive,
        UiRenderContext renderContext
    ) {
        var sr = renderContext.shapeRenderer();
        var batch = renderContext.spriteBatch();
        float lineHeight = UI_UBUNTU_24.getLineHeight();
        float capHeight = UI_UBUNTU_24.getCapHeight();

        sr.begin(ShapeType.Line);
        sr.setColor(isActive ? Color.WHITE : Color.GRAY);
        sr.rect(x, y, width, height);
        sr.end();

        var location = inventory.getLocation(side);
        String header = location.label();
        var items = inventory.getItemsAt(gc, location);
        int cursor = inventory.getCursor(side);

        int indicatorWidth = ARROW_WIDTH + ARROW_MARGIN;
        int textX = x + PADDING + indicatorWidth;
        int textY = y + PADDING;

        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, isActive ? Color.YELLOW : Color.LIGHT_GRAY);
        FontRepo.draw(UI_UBUNTU_24, batch, header, x + PADDING, textY);
        batch.end();

        if (items.isEmpty()) {
            batch.begin();
            batch.setProjectionMatrix(renderContext.camera().combined);
            FontRepo.setFontColor(UI_UBUNTU_24, Color.GRAY);
            FontRepo.draw(UI_UBUNTU_24, batch, "(empty)", textX, textY + lineHeight);
            batch.end();
            return;
        }

        int safeCursor = Math.max(0, Math.min(cursor, items.size() - 1));
        float arrowX = x + PADDING;
        float arrowCenterY = textY + (safeCursor + 1) * lineHeight + capHeight / 2;

        sr.begin(ShapeType.Filled);
        sr.setColor(isActive ? Color.WHITE : Color.GRAY);
        sr.triangle(
            arrowX,
            arrowCenterY - ARROW_HEIGHT / 2f,
            arrowX,
            arrowCenterY + ARROW_HEIGHT / 2f,
            arrowX + ARROW_WIDTH,
            arrowCenterY
        );
        sr.end();

        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            float rowY = textY + (i + 1) * lineHeight;
            renderItemRow(item, textX, rowY, batch);
        }
        batch.end();
    }

    private void renderItemRow(Entity item, float x, float y, SpriteBatch batch) {
        var glyph = item.get(TileGlyph.class);
        String glyphChar = glyph != null ? String.valueOf(glyph.glyph()) : "?";
        var color = Color.WHITE;
        var colorComp = item.get(ColorComp.class);
        if (colorComp != null) {
            color = colorComp.color;
        }

        FontRepo.setFontColor(UI_UBUNTU_24, color);
        FontRepo.draw(UI_UBUNTU_24, batch, glyphChar, x, y);

        m_glyphLayout.setText(UI_UBUNTU_24, glyphChar);
        float nameX = x + m_glyphLayout.width + GLYPH_MARGIN;

        var descriptor = item.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        FontRepo.setFontColor(UI_UBUNTU_24, Color.WHITE);
        FontRepo.draw(UI_UBUNTU_24, batch, name, nameX, y);
    }

    private void renderFooter(
        UiRenderContext renderContext,
        int panelX,
        int panelY,
        int panelWidth,
        int panelHeight
    ) {
        var batch = renderContext.spriteBatch();
        float lineHeight = UI_UBUNTU_24.getLineHeight();
        float footerY = panelY + panelHeight - PADDING - lineHeight;

        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, Color.LIGHT_GRAY);
        FontRepo.draw(UI_UBUNTU_24, batch, FOOTER, panelX + PADDING, footerY);
        batch.end();
    }
}
