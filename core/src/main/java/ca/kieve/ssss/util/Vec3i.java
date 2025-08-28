package ca.kieve.ssss.util;

import java.util.Objects;

public class Vec3i {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    public static final Vec3i X = new Vec3i(1, 0, 0);
    public static final Vec3i Y = new Vec3i(0, 1, 0);

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

    public void set(Vec3i val) {
        this.x = val.x;
        this.y = val.y;
        this.z = val.z;
    }

    public void addMut(Vec3i val) {
        this.x += val.x;
        this.y += val.y;
        this.z += val.z;
    }

    public Vec3i add(Vec3i val) {
        var result = copy();
        result.addMut(val);
        return result;
    }

    public void productMut(int value) {
        this.x *= value;
        this.y *= value;
        this.z *= value;
    }

    public Vec3i product(int value) {
        var result = copy();
        result.productMut(value);
        return result;
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

    @Override
    public String toString() {
        return "Vec3i{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }
}
