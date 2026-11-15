package com.transporttycoon.model;

import com.transporttycoon.GameConstants;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Represents the main game state and logic
 */
public class Game {
    private TimeSpeed timeSpeed;
    private Player player;
    private GameMap map;
    private VehicleManager vehicleManager;
    private RouteManager routeManager;
    private StopManager stopManager;
    private TrafficLightManager trafficLightManager;
    private List<Vehicle> vehicles;
    private List<Route> routes;
    private float gameTime;
    private boolean running;
    private float monthlyTickTimer;
    private static final float MONTH_DURATION = 30f;
    private int currentYear;
    private int currentMonth;
    private final Random random;
    private float initialForestSpawnTimer;
    private boolean initialForestSpawned;

    public Game() {
        this.timeSpeed = TimeSpeed.NORMAL;
        this.player = new Player(50000.0);
        this.map = createTestMap();
        this.vehicleManager = new VehicleManager(map, player);
        this.routeManager = new RouteManager();
        this.stopManager = new StopManager(map, player);
        this.trafficLightManager = new TrafficLightManager(map);
        this.vehicleManager.setTrafficLightManager(trafficLightManager);
        this.vehicles = new ArrayList<>();
        this.routes = new ArrayList<>();
        this.gameTime = 0;
        this.monthlyTickTimer = 0;
        this.running = false;
        this.currentYear = 1900;
        this.currentMonth = 1;
        this.random = new Random();
        this.initialForestSpawnTimer = 0f;
        this.initialForestSpawned = map.hasAnyTrees();
    }
    
    /**
     * Starts the game
     */
    public void start() {
        this.running = true;
        System.out.println("Transport Tycoon Game Started!");
        System.out.println(map.getMapAsText());
    }
    
    /**
     * Updates the game state each frame
     */
    public void update(float deltaTime) {
        if (!running || timeSpeed.isPaused()) {
            return;
        }
        
        float adjustedDeltaTime = deltaTime * timeSpeed.getMultiplier();
        gameTime += adjustedDeltaTime;
        monthlyTickTimer += adjustedDeltaTime;

        if (!initialForestSpawned) {
            initialForestSpawnTimer += adjustedDeltaTime;
            if (initialForestSpawnTimer >= GameConstants.INITIAL_FOREST_SPAWN_DELAY_SECONDS) {
                initialForestSpawned = map.spawnRandomInitialTree(random) || map.hasAnyTrees();
            }
        }
        
        // Check for bankruptcy
        if (checkBankruptcy()) {
            System.out.println("BANKRUPTCY! Game Over.");
            running = false;
            return;
        }
        
        // Update traffic lights
        trafficLightManager.update(adjustedDeltaTime);
        
        // Update vehicle fleet (movement, cargo, income, maintenance)
        vehicleManager.update(adjustedDeltaTime, currentYear);

        // Update stop cargo accumulation
        stopManager.update();

        // Update production in all industrial facilities
        updateProduction(adjustedDeltaTime);
        
        // Process monthly updates
        if (monthlyTickTimer >= MONTH_DURATION) {
            processMonthlyUpdates();
            monthlyTickTimer = 0;
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            player.resetMonthlyStats();
        }
    }
    
    /**
     * Processes monthly updates (maintenance costs, salary, city demands, etc.)
     */
    private void processMonthlyUpdates() {
        map.updateForests(random);

        // Note: per-tick maintenance costs are deducted continuously by VehicleManager.
        // A separate monthly charge here would double-bill every vehicle.

        // Process city monthly updates (reset deliveries, adjust demands).
        // Use a Set to ensure each City object is processed ONCE even though
        // a 3×3 city occupies 9 tiles that all reference the same City instance.
        Set<City> processedCities = new LinkedHashSet<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getContent() instanceof City) {
                    processedCities.add((City) tile.getContent());
                }
            }
        }
        for (City city : processedCities) {
            city.endMonth();
        }
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              MONTHLY FINANCIAL REPORT                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Current Balance:     %-38s║%n", player.getFormattedCapital());
        System.out.printf("║ Monthly Income:      +$%-37.2f║%n", player.getMonthlyIncome());
        System.out.printf("║ Monthly Expenses:    -$%-37.2f║%n", player.getMonthlyExpenses());
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        double profit = player.getMonthlyProfit();
        if (profit >= 0) {
            System.out.printf("║ NET PROFIT:          +$%-37.2f║%n", profit);
        } else {
            System.out.printf("║ NET LOSS:            -$%-37.2f║%n", Math.abs(profit));
        }
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        // Print income breakdown
        System.out.println("\n--- INCOME BREAKDOWN ---");
        int deliveryCount = 0;
        int bonusCount = 0;
        double deliveryIncome = 0;
        double bonusIncome = 0;
        
        // Use a large window so the monthly breakdown captures all delivery
        // transactions from the past month, not just the most recent 100.
        for (Player.Transaction tx : player.getRecentTransactions(10_000)) {
            if (tx.type == Player.TransactionType.INCOME) {
                if (tx.description.contains("BONUS")) {
                    bonusCount++;
                    bonusIncome += tx.amount;
                } else if (tx.description.contains("Delivery")) {
                    deliveryCount++;
                    deliveryIncome += tx.amount;
                }
            }
        }
        
        System.out.printf("Standard Deliveries: %d trips = $%.2f%n", deliveryCount, deliveryIncome);
        System.out.printf("Bonus Deliveries:    %d trips = $%.2f%n", bonusCount, bonusIncome);
        System.out.printf("Total Delivery Income: $%.2f%n", deliveryIncome + bonusIncome);
        
        // Print city status
        printCityStatus();
    }
    
    /**
     * Prints status of all cities including demands and satisfaction.
     * Deduplicates via a Set so each City object is printed exactly once.
     */
    private void printCityStatus() {
        System.out.println("--- CITY STATUS ---");
        Set<City> seen = new LinkedHashSet<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getContent() instanceof City) {
                    seen.add((City) tile.getContent());
                }
            }
        }
        for (City city : seen) {
            System.out.println(city.toString());
        }
    }
    
    /**
     * Sets the game time speed
     */
    public void setTimeSpeed(TimeSpeed speed) {
        this.timeSpeed = speed;
        System.out.println("Time speed changed to: " + speed.name());
    }
    
    /**
     * Checks if the player is bankrupt
     */
    public boolean checkBankruptcy() {
        return player.isBankrupt();
    }
    
    /**
     * Updates production in all industrial facilities and city demands.
     * Uses a Set to deduplicate — a 2×2 facility occupies 4 tiles that all
     * reference the same IndustrialFacility instance, so we must not call
     * produce() four times per frame.
     */
    private void updateProduction(float deltaTime) {
        // Update all cities (demand fluctuations) — deduplicated
        Set<City> cities = new LinkedHashSet<>();
        Set<IndustrialFacility> facilities = new LinkedHashSet<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;
                if (tile.getContent() instanceof City)
                    cities.add((City) tile.getContent());
                else if (tile.getContent() instanceof IndustrialFacility)
                    facilities.add((IndustrialFacility) tile.getContent());
            }
        }
        for (City city : cities) {
            city.update(deltaTime);
        }
        // update(deltaTime) advances the internal production timer and calls produce()
        // only every PRODUCTION_INTERVAL seconds — prevents instant stock-fill each frame.
        for (IndustrialFacility facility : facilities) {
            facility.update(deltaTime);
        }
    }
    
    /**
     * Buys a new vehicle via VehicleManager and positions it at the given stop.
     */
    public boolean buyVehicle(VehicleType type, Stop initialStop) {
        Vehicle vehicle = vehicleManager.purchaseVehicle(type, currentYear);
        if (vehicle == null) {
            System.out.println("Insufficient funds to buy vehicle!");
            return false;
        }
        // Position at the initial stop – will depart once a route is assigned
        vehicle.setAssignedRoute(null);
        vehicle.setPosition(initialStop.getX(), initialStop.getY());
        vehicles.add(vehicle);
        System.out.println("Bought new vehicle: " + vehicle.getName());
        return true;
    }

    public boolean buyVehicleFromGarage(VehicleType type, IndustrialFacility garage, int garageRoadX, int garageRoadY) {
        if (garage == null || garage.getFacilityType() != IndustrialFacility.FacilityType.GARAGE) {
            System.out.println("Vehicle purchase requires a valid garage.");
            return false;
        }

        Vehicle vehicle = vehicleManager.purchaseVehicleAtGarage(type, currentYear, garage, garageRoadX, garageRoadY);
        if (vehicle == null) {
            System.out.println("Insufficient funds to buy vehicle!");
            return false;
        }

        vehicles.add(vehicle);
        System.out.println("Bought new vehicle at garage: " + vehicle.getName());
        return true;
    }

    public int sellOverAgedVehicles() {
        List<Vehicle> soldVehicles = vehicleManager.sellOverAgedVehicles(currentYear, GameConstants.VEHICLE_OVER_AGE_DAYS);
        vehicles.removeAll(soldVehicles);
        return soldVehicles.size();
    }
    
    /**
     * Creates a route and assigns it to a vehicle
     */
    public boolean assignRoute(Vehicle vehicle, Route route) {
        if (vehicle == null || route == null) {
            return false;
        }

        vehicleManager.assignRoute(vehicle, route);
        if (!routes.contains(route)) {
            routes.add(route);
        }
        
        System.out.println("Assigned route to " + vehicle.getName());
        return true;
    }
    
    /**
     * Creates a test map with some cities and facilities for demonstration
     */
    private GameMap createTestMap() {
        GameMap testMap = new GameMap(20, 15);
        
        // Add some cities
        City newYork = new City("New York", 100000);
        testMap.placeCity(newYork, 2, 2);
        
        City chicago = new City("Chicago", 75000);
        testMap.placeCity(chicago, 12, 8);
        
        // Add some industrial facilities
        IndustrialFacility ironMine = IndustrialFacility.createMine("Iron Mine Alpha");
        testMap.placeFacility(ironMine, 8, 3);
        
        IndustrialFacility steelMill = IndustrialFacility.createSteelFactory("Steel Works Beta");
        testMap.placeFacility(steelMill, 15, 5);
        
        IndustrialFacility farm = IndustrialFacility.createFarm("Green Valley Farm");
        testMap.placeFacility(farm, 5, 10);
        
        IndustrialFacility paperMill = IndustrialFacility.createPaperFactory("Paper Mill Delta");
        testMap.placeFacility(paperMill, 10, 12);
        
        return testMap;
    }
    
    // Getters
    public TimeSpeed getTimeSpeed() { return timeSpeed; }
    public Player getPlayer() { return player; }
    public GameMap getMap() { return map; }
    public VehicleManager getVehicleManager() { return vehicleManager; }
    public RouteManager getRouteManager() { return routeManager; }
    public StopManager getStopManager() { return stopManager; }
    public TrafficLightManager getTrafficLightManager() { return trafficLightManager; }
    public float getGameTime() { return gameTime; }
    public int getGameYear() { return currentYear; }
    public int getGameMonth() { return currentMonth; }
    public boolean isRunning() { return running; }
    public List<Vehicle> getVehicles() { return new ArrayList<>(vehicles); }
    public List<Route> getRoutes() { return new ArrayList<>(routes); }

    // ── Persistence setters (used by PersistenceManager on load) ─────────────

    public void setGameTimeDirect(float gameTime) {
        this.gameTime = gameTime;
    }

    public void setGameYearDirect(int year) {
        this.currentYear = year;
    }

    public void setGameMonthDirect(int month) {
        this.currentMonth = month;
    }

    /** Adds a vehicle directly to the game list (used on load — bypasses purchase). */
    public void addVehicle(Vehicle vehicle) {
        if (vehicle != null && !vehicles.contains(vehicle)) {
            vehicles.add(vehicle);
        }
    }

    /** Adds a route directly to the game list (used on load). */
    public void addRoute(Route route) {
        if (route != null && !routes.contains(route)) {
            routes.add(route);
        }
    }
}