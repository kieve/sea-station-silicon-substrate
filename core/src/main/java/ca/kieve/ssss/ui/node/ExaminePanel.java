package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.system.ExamineSystem.ExamineItem;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

/**
 * UI panel that displays names of entities under the examine crosshair.
 * Shows '>' prefix for selected entity in selection mode.
 */
public class ExaminePanel extends UiNode {
    private static final int LINE_HEIGHT = 24;
    private static final int PADDING = 5;

    private final GlyphLayout m_glyphLayout = new GlyphLayout();

    private List<String> m_entityNames = new ArrayList<>();
    private int m_selectedIndex = 0;
    private boolean m_selectionMode = false;
    private boolean m_active = false;

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var examineContext = gc.examine();

        m_active = examineContext.isActive();
        if (!m_active) {
            return;
        }

        m_selectionMode = examineContext.isSelectionMode();
        m_selectedIndex = examineContext.getSelectedIndex();

        // Build the list of examine items (main level + ceiling + floor)
        var items = new ArrayList<ExamineItem>();

        // Get entities at main level (crosshair position)
        var mainPos = examineContext.getCrosshairPos();
        var mainEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(mainPos));
        for (var entity : mainEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.MAIN));
        }

        // Get entities at ceiling level (z+1)
        var ceilingPos = examineContext.getCeilingPos();
        var ceilingEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(ceilingPos));
        for (var entity : ceilingEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.CEILING));
        }

        // Get entities at floor level (z-1)
        var floorPos = examineContext.getFloorPos();
        var floorEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(floorPos));
        for (var entity : floorEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.FLOOR));
        }

        // Convert items to display names
        m_entityNames.clear();
        for (var item : items) {
            var name = item.entity().get(Descriptor.class).name();
            String displayName = switch (item.type()) {
                case MAIN -> name;
                case FLOOR -> name + " (Floor)";
                case CEILING -> name + " (Ceiling)";
            };
            m_entityNames.add(displayName);
        }
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        if (!m_active || m_entityNames.isEmpty()) {
            return;
        }

        var pos = getScreenPosition();
        var size = getSize();

        // Build text content first to measure it
        var textBuilder = new StringBuilder();
        for (int i = 0; i < m_entityNames.size(); i++) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append("\n");
            }

            if (m_selectionMode && i == m_selectedIndex) {
                textBuilder.append(">");
            }
            textBuilder.append(m_entityNames.get(i));
        }

        String text = textBuilder.toString();

        // Measure text dimensions
        m_glyphLayout.setText(UI_UBUNTU_24, text);
        int textWidth = (int) Math.ceil(m_glyphLayout.width);
        int textHeight = (int) Math.ceil(m_glyphLayout.height);

        // Calculate panel dimensions based on content
        int panelWidth = textWidth + PADDING * 2;
        int panelHeight = textHeight + PADDING * 2;

        // Ensure panel fits within available space
        panelWidth = Math.min(panelWidth, size.w());
        panelHeight = Math.min(panelHeight, size.h());

        // Center horizontally at the top
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

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, Color.WHITE);
        FontRepo.draw(
            UI_UBUNTU_24,
            batch,
            text,
            panelX + PADDING,
            panelY + PADDING
        );
        batch.end();
    }

    public List<String> getEntityNames() {
        return m_entityNames;
    }
}
