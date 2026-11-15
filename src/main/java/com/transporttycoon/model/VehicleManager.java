package com.transporttycoon.model;

import com.transporttycoon.GameConstants;
import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * VehicleManager – the central hub for all vehicle-related operations.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Track every {@link Vehicle} that has been purchased.</li>
 *   <li>Update each vehicle every game tick (movement, cargo, income).</li>
 *   <li>Handle vehicle purchases (deduct cost, validate funds).</li>
 *   <li>Assign / remove routes to / from vehicles.</li>
 *   <li>Compute and cache tile-paths via {@link Pathfinder} whenever a
 *       vehicle departs from a stop.</li>
 *   <li>Deduct per-tick maintenance costs and credit income to the
 *       {@link Player}.</li>
 * </ul>
 */
public class VehicleManager {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final GameMap map;
    private final Player player;
    private final Pathfinder pathfinder;
    private com.transporttycoon.view.MapView mapView; // For delivery notifications
    private TrafficLightManager trafficLightManager; // For traffic light checking

    // ── Vehicle registry ──────────────────────────────────────────────────────
    private final List<Vehicle> vehicles = new ArrayList<>();

    // ── Tick counter for maintenance billing ─────────────────────────────────
    private int tickCount = 0;    
    // ── Path logging throttle ────────────────────────────────────────────
    private int pathLogThrottle = 0;
    private static final int PATH_LOG_FREQUENCY = 60; // Log every 60 ticks (~1 second at 60 FPS)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new VehicleManager.
     *
     * @param map    the game map (used for pathfinding)
     * @param player the active player (funds are debited / credited here)
     */
    public VehicleManager(GameMap map, Player player) {
        if (map == null)    throw new IllegalArgumentException("map must not be null");
        if (player == null) throw new IllegalArgumentException("player must not be null");
        this.map = map;
        this.player = player;
        this.pathfinder = new Pathfinder(map);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /**
     * Attempts to purchase a new vehicle of the given type.
     *
     * <p>Deducts the purchase cost from the player's capital.  The vehicle is
     * added to the fleet and returned so the caller can immediately assign a
     * route.</p>
     *
     * @param type        the type of vehicle to buy
     * @param currentYear the in-game year, used to track vehicle age
     * @return the newly purchased {@link Vehicle}, or {@code null} if the
     *         player cannot afford it
     */
    public Vehicle purchaseVehicle(VehicleType type, int currentYear) {
        int cost = type.getPurchaseCost();
        if (player.getCapital() < cost) {
            System.out.printf("[VehicleManager] Cannot afford %s ($%d needed, $%.0f available)%n",
                    type.getDisplayName(), cost, player.getCapital());
            return null;
        }

        player.deductFunds(cost);
        Vehicle vehicle = new Vehicle(type, currentYear);
        vehicles.add(vehicle);

        System.out.printf("[VehicleManager] Purchased %s #%d for $%d  (balance: $%.0f)%n",
                type.getDisplayName(), vehicle.getId(), cost, player.getCapital());
        return vehicle;
    }

    public Vehicle purchaseVehicleAtGarage(VehicleType type, int currentYear,
                                           IndustrialFacility garage, int garageRoadX, int garageRoadY) {
        Vehicle vehicle = purchaseVehicle(type, currentYear);
        if (vehicle == null) return null;

        vehicle.assignHomeGarage(garage, garageRoadX, garageRoadY);
        vehicle.setPosition(garageRoadX, garageRoadY);
        return vehicle;
    }

    /**
     * Creates a vehicle without deducting cost — used by PersistenceManager
     * when restoring a saved game (cost was already paid in the original session).
     */
    public Vehicle purchaseVehicleFree(VehicleType type, int purchaseYear) {
        Vehicle vehicle = new Vehicle(type, purchaseYear);
        vehicles.add(vehicle);
        return vehicle;
    }

    // ── Route management ──────────────────────────────────────────────────────

    /**
     * Assigns a route to a vehicle and registers the vehicle on that route.
     * If the vehicle already had a different route it is removed from that one.
     *
     * @param vehicle the vehicle to update
     * @param route   the route to assign (may be {@code null} to clear)
     */
    public void assignRoute(Vehicle vehicle, Route route) {
        if (vehicle == null) throw new IllegalArgumentException("vehicle must not be null");

        Route old = vehicle.getAssignedRoute();
        if (old != null && old != route) {
            old.unassignVehicle(vehicle);
        }

        // Release any tile this vehicle holds before changing route
        vehicle.releaseTileOccupancy();

        vehicle.setAssignedRoute(route);

        if (route != null) {
            route.assignVehicle(vehicle);

            // Pre-compute path to the first target stop so the vehicle can
            // start moving on the very first tick.
            refreshPath(vehicle);

            System.out.printf("[VehicleManager] Vehicle #%d assigned to route '%s'%n",
                    vehicle.getId(), route.getName());
        } else {
            System.out.printf("[VehicleManager] Vehicle #%d route cleared%n", vehicle.getId());
        }
    }

    /**
     * Removes a vehicle from its current route (if any).
     *
     * @param vehicle the vehicle to unassign
     */
    public void unassignRoute(Vehicle vehicle) {
        assignRoute(vehicle, null);
    }
    
    /**
     * Sets the traffic light manager for vehicle traffic light checking
     */
    public void setTrafficLightManager(TrafficLightManager trafficLightManager) {
        this.trafficLightManager = trafficLightManager;
    }

    // ── Tick update ───────────────────────────────────────────────────────────

    /**
     * Updates every vehicle in the fleet for one game tick.
     *
     * <ul>
     *   <li>Calls {@link Vehicle#update} which handles movement and
     *       load/unload.  Income returned is credited to the player.</li>
     *   <li>After the update, if a vehicle just departed from a stop (it
     *       transitioned to moving but has no path yet), the pathfinder
     *       computes the route to the next stop.</li>
     *   <li>Every tick, maintenance costs are deducted.</li>
     *   <li>Attempts direct delivery to nearby cities/facilities for bonus income.</li>
     * </ul>
     *
     * @param deltaTime seconds elapsed this tick (already speed-scaled)
     */
    public void update(float deltaTime) {
        update(deltaTime, 0);
    }

    public void update(float deltaTime, int currentYear) {
        tickCount++;

        for (Vehicle vehicle : vehicles) {
            handleGarageMaintenance(vehicle, currentYear, deltaTime);

            if (vehicle.isUnderGarageMaintenance()) {
                if (vehicle.tickGarageMaintenance()) {
                    vehicle.finishGarageMaintenance();
                    if (vehicle.getAssignedRoute() != null) {
                        refreshPath(vehicle);
                    }
                    System.out.printf("[VehicleManager] Vehicle #%d finished garage maintenance%n", vehicle.getId());
                }
                continue;
            }

            if (vehicle.isMaintenanceRequired()) {
                double maintenanceServiceCost = calculateGarageServiceCost(vehicle, currentYear);
                player.recordTransaction(
                    String.format("Garage maintenance: Vehicle #%d", vehicle.getId()),
                    maintenanceServiceCost,
                    Player.TransactionType.EXPENSE
                );
                vehicle.beginGarageMaintenance(GameConstants.VEHICLE_MAINTENANCE_DURATION_TICKS);
                System.out.printf("[VehicleManager] Vehicle #%d sent to garage for maintenance%n", vehicle.getId());
                continue;
            }

            if (vehicle.getAssignedRoute() == null) continue;

            // Remember pre-update state so we can act on it before vehicle.update() runs.
            boolean wasAtStop = vehicle.isAtStop();
            boolean hadPath = vehicle.getCurrentPath() != null && !vehicle.getCurrentPath().isEmpty();

            // Attempt direct delivery BEFORE vehicle.update() so the vehicle still
            // holds its cargo when we search for nearby destinations.  vehicle.update()
            // will then call performUnload() only for the cargo that was not directly
            // delivered, earning standard (non-bonus) income on the remainder.
            double bonusIncome = 0;
            if (wasAtStop && vehicle.getCurrentStop() != null) {
                bonusIncome = attemptDirectDelivery(vehicle);
            }

            // Let the vehicle do its work; collect standard unload income
            double standardIncome = vehicle.update(deltaTime, map, trafficLightManager != null ? trafficLightManager : new TrafficLightManager(map));
            
            // Record income with proper descriptions
            if (standardIncome > 0) {
                player.recordTransaction(
                    String.format("Delivery by Vehicle #%d", vehicle.getId()),
                    standardIncome,
                    Player.TransactionType.INCOME
                );
            }
            
            if (bonusIncome > 0) {
                player.recordTransaction(
                    String.format("BONUS Delivery by Vehicle #%d", vehicle.getId()),
                    bonusIncome,
                    Player.TransactionType.INCOME
                );
            }

            // Only refresh path if vehicle just transitioned to moving AND doesn't have a valid path
            boolean nowMoving = vehicle.isMoving();
            if (nowMoving && vehicle.getTargetStop() != null && !hadPath) {
                if (vehicle.getCurrentPath() == null || vehicle.getCurrentPath().isEmpty()) {
                    refreshPath(vehicle);
                }
            }

            // Deduct maintenance every tick (accumulate silently to avoid ledger spam;
            // the monthly report totals expenses via player.getMonthlyExpenses())
            double maintenance = vehicle.getMaintenanceCost();
            player.deductFundsQuiet(maintenance);
            vehicle.recordMaintenancePaid(maintenance);
        }
    }

    private void handleGarageMaintenance(Vehicle vehicle, int currentYear, float deltaTime) {
        if (!vehicle.hasHomeGarage()) {
            return;
        }

        vehicle.addOperationalAgeTime(deltaTime);

        if (vehicle.isUnderGarageMaintenance() || vehicle.isMaintenanceRequired()) {
            return;
        }

        int maintenanceProgressTicks = Math.max(1, Math.round(deltaTime * 60f));
        for (int i = 0; i < maintenanceProgressTicks; i++) {
            vehicle.incrementMaintenanceTicks();
        }
        vehicle.addMaintenanceElapsedSeconds(deltaTime);

        int intervalTicks = calculateMaintenanceIntervalTicks(vehicle, currentYear);
        float intervalSeconds = intervalTicks / 60f;
        if (vehicle.getMaintenanceElapsedSeconds() >= intervalSeconds) {
            vehicle.setMaintenanceRequired(true);
        }
    }

    private int calculateMaintenanceIntervalTicks(Vehicle vehicle, int currentYear) {
        float effectiveAgeYears = vehicle.getEffectiveVehicleAgeYears(currentYear);
        int interval = (int) (GameConstants.VEHICLE_BASE_MAINTENANCE_INTERVAL_TICKS
            - (effectiveAgeYears * GameConstants.VEHICLE_MAINTENANCE_INTERVAL_AGE_PENALTY_TICKS));
        return Math.max(GameConstants.VEHICLE_MIN_MAINTENANCE_INTERVAL_TICKS, interval);
    }

    private double calculateGarageServiceCost(Vehicle vehicle, int currentYear) {
        int age = vehicle.getVehicleAge(currentYear);
        double ageMultiplier = 1.0 + (age * 0.08);
        return vehicle.getMaintenanceCost() * 25.0 * ageMultiplier;
    }

    public List<Vehicle> sellOverAgedVehicles(int currentYear, int overAgeDays) {
        List<Vehicle> soldVehicles = new ArrayList<>();
        List<Vehicle> snapshot = new ArrayList<>(vehicles);
        for (Vehicle vehicle : snapshot) {
            if (vehicle.getOperationalAgeDays() >= overAgeDays) {
                sellVehicle(vehicle, currentYear);
                soldVehicles.add(vehicle);
            }
        }
        return soldVehicles;
    }

    public double sellVehicle(Vehicle vehicle, int currentYear) {
        if (vehicle == null || !vehicles.contains(vehicle)) {
            return 0;
        }

        Route assignedRoute = vehicle.getAssignedRoute();
        if (assignedRoute != null) {
            assignedRoute.unassignVehicle(vehicle);
            vehicle.setAssignedRoute(null);
        }
        vehicle.releaseTileOccupancy();

        int age = vehicle.getVehicleAge(currentYear);
        double depreciationRatio = Math.max(
                GameConstants.VEHICLE_SELL_VALUE_MIN_RATIO,
                1.0 - (age * 0.05)
        );
        double resaleValue = vehicle.getType().getPurchaseCost() * depreciationRatio;

        vehicles.remove(vehicle);
        player.recordTransaction(
            String.format("Sold Vehicle #%d (%s)", vehicle.getId(), vehicle.getType().getDisplayName()),
            resaleValue,
            Player.TransactionType.INCOME
        );
        return resaleValue;
    }
    
    /**
     * Sets the MapView reference for delivery notifications
     */
    public void setMapView(com.transporttycoon.view.MapView mapView) {
        this.mapView = mapView;
    }
    
    /**
     * Attempts to deliver cargo from vehicle to nearby cities or facilities
     * @return bonus income earned from direct deliveries
     */
    private double attemptDirectDelivery(Vehicle vehicle) {
        Stop stop = vehicle.getCurrentStop();
        if (stop == null) return 0;
        
        final int SEARCH_RADIUS = 3;
        int stopX = stop.getX();
        int stopY = stop.getY();
        double bonusIncome = 0;
        
        // Search for nearby cities and facilities
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                Tile tile = map.getTile(stopX + dx, stopY + dy);
                if (tile == null || tile.isEmpty()) continue;
                
                // Try delivery to city
                if (tile.getContent() instanceof City) {
                    City city = (City) tile.getContent();
                    
                    // Try passenger delivery
                    int passengers = vehicle.getCargoLoaded(CargoType.PASSENGER);
                    if (passengers > 0) {
                        int delivered = vehicle.deliverToCity(city, CargoType.PASSENGER, passengers);
                        if (delivered > 0) {
                            bonusIncome += delivered * GameConstants.PASSENGER_INCOME_PER_UNIT * 2;
                            System.out.printf("[Vehicle %d] ✓ Delivered %d passengers to %s (+$%d BONUS)%n",
                                vehicle.getId(), delivered, city.getName(), delivered * GameConstants.PASSENGER_INCOME_PER_UNIT * 2);
                            
                            // Show notification
                            if (mapView != null) {
                                mapView.addDeliveryNotification(
                                    "+" + delivered + " " + CargoType.PASSENGER.getIcon(),
                                    tile.getX(), tile.getY(),
                                    new com.badlogic.gdx.graphics.Color(0.3f, 1f, 0.3f, 1f)
                                );
                            }
                        }
                    }
                    
                    // Try cargo delivery (paper, steel)
                    for (CargoType cargo : city.getDemands()) {
                        int amount = vehicle.getCargoLoaded(cargo);
                        if (amount > 0) {
                            int delivered = vehicle.deliverToCity(city, cargo, amount);
                            if (delivered > 0) {
                                bonusIncome += delivered * GameConstants.CARGO_INCOME_PER_UNIT * 2;
                                System.out.printf("[Vehicle %d] ✓ Delivered %d x %s to %s (+$%d BONUS)%n",
                                    vehicle.getId(), delivered, cargo.getDisplayName(), city.getName(),
                                    delivered * GameConstants.CARGO_INCOME_PER_UNIT * 2);
                                
                                // Show notification
                                if (mapView != null) {
                                    mapView.addDeliveryNotification(
                                        "+" + delivered + " " + cargo.getIcon(),
                                        tile.getX(), tile.getY(),
                                        new com.badlogic.gdx.graphics.Color(0.3f, 0.8f, 1f, 1f)
                                    );
                                }
                            }
                        }
                    }
                }
                
                // Try delivery to industrial facility (raw materials)
                if (tile.getContent() instanceof IndustrialFacility) {
                    IndustrialFacility facility = (IndustrialFacility) tile.getContent();
                    CargoType consumed = facility.getConsumes();
                    
                    if (consumed != null) {
                        int amount = vehicle.getCargoLoaded(consumed);
                        if (amount > 0) {
                            int delivered = vehicle.deliverToFacility(facility, consumed, amount);
                            if (delivered > 0) {
                                bonusIncome += delivered * GameConstants.CARGO_INCOME_PER_UNIT * 1.5;
                                System.out.printf("[Vehicle %d] ✓ Delivered %d x %s to %s (+$%d)%n",
                                    vehicle.getId(), delivered, consumed.getDisplayName(), facility.getName(),
                                    (int)(delivered * GameConstants.CARGO_INCOME_PER_UNIT * 1.5));
                                
                                // Show notification
                                if (mapView != null) {
                                    mapView.addDeliveryNotification(
                                        "+" + delivered + " " + consumed.getIcon(),
                                        tile.getX(), tile.getY(),
                                        new com.badlogic.gdx.graphics.Color(1f, 0.7f, 0.3f, 1f)
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return bonusIncome;
    }

    // ── Path management ───────────────────────────────────────────────────────

    /**
     * Asks the {@link Pathfinder} to compute (or refresh) the path from the
     * vehicle's current position to its target stop and hands it back to the
     * vehicle.
     *
     * <p>This is called automatically after stop-arrival and route assignment.</p>
     *
     * @param vehicle the vehicle needing a new path
     */
    public void refreshPath(Vehicle vehicle) {
        if (vehicle == null) return;

        Stop target = vehicle.getTargetStop();

        // If targetStop is null the vehicle hasn't departed yet; compute path
        // to the next stop in its route so it can start immediately.
        if (target == null) {
            target = vehicle.getNextStop();
            if (target == null) return;
        }

        Stop current = vehicle.getCurrentStop();
        if (current == null) return;

        List<int[]> path = pathfinder.findPath(current, target);
        if (path == null || path.isEmpty()) {
            System.out.printf("[VehicleManager] No road path for vehicle #%d: %s → %s%n",
                    vehicle.getId(), current.getName(), target.getName());
            vehicle.abortMovementNoPath();
            return;
        }

        vehicle.setCurrentPath(path);

        // Log path computation only every N ticks to reduce spam
        if (pathLogThrottle++ % PATH_LOG_FREQUENCY == 0) {
            System.out.printf("[VehicleManager] Path computed for vehicle #%d: %s → %s (%d tiles)%n",
                    vehicle.getId(), current.getName(), target.getName(), path.size());
        }
    }

    // ── Fleet queries ─────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable snapshot of the entire vehicle fleet.
     *
     * @return read-only list of all vehicles
     */
    public List<Vehicle> getAllVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    /**
     * Returns all vehicles currently assigned to a specific route.
     *
     * @param route the route to filter by
     * @return list of vehicles on that route
     */
    public List<Vehicle> getVehiclesOnRoute(Route route) {
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (route.equals(v.getAssignedRoute())) {
                result.add(v);
            }
        }
        return result;
    }

    /**
     * Finds a vehicle by its unique ID.
     *
     * @param id the vehicle ID
     * @return the vehicle, or {@code null} if not found
     */
    public Vehicle getVehicleById(int id) {
        for (Vehicle v : vehicles) {
            if (v.getId() == id) return v;
        }
        return null;
    }

    /**
     * Returns the total number of vehicles in the fleet.
     *
     * @return fleet size
     */
    public int getFleetSize() {
        return vehicles.size();
    }

    /**
     * Returns the total ongoing maintenance cost per tick for the whole fleet.
     *
     * @return summed maintenance cost
     */
    public int getTotalMaintenanceCostPerTick() {
        return vehicles.stream().mapToInt(Vehicle::getMaintenanceCost).sum();
    }

    /**
     * Prints a summary of every vehicle's current state to standard output.
     * Useful for debugging.
     */
    public void printFleetStatus() {
        System.out.println("=== Fleet Status (" + vehicles.size() + " vehicles) ===");
        for (Vehicle v : vehicles) {
            System.out.printf("  %s | income=$%.0f | maintenance=$%.0f%n",
                    v, v.getTotalIncome(), v.getTotalMaintenancePaid());
        }
        System.out.println("  Total maintenance/tick: $" + getTotalMaintenanceCostPerTick());
        System.out.println("  Player balance: " + player.getFormattedCapital());
        System.out.println("=========================================");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the {@link Pathfinder} instance used by this manager.
     *
     * @return the pathfinder
     */
    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    /**
     * Returns the total tick count since this manager was created.
     *
     * @return tick count
     */
    public int getTickCount() {
        return tickCount;
    }
}
