package com.transporttycoon.model;

import com.transporttycoon.GameConstants;

/**
 * Enum representing different types of vehicles available in the game.
 *
 * <p>Categories (per spec – at least 2 types per category):</p>
 * <ul>
 *   <li><b>Passenger buses:</b> {@link #BUS}, {@link #MINIBUS}</li>
 *   <li><b>Cargo trucks:</b> {@link #TRUCK}, {@link #CARGO_TRUCK}</li>
 * </ul>
 */
public enum VehicleType {
    // ── Passenger category ────────────────────────────────────────────────────
    /** Standard city bus – high capacity, moderate speed. */
    BUS("Bus", GameConstants.BUS_PURCHASE_COST, GameConstants.BUS_CAPACITY,
            GameConstants.BUS_SPEED, GameConstants.BUS_MAINTENANCE_COST),

    /** Minibus – lower capacity, faster, cheaper to buy and maintain. */
    MINIBUS("Minibus", GameConstants.MINIBUS_PURCHASE_COST, GameConstants.MINIBUS_CAPACITY,
            GameConstants.MINIBUS_SPEED, GameConstants.MINIBUS_MAINTENANCE_COST),

    // ── Cargo category ────────────────────────────────────────────────────────
    /** General-purpose truck – balanced stats for raw material transport. */
    TRUCK("Truck", GameConstants.TRUCK_PURCHASE_COST, GameConstants.TRUCK_CAPACITY,
            GameConstants.TRUCK_SPEED, GameConstants.TRUCK_MAINTENANCE_COST),

    /** Heavy cargo truck – large capacity, slower speed, higher cost. */
    CARGO_TRUCK("Cargo Truck", GameConstants.CARGO_TRUCK_PURCHASE_COST,
            GameConstants.CARGO_TRUCK_CAPACITY, GameConstants.CARGO_TRUCK_SPEED,
            GameConstants.CARGO_TRUCK_MAINTENANCE_COST);
    
    private final String displayName;
    private final int purchaseCost;
    private final int capacity;
    private final int speed; // tiles per minute
    private final int maintenanceCost; // per game tick
    
    VehicleType(String displayName, int purchaseCost, int capacity, int speed, int maintenanceCost) {
        this.displayName = displayName;
        this.purchaseCost = purchaseCost;
        this.capacity = capacity;
        this.speed = speed;
        this.maintenanceCost = maintenanceCost;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getPurchaseCost() {
        return purchaseCost;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public int getSpeed() {
        return speed;
    }
    
    public int getMaintenanceCost() {
        return maintenanceCost;
    }
    
    /**
     * Check if this vehicle type can carry a specific cargo type.
     * Buses/Minibuses only carry passengers; trucks carry industrial cargo.
     */
    public boolean canCarry(CargoType cargoType) {
        if (this == BUS || this == MINIBUS) {
            return cargoType == CargoType.PASSENGER;
        }
        // Trucks can carry any cargo except passengers
        return cargoType != CargoType.PASSENGER;
    }

    /** Returns true if this vehicle belongs to the passenger category. */
    public boolean isPassengerVehicle() {
        return this == BUS || this == MINIBUS;
    }

    /** Returns true if this vehicle belongs to the cargo category. */
    public boolean isCargoVehicle() {
        return this == TRUCK || this == CARGO_TRUCK;
    }
    
    public int getCost() {
        return purchaseCost;
    }
}
