package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all traffic lights in the game
 */
public class TrafficLightManager {
    private List<TrafficLight> trafficLights;
    private GameMap map;
    
    public TrafficLightManager(GameMap map) {
        this.trafficLights = new ArrayList<>();
        this.map = map;
    }
    
    /**
     * Places a traffic light at the specified intersection
     * Returns true if successful, false if invalid location
     */
    public boolean placeTrafficLight(int x, int y) {
        // Check if location already has a traffic light
        if (getTrafficLightAt(x, y) != null) {
            System.out.println("Traffic light already exists at (" + x + ", " + y + ")");
            return false;
        }
        
        // Check if this is a valid intersection (3-way or 4-way)
        Tile tile = map.getTile(x, y);
        if (tile == null || !tile.hasRoad()) {
            System.out.println("Cannot place traffic light: no road at (" + x + ", " + y + ")");
            return false;
        }
        
        // Count connections
        boolean north = hasRoadAt(x, y + 1);
        boolean south = hasRoadAt(x, y - 1);
        boolean east = hasRoadAt(x + 1, y);
        boolean west = hasRoadAt(x - 1, y);
        
        int connections = 0;
        if (north) connections++;
        if (south) connections++;
        if (east) connections++;
        if (west) connections++;
        
        // Must be 3-way or 4-way intersection
        if (connections < 3) {
            System.out.println("Cannot place traffic light: not an intersection (only " + connections + " connections)");
            return false;
        }
        
        // Determine which directions exist
        boolean hasNorthSouth = north || south;
        boolean hasEastWest = east || west;
        
        TrafficLight light = new TrafficLight(x, y, hasNorthSouth, hasEastWest);
        trafficLights.add(light);
        
        System.out.println("Traffic light placed at (" + x + ", " + y + ") with " + connections + " connections");
        return true;
    }
    
    /**
     * Removes a traffic light at the specified location
     */
    public boolean removeTrafficLight(int x, int y) {
        TrafficLight light = getTrafficLightAt(x, y);
        if (light != null) {
            trafficLights.remove(light);
            System.out.println("Traffic light removed at (" + x + ", " + y + ")");
            return true;
        }
        return false;
    }
    
    /**
     * Gets the traffic light at the specified location, or null if none exists
     */
    public TrafficLight getTrafficLightAt(int x, int y) {
        for (TrafficLight light : trafficLights) {
            if (light.getX() == x && light.getY() == y) {
                return light;
            }
        }
        return null;
    }
    
    /**
     * Updates all traffic lights
     */
    public void update(float deltaTime) {
        for (TrafficLight light : trafficLights) {
            light.update(deltaTime);
        }
    }
    
    /**
     * Checks if a vehicle should stop at a specific tile due to a red light
     */
    public boolean shouldVehicleStop(int x, int y, TrafficLight.Direction approachDirection) {
        TrafficLight light = getTrafficLightAt(x, y);
        if (light != null) {
            return light.shouldStop(approachDirection);
        }
        return false;
    }
    
    private boolean hasRoadAt(int x, int y) {
        Tile tile = map.getTile(x, y);
        return tile != null && tile.hasRoad();
    }
    
    public List<TrafficLight> getAllTrafficLights() {
        return new ArrayList<>(trafficLights);
    }
    
    public int getTrafficLightCount() {
        return trafficLights.size();
    }
}
