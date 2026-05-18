package ca.kieve.ssss.context;

import ca.kieve.ssss.world.WorldModel;

/**
 * Holds the loaded {@link WorldModel}. Mirrors the
 * {@link MapContext}/{@link PathingContext} pattern: created empty as a
 * record component on {@link GameContext}, populated by the engine after
 * the map generator runs.
 */
public class WorldContext {
    private WorldModel m_model;

    public WorldModel getModel() {
        return m_model;
    }

    public void setModel(WorldModel model) {
        m_model = model;
    }
}
