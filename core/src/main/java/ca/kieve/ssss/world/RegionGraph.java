package ca.kieve.ssss.world;

import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Static connectivity between {@link MapRegion}s, expressed as a list of
 * {@link Portal}s plus three indices for fast lookup: per-region adjacency
 * (which other regions touch this one), per-region-pair portal lists (the
 * tile pairs spanning two specific regions), and a flat per-region portal
 * list. Built once at world-load time.
 *
 * <p>Topology is based on the static map only — a cell pair is a portal
 * if both sides are non-solid in {@link WorldModel} and belong to
 * different regions. Dynamic state (open doors, blocking NPCs) is
 * evaluated per-query, not here (Phase 4c).
 */
public final class RegionGraph {
    private record RegionPair(String regionA, String regionB) {
        /** Canonical form: lexicographically-smaller region first. */
        static RegionPair of(String a, String b) {
            return a.compareTo(b) <= 0 ? new RegionPair(a, b) : new RegionPair(b, a);
        }
    }

    private final List<Portal> m_portals;
    private final Map<String, Set<String>> m_neighbors;
    private final Map<RegionPair, List<Portal>> m_portalsByPair;
    private final Map<String, List<Portal>> m_byRegion;

    private RegionGraph(
        List<Portal> portals,
        Map<String, Set<String>> neighbors,
        Map<RegionPair, List<Portal>> portalsByPair,
        Map<String, List<Portal>> byRegion
    ) {
        m_portals = portals;
        m_neighbors = neighbors;
        m_portalsByPair = portalsByPair;
        m_byRegion = byRegion;
    }

    /**
     * Walks the world and discovers every portal between adjacent
     * regions in the same z-plane. To avoid double-counting pairs, only
     * each cell's east (+x) and north (+y) neighbours are considered.
     */
    public static RegionGraph discover(WorldModel world, MapContext map) {
        List<Portal> portals = new ArrayList<>();
        Map<String, Set<String>> neighbors = new HashMap<>();
        Map<RegionPair, List<Portal>> portalsByPair = new HashMap<>();
        Map<String, List<Portal>> byRegion = new HashMap<>();

        world.box().forEach(cell -> {
            if (!world.isPassable(cell)) {
                return;
            }
            MapRegion here = map.regionAt(cell);
            if (here == null) {
                return;
            }
            consider(
                portals,
                neighbors,
                portalsByPair,
                byRegion,
                world,
                map,
                here,
                cell,
                cell.add(Vec3i.EAST)
            );
            consider(
                portals,
                neighbors,
                portalsByPair,
                byRegion,
                world,
                map,
                here,
                cell,
                cell.add(Vec3i.NORTH)
            );
        });

        return new RegionGraph(
            Collections.unmodifiableList(portals),
            Collections.unmodifiableMap(neighbors),
            Collections.unmodifiableMap(portalsByPair),
            Collections.unmodifiableMap(byRegion)
        );
    }

    private static void consider(
        List<Portal> portals,
        Map<String, Set<String>> neighbors,
        Map<RegionPair, List<Portal>> portalsByPair,
        Map<String, List<Portal>> byRegion,
        WorldModel world,
        MapContext map,
        MapRegion here,
        Vec3i fromCell,
        Vec3i toCell
    ) {
        if (!world.isInBounds(toCell)) {
            return;
        }
        if (!world.isPassable(toCell)) {
            return;
        }
        MapRegion there = map.regionAt(toCell);
        if (there == null || there == here) {
            return;
        }
        Portal portal = new Portal(here.id(), fromCell, there.id(), toCell);
        portals.add(portal);
        neighbors.computeIfAbsent(here.id(), k -> new HashSet<>()).add(there.id());
        neighbors.computeIfAbsent(there.id(), k -> new HashSet<>()).add(here.id());
        portalsByPair
            .computeIfAbsent(RegionPair.of(here.id(), there.id()), k -> new ArrayList<>())
            .add(portal);
        byRegion.computeIfAbsent(here.id(), k -> new ArrayList<>()).add(portal);
        byRegion.computeIfAbsent(there.id(), k -> new ArrayList<>()).add(portal);
    }

    /** All portals discovered, in iteration order (east-then-north sweep). */
    public List<Portal> portals() {
        return m_portals;
    }

    /** Portals touching the given region. Empty list if the region has none. */
    public List<Portal> portalsFor(String regionId) {
        return m_byRegion.getOrDefault(regionId, List.of());
    }

    /** Regions directly connected to {@code regionId}. */
    public Set<String> neighborsOf(String regionId) {
        return m_neighbors.getOrDefault(regionId, Set.of());
    }

    /**
     * All portals that connect the two regions, in either direction.
     * Returns an empty list if the regions aren't adjacent.
     */
    public List<Portal> portalsBetween(String regionA, String regionB) {
        return m_portalsByPair.getOrDefault(RegionPair.of(regionA, regionB), List.of());
    }

    /**
     * Topology-only variant of
     * {@link #findPortalTowards(String, String, Vec3i, Predicate)} —
     * treats every portal as traversable. Useful for region-graph queries
     * that don't depend on current passability.
     */
    public Portal findPortalTowards(String fromRegion, String toRegion, Vec3i fromCell) {
        return findPortalTowards(fromRegion, toRegion, fromCell, p -> true);
    }

    /**
     * Picks the portal in {@code fromRegion} that an entity at
     * {@code fromCell} should head toward to reach {@code toRegion}.
     * BFS over the region graph picks the next hop on a shortest path,
     * considering only edges where at least one portal satisfies
     * {@code traversable}; among the portals in that edge, the closest
     * one to {@code fromCell} (manhattan distance, x/y only) that also
     * satisfies the predicate wins.
     *
     * <p>Returns {@code null} if {@code toRegion} is unreachable, or if
     * the two regions are the same.
     */
    public Portal findPortalTowards(
        String fromRegion,
        String toRegion,
        Vec3i fromCell,
        Predicate<Portal> traversable
    ) {
        if (fromRegion.equals(toRegion)) {
            return null;
        }
        String firstHop = bfsFirstHop(fromRegion, toRegion, traversable);
        if (firstHop == null) {
            return null;
        }
        return closestPortal(fromRegion, firstHop, fromCell, traversable);
    }

    private String bfsFirstHop(String fromRegion, String toRegion, Predicate<Portal> traversable) {
        Map<String, String> cameFrom = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        visited.add(fromRegion);
        queue.add(fromRegion);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : neighborsOf(current)) {
                if (!visited.add(neighbor)) {
                    continue;
                }
                if (!hasTraversablePortal(current, neighbor, traversable)) {
                    continue;
                }
                cameFrom.put(neighbor, current);
                if (neighbor.equals(toRegion)) {
                    // Trace back to the first hop adjacent to fromRegion.
                    String r = toRegion;
                    while (!cameFrom.get(r).equals(fromRegion)) {
                        r = cameFrom.get(r);
                    }
                    return r;
                }
                queue.add(neighbor);
            }
        }
        return null;
    }

    private boolean hasTraversablePortal(String a, String b, Predicate<Portal> traversable) {
        for (Portal p : portalsBetween(a, b)) {
            if (traversable.test(p)) {
                return true;
            }
        }
        return false;
    }

    private Portal closestPortal(
        String fromRegion,
        String firstHop,
        Vec3i fromCell,
        Predicate<Portal> traversable
    ) {
        Portal best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Portal p : portalsBetween(fromRegion, firstHop)) {
            if (!traversable.test(p)) {
                continue;
            }
            int d = fromCell.manhattanDistTo(p.cellIn(fromRegion));
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }
}
