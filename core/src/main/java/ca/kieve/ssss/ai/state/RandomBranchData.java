package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.ai.reset.ResetCondition;

/**
 * Per-entity data for RandomBranchState.
 * Stores the selected branch and the reset condition instance that tracks
 * entity-specific state for determining when to re-randomize.
 */
public class RandomBranchData {
    private String m_selectedBranch;
    private ResetCondition m_resetCondition;

    public String getSelectedBranch() {
        return m_selectedBranch;
    }

    public void setSelectedBranch(String selectedBranch) {
        m_selectedBranch = selectedBranch;
    }

    public ResetCondition getResetCondition() {
        return m_resetCondition;
    }

    public void setResetCondition(ResetCondition resetCondition) {
        m_resetCondition = resetCondition;
    }
}
