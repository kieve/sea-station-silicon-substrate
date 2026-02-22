package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;

/**
 * Interface for systems that run once after all map entities are created.
 * Used for post-spawn processing like randomizing ScurryConfig.
 */
public interface MapInitSystem {
    void run(GameContext context);
}
