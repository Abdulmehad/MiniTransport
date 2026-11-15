package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TrafficLightManager - manages all traffic lights in the game
 */
class TrafficLightManagerTest {
    
    private TrafficLightManager manager;
    private GameMap map;
    
    @BeforeEach
    void setUp() {
        map = new GameMap(20, 15);
        manager = new TrafficLightManager(map);
        
        // Create a 4-way intersection at (5, 5)
        map.getTile(5, 5).setRoad(true);
        map.getTile(5, 6).setRoad(true); // North
        map.getTile(5, 4).setRoad(true); // South
        map.getTile(6, 5).setRoad(true); // East
        map.getTile(4, 5).setRoad(true); // West
    }
    
    @Test
    void testPlaceTrafficLight_FourWayIntersection() {
        boolean success = manager.placeTrafficLight(5, 5);
        
        assertTrue(success);
        assertEquals(1, manager.getTrafficLightCount());
        assertNotNull(manager.getTrafficLightAt(5, 5));
    }
    
    @Test
    void testPlaceTrafficLight_ThreeWayIntersection() {
        // Create a 3-way intersection at (10, 10)
        map.getTile(10, 10).setRoad(true);
        map.getTile(10, 11).setRoad(true); // North
        map.getTile(11, 10).setRoad(true); // East
        map.getTile(9, 10).setRoad(true);  // West
        // No south connection
        
        boolean success = manager.placeTrafficLight(10, 10);
        
        assertTrue(success);
        assertEquals(1, manager.getTrafficLightCount());
    }
    
    @Test
    void testPlaceTrafficLight_NotEnoughConnections() {
        // Create a tile with only 2 connections (not an intersection)
        map.getTile(8, 8).setRoad(true);
        map.getTile(8, 9).setRoad(true); // North
        map.getTile(9, 8).setRoad(true); // East
        
        boolean success = manager.placeTrafficLight(8, 8);
        
        assertFalse(success);
        assertEquals(0, manager.getTrafficLightCount());
    }
    
    @Test
    void testPlaceTrafficLight_NoRoad() {
        // Try to place on empty tile
        boolean success = manager.placeTrafficLight(15, 15);
        
        assertFalse(success);
        assertEquals(0, manager.getTrafficLightCount());
    }
    
    @Test
    void testPlaceTrafficLight_AlreadyExists() {
        // Place first light
        manager.placeTrafficLight(5, 5);
        
        // Try to place another at same location
        boolean success = manager.placeTrafficLight(5, 5);
        
        assertFalse(success);
        assertEquals(1, manager.getTrafficLightCount());
    }
    
    @Test
    void testRemoveTrafficLight() {
        manager.placeTrafficLight(5, 5);
        assertEquals(1, manager.getTrafficLightCount());
        
        boolean removed = manager.removeTrafficLight(5, 5);
        
        assertTrue(removed);
        assertEquals(0, manager.getTrafficLightCount());
        assertNull(manager.getTrafficLightAt(5, 5));
    }
    
    @Test
    void testRemoveTrafficLight_NotExists() {
        boolean removed = manager.removeTrafficLight(5, 5);
        
        assertFalse(removed);
    }
    
    @Test
    void testGetTrafficLightAt() {
        manager.placeTrafficLight(5, 5);
        
        TrafficLight light = manager.getTrafficLightAt(5, 5);
        
        assertNotNull(light);
        assertEquals(5, light.getX());
        assertEquals(5, light.getY());
    }
    
    @Test
    void testGetTrafficLightAt_NotExists() {
        TrafficLight light = manager.getTrafficLightAt(10, 10);
        
        assertNull(light);
    }
    
    @Test
    void testUpdate_AllLights() {
        // Place multiple lights
        manager.placeTrafficLight(5, 5);
        
        // Create another intersection
        map.getTile(10, 10).setRoad(true);
        map.getTile(10, 11).setRoad(true);
        map.getTile(11, 10).setRoad(true);
        map.getTile(9, 10).setRoad(true);
        manager.placeTrafficLight(10, 10);
        
        // Update all lights
        manager.update(2.0f);
        
        // Verify both lights were updated
        TrafficLight light1 = manager.getTrafficLightAt(5, 5);
        TrafficLight light2 = manager.getTrafficLightAt(10, 10);
        
        assertTrue(light1.getTimer() > 0);
        assertTrue(light2.getTimer() > 0);
    }
    
    @Test
    void testShouldVehicleStop_RedLight() {
        manager.placeTrafficLight(5, 5);
        TrafficLight light = manager.getTrafficLightAt(5, 5);
        
        // North-South is green initially, so East should stop
        boolean shouldStop = manager.shouldVehicleStop(5, 5, TrafficLight.Direction.EAST);
        
        assertTrue(shouldStop);
    }
    
    @Test
    void testShouldVehicleStop_GreenLight() {
        manager.placeTrafficLight(5, 5);
        
        // North-South is green initially, so North should not stop
        boolean shouldStop = manager.shouldVehicleStop(5, 5, TrafficLight.Direction.NORTH);
        
        assertFalse(shouldStop);
    }
    
    @Test
    void testShouldVehicleStop_NoTrafficLight() {
        // No traffic light at this location
        boolean shouldStop = manager.shouldVehicleStop(10, 10, TrafficLight.Direction.NORTH);
        
        assertFalse(shouldStop);
    }
    
    @Test
    void testGetAllTrafficLights() {
        manager.placeTrafficLight(5, 5);
        
        // Create another intersection
        map.getTile(10, 10).setRoad(true);
        map.getTile(10, 11).setRoad(true);
        map.getTile(11, 10).setRoad(true);
        map.getTile(9, 10).setRoad(true);
        manager.placeTrafficLight(10, 10);
        
        assertEquals(2, manager.getAllTrafficLights().size());
    }
    
    @Test
    void testMultipleTrafficLights_IndependentOperation() {
        // Place two lights with different durations
        manager.placeTrafficLight(5, 5);
        TrafficLight light1 = manager.getTrafficLightAt(5, 5);
        light1.setGreenDurationNorthSouth(3.0f);
        
        map.getTile(10, 10).setRoad(true);
        map.getTile(10, 11).setRoad(true);
        map.getTile(11, 10).setRoad(true);
        map.getTile(9, 10).setRoad(true);
        manager.placeTrafficLight(10, 10);
        TrafficLight light2 = manager.getTrafficLightAt(10, 10);
        light2.setGreenDurationNorthSouth(7.0f);
        
        // Update for 4 seconds
        manager.update(4.0f);
        
        // Light 1 should have switched (3s duration)
        assertEquals(TrafficLight.TrafficLightState.EAST_WEST_GREEN, light1.getCurrentState());
        
        // Light 2 should still be in first state (7s duration)
        assertEquals(TrafficLight.TrafficLightState.NORTH_SOUTH_GREEN, light2.getCurrentState());
    }
}
