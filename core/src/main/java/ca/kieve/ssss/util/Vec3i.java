package ca.kieve.ssss.util;

import java.util.Objects;

public class Vec3i {
    public int x;
    public int y;
    public int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i(Vec3i copy) {
        this.x = copy.x;
        this.y = copy.y;
        this.z = copy.z;
    }

    public Vec3i copy() {
        return new Vec3i(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vec3i vec3i)) return false;
        return x == vec3i.x && y == vec3i.y && z == vec3i.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
