package com.transporttycoon.model;

import java.util.*;

/**
 * Pathfinder – finds the shortest walkable tile-path between two map positions.
 *
 * <p>Uses the A* algorithm with Manhattan-distance heuristic.  A tile is
 * considered walkable when it is either empty (no content) or contains a
 * {@link Stop} (stops are always reachable).  Buildings ({@link City} and
 * {@link IndustrialFacility}) are treated as obstacles.</p>
 *
 * <p>If A* cannot find a path (e.g. the destination is completely surrounded
 * by buildings) the method falls back to a straight-line path so that vehicles
 * never get stuck.</p>
 */
public class Pathfinder {

    // ── Node used internally by A* ─────────────────────────────────────────

    private static final class Node implements Comparable<Node> {
        final int x;
        final int y;
        /** Cost from start to this node (g-score). */
        double g;
        /** Estimated total cost start → goal through this node (f = g + h). */
        double f;
        Node parent;

        Node(int x, int y, double g, double f, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node n = (Node) o;
            return x == n.x && y == n.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    // ── 4-directional movement (no diagonals on a road grid) ──────────────

    private static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    // ── Algorithm selection ────────────────────────────────────────────────

    /**
     * Available pathfinding algorithms.
     */
    public enum Algorithm {
        /** A* with Manhattan-distance heuristic (default, fast). */
        A_STAR,
        /** Breadth-first search – optimal on uniform-cost grids, slower than A*. */
        BFS
    }

    private Algorithm algorithm = Algorithm.A_STAR;

    // ─────────────────────────────────────────────────────────────────────────

    private final GameMap map;

    /**
     * Creates a new Pathfinder bound to the given map.
     *
     * @param map the game map used for walkability checks
     */
    public Pathfinder(GameMap map) {
        if (map == null) throw new IllegalArgumentException("map must not be null");
        this.map = map;
    }

    /**
     * Sets the algorithm to use for subsequent {@link #findPath} calls.
     *
     * @param algorithm the algorithm to use
     */
    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Returns the currently active algorithm.
     *
     * @return algorithm
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Find a path from tile ({@code startX}, {@code startY}) to tile
     * ({@code goalX}, {@code goalY}).
     *
     * @param startX start tile x-coordinate
     * @param startY start tile y-coordinate
     * @param goalX  destination tile x-coordinate
     * @param goalY  destination tile y-coordinate
     * @return ordered list of {@code [x, y]} tile coordinates from start
     *         (inclusive) to goal (inclusive), or a straight-line fallback if
     *         no walkable path exists
     */
    public List<int[]> findPath(int startX, int startY, int goalX, int goalY) {
        // Trivial case
        if (startX == goalX && startY == goalY) {
            return Collections.singletonList(new int[]{startX, startY});
        }

        // Ensure start/goal are within bounds
        if (!inBounds(startX, startY) || !inBounds(goalX, goalY)) {
            return straightLineFallback(startX, startY, goalX, goalY);
        }

        return algorithm == Algorithm.BFS
                ? findPathBFS(startX, startY, goalX, goalY)
                : findPathAStar(startX, startY, goalX, goalY);
    }

    // ── A* implementation ─────────────────────────────────────────────────────

    private List<int[]> findPathAStar(int startX, int startY, int goalX, int goalY) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Long, Node> openMap   = new HashMap<>();   // key = encoded position
        Set<Long>        closed   = new HashSet<>();

        Node start = new Node(startX, startY, 0, heuristic(startX, startY, goalX, goalY), null);
        open.add(start);
        openMap.put(encode(startX, startY), start);

        while (!open.isEmpty()) {
            Node current = open.poll();
            long currentKey = encode(current.x, current.y);
            openMap.remove(currentKey);

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }

            closed.add(currentKey);

            for (int[] dir : DIRS) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (!inBounds(nx, ny)) continue;
                if (!isWalkable(nx, ny, goalX, goalY)) continue;

                long neighborKey = encode(nx, ny);
                if (closed.contains(neighborKey)) continue;

                double tentativeG = current.g + 1.0; // uniform cost per tile
                Node existing = openMap.get(neighborKey);

                if (existing == null) {
                    Node neighbor = new Node(nx, ny, tentativeG,
                            tentativeG + heuristic(nx, ny, goalX, goalY), current);
                    open.add(neighbor);
                    openMap.put(neighborKey, neighbor);
                } else if (tentativeG < existing.g) {
                    // Better path found – update node
                    open.remove(existing);
                    existing.g = tentativeG;
                    existing.f = tentativeG + heuristic(nx, ny, goalX, goalY);
                    existing.parent = current;
                    open.add(existing);
                }
            }
        }

        // No path found on the road network
        System.out.printf("[Pathfinder] A* found no road path (%d,%d)→(%d,%d)%n",
                startX, startY, goalX, goalY);
        return new ArrayList<>();
    }

    // ── BFS implementation ────────────────────────────────────────────────────

    /**
     * Breadth-first search pathfinding.  Guarantees the shortest path (fewest
     * tiles) but explores more nodes than A* on large maps.
     */
    private List<int[]> findPathBFS(int startX, int startY, int goalX, int goalY) {
        Queue<Node>  queue  = new LinkedList<>();
        Set<Long>    visited = new HashSet<>();

        Node start = new Node(startX, startY, 0, 0, null);
        queue.add(start);
        visited.add(encode(startX, startY));

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }

            for (int[] dir : DIRS) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                if (!inBounds(nx, ny)) continue;
                if (!isWalkable(nx, ny, goalX, goalY)) continue;
                long key = encode(nx, ny);
                if (visited.contains(key)) continue;
                visited.add(key);
                queue.add(new Node(nx, ny, 0, 0, current));
            }
        }

        System.out.printf("[Pathfinder] BFS found no road path (%d,%d)→(%d,%d)%n",
                startX, startY, goalX, goalY);
        return new ArrayList<>();
    }

    /**
     * Convenience overload accepting {@link Stop} objects directly.
     *
     * @param from origin stop
     * @param to   destination stop
     * @return path as list of [x, y] tile coordinates
     */
    public List<int[]> findPath(Stop from, Stop to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Stops must not be null");
        }
        return findPath(from.getX(), from.getY(), to.getX(), to.getY());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Manhattan-distance heuristic (admissible for 4-directional movement).
     */
    private static double heuristic(int x, int y, int gx, int gy) {
        return Math.abs(x - gx) + Math.abs(y - gy);
    }

    /**
     * A tile is walkable only when it is part of the road network.
     */
    private boolean isWalkable(int x, int y, int goalX, int goalY) {
        return map.isRoad(x, y);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < map.getWidth() && y < map.getHeight();
    }

    /** Encode (x, y) as a single long for fast map lookup. */
    private static long encode(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /** Reconstruct the ordered path by walking parent pointers. */
    private static List<int[]> reconstructPath(Node goal) {
        LinkedList<int[]> path = new LinkedList<>();
        Node current = goal;
        while (current != null) {
            path.addFirst(new int[]{current.x, current.y});
            current = current.parent;
        }
        return new ArrayList<>(path);
    }

    /**
     * Straight-line fallback using Bresenham's line algorithm.
     * Used when A* cannot find a route (disconnected map).
     */
    private static List<int[]> straightLineFallback(int x0, int y0, int x1, int y1) {
        List<int[]> path = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            path.add(new int[]{x, y});
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
        return path;
    }
}
