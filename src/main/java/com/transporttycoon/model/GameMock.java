package com.transporttycoon.model;

import com.transporttycoon.GameConstants;

/**
 * Mock Game class for Person 1 (World/Map) to test map logic independently.
 * Replace with actual Game once Person 3 completes their implementation.
 */
public class GameMock {
    private int capital;
    private GameMap map;
    
    public GameMock() {
        this.capital = GameConstants.STARTING_CAPITAL;
        this.map = new GameMap(GameConstants.MAP_WIDTH, GameConstants.MAP_HEIGHT);
    }
    
    public int getCapital() {
        return capital;
    }
    
    public void addIncome(int amount) {
        capital += amount;
    }
    
    public void subtractCost(int amount) {
        capital -= amount;
    }
    
    public boolean canAfford(int cost) {
        return capital >= cost;
    }
    
    public GameMap getMap() {
        return map;
    }
    
    public void update(float deltaTime) {
        // Mock implementation - would be handled by Person 3
    }
}
