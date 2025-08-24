package ca.kieve.ssss.component;

import com.badlogic.gdx.graphics.Camera;

import ca.kieve.ssss.util.Vec3i;

public class CameraComp implements Component {
    private Camera m_gdx = null;

    public Camera gdx() {
        return m_gdx;
    }

    public void setGdx(Camera gdx) {
        m_gdx = gdx;
    }

    public void setPosition(Position pos) {
        this.setPosition(pos.getPosition());
    }

    public void setPosition(Vec3i pos) {
        if (this.m_gdx == null) {
            return;
        }
        m_gdx.position.set(pos.x, pos.y, 0);
    }
}
