package ca.kieve.ssss.ui.node;

import static ca.kieve.ssss.repository.FontRepo.UI_UBUNTU_24;

import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.repository.FontRepo;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiRenderContext;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

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

        // Find the player entity (microchip with Player marker)
        var playerResults = gc.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            m_text = "Can't find the player?";
            return;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);
        var playerHealth = playerEntity.get(Health.class);

        StringBuilder textBuilder = new StringBuilder();

        // Always show the player's HP (the microchip's HP)
        if (playerHealth != null) {
            textBuilder.append("HP: ")
                .append(playerHealth.hp)
                .append("/")
                .append(playerHealth.maxHp)
                .append("\n");
        }

        // If socketed, also show the mech's HP
        if (socketPlug != null && socketPlug.currentBody != null) {
            var socket = socketPlug.currentBody.get(Socket.class);
            if (socket != null) {
                textBuilder.append("Mech HP: ")
                    .append(socket.socketedHp)
                    .append("/")
                    .append(socket.socketedMaxHp)
                    .append("\n");
            }
        }

        // Get position from controlled entity (body if socketed, player otherwise)
        Position posComp;
        if (socketPlug != null && socketPlug.currentBody != null) {
            posComp = socketPlug.currentBody.get(Position.class);
        } else {
            posComp = playerWith.comp2();
        }

        if (posComp != null) {
            var pos = posComp.getPosition();
            textBuilder.append("Position: { ")
                .append(pos.x)
                .append(", ")
                .append(pos.y)
                .append(" }");
        }

        var clock = gc.clock();
        var currentTime = clock.getCurrentTime();

        textBuilder.append("\n")
            .append("Time: ")
            .append(currentTime);

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
        FontRepo.setFontColor(UI_UBUNTU_24, Color.BLACK);
        FontRepo.draw(UI_UBUNTU_24, batch, m_text, pos.x() + 5, pos.y() + 5);
        batch.end();
    }
}
