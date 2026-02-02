package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.InteractContext;
import ca.kieve.ssss.context.InteractContext.Phase;
import ca.kieve.ssss.event.Interaction;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

/**
 * UI panel that displays available interactions when multiple options exist.
 * Shows '>' prefix for the currently selected interaction.
 */
public class InteractPanel extends UiNode {
    private static final int PADDING = 5;

    private final GlyphLayout m_glyphLayout = new GlyphLayout();

    private List<String> m_labels = new ArrayList<>();
    private int m_selectedIndex = 0;
    private boolean m_active = false;

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var interactContext = gc.interact();

        m_active = interactContext.isActive()
            && interactContext.getPhase() == Phase.ITEM_SELECT;
        if (!m_active) {
            return;
        }

        m_selectedIndex = interactContext.getSelectedIndex();

        List<Interaction> interactions = interactContext.getInteractions();
        m_labels.clear();
        for (var interaction : interactions) {
            m_labels.add(interaction.getLabel());
        }
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        if (!m_active || m_labels.isEmpty()) {
            return;
        }

        var pos = getScreenPosition();
        var size = getSize();

        var textBuilder = new StringBuilder();
        for (int i = 0; i < m_labels.size(); i++) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append("\n");
            }

            if (i == m_selectedIndex) {
                textBuilder.append(">");
            }
            textBuilder.append(m_labels.get(i));
        }

        String text = textBuilder.toString();

        m_glyphLayout.setText(UI_UBUNTU_24, text);
        int textWidth = (int) Math.ceil(m_glyphLayout.width);
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
}
