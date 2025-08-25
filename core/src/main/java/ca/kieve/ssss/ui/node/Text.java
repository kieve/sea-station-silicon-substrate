package ca.kieve.ssss.ui.node;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

public class Text extends UiNode {
    private String m_text;

    public Text(String text) {
        m_text = text;
    }

    public void setText(String text) {
        m_text = text;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var searchResults = gc.ecs().findEntitiesWith(
            Position.class,
            WasdController.class
        );

        var optionalResults = searchResults.stream().findFirst();
        if (optionalResults.isEmpty()) {
            m_text = "Can't find the player?";
            return;
        }

        StringBuilder textBuilder = new StringBuilder();

        var withResult = optionalResults.get();
        var pos = withResult.comp1().getPosition();

        textBuilder.append("Position: { ")
            .append(pos.x)
            .append(", ")
            .append(pos.y)
            .append(" }");


        var clock = gc.clock();
        var currentTime = clock.getCurrentTime();

        textBuilder.append("\n")
            .append("Time: ")
            .append(currentTime);

        var logMessages = gc.log().getMessages();
        logMessages.forEach(message -> textBuilder.append("\n").append(message));

        m_text = textBuilder.toString();
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
        sr.setColor(Color.RED);
        sr.rect(pos.x() + 1, pos.y() + 1, size.w() - 1, size.h() - 2);
        sr.end();

        var batch = renderContext.spriteBatch();
        batch.begin();
        batch.setProjectionMatrix(renderContext.camera().combined);
        FontRepo.UI_UBUNTU_32.setColor(Color.BLACK);
        FontRepo.UI_UBUNTU_32.draw(batch, m_text, pos.x() + 5, pos.y() + 5);
        batch.end();
    }
}
