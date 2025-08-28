package ca.kieve.ssss.component.ai;

import ca.kieve.ssss.component.Component;
import ca.kieve.ssss.util.Vec3i;

public class AiSeesawController implements Component {
    public Vec3i m_initialPos;
    public boolean goUp = true;

    public AiSeesawController(Vec3i initialPos) {
        m_initialPos = initialPos.copy();
    }
}
