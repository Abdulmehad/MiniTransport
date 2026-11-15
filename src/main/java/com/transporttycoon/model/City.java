package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a city on the map that demands passengers and goods
 * Cities occupy at least 3x3 tiles and have internal road networks
 */
public class City extends TileContent {
    // Demand tracking: maps each demanded cargo type to monthly demand amount
    private java.util.Map<CargoType, Integer> cargoDemandsMonthly;
    private java.util.Map<CargoType, Integer> cargoDelivered;
    
    private int population;
    private int passengerDemandMonthly;
    private int passengersDelivered;
    private float cityHappiness; // 0.0 to 1.0, affects growth
    
    private List<Tile> occupiedTiles;
    private List<CargoType> demandedGoods;
    
    public City(String name, int population) {
        super(name);
        this.population = population;
        this.occupiedTiles = new ArrayList<>();
        this.demandedGoods = new ArrayList<>();
        
        // Initialize demand tracking maps
        this.cargoDemandsMonthly = new java.util.HashMap<>();
        this.cargoDelivered = new java.util.HashMap<>();
        
        // Calculate demands based on population
        this.passengerDemandMonthly = calculatePassengerDemand(population);
        this.passengersDelivered = 0;
        this.cityHappiness = 0.7f; // Start at neutral happiness
        
        // Cities typically demand finished products
        this.demandedGoods.addAll(Arrays.asList(CargoType.PAPER, CargoType.STEEL));
        
        // Initialize cargo demands (roughly proportional to population)
        cargoDemandsMonthly.put(CargoType.PAPER, population / 5);  // 1 unit per 5 people
        cargoDemandsMonthly.put(CargoType.STEEL, population / 8);  // 1 unit per 8 people
        cargoDelivered.put(CargoType.PAPER, 0);
        cargoDelivered.put(CargoType.STEEL, 0);
    }
    
    /**
     * Calculates base passenger demand from population
     */
    private int calculatePassengerDemand(int pop) {
        return Math.max(10, pop / 50); // At least 10, 1 per 50 people
    }
    
    @Override
    public int getWidth() {
        return 3; // Minimum 3x3 as per specification
    }
    
    @Override
    public int getHeight() {
        return 3; // Minimum 3x3 as per specification
    }
    
    @Override
    public char getDisplayChar() {
        return 'C';
    }
    
    public int getPopulation() {
        return population;
    }
    
    public void setPopulation(int population) {
        this.population = population;
        this.passengerDemandMonthly = calculatePassengerDemand(population);
    }
    
    /**
     * Gets the monthly passenger demand for this city
     */
    public int getPassengerDemandMonthly() {
        return passengerDemandMonthly;
    }
    
    /**
     * Gets the monthly cargo demand for a specific cargo type
     * Returns 0 if the city doesn't demand this cargo
     */
    public int getCargoDemandMonthly(CargoType cargo) {
        return cargoDemandsMonthly.getOrDefault(cargo, 0);
    }
    
    /**
     * Gets all cargo demands as a map (cargo type -> monthly amount)
     */
    public java.util.Map<CargoType, Integer> getAllCargoDemands() {
        return new java.util.HashMap<>(cargoDemandsMonthly);
    }
    
    /**
     * Gets how much of a cargo type has been delivered this month
     */
    public int getCargoDelivered(CargoType cargo) {
        return cargoDelivered.getOrDefault(cargo, 0);
    }
    
    /**
     * Gets how many passengers have been delivered this month
     */
    public int getPassengersDelivered() {
        return passengersDelivered;
    }
    
    /**
     * Gets the current city happiness (0.0 to 1.0)
     */
    public float getCityHappiness() {
        return cityHappiness;
    }
    
    /**
     * Delivers cargo to the city
     * @param cargo The cargo type to deliver
     * @param amount Amount to deliver
     * @return Amount actually accepted (capped at remaining demand)
     */
    public int deliverCargo(CargoType cargo, int amount) {
        if (!cargoDemandsMonthly.containsKey(cargo)) {
            return 0; // City doesn't want this cargo
        }
        
        int currentDelivered = cargoDelivered.getOrDefault(cargo, 0);
        int demand = cargoDemandsMonthly.get(cargo);
        int remaining = Math.max(0, demand - currentDelivered);
        int accepted = Math.min(amount, remaining);
        
        cargoDelivered.put(cargo, currentDelivered + accepted);
        return accepted;
    }
    
    /**
     * Delivers passengers to the city
     * @param amount Number of passengers to deliver
     * @return Amount actually accepted (capped at remaining demand)
     */
    public int deliverPassengers(int amount) {
        int remaining = Math.max(0, passengerDemandMonthly - passengersDelivered);
        int accepted = Math.min(amount, remaining);
        passengersDelivered += accepted;
        return accepted;
    }
    
    /**
     * Resets monthly delivery counters and updates city happiness based on satisfaction
     * Should be called once per game month
     */
    public void endMonth() {
        // Calculate cargo satisfaction (0.0 to 1.0)
        float cargoSatisfaction = 0.0f;
        int totalDemand = 0;
        int totalDelivered = 0;
        
        for (CargoType cargo : demandedGoods) {
            int demand = cargoDemandsMonthly.getOrDefault(cargo, 0);
            int delivered = cargoDelivered.getOrDefault(cargo, 0);
            totalDemand += demand;
            totalDelivered += delivered;
        }
        
        if (totalDemand > 0) {
            cargoSatisfaction = (float) totalDelivered / totalDemand;
        }
        
        // Calculate passenger satisfaction
        float passengerSatisfaction = passengerDemandMonthly > 0 
            ? (float) passengersDelivered / passengerDemandMonthly 
            : 0.0f;
        
        // Overall happiness = average of cargo and passenger satisfaction
        float monthSatisfaction = (cargoSatisfaction + passengerSatisfaction) / 2.0f;
        
        // Gradually adjust happiness toward satisfaction level (smoothing)
        cityHappiness = cityHappiness * 0.7f + monthSatisfaction * 0.3f;
        
        // Population grows if happy, shrinks if unhappy
        if (cityHappiness > 0.7f) {
            population += (int) (population * 0.01f); // 1% growth
        } else if (cityHappiness < 0.3f) {
            population -= (int) (population * 0.005f); // 0.5% decline
        }
        
        // Recalculate demands for next month based on new population
        passengerDemandMonthly = calculatePassengerDemand(population);
        cargoDemandsMonthly.put(CargoType.PAPER, population / 5);
        cargoDemandsMonthly.put(CargoType.STEEL, population / 8);
        
        // Reset delivery counters
        for (CargoType cargo : cargoDelivered.keySet()) {
            cargoDelivered.put(cargo, 0);
        }
        passengersDelivered = 0;
    }
    
    public List<Tile> getOccupiedTiles() {
        return new ArrayList<>(occupiedTiles);
    }
    
    public void addOccupiedTile(Tile tile) {
        if (!occupiedTiles.contains(tile)) {
            occupiedTiles.add(tile);
        }
    }
    
    public List<CargoType> getDemands() {
        return new ArrayList<>(demandedGoods);
    }
    
    public int getPassengerDemand() {
        return passengerDemandMonthly;
    }
    
    // ── Demand fluctuation tracking ──────────────────────────────────────────
    private float demandFluctuationTimer = 0f;
    private static final float DEMAND_UPDATE_INTERVAL = 5.0f; // Update every 5 game seconds
    
    /**
     * Simulates time passing and updates city demands gradually
     * Demands change slowly over time, not instantly
     */
    public void update(float deltaTime) {
        demandFluctuationTimer += deltaTime;
        
        if (demandFluctuationTimer >= DEMAND_UPDATE_INTERVAL) {
            demandFluctuationTimer = 0f;
            updateDemandFluctuations();
        }
    }
    
    /**
     * Gradually adjusts demands based on city happiness and random factors
     * Demands increase when satisfied, decrease when unsatisfied
     */
    private void updateDemandFluctuations() {
        // Passenger demand fluctuates based on happiness
        if (cityHappiness > 0.6f) {
            // Happy city = more travel demand (slow increase)
            passengerDemandMonthly += (int) (passengerDemandMonthly * 0.01f);
        } else if (cityHappiness < 0.4f) {
            // Unhappy city = less travel (slow decrease)
            passengerDemandMonthly -= (int) (passengerDemandMonthly * 0.01f);
        }
        
        // Ensure minimum demand
        passengerDemandMonthly = Math.max(10, passengerDemandMonthly);
        
        // Cargo demands fluctuate similarly
        for (CargoType cargo : demandedGoods) {
            int currentDemand = cargoDemandsMonthly.getOrDefault(cargo, 0);
            int change = 0;
            
            if (cityHappiness > 0.6f) {
                // Increase demand by 1-2%
                change = (int) (currentDemand * 0.015f);
            } else if (cityHappiness < 0.4f) {
                // Decrease demand by 1%
                change = -(int) (currentDemand * 0.01f);
            }
            
            // Add small random variation (-5% to +5%)
            double randomFactor = 0.95 + Math.random() * 0.1;
            change = (int) (change * randomFactor);
            
            int newDemand = Math.max(5, currentDemand + change);
            cargoDemandsMonthly.put(cargo, newDemand);
        }
    }
    
    @Override
    public String toString() {
        return String.format("City '%s' (Pop: %d, Happiness: %.0f%%, Cargo Demand: %d units, Passenger Demand: %d)", 
                           name, population, cityHappiness * 100, 
                           cargoDemandsMonthly.values().stream().mapToInt(Integer::intValue).sum(),
                           passengerDemandMonthly);
    }
}