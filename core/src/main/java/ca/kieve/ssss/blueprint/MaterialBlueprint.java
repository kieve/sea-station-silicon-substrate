package ca.kieve.ssss.blueprint;

import ca.kieve.ssss.component.Component;
import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Hardness;
import ca.kieve.ssss.component.Material;

import java.util.List;

import static ca.kieve.ssss.component.Density.AIR;
import static ca.kieve.ssss.component.Density.SOLID;
import static ca.kieve.ssss.component.Material.STONE;
import static ca.kieve.ssss.component.Material.WOOD;

public class MaterialBlueprint {
    private MaterialBlueprint() {
        // Do not instantiate
    }

    public static List<Component> createWoodComponents() {
        return createMaterial(WOOD, AIR, 10);
    }

    public static List<Component> createStoneComponents() {
        return createMaterial(STONE, SOLID, 100);
    }

    private static List<Component> createMaterial(
        Material material,
        Density density,
        int hardness
    ) {
        return List.of(
            material,
            density,
            new Hardness(hardness)
        );
    }
}
