package ca.kieve.ssss.component;

public class Speed {
    public int val;
    public long canActAt = 0;
    public boolean canAct = false;

    public Speed(int val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "Speed{" +
            "val=" + val +
            ", canActAt=" + canActAt +
            ", canAct=" + canAct +
            '}';
    }

}
