package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TrafficLight class - intersection control system
 */
class TrafficLightTest {
    
    private TrafficLight trafficLight;
    
    @BeforeEach
    void setUp() {
        // Create a traffic light at position (5, 5) with both N-S and E-W directions
        trafficLight = new TrafficLight(5, 5, true, true);
    }
    
    @Test
    void testInitialState() {
        // Traffic light should start in NORTH_SOUTH_GREEN state
        assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, 
                     trafficLight.getCurrentState());
        assertEquals(0f, trafficLight.getTimer(), 0.01f);
    }
    
    @Test
    void testPosition() {
        assertEquals(5, trafficLight.getX());
        assertEquals(5, trafficLight.getY());
    }
    
    @Test
    void testDirectionAvailability() {
        assertTrue(trafficLight.hasNorthSouth());
        assertTrue(trafficLight.hasEastWest());
    }
    
    @Test
    void testLightSwitchingFromNorthSouthToEastWest() {
        // Start in NORTH_SOUTH_GREEN
        assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, 
                     trafficLight.getCurrentState());
        
        // Update for full duration (default 5 seconds)
        float duration = trafficLight.getGreenDurationNorthSouth();
        trafficLight.update(duration + 0.1f);
        
        // Should switch to EAST_WEST_GREEN
        assertEquals(TrafficLight.TrafficLightState.EAST_WEST_GREEN, 
                     trafficLight.getCurrentState());
    }
    
    @Test
    void testLightSwitchingFromEastWestToNorthSouth() {
        // Switch to EAST_WEST_GREEN first
        trafficLight.update(trafficLight.getGreenDurationNorthSouth() + 0.1f);
        assertEquals(TrafficLight.TrafficLightState.EAST_WEST_GREEN, 
                     trafficLight.getCurrentState());
        
        // Update for full E-W duration
        trafficLight.update(trafficLight.getGreenDurationEastWest() + 0.1f);
        
        // Should switch back to NORTH_SOUTH_GREEN
        assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, 
                     trafficLight.getCurrentState());
    }
    
    @Test
    void testVehicleShouldStopAtRedLight_NorthSouthGreen() {
        // When North-South is green, East-West should stop
        assertFalse(trafficLight.shouldStop(TrafficLight.Direction.NORTH));
        assertFalse(trafficLight.shouldStop(TrafficLight.Direction.SOUTH));
        assertTrue(trafficLight.shouldStop(TrafficLight.Direction.EAST));
        assertTrue(trafficLight.shouldStop(TrafficLight.Direction.WEST));
    }
    
    @Test
    void testVehicleShouldStopAtRedLight_EastWestGreen() {
        // Switch to EAST_WEST_GREEN
        trafficLight.update(trafficLight.getGreenDurationNorthSouth() + 0.1f);
        
        // When East-West is green, North-South should stop
        assertTrue(trafficLight.shouldStop(TrafficLight.Direction.NORTH));
        assertTrue(trafficLight.shouldStop(TrafficLight.Direction.SOUTH));
        assertFalse(trafficLight.shouldStop(TrafficLight.Direction.EAST));
        assertFalse(trafficLight.shouldStop(TrafficLight.Direction.WEST));
    }
    
    @Test
    void testGetLightColor_NorthSouthGreen() {
        assertEquals(TrafficLight.LightColor.GREEN, 
                     trafficLight.getLightColor(TrafficLight.Direction.NORTH));
        assertEquals(TrafficLight.LightColor.GREEN, 
                     trafficLight.getLightColor(TrafficLight.Direction.SOUTH));
        assertEquals(TrafficLight.LightColor.RED, 
                     trafficLight.getLightColor(TrafficLight.Direction.EAST));
        assertEquals(TrafficLight.LightColor.RED, 
                     trafficLight.getLightColor(TrafficLight.Direction.WEST));
    }
    
    @Test
    void testGetLightColor_EastWestGreen() {
        // Switch to EAST_WEST_GREEN
        trafficLight.update(trafficLight.getGreenDurationNorthSouth() + 0.1f);
        
        assertEquals(TrafficLight.LightColor.RED, 
                     trafficLight.getLightColor(TrafficLight.Direction.NORTH));
        assertEquals(TrafficLight.LightColor.RED, 
                     trafficLight.getLightColor(TrafficLight.Direction.SOUTH));
        assertEquals(TrafficLight.LightColor.GREEN, 
                     trafficLight.getLightColor(TrafficLight.Direction.EAST));
        assertEquals(TrafficLight.LightColor.GREEN, 
                     trafficLight.getLightColor(TrafficLight.Direction.WEST));
    }
    
    @Test
    void testConfigurableGreenDuration_NorthSouth() {
        trafficLight.setGreenDurationNorthSouth(10.0f);
        assertEquals(10.0f, trafficLight.getGreenDurationNorthSouth(), 0.01f);
    }
    
    @Test
    void testConfigurableGreenDuration_EastWest() {
        trafficLight.setGreenDurationEastWest(8.0f);
        assertEquals(8.0f, trafficLight.getGreenDurationEastWest(), 0.01f);
    }
    
    @Test
    void testGreenDurationMinimumBound() {
        // Test minimum bound (1 second)
        trafficLight.setGreenDurationNorthSouth(0.5f);
        assertEquals(1.0f, trafficLight.getGreenDurationNorthSouth(), 0.01f);
        
        trafficLight.setGreenDurationEastWest(0.1f);
        assertEquals(1.0f, trafficLight.getGreenDurationEastWest(), 0.01f);
    }
    
    @Test
    void testGreenDurationMaximumBound() {
        // Test maximum bound (30 seconds)
        trafficLight.setGreenDurationNorthSouth(50.0f);
        assertEquals(30.0f, trafficLight.getGreenDurationNorthSouth(), 0.01f);
        
        trafficLight.setGreenDurationEastWest(100.0f);
        assertEquals(30.0f, trafficLight.getGreenDurationEastWest(), 0.01f);
    }
    
    @Test
    void testRemainingTime() {
        float remaining = trafficLight.getRemainingTime();
        assertEquals(5.0f, remaining, 0.01f); // Default duration is 5 seconds
        
        // Update by 2 seconds
        trafficLight.update(2.0f);
        remaining = trafficLight.getRemainingTime();
        assertEquals(3.0f, remaining, 0.01f);
    }
    
    @Test
    void testTimerReset_OnStateChange() {
        // Update to just before state change
        trafficLight.update(4.9f);
        assertTrue(trafficLight.getTimer() > 0);
        
        // Update past state change
        trafficLight.update(0.2f);
        
        // Timer should be reset (close to 0)
        assertTrue(trafficLight.getTimer() < 1.0f);
    }
    
    @Test
    void testMultipleCycles() {
        // Go through 3 complete cycles
        for (int i = 0; i < 3; i++) {
            // North-South green
            assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, 
                         trafficLight.getCurrentState());
            trafficLight.update(5.1f);
            
            // East-West green
            assertEquals(TrafficLight.TrafficLightState.EAST_WEST_GREEN, 
                         trafficLight.getCurrentState());
            trafficLight.update(5.1f);
        }
        
        // Should be back to North-South green
        assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, 
                     trafficLight.getCurrentState());
    }
    
    @Test
    void testThreeWayIntersection_NorthSouthOnly() {
        TrafficLight threeWay = new TrafficLight(3, 3, true, false);
        
        assertTrue(threeWay.hasNorthSouth());
        assertFalse(threeWay.hasEastWest());
    }
    
    @Test
    void testThreeWayIntersection_EastWestOnly() {
        TrafficLight threeWay = new TrafficLight(3, 3, false, true);
        
        assertFalse(threeWay.hasNorthSouth());
        assertTrue(threeWay.hasEastWest());
    }
}
