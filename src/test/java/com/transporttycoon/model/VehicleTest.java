package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Vehicle} – cargo loading/unloading and state transitions.
 */
class VehicleTest {

    private Vehicle truck;
    private Vehicle bus;
    private Stop stop;

    @BeforeEach
    void setUp() {
        truck = new Vehicle(VehicleType.TRUCK, 1);
        bus   = new Vehicle(VehicleType.BUS,   1);
        stop  = new Stop("Test Stop", 3, 3);
    }

    // ── Cargo loading ──────────────────────────────────────────────────────

    @Test
    void loadCargo_trucksCanCarryIron() {
        stop.addCargo(CargoType.IRON, 20);
        int loaded = truck.loadCargo(stop, CargoType.IRON, 20);
        assertEquals(20, loaded);
        assertEquals(20, truck.getCargoLoaded(CargoType.IRON));
        assertEquals(20, truck.getCurrentCapacity());
    }

    @Test
    void loadCargo_trucksCannotCarryPassengers() {
        stop.addCargo(CargoType.PASSENGER, 10);
        int loaded = truck.loadCargo(stop, CargoType.PASSENGER, 10);
        assertEquals(0, loaded);
        assertEquals(0, truck.getCurrentCapacity());
    }

    @Test
    void loadCargo_busCanCarryPassengers() {
        stop.addCargo(CargoType.PASSENGER, 5);
        int loaded = bus.loadCargo(stop, CargoType.PASSENGER, 5);
        assertEquals(5, loaded);
    }

    @Test
    void loadCargo_busCannotCarryIron() {
        stop.addCargo(CargoType.IRON, 10);
        int loaded = bus.loadCargo(stop, CargoType.IRON, 10);
        assertEquals(0, loaded);
    }

    @Test
    void loadCargo_respectsCapacityLimit() {
        int capacity = VehicleType.TRUCK.getCapacity();
        stop.addCargo(CargoType.IRON, capacity + 100);
        int loaded = truck.loadCargo(stop, CargoType.IRON, capacity + 100);
        assertEquals(capacity, loaded);
        assertEquals(0, truck.getRemainingCapacity());
    }

    // ── Cargo unloading ────────────────────────────────────────────────────

    @Test
    void unloadCargo_reducesVehicleCargoAndAddsToStop() {
        stop.addCargo(CargoType.WOOD, 30);
        truck.loadCargo(stop, CargoType.WOOD, 30);

        Stop destination = new Stop("Dest", 8, 8);
        truck.unloadCargo(destination, CargoType.WOOD, 30);

        assertEquals(0, truck.getCargoLoaded(CargoType.WOOD));
        assertEquals(0, truck.getCurrentCapacity());
        assertEquals(30, destination.getCargoAmount(CargoType.WOOD));
    }

    @Test
    void unloadCargo_cannotUnloadMoreThanLoaded() {
        stop.addCargo(CargoType.STEEL, 10);
        truck.loadCargo(stop, CargoType.STEEL, 10);

        Stop dest = new Stop("D", 1, 1);
        truck.unloadCargo(dest, CargoType.STEEL, 999); // try to unload more than loaded

        assertEquals(0, truck.getCargoLoaded(CargoType.STEEL));
        assertEquals(10, dest.getCargoAmount(CargoType.STEEL));
    }

    // ── Route / state ──────────────────────────────────────────────────────

    @Test
    void setAssignedRoute_positionsAtFirstStop() {
        Stop s1 = new Stop("S1", 1, 1);
        Stop s2 = new Stop("S2", 5, 5);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);

        truck.setAssignedRoute(route);

        assertEquals(s1, truck.getCurrentStop());
        assertEquals(VehicleState.AT_STOP, truck.getState());
        assertFalse(truck.isMoving());
    }

    @Test
    void setAssignedRoute_null_setsIdleState() {
        truck.setAssignedRoute(null);
        assertEquals(VehicleState.IDLE, truck.getState());
        assertNull(truck.getAssignedRoute());
    }

    @Test
    void getNextStop_circularRoute() {
        Stop s1 = new Stop("S1", 0, 0);
        Stop s2 = new Stop("S2", 4, 4);
        Stop s3 = new Stop("S3", 8, 8);
        Route route = new Route("R");
        route.addStop(s1);
        route.addStop(s2);
        route.addStop(s3);

        truck.setAssignedRoute(route);

        // At s1 → next is s2
        assertEquals(s2, truck.getNextStop());
    }

    // ── Misc ───────────────────────────────────────────────────────────────

    @Test
    void getRemainingCapacity_startsAtFullCapacity() {
        assertEquals(VehicleType.TRUCK.getCapacity(), truck.getRemainingCapacity());
    }

    @Test
    void getMaintenanceCost_matchesVehicleType() {
        assertEquals(VehicleType.TRUCK.getMaintenanceCost(), truck.getMaintenanceCost());
    }
}
