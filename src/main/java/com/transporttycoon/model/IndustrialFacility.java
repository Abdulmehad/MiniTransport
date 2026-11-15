package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an industrial facility (mine, farm, factory) that produces and consumes goods
 * Industrial facilities occupy at least 2x2 tiles
 */
public class IndustrialFacility extends TileContent {
    public enum FacilityType {
        MINE("Mine", 'M'),
        FARM("Farm", 'F'),
        FACTORY("Factory", 'X'),
        GARAGE("Garage", 'G');
        
        private final String displayName;
        private final char symbol;
        
        FacilityType(String displayName, char symbol) {
            this.displayName = displayName;
            this.symbol = symbol;
        }
        
        public String getDisplayName() { return displayName; }
        public char getSymbol() { return symbol; }
    }
    
    private FacilityType facilityType;
    private CargoType production;
    private CargoType consumption;
    private List<Tile> occupiedTiles;
    private int productionRate;
    private int currentStock;
    private int maxCapacity;
    private int consumptionStock; // Stock of consumed materials (for factories)
    private int consumptionNeeded; // Amount needed to produce
    private float productionTimer; // Timer for production cycles
    private static final float PRODUCTION_INTERVAL = 2.0f; // Produce every 2 seconds
    
    public IndustrialFacility(String name, FacilityType facilityType) {
        super(name);
        this.facilityType = facilityType;
        initializeProductionAndConsumption(null);
        this.occupiedTiles = new ArrayList<>();
        this.productionRate = 10; // Units per time cycle
        this.currentStock = 0;
        this.maxCapacity = 100;
        this.consumptionStock = 0;
        this.consumptionNeeded = 5; // Units needed to produce
        this.productionTimer = 0f;
    }

    public IndustrialFacility(String name, FacilityType facilityType, CargoType factoryProduct) {
        super(name);
        this.facilityType = facilityType;
        initializeProductionAndConsumption(factoryProduct);
        this.occupiedTiles = new ArrayList<>();
        this.productionRate = 10; // Units per time cycle
        this.currentStock = 0;
        this.maxCapacity = 100;
        this.consumptionStock = 0;
        this.consumptionNeeded = 5; // Units needed to produce
        this.productionTimer = 0f;
    }

    public static IndustrialFacility createMine(String name) {
        return new IndustrialFacility(name, FacilityType.MINE);
    }

    public static IndustrialFacility createFarm(String name) {
        return new IndustrialFacility(name, FacilityType.FARM);
    }

    public static IndustrialFacility createPaperFactory(String name) {
        return new IndustrialFacility(name, FacilityType.FACTORY, CargoType.PAPER);
    }

    public static IndustrialFacility createSteelFactory(String name) {
        return new IndustrialFacility(name, FacilityType.FACTORY, CargoType.STEEL);
    }

    public static IndustrialFacility createGarage(String name) {
        return new IndustrialFacility(name, FacilityType.GARAGE);
    }

    private void initializeProductionAndConsumption(CargoType factoryProduct) {
        switch (facilityType) {
            case MINE:
                production = CargoType.IRON;
                consumption = null;
                break;
            case FARM:
                production = CargoType.WOOD;
                consumption = null;
                break;
            case FACTORY:
                if (factoryProduct == null) {
                    production = CargoType.PAPER;
                    consumption = CargoType.WOOD;
                    break;
                }
                if (factoryProduct == CargoType.PAPER) {
                    production = CargoType.PAPER;
                    consumption = CargoType.WOOD;
                } else if (factoryProduct == CargoType.STEEL) {
                    production = CargoType.STEEL;
                    consumption = CargoType.IRON;
                } else {
                    throw new IllegalArgumentException("Factory can only produce PAPER or STEEL");
                }
                break;
            case GARAGE:
                production = null;
                consumption = null;
                break;
            default:
                throw new IllegalStateException("Unsupported facility type: " + facilityType);
        }
    }
    
    @Override
    public int getWidth() {
        return 2; // Minimum 2x2 as per specification
    }
    
    @Override
    public int getHeight() {
        return 2; // Minimum 2x2 as per specification
    }
    
    @Override
    public char getDisplayChar() {
        return facilityType.getSymbol();
    }
    
    public FacilityType getFacilityType() {
        return facilityType;
    }
    
    public CargoType getProduction() {
        return production;
    }
    
    public CargoType getConsumption() {
        return consumption;
    }

    public CargoType getProduces() {
        return getProduction();
    }
    
    public CargoType getConsumes() {
        return getConsumption();
    }
    
    public List<Tile> getOccupiedTiles() {
        return new ArrayList<>(occupiedTiles);
    }
    
    public void addOccupiedTile(Tile tile) {
        if (!occupiedTiles.contains(tile)) {
            occupiedTiles.add(tile);
        }
    }
    
    public int getCurrentStock() {
        return currentStock;
    }
    
    public int getProductionRate() {
        return productionRate;
    }
    
    /**
     * Advances the production timer and triggers production once every
     * {@code PRODUCTION_INTERVAL} seconds of game-time.
     *
     * <p>Call this from the main game-update loop (passing the
     * already-speed-adjusted {@code deltaTime}), <em>not</em> from
     * StopManager – StopManager only needs to read the current stock.</p>
     *
     * @param deltaTime game-time seconds elapsed since the last frame
     */
    public void update(float deltaTime) {
        productionTimer += deltaTime;
        if (productionTimer >= PRODUCTION_INTERVAL) {
            productionTimer -= PRODUCTION_INTERVAL; // keep fractional remainder
            produce();
        }
    }

    /**
     * Attempts to produce goods if conditions are met.
     * Mines and farms produce freely; factories need consumed materials.
     *
     * <p>Prefer calling {@link #update(float)} from the game loop instead of
     * calling this directly, so that production is properly time-throttled.</p>
     */
    public void produce() {
        if (production == null) {
            return;
        }

        // Factories need raw materials to produce
        if (consumption != null) {
            // Check if we have enough raw materials
            if (consumptionStock < consumptionNeeded) {
                // Not enough materials to produce
                return;
            }
            // Consume materials
            consumptionStock -= consumptionNeeded;
        }
        
        // Produce goods if there's capacity
        if (currentStock + productionRate <= maxCapacity) {
            currentStock += productionRate;
        }
    }
    
    /**
     * Delivers raw materials to this facility (for factories that consume goods)
     * @param type The cargo type to deliver
     * @param amount Amount to deliver
     * @return Amount actually accepted
     */
    public int deliverRawMaterial(CargoType type, int amount) {
        if (consumption != type) {
            return 0; // This facility doesn't consume this type
        }
        
        int accepted = Math.min(amount, maxCapacity - consumptionStock);
        consumptionStock += accepted;
        return accepted;
    }
    
    /**
     * Gets the current stock of consumed materials (for factories)
     */
    public int getConsumptionStock() {
        return consumptionStock;
    }
    
    /**
     * Gets how much raw material is needed for production
     */
    public int getConsumptionNeeded() {
        return consumptionNeeded;
    }
    
    /**
     * Attempts to consume the specified cargo type and amount
     * Used when delivering raw materials to factories
     */
    public boolean deliverCargo(CargoType type, int amount) {
        if (consumption == type) {
            return deliverRawMaterial(type, amount) > 0;
        }
        return false;
    }
    
    /**
     * Loads cargo onto a vehicle if available
     */
    public int loadCargo(int requestedAmount) {
        int availableAmount = Math.min(currentStock, requestedAmount);
        currentStock -= availableAmount;
        return availableAmount;
    }
    
    @Override
    public String toString() {
        return String.format("%s '%s' (Stock: %d/%d, Produces: %s)", 
                           facilityType.getDisplayName(), name, currentStock, maxCapacity,
                           production.getDisplayName());
    }
}