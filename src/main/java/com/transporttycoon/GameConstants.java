package com.transporttycoon;

/**
 * Shared game constants and configuration values.
 * Used by all team members to ensure consistency.
 */
public class GameConstants {
    
    // ========== MAP CONSTANTS ==========
    public static final int MAP_WIDTH = 50;
    public static final int MAP_HEIGHT = 50;
    public static final int TILE_SIZE = 32; // pixels
    
    // ========== ECONOMY CONSTANTS ==========
    public static final int STARTING_CAPITAL = 50000;
    public static final int ROAD_COST_PER_TILE = 100;
    public static final int FOREST_CLEARING_COST_PER_TREE = 25;
    public static final int STOP_COST = 500;
    public static final int GARAGE_BUILD_COST = 2500;

    // ========== FOREST CONSTANTS ==========
    public static final int MAX_TREES_PER_TILE = 4;
    public static final float INITIAL_FOREST_SPAWN_DELAY_SECONDS = 60f;
    public static final double FOREST_GROWTH_CHANCE = 0.35;
    public static final double FOREST_SPREAD_CHANCE_PER_NEIGHBOR = 0.20;
    public static final double FOREST_SPAWN_CHANCE = 0.002;
    
    // ========== VEHICLE COSTS ==========
    public static final int TRUCK_PURCHASE_COST = 800;
    public static final int TRUCK_CAPACITY = 50;
    public static final int TRUCK_SPEED = 80; // tiles per minute
    public static final int TRUCK_MAINTENANCE_COST = 1; // per game tick
    
    public static final int CARGO_TRUCK_PURCHASE_COST = 1500;
    public static final int CARGO_TRUCK_CAPACITY = 80;
    public static final int CARGO_TRUCK_SPEED = 60;
    public static final int CARGO_TRUCK_MAINTENANCE_COST = 2;
    
    public static final int BUS_PURCHASE_COST = 600;
    public static final int BUS_CAPACITY = 40;
    public static final int BUS_SPEED = 120;
    public static final int BUS_MAINTENANCE_COST = 1;

    public static final int MINIBUS_PURCHASE_COST = 400;
    public static final int MINIBUS_CAPACITY = 20;
    public static final int MINIBUS_SPEED = 150;
    public static final int MINIBUS_MAINTENANCE_COST = 1;

    // ========== GARAGE / MAINTENANCE CONSTANTS ==========
    public static final int VEHICLE_BASE_MAINTENANCE_INTERVAL_TICKS = 10800;
    public static final int VEHICLE_MAINTENANCE_INTERVAL_AGE_PENALTY_TICKS = 360;
    public static final int VEHICLE_MIN_MAINTENANCE_INTERVAL_TICKS = 1800;
    public static final int VEHICLE_MAINTENANCE_DURATION_TICKS = 120;
    public static final int VEHICLE_OVER_AGE_DAYS = 5;
    public static final double VEHICLE_SELL_VALUE_MIN_RATIO = 0.10;
    public static final float VEHICLE_OPERATIONAL_SECONDS_PER_AGE_YEAR = 180f;
    
    // ========== TIME CONSTANTS ==========
    public static final int TICKS_PER_SECOND = 10;
    public static final float NORMAL_SPEED_MULTIPLIER = 1.0f;
    public static final float FAST_SPEED_MULTIPLIER = 2.0f;
    public static final float VERY_FAST_SPEED_MULTIPLIER = 4.0f;
    
    // ========== CITY/FACILITY CONSTANTS ==========
    public static final int CITY_MIN_SIZE = 3; // 3x3 grid
    public static final int FACILITY_MIN_SIZE = 2; // 2x2 grid
    
    // ========== INCOME CONSTANTS ==========
    public static final int CARGO_INCOME_PER_UNIT = 10;
    public static final int PASSENGER_INCOME_PER_UNIT = 5;

    // ========== PATHFINDER CONSTANTS ==========
    /** Tile radius to search for a producing facility when accumulating stop cargo. */
    public static final int STOP_FACILITY_SEARCH_RADIUS = 3;
    /** Number of game ticks between each cargo accumulation cycle at stops. */
    public static final int CARGO_ACCUMULATION_INTERVAL = 10;
}
