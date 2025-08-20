package ca.kieve.ssss.context;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PositionContext {
    private static final int XY_SIZE = 200;
    private static final int Z_SIZE = 20;

    private final
        ArrayList< // X
        ArrayList< // Y
        ArrayList< // Z
        ArrayList<Entity>
        >>> m_cache;

    public PositionContext() {
        m_cache = new ArrayList<>();
        for (int x = 0; x < XY_SIZE; x++) {
            var yLine = new ArrayList<ArrayList<ArrayList<Entity>>>(XY_SIZE);
            for (int y = 0; y < XY_SIZE; y++) {
                var zLine = new ArrayList<ArrayList<Entity>>(Z_SIZE);
                for (int z = 0; z < Z_SIZE; z++) {
                    zLine.add(new ArrayList<>());
                }
                yLine.add(zLine);
            }
            m_cache.add(yLine);
        }
    }

    public void add(Entity entity, Vec3i pos) {
        if (pos == null) throw new IllegalArgumentException("pos cannot be null");
        var list = getList(pos);
        list.add(entity);
    }

    public void remove(Entity entity, Vec3i pos) {
        if (pos == null) throw new IllegalArgumentException("pos cannot be null");
        var list = getList(pos);
        list.remove(entity);
    }

    public void move(Entity entity, Vec3i from, Vec3i to) {
        if (from == null) throw new IllegalArgumentException("from cannot be null");
        if (to == null) throw new IllegalArgumentException("to cannot be null");

        var fromList = getList(from);
        var toList = getList(to);
        fromList.remove(entity);
        toList.add(entity);
    }

    public List<Entity> getAt(Vec3i pos) {
        if (pos == null) throw new IllegalArgumentException("pos cannot be null");
        var results = getList(pos);
        if (results == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(results);
    }

    private List<Entity> getList(Vec3i pos) {
        var xList = m_cache.get(pos.x);
        if (xList == null) {
            m_cache.add(pos.x, new ArrayList<>());
            xList = m_cache.get(pos.x);
        }

        var yList = xList.get(pos.y);
        if (yList == null) {
            xList.add(pos.y, new ArrayList<>());
            yList = xList.get(pos.y);
        }

        var zList = yList.get(pos.z);
        if (zList == null) {
            yList.add(pos.z, new ArrayList<>());
            zList = yList.get(pos.z);
        }

        return zList;
    }
}
