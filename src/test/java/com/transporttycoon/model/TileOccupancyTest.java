package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Tile} direction-aware occupancy
 * (spec: one vehicle per direction per road tile, max 2 total).
 */
class TileOccupancyTest {

    private Tile tile;
    private Vehicle v1;
    private Vehicle v2;
    private Vehicle v3;

    @BeforeEach
    void setUp() {
        tile = new Tile(3, 3);
        tile.setRoad(true);
        v1 = new Vehicle(VehicleType.TRUCK, 1);
        v2 = new Vehicle(VehicleType.TRUCK, 1);
        v3 = new Vehicle(VehicleType.BUS,   1);
    }

    @Test
    void newTile_bothDirectionsFree() {
        assertTrue(tile.isDirectionFree(Tile.TrafficDirection.POSITIVE));
        assertTrue(tile.isDirectionFree(Tile.TrafficDirection.NEGATIVE));
    }

    @Test
    void enter_positiveLane_succeeds_whenFree() {
        assertTrue(tile.enter(v1, Tile.TrafficDirection.POSITIVE));
        assertEquals(v1, tile.getOccupantPositive());
    }

    @Test
    void enter_negativeLane_succeeds_whenFree() {
        assertTrue(tile.enter(v2, Tile.TrafficDirection.NEGATIVE));
        assertEquals(v2, tile.getOccupantNegative());
    }

    @Test
    void twoVehicles_oppositeDirections_bothFit() {
        // Spec allows max 2 vehicles on one tile (one per direction)
        assertTrue(tile.enter(v1, Tile.TrafficDirection.POSITIVE));
        assertTrue(tile.enter(v2, Tile.TrafficDirection.NEGATIVE));
        assertFalse(tile.isDirectionFree(Tile.TrafficDirection.POSITIVE));
        assertFalse(tile.isDirectionFree(Tile.TrafficDirection.NEGATIVE));
    }

    @Test
    void enter_sameDirection_blocked_whenOccupied() {
        tile.enter(v1, Tile.TrafficDirection.POSITIVE);
        // v3 tries to enter same direction – should be blocked
        assertFalse(tile.enter(v3, Tile.TrafficDirection.POSITIVE));
        // occupant is still v1
        assertEquals(v1, tile.getOccupantPositive());
    }

    @Test
    void leave_releasesLane() {
        tile.enter(v1, Tile.TrafficDirection.POSITIVE);
        tile.leave(v1, Tile.TrafficDirection.POSITIVE);
        assertTrue(tile.isDirectionFree(Tile.TrafficDirection.POSITIVE));
        assertNull(tile.getOccupantPositive());
    }

    @Test
    void leave_byWrongVehicle_doesNothing() {
        tile.enter(v1, Tile.TrafficDirection.POSITIVE);
        tile.leave(v2, Tile.TrafficDirection.POSITIVE); // v2 was never in POSITIVE
        assertEquals(v1, tile.getOccupantPositive()); // v1 still there
    }

    @Test
    void afterLeave_newVehicle_canEnter() {
        tile.enter(v1, Tile.TrafficDirection.POSITIVE);
        tile.leave(v1, Tile.TrafficDirection.POSITIVE);
        assertTrue(tile.enter(v3, Tile.TrafficDirection.POSITIVE));
        assertEquals(v3, tile.getOccupantPositive());
    }
}
