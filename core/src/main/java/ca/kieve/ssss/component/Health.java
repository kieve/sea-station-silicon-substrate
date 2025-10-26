package ca.kieve.ssss.component;

public class Health implements Component{
    long maxHp;
    long hp;

    public Health() {
        this(0);
    }

    public Health(long maxHp) {
        this(maxHp, maxHp);
    }

    public Health(long maxHp, long hp) {
        this.maxHp = maxHp;
        this.hp = hp;
    }
}
