package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the continuous movement implementation (AAWT2S feature).
 *
 * <p>Spec requirement: "The movement of vehicles between tiles on the game map
 * should not be jumpy but continuous. Vehicles can still logically be exclusively
 * on a single tile at any time, but their movement between tiles should be
 * animated and smooth."</p>
 *
 * <p>These tests do NOT require a LibGDX context – they operate purely on the
 * model layer.</p>
 */
class ContinuousMovementTest {

    private Vehicle truck;
    private GameMap map;

    @BeforeEach
    void setUp() {
        truck = new Vehicle(VehicleType.TRUCK, 1900);
        map = new GameMap(20, 20);
        // Build a straight horizontal road: x=0..9, y=5
        for (int x = 0; x <= 9; x++) {
            map.placeRoad(x, 5);
        }
    }

    // ── setCurrentPath seeds visual fields ────────────────────────────────────

    @Test
    void setCurrentPath_seedsVisualXY_toFirstTile() {
        List<int[]> path = Arrays.asList(
            new int[]{2, 5},
            new int[]{3, 5},
            new int[]{4, 5}
        );
        truck.setCurrentPath(path);

        assertEquals(2f, truck.getVisualX(), 0.001f,
            "visualX should be seeded to the first tile in the path");
        assertEquals(5f, truck.getVisualY(), 0.001f,
            "visualY should be seeded to the first tile in the path");
    }

    @Test
    void setCurrentPath_seedsDirectionVector_fromFirstToSecondTile() {
        List<int[]> path = Arrays.asList(
            new int[]{2, 5},
            new int[]{3, 5},  // moving east (+x)
            new int[]{4, 5}
        );
        truck.setCurrentPath(path);

        assertEquals(1f, truck.getDirDx(), 0.001f, "dirDx should be +1 for eastward movement");
        assertEquals(0f, truck.getDirDy(), 0.001f, "dirDy should be 0 for horizontal movement");
    }

    @Test
    void setCurrentPath_northwardMovement_correctDirection() {
        List<int[]> path = Arrays.asList(
            new int[]{5, 2},
            new int[]{5, 3},  // moving north (+y)
            new int[]{5, 4}
        );
        truck.setCurrentPath(path);

        assertEquals(0f, truck.getDirDx(), 0.001f);
        assertEquals(1f, truck.getDirDy(), 0.001f, "dirDy should be +1 for northward movement");
    }

    @Test
    void setCurrentPath_westwardMovement_correctDirection() {
        List<int[]> path = Arrays.asList(
            new int[]{5, 5},
            new int[]{4, 5},  // moving west (-x)
            new int[]{3, 5}
        );
        truck.setCurrentPath(path);

        assertEquals(-1f, truck.getDirDx(), 0.001f, "dirDx should be -1 for westward movement");
        assertEquals(0f,  truck.getDirDy(), 0.001f);
    }

    // ── setPosition resets visual fields ─────────────────────────────────────

    @Test
    void setPosition_resetsVisualXY() {
        // First give it a path so visualX/Y are set to something else
        List<int[]> path = Arrays.asList(new int[]{0, 5}, new int[]{1, 5});
        truck.setCurrentPath(path);

        // Now teleport the vehicle
        truck.setPosition(7f, 5f);

        assertEquals(7f, truck.getVisualX(), 0.001f,
            "setPosition must reset visualX to avoid stale interpolation");
        assertEquals(5f, truck.getVisualY(), 0.001f,
            "setPosition must reset visualY to avoid stale interpolation");
    }

    @Test
    void setPosition_alsoUpdatesLogicalXY() {
        truck.setPosition(3f, 8f);
        assertEquals(3f, truck.getX(), 0.001f);
        assertEquals(8f, truck.getY(), 0.001f);
    }

    // ── movementProgress produces fractional position ─────────────────────────

    @Test
    void movementProgress_zero_vehicleAtTileOrigin() {
        Stop s1 = new Stop("S1", 0, 5);
        Stop s2 = new Stop("S2", 5, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        truck.setAssignedRoute(route);

        // setCurrentPath seeds visualX/Y to the first tile
        List<int[]> path = Arrays.asList(new int[]{0, 5}, new int[]{1, 5});
        truck.setCurrentPath(path);

        // visualX/Y are seeded at path start — verify the anchor is at tile 0
        assertEquals(0f, truck.getVisualX(), 0.001f,
            "visualX should be seeded to the first tile (x=0) when path is set");
        assertEquals(5f, truck.getVisualY(), 0.001f);
    }

    @Test
    void movementProgress_half_vehicleAtMidpointBetweenTiles() {
        Stop s1 = new Stop("S1", 0, 5);
        Stop s2 = new Stop("S2", 5, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        truck.setAssignedRoute(route);

        List<int[]> path = Arrays.asList(new int[]{2, 5}, new int[]{3, 5}, new int[]{4, 5});
        truck.setCurrentPath(path);

        TrafficLightManager tlm = new TrafficLightManager(map);

        // Tick 1: depart from stop (atStop → moving), no spatial movement yet
        truck.update(0.001f, map, tlm);
        // Re-set path since departure clears it and VehicleManager would normally refresh it
        truck.setCurrentPath(path);

        // Tick 2: now moving — drive for exactly half a tile's worth of time
        // TRUCK speed = 80 tiles/min = 80/60 tiles/sec → half tile ≈ 0.375s
        float halfTileTime = 60f / (VehicleType.TRUCK.getSpeed() * 2f);
        truck.update(halfTileTime, map, tlm);

        float x = truck.getX();
        // x should be between 2 and 3 (fractional, not snapped)
        assertTrue(x > 2f && x < 3f,
            "After half a tile's travel time, x=" + x + " should be between 2.0 and 3.0");
    }

    // ── Position is strictly fractional (not snapped) during movement ─────────

    @Test
    void xPosition_isNotIntegerSnapped_duringMovement() {
        Stop s1 = new Stop("S1", 0, 5);
        Stop s2 = new Stop("S2", 9, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        truck.setAssignedRoute(route);

        List<int[]> path = Arrays.asList(
            new int[]{0, 5}, new int[]{1, 5}, new int[]{2, 5},
            new int[]{3, 5}, new int[]{4, 5}, new int[]{5, 5}
        );
        truck.setCurrentPath(path);

        TrafficLightManager tlm = new TrafficLightManager(map);

        // First tick: vehicle departs from stop (atStop → moving), path is consumed
        truck.update(0.01f, map, tlm);
        // Second tick: vehicle is now moving — small delta keeps it mid-tile
        truck.update(0.05f, map, tlm);

        float x = truck.getX();
        // x must be strictly between 0 and 1 (fractional, not snapped to integer)
        assertTrue(x > 0f && x < 1f,
            "After a partial tick while moving, x=" + x + " should be fractional (between 0 and 1)");
    }

    // ── Visual fields stay in sync with logical step ──────────────────────────

    @Test
    void visualXY_updatesWhenVehicleAdvancesToNextTile() {
        Stop s1 = new Stop("S1", 0, 5);
        Stop s2 = new Stop("S2", 9, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        truck.setAssignedRoute(route);

        List<int[]> path = Arrays.asList(
            new int[]{0, 5}, new int[]{1, 5}, new int[]{2, 5},
            new int[]{3, 5}, new int[]{4, 5}
        );
        truck.setCurrentPath(path);

        TrafficLightManager tlm = new TrafficLightManager(map);

        // Drive long enough to advance at least one full tile
        // TRUCK speed = 80 tiles/min = 80/60 tiles/sec → 1 tile takes ~0.75s
        float elapsed = 0f;
        float initialVisualX = truck.getVisualX();
        boolean advanced = false;
        while (elapsed < 2f) {
            truck.update(0.05f, map, tlm);
            elapsed += 0.05f;
            if (truck.getVisualX() > initialVisualX) {
                advanced = true;
                break;
            }
        }

        assertTrue(advanced,
            "visualX should advance beyond the initial tile after enough time has passed");
    }

    // ── Direction vector stays non-zero while moving ──────────────────────────

    @Test
    void directionVector_isNonZero_whileMoving() {
        Stop s1 = new Stop("S1", 0, 5);
        Stop s2 = new Stop("S2", 5, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        truck.setAssignedRoute(route);

        List<int[]> path = Arrays.asList(
            new int[]{0, 5}, new int[]{1, 5}, new int[]{2, 5},
            new int[]{3, 5}, new int[]{4, 5}, new int[]{5, 5}
        );
        truck.setCurrentPath(path);

        TrafficLightManager tlm = new TrafficLightManager(map);
        truck.update(0.1f, map, tlm);

        float dx = truck.getDirDx();
        float dy = truck.getDirDy();
        assertTrue(dx != 0f || dy != 0f,
            "Direction vector must be non-zero while vehicle is moving");
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    void newVehicle_visualFieldsInitialisedToZero() {
        Vehicle fresh = new Vehicle(VehicleType.BUS, 1900);
        assertEquals(0f, fresh.getVisualX(), 0.001f);
        assertEquals(0f, fresh.getVisualY(), 0.001f);
        assertEquals(0f, fresh.getDirDx(),   0.001f);
        assertEquals(0f, fresh.getDirDy(),   0.001f);
    }
}
