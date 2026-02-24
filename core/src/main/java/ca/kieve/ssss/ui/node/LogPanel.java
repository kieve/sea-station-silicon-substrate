package ca.kieve.ssss.ui.node;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.LogContext.LogEntry;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

/**
 * UI widget that displays the game log messages.
 * Newest messages appear at the bottom, older messages scroll up.
 */
public class LogPanel extends UiNode {
    private static final int LINE_HEIGHT = 24;
    private static final int PADDING = 5;

    private List<LogEntry> m_messages = List.of();
    private int m_firstMessageNumber = 1;
    private int m_totalMessageCount = 0;

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var log = gc.log();
        m_messages = log.getMessages();
        m_firstMessageNumber = log.getFirstMessageNumber();
        m_totalMessageCount = log.getTotalMessageCount();
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        var pos = getScreenPosition();
        var size = getSize();

        var sr = renderContext.shapeRenderer();
        sr.begin(ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(pos.x(), pos.y(), size.w(), size.h());
        sr.end();
        sr.begin(ShapeType.Line);
        sr.setColor(Color.GREEN);
        sr.rect(pos.x() + 1, pos.y() + 1, size.w() - 1, size.h() - 2);
        sr.end();

        if (m_messages.isEmpty()) {
            return;
        }

        // Calculate how many lines can fit (only top padding affects this)
        int maxLines = (size.h() - PADDING) / LINE_HEIGHT;
        float availableWidth = size.w() - (2 * PADDING);

        // Calculate width needed for line numbers based on total count
        int maxLineNumber = Math.max(1, m_totalMessageCount);
        int digitWidth = String.valueOf(maxLineNumber).length();
        String formatString = "[%" + digitWidth + "d] ";

        // Create indent string that matches the prefix width (e.g., "[  1] " -> "      ")
        String indent = " ".repeat(digitWidth + 3); // "[" + digits + "] "

        // Wrap all messages and collect wrapped lines with their message index
        List<String> allWrappedLines = new ArrayList<>();
        for (int i = 0; i < m_messages.size(); i++) {
            var entry = m_messages.get(i);
            int lineNumber = m_firstMessageNumber + i;

            String prefix = String.format(formatString, lineNumber);
            String message = entry.message();
            if (entry.count() > 1) {
                message += " (x" + entry.count() + ")";
            }

            // Wrap the message (first line includes prefix, continuation lines get indent)
            String fullLine = prefix + message;
            List<String> wrapped =
                FontRepo.wrapText(UI_UBUNTU_24, fullLine, availableWidth, indent);
            allWrappedLines.addAll(wrapped);
        }

        // Take only the most recent lines that fit
        int startIndex = Math.max(0, allWrappedLines.size() - maxLines);
        var visibleLines = allWrappedLines.subList(startIndex, allWrappedLines.size());

        // Build final text
        var textBuilder = new StringBuilder();
        for (String line : visibleLines) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append("\n");
            }
            textBuilder.append(line);
        }

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.setFontColor(UI_UBUNTU_24, Color.WHITE);
        FontRepo.draw(
            UI_UBUNTU_24,
            batch,
            textBuilder.toString(),
            pos.x() + PADDING,
            pos.y() + PADDING
        );
        batch.end();
    }
}
