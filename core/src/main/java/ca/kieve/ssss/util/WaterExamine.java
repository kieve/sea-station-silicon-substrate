package ca.kieve.ssss.util;

import ca.kieve.ssss.context.FluidContext;

public final class WaterExamine {
    private WaterExamine() {
    }

    public static String name(FluidContext fluid, Vec3i pos) {
        return switch (fluid.getLevel(pos)) {
        case 1 -> "Shallow water";
        case 2 -> "Water";
        default -> "Deep water";
        };
    }

    public static String description(FluidContext fluid, Vec3i pos) {
        return switch (fluid.getLevel(pos)) {
        case 1 -> "Cold seawater. A thin film barely covers the floor.";
        case 2 -> "Cold seawater. It pools across the floor.";
        case 3 -> "Cold seawater. It runs deep enough to wade through.";
        default -> "Cold seawater. It floods the space from floor to ceiling.";
        };
    }
}
