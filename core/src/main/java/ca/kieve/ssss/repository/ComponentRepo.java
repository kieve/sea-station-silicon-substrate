package ca.kieve.ssss.repository;

import ca.kieve.ssss.component.Collider;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Examinable;
import ca.kieve.ssss.component.RenderingHint;

public class ComponentRepo {
    private ComponentRepo() {
        // Do not instantiate
    }

    // Marker components
    public static final Collider COLLIDER = new Collider();
    public static final Examinable EXAMINABLE = new Examinable();

    // Rendering hints
    public static final RenderingHint FLOOR_HINT = new RenderingHint(0);
    public static final RenderingHint WALL_HINT = new RenderingHint(1);
    public static final RenderingHint ENTITY_HINT = new RenderingHint(2);

    // Descriptors
    public static final Descriptor BLOCK = new Descriptor("Block", "A solid block.");
    public static final Descriptor REINFORCED_BLOCK =
        new Descriptor("Block", "A reinforced block.");
}

