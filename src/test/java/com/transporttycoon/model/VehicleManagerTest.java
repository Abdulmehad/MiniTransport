package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VehicleManager}.
 */
class VehicleManagerTest {

    private GameMap map;
    private Player player;
    private VehicleManager manager;

    @BeforeEach
    void setUp() {
        map = new GameMap(20, 20);
        player = new Player(50000.0);
        manager = new VehicleManager(map, player);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    @Test
    void purchaseVehicle_deductsCostFromPlayer() {
        double before = player.getCapital();
        Vehicle v = manager.purchaseVehicle(VehicleType.TRUCK, 1);
        assertNotNull(v);
        assertEquals(before - VehicleType.TRUCK.getPurchaseCost(), player.getCapital(), 0.01);
    }

    @Test
    void purchaseVehicle_addsToFleet() {
        manager.purchaseVehicle(VehicleType.TRUCK, 1);
        assertEquals(1, manager.getFleetSize());
    }

    @Test
    void purchaseVehicle_insufficientFunds_returnsNull() {
        Player broke = new Player(0.0);
        VehicleManager vm = new VehicleManager(map, broke);
        Vehicle v = vm.purchaseVehicle(VehicleType.TRUCK, 1);
        assertNull(v);
        assertEquals(0, vm.getFleetSize());
    }

    @Test
    void purchaseVehicle_multipleTypes_allAddedToFleet() {
        manager.purchaseVehicle(VehicleType.TRUCK, 1);
        manager.purchaseVehicle(VehicleType.BUS, 1);
        manager.purchaseVehicle(VehicleType.CARGO_TRUCK, 1);
        assertEquals(3, manager.getFleetSize());
    }

    // ── Route assignment ──────────────────────────────────────────────────────

    @Test
    void assignRoute_vehicleMovesToFirstStop() {
        Stop s1 = new Stop("Alpha", 1, 1);
        Stop s2 = new Stop("Beta", 5, 5);
        Route route = new Route("Test Route");
        route.addStop(s1);
        route.addStop(s2);

        Vehicle v = manager.purchaseVehicle(VehicleType.TRUCK, 1);
        assertNotNull(v);
        manager.assignRoute(v, route);

        assertEquals(s1, v.getCurrentStop());
        assertEquals(route, v.getAssignedRoute());
    }

    @Test
    void unassignRoute_clearsVehicleRoute() {
        Stop s1 = new Stop("A", 0, 0);
        Stop s2 = new Stop("B", 5, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);

        Vehicle v = manager.purchaseVehicle(VehicleType.TRUCK, 1);
        assertNotNull(v);
        manager.assignRoute(v, route);
        manager.unassignRoute(v);

        assertNull(v.getAssignedRoute());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Test
    void getVehicleById_findsCorrectVehicle() {
        Vehicle v = manager.purchaseVehicle(VehicleType.BUS, 1);
        assertNotNull(v);
        assertEquals(v, manager.getVehicleById(v.getId()));
    }

    @Test
    void getVehicleById_unknownId_returnsNull() {
        assertNull(manager.getVehicleById(9999));
    }

    @Test
    void getTotalMaintenanceCostPerTick_sumsAllVehicles() {
        manager.purchaseVehicle(VehicleType.TRUCK, 1);
        manager.purchaseVehicle(VehicleType.BUS, 1);
        int expected = VehicleType.TRUCK.getMaintenanceCost() + VehicleType.BUS.getMaintenanceCost();
        assertEquals(expected, manager.getTotalMaintenanceCostPerTick());
    }

    @Test
    void purchaseVehicleAtGarage_assignsHomeGarage() {
        IndustrialFacility garage = IndustrialFacility.createGarage("Depot");
        Vehicle v = manager.purchaseVehicleAtGarage(VehicleType.TRUCK, 1900, garage, 4, 4);

        assertNotNull(v);
        assertTrue(v.hasHomeGarage());
        assertEquals(garage, v.getHomeGarage());
        assertEquals(4f, v.getX(), 0.01f);
        assertEquals(4f, v.getY(), 0.01f);
    }

    @Test
    void update_olderVehiclesEnterGarageMaintenanceSooner() {
        IndustrialFacility garage = IndustrialFacility.createGarage("Depot");
        Vehicle young = manager.purchaseVehicleAtGarage(VehicleType.BUS, 1900, garage, 2, 2);
        Vehicle old = manager.purchaseVehicleAtGarage(VehicleType.BUS, 1870, garage, 3, 3);

        assertNotNull(young);
        assertNotNull(old);

        for (int i = 0; i < 2000; i++) {
            manager.update(1f / 60f, 1900);
        }

        assertFalse(young.isUnderGarageMaintenance());
        assertTrue(old.isUnderGarageMaintenance()
            || old.getTicksSinceGarageMaintenance() < young.getTicksSinceGarageMaintenance());
    }

    @Test
    void sellOverAgedVehicles_removesFleetAndCreditsPlayer() {
        IndustrialFacility garage = IndustrialFacility.createGarage("Depot");
        manager.purchaseVehicleAtGarage(VehicleType.TRUCK, 1900, garage, 1, 1);
        manager.purchaseVehicleAtGarage(VehicleType.BUS, 1899, garage, 1, 2);

        for (int i = 0; i < 301; i++) {
            manager.update(1f, 1900);
        }

        double afterPurchases = player.getCapital();
        assertEquals(2, manager.getFleetSize());

        java.util.List<Vehicle> sold = manager.sellOverAgedVehicles(1900, 5);
        assertEquals(2, sold.size());
        assertEquals(0, manager.getFleetSize());
        assertTrue(player.getCapital() > afterPurchases);
    }

    // ── Constructor guards ────────────────────────────────────────────────────

    @Test
    void constructor_nullMap_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleManager(null, player));
    }

    @Test
    void constructor_nullPlayer_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleManager(map, null));
    }
}
