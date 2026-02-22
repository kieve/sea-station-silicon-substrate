package ca.kieve.ssss.system;

import ca.kieve.ssss.component.ScurryConfig;
import ca.kieve.ssss.component.ScurryInit;
import ca.kieve.ssss.context.GameContext;

public class ScurryInitSystem implements MapInitSystem {
    @Override
    public void run(GameContext context) {
        var results = context.ecs().findEntitiesWith(ScurryInit.class);
        for (var result : results) {
            var entity = result.entity();
            var scurryInit = result.comp();

            boolean clockwise = context.random().nextBoolean();
            entity.add(new ScurryConfig(clockwise, scurryInit.initialDirection()));
            entity.removeType(ScurryInit.class);
        }
    }
}
