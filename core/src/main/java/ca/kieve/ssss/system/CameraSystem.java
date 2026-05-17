package ca.kieve.ssss.system;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.GameContext;

public class CameraSystem extends System {
    private final ExamineContext m_examineContext;

    public CameraSystem(GameContext gameContext) {
        super(gameContext);
        m_examineContext = gameContext.examine();
    }

    @Override
    public void run() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(CameraComp.class, Position.class);

        var optionalResults = searchResults.stream().findFirst();
        if (optionalResults.isEmpty()) {
            return;
        }

        var withResult = optionalResults.get();
        var camera = withResult.comp1();

        if (m_examineContext.isActive()) {
            camera.setPosition(m_examineContext.getCrosshairPos());
        } else {
            camera.setPosition(withResult.comp2());
        }
        if (camera.gdx() == null) {
            return;
        }
        camera.gdx().update();
    }
}
