package ca.kieve.ssss.context;

import ca.kieve.ssss.util.BoundingBox3i;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;

import static ca.kieve.ssss.util.MathUtil.clamp;

public class FluidContext {
    public static final int MAX_LEVEL = 4;
    public static final double FULL = 1.0;
    public static final double MIN_MASS = 0.001;

    public static final double LEVEL_1_CEIL = 0.10;
    public static final double LEVEL_2_CEIL = 0.55;

    private static final double[] LEVEL_MASS = { 0.0, LEVEL_1_CEIL, LEVEL_2_CEIL, 0.78, FULL };

    private final List<Vec3i> m_sourceCells = new ArrayList<>();

    private double[][][] m_mass;
    private double[][][] m_source;
    private BoundingBox3i m_box;

    public static double levelMass(int level) {
        return LEVEL_MASS[clamp(level, 0, MAX_LEVEL)];
    }

    public void init(Vec3i size) {
        m_box = new BoundingBox3i(Vec3i.ZERO, size);
        m_mass = new double[size.x][size.y][size.z];
        m_source = new double[size.x][size.y][size.z];
        m_sourceCells.clear();
    }

    public double getMass(Vec3i pos) {
        if (!inBounds(pos)) {
            return 0;
        }
        return m_mass[pos.x][pos.y][pos.z];
    }

    public void setMass(Vec3i pos, double mass) {
        if (!inBounds(pos)) {
            return;
        }
        m_mass[pos.x][pos.y][pos.z] = Math.max(0, mass);
    }

    public void addMass(Vec3i pos, double delta) {
        if (!inBounds(pos)) {
            return;
        }
        m_mass[pos.x][pos.y][pos.z] = Math.max(0, m_mass[pos.x][pos.y][pos.z] + delta);
    }

    public void setSource(Vec3i pos, double pressureDepth) {
        if (!inBounds(pos)) {
            return;
        }
        double depth = Math.max(0, pressureDepth);
        boolean wasSource = m_source[pos.x][pos.y][pos.z] > 0;
        m_source[pos.x][pos.y][pos.z] = depth;
        if (depth > 0 && !wasSource) {
            m_sourceCells.add(pos.copy());
        } else if (depth <= 0 && wasSource) {
            m_sourceCells.remove(pos);
        }
    }

    public double sourceDepth(Vec3i pos) {
        if (!inBounds(pos)) {
            return 0;
        }
        return m_source[pos.x][pos.y][pos.z];
    }

    public boolean isSource(Vec3i pos) {
        return sourceDepth(pos) > 0;
    }

    public List<Vec3i> sourceCells() {
        return m_sourceCells;
    }

    public int getLevel(Vec3i pos) {
        return levelForMass(getMass(pos));
    }

    public static int levelForMass(double mass) {
        if (mass <= 0) {
            return 0;
        }
        if (mass >= FULL) {
            return MAX_LEVEL;
        }
        if (mass > LEVEL_2_CEIL) {
            return 3;
        }
        if (mass > LEVEL_1_CEIL) {
            return 2;
        }
        return 1;
    }

    public void setLevel(Vec3i pos, int level) {
        setMass(pos, levelMass(level));
    }

    public boolean hasWater(Vec3i pos) {
        return getMass(pos) > 0;
    }

    public double totalVolume() {
        if (m_mass == null) {
            return 0;
        }
        double total = 0;
        for (double[][] plane : m_mass) {
            for (double[] column : plane) {
                for (double cell : column) {
                    total += cell;
                }
            }
        }
        return total;
    }

    public List<Vec3i> waterCells() {
        List<Vec3i> cells = new ArrayList<>();
        if (m_box == null) {
            return cells;
        }
        m_box.forEach(cell -> {
            if (m_mass[cell.x][cell.y][cell.z] > 0) {
                cells.add(cell);
            }
        });
        return cells;
    }

    public boolean inBounds(Vec3i pos) {
        return m_mass != null && m_box.contains(pos);
    }
}
