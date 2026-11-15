package com.transporttycoon.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VehicleType} – ensures the spec requirements are met:
 * at least 2 vehicle types per category, correct cargo restrictions, stats.
 */
class VehicleTypeTest {

    // ── Category membership ───────────────────────────────────────────────────

    @Test
    void passengerCategory_hasTwoTypes() {
        long passengerCount = java.util.Arrays.stream(VehicleType.values())
                .filter(VehicleType::isPassengerVehicle)
                .count();
        assertTrue(passengerCount >= 2,
                "Spec requires at least 2 vehicle types per category (passenger)");
    }

    @Test
    void cargoCategory_hasTwoTypes() {
        long cargoCount = java.util.Arrays.stream(VehicleType.values())
                .filter(VehicleType::isCargoVehicle)
                .count();
        assertTrue(cargoCount >= 2,
                "Spec requires at least 2 vehicle types per category (cargo)");
    }

    @Test
    void bus_isPassengerVehicle() {
        assertTrue(VehicleType.BUS.isPassengerVehicle());
        assertFalse(VehicleType.BUS.isCargoVehicle());
    }

    @Test
    void minibus_isPassengerVehicle() {
        assertTrue(VehicleType.MINIBUS.isPassengerVehicle());
        assertFalse(VehicleType.MINIBUS.isCargoVehicle());
    }

    @Test
    void truck_isCargoVehicle() {
        assertTrue(VehicleType.TRUCK.isCargoVehicle());
        assertFalse(VehicleType.TRUCK.isPassengerVehicle());
    }

    @Test
    void cargoTruck_isCargoVehicle() {
        assertTrue(VehicleType.CARGO_TRUCK.isCargoVehicle());
        assertFalse(VehicleType.CARGO_TRUCK.isPassengerVehicle());
    }

    // ── canCarry rules ────────────────────────────────────────────────────────

    @Test
    void bus_canCarryPassengers_notCargo() {
        assertTrue(VehicleType.BUS.canCarry(CargoType.PASSENGER));
        assertFalse(VehicleType.BUS.canCarry(CargoType.IRON));
        assertFalse(VehicleType.BUS.canCarry(CargoType.WOOD));
        assertFalse(VehicleType.BUS.canCarry(CargoType.STEEL));
        assertFalse(VehicleType.BUS.canCarry(CargoType.PAPER));
    }

    @Test
    void minibus_canCarryPassengers_notCargo() {
        assertTrue(VehicleType.MINIBUS.canCarry(CargoType.PASSENGER));
        assertFalse(VehicleType.MINIBUS.canCarry(CargoType.IRON));
        assertFalse(VehicleType.MINIBUS.canCarry(CargoType.WOOD));
    }

    @Test
    void truck_canCarryAllCargo_notPassengers() {
        assertFalse(VehicleType.TRUCK.canCarry(CargoType.PASSENGER));
        assertTrue(VehicleType.TRUCK.canCarry(CargoType.IRON));
        assertTrue(VehicleType.TRUCK.canCarry(CargoType.WOOD));
        assertTrue(VehicleType.TRUCK.canCarry(CargoType.STEEL));
        assertTrue(VehicleType.TRUCK.canCarry(CargoType.PAPER));
    }

    @Test
    void cargoTruck_canCarryAllCargo_notPassengers() {
        assertFalse(VehicleType.CARGO_TRUCK.canCarry(CargoType.PASSENGER));
        assertTrue(VehicleType.CARGO_TRUCK.canCarry(CargoType.IRON));
        assertTrue(VehicleType.CARGO_TRUCK.canCarry(CargoType.STEEL));
    }

    // ── Distinct stats between types of same category ─────────────────────────

    @Test
    void bus_and_minibus_haveDistinctStats() {
        // They must differ in at least one significant attribute
        assertNotEquals(VehicleType.BUS.getCapacity(), VehicleType.MINIBUS.getCapacity(),
                "Bus and Minibus should have different capacities");
    }

    @Test
    void truck_and_cargoTruck_haveDistinctStats() {
        assertNotEquals(VehicleType.TRUCK.getCapacity(), VehicleType.CARGO_TRUCK.getCapacity(),
                "Truck and Cargo Truck should have different capacities");
    }

    @Test
    void minibus_isFasterThanBus() {
        assertTrue(VehicleType.MINIBUS.getSpeed() > VehicleType.BUS.getSpeed(),
                "Minibus should be faster than Bus");
    }

    @Test
    void minibus_isCheaperThanBus() {
        assertTrue(VehicleType.MINIBUS.getPurchaseCost() < VehicleType.BUS.getPurchaseCost(),
                "Minibus should cost less than Bus");
    }

    @Test
    void cargoTruck_hasHigherCapacityThanTruck() {
        assertTrue(VehicleType.CARGO_TRUCK.getCapacity() > VehicleType.TRUCK.getCapacity(),
                "Cargo Truck should carry more than regular Truck");
    }
}
