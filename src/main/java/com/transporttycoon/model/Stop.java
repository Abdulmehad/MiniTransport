package com.transporttycoon.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a stop on the road network where vehicles can load/unload cargo.
 * Stops are placed by the player near cities and industrial facilities.
 * Created by Person 1 (World/Map) with initialization in Person 2 (Transportation)
 */
public class Stop {
    private int x;
    private int y;
    private String name;
    private boolean isActive;
    
    // Current cargo waiting at this stop
    private Map<CargoType, Integer> cargoWaiting;
    
    public Stop(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.isActive = true;
        this.cargoWaiting = new HashMap<>();
        
        // Initialize cargo types with 0 units
        for (CargoType type : CargoType.values()) {
            cargoWaiting.put(type, 0);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getCargoAmount(CargoType type) {
        return cargoWaiting.getOrDefault(type, 0);
    }
    
    public void addCargo(CargoType type, int amount) {
        cargoWaiting.put(type, cargoWaiting.getOrDefault(type, 0) + amount);
    }
    
    public int removeCargo(CargoType type, int amount) {
        int available = cargoWaiting.getOrDefault(type, 0);
        int toRemove = Math.min(available, amount);
        cargoWaiting.put(type, available - toRemove);
        return toRemove;
    }
    
    public int getTotalCargoUnits() {
        return cargoWaiting.values().stream().mapToInt(Integer::intValue).sum();
    }
}
