package ca.kieve.ssss.component;

import com.badlogic.gdx.graphics.OrthographicCamera;

import ca.kieve.ssss.util.Vec3i;

public record CameraComp(OrthographicCamera gdx) implements Component {
    public CameraComp() {
        this(new OrthographicCamera());
    }

    public void setPosition(Position pos) {
        this.setPosition(pos.getPosition());
    }

    public void setPosition(Vec3i pos) {
        gdx.position.set(pos.x, pos.y, 0);
    }
}
