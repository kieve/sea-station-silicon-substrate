package ca.kieve.ssss.repository;

import ca.kieve.ssss.component.Collider;

public class ComponentRepo {
    private ComponentRepo() {
        // Do not instantiate
    }

    public static final Collider COLLIDER = new Collider();
}
