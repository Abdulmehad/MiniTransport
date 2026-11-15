package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Pathfinder}.
 */
class PathfinderTest {

    private GameMap map;
    private Pathfinder pathfinder;

    @BeforeEach
    void setUp() {
        map = new GameMap(10, 10);
        pathfinder = new Pathfinder(map);
    }

    @Test
    void findPath_sameStartAndGoal_returnsSingleTile() {
        List<int[]> path = pathfinder.findPath(3, 3, 3, 3);
        assertEquals(1, path.size());
        assertArrayEquals(new int[]{3, 3}, path.get(0));
    }

    @Test
    void findPath_straightLine_returnsCorrectLength() {
        // Horizontal path from (0,0) to (4,0) - need to place roads first
        map.placeHorizontalRoad(0, 4, 0);
        List<int[]> path = pathfinder.findPath(0, 0, 4, 0);
        assertFalse(path.isEmpty());
        assertArrayEquals(new int[]{0, 0}, path.get(0));
        assertArrayEquals(new int[]{4, 0}, path.get(path.size() - 1));
    }

    @Test
    void findPath_goalAlwaysReachable_evenWithBuildings() {
        // Place a city as obstacle block
        City city = new City("Test City", 10000);
        map.placeCity(city, 2, 0);

        // Build road around the city from (0,0) to (5,0)
        map.placeHorizontalRoad(0, 1, 0);  // Before city
        map.placeVerticalRoad(1, 0, 3);    // Go around
        map.placeHorizontalRoad(1, 5, 3);  // Horizontal at y=3
        map.placeVerticalRoad(5, 3, 0);    // Back down
        
        List<int[]> path = pathfinder.findPath(0, 0, 5, 0);
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertArrayEquals(new int[]{5, 0}, path.get(path.size() - 1));
    }

    @Test
    void findPath_usesRoadTiles_whenAvailable() {
        // Lay a road and ensure pathfinder traverses it
        map.placeHorizontalRoad(0, 5, 3);
        List<int[]> path = pathfinder.findPath(0, 3, 5, 3);
        assertNotNull(path);
        assertArrayEquals(new int[]{0, 3}, path.get(0));
        assertArrayEquals(new int[]{5, 3}, path.get(path.size() - 1));
    }

    @Test
    void findPathBFS_returnsValidPath() {
        pathfinder.setAlgorithm(Pathfinder.Algorithm.BFS);
        // Build a road path from (0,0) to (3,3)
        map.placeHorizontalRoad(0, 3, 0);
        map.placeVerticalRoad(3, 0, 3);
        
        List<int[]> path = pathfinder.findPath(0, 0, 3, 3);
        assertFalse(path.isEmpty());
        assertArrayEquals(new int[]{0, 0}, path.get(0));
        assertArrayEquals(new int[]{3, 3}, path.get(path.size() - 1));
    }

    @Test
    void findPath_fromStop_toStop() {
        Stop a = new Stop("A", 1, 1);
        Stop b = new Stop("B", 7, 7);
        
        // Build a road path connecting the stops
        map.placeHorizontalRoad(1, 7, 1);
        map.placeVerticalRoad(7, 1, 7);
        
        List<int[]> path = pathfinder.findPath(a, b);
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertArrayEquals(new int[]{1, 1}, path.get(0));
        assertArrayEquals(new int[]{7, 7}, path.get(path.size() - 1));
    }

    @Test
    void findPath_nullStops_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> pathfinder.findPath(null, null));
    }
}
