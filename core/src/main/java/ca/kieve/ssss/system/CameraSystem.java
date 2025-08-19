package ca.kieve.ssss.system;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;

public class CameraSystem extends System{
    public CameraSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void run() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            CameraComp.class,
            Position.class
        );

        var optionalResults = searchResults.stream().findFirst();
        if (optionalResults.isEmpty()) {
            return;
        }

        var withResult = optionalResults.get();
        var camera = withResult.comp1();
        var pos =  withResult.comp2();

        camera.setPosition(pos);
        camera.gdx().update();
    }
}
