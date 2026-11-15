package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a route that vehicles follow, consisting of multiple stops in circular order.
 * Created by Person 2 (Transportation/Vehicles)
 */
public class Route {
    private String name;
    private List<Stop> stops;
    private List<Vehicle> assignedVehicles;
    
    public Route(String name) {
        this.name = name;
        this.stops = new ArrayList<>();
        this.assignedVehicles = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addStop(Stop stop) {
        stops.add(stop);
    }
    
    public void removeStop(Stop stop) {
        stops.remove(stop);
    }
    
    public List<Stop> getStops() {
        return new ArrayList<>(stops);
    }
    
    public int getStopCount() {
        return stops.size();
    }
    
    public Stop getStop(int index) {
        if (index >= 0 && index < stops.size()) {
            return stops.get(index);
        }
        return null;
    }
    
    /**
     * Get the next stop in the circular route.
     * After the last stop, returns the first stop.
     */
    public Stop getNextStop(Stop currentStop) {
        int currentIndex = stops.indexOf(currentStop);
        if (currentIndex >= 0) {
            int nextIndex = (currentIndex + 1) % stops.size();
            return stops.get(nextIndex);
        }
        return stops.isEmpty() ? null : stops.get(0);
    }
    
    public void assignVehicle(Vehicle vehicle) {
        if (!assignedVehicles.contains(vehicle)) {
            assignedVehicles.add(vehicle);
        }
    }
    
    public void unassignVehicle(Vehicle vehicle) {
        assignedVehicles.remove(vehicle);
    }
    
    public List<Vehicle> getAssignedVehicles() {
        return new ArrayList<>(assignedVehicles);
    }
}
