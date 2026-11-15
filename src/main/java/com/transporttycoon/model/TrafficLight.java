package com.transporttycoon.model;

/**
 * Represents a traffic light at an intersection.
 * Controls vehicle flow by switching between red and green signals for different directions.
 */
public class TrafficLight {
    private int x;
    private int y;
    private TrafficLightState currentState;
    private float timer;
    private float greenDurationNorthSouth;  // seconds
    private float greenDurationEastWest;    // seconds
    private boolean hasNorthSouth;
    private boolean hasEastWest;
    
    public enum TrafficLightState {
        NORTH_SOUTH_GREEN,  // North-South has green, East-West has red
        EAST_WEST_GREEN     // East-West has green, North-South has red
    }
    
    /**
     * Creates a traffic light at the specified intersection
     */
    public TrafficLight(int x, int y, boolean hasNorthSouth, boolean hasEastWest) {
        this.x = x;
        this.y = y;
        this.hasNorthSouth = hasNorthSouth;
        this.hasEastWest = hasEastWest;
        this.greenDurationNorthSouth = 5.0f;  // default 5 seconds
        this.greenDurationEastWest = 5.0f;    // default 5 seconds
        this.currentState = TrafficLightState.NORTH_SOUTH_GREEN;
        this.timer = 0;
    }
    
    /**
     * Updates the traffic light state based on elapsed time
     */
    public void update(float deltaTime) {
        timer += deltaTime;
        
        float currentDuration = (currentState == TrafficLightState.NORTH_SOUTH_GREEN) 
            ? greenDurationNorthSouth 
            : greenDurationEastWest;
        
        if (timer >= currentDuration) {
            // Switch state
            currentState = (currentState == TrafficLightState.NORTH_SOUTH_GREEN) 
                ? TrafficLightState.EAST_WEST_GREEN 
                : TrafficLightState.NORTH_SOUTH_GREEN;
            timer = 0;
        }
    }
    
    /**
     * Checks if a vehicle approaching from a specific direction should stop
     */
    public boolean shouldStop(Direction approachDirection) {
        if (currentState == TrafficLightState.NORTH_SOUTH_GREEN) {
            // North-South is green, East-West is red
            return approachDirection == Direction.EAST || approachDirection == Direction.WEST;
        } else {
            // East-West is green, North-South is red
            return approachDirection == Direction.NORTH || approachDirection == Direction.SOUTH;
        }
    }
    
    /**
     * Gets the light color for a specific direction
     */
    public LightColor getLightColor(Direction direction) {
        if (currentState == TrafficLightState.NORTH_SOUTH_GREEN) {
            if (direction == Direction.NORTH || direction == Direction.SOUTH) {
                return LightColor.GREEN;
            } else {
                return LightColor.RED;
            }
        } else {
            if (direction == Direction.EAST || direction == Direction.WEST) {
                return LightColor.GREEN;
            } else {
                return LightColor.RED;
            }
        }
    }
    
    public enum LightColor {
        RED, GREEN
    }
    
    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }
    
    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public TrafficLightState getCurrentState() { return currentState; }
    public float getTimer() { return timer; }
    
    public float getGreenDurationNorthSouth() { return greenDurationNorthSouth; }
    public void setGreenDurationNorthSouth(float duration) { 
        this.greenDurationNorthSouth = Math.max(1.0f, Math.min(30.0f, duration)); 
    }
    
    public float getGreenDurationEastWest() { return greenDurationEastWest; }
    public void setGreenDurationEastWest(float duration) { 
        this.greenDurationEastWest = Math.max(1.0f, Math.min(30.0f, duration)); 
    }
    
    public boolean hasNorthSouth() { return hasNorthSouth; }
    public boolean hasEastWest() { return hasEastWest; }
    
    /**
     * Gets the remaining time for the current green light
     */
    public float getRemainingTime() {
        float currentDuration = (currentState == TrafficLightState.NORTH_SOUTH_GREEN) 
            ? greenDurationNorthSouth 
            : greenDurationEastWest;
        return currentDuration - timer;
    }
}
