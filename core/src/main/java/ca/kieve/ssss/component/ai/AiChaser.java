package ca.kieve.ssss.component.ai;

import ca.kieve.ssss.component.Component;

public class AiChaser implements Component {
    // Range within which the entity will chase the player. 0 = infinite.
    public int range = 20;
    
    public AiChaser() {
    }
    
    public AiChaser(int range) {
        this.range = range;
    }
}
