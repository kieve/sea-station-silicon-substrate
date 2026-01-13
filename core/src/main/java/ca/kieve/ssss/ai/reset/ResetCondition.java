package ca.kieve.ssss.ai.reset;

import ca.kieve.ssss.content.YamlInitializable;

/**
 * Interface for conditions that determine when a random branch selection should reset.
 * Implementations track relevant state and indicate when re-randomization should occur.
 */
public interface ResetCondition extends YamlInitializable {
    /**
     * Determines if the branch selection should be reset.
     *
     * @param context The reset context providing access to game state and entity
     * @return true if the selection should be reset and re-randomized
     */
    boolean shouldReset(ResetContext context);

    /**
     * Records the current state for future comparison.
     * Called after a branch selection is made.
     *
     * @param context The reset context providing access to game state and entity
     */
    void recordState(ResetContext context);
}
