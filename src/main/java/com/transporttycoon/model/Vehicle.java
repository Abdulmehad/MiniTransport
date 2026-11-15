package com.transporttycoon.model;

import com.transporttycoon.GameConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a road vehicle in the game.
 * Vehicles follow routes, transport cargo, and generate income.
 * Handles movement between stops along pre-computed paths, cargo loading/unloading,
 * and progress tracking along a circular route.
 */
public class Vehicle {
    private static int nextId = 1;

    // ── Identity ──────────────────────────────────────────────────────────────
    private final int id;
    private final VehicleType type;
    private final int purchaseYear;
    private IndustrialFacility homeGarage;
    private int homeGarageRoadX = -1;
    private int homeGarageRoadY = -1;

    // ── Route & navigation ────────────────────────────────────────────────────
    private Route assignedRoute;
    private Stop currentStop;          // stop the vehicle is currently at (null while moving)
    private Stop targetStop;           // next stop the vehicle is heading to
    private List<int[]> currentPath;   // tile-coordinate path [{x,y},…] computed by Pathfinder
    private int pathIndex;             // index into currentPath (which tile we are driving toward)

    // ── Traffic / tile occupancy ──────────────────────────────────────────────
    /** Direction this vehicle is currently travelling in (for collision detection). */
    private Tile.TrafficDirection travelDirection = Tile.TrafficDirection.POSITIVE;
    /** Tile that this vehicle currently occupies (for release on advance). */
    private Tile occupiedTile = null;
    /** True when this vehicle is blocked by another vehicle and waiting. */
    private boolean waiting = false;

    // ── State flags ───────────────────────────────────────────────────────────
    private boolean isMoving;
    private boolean atStop;            // true while performing load/unload at a stop
    private VehicleState state;        // explicit state for UI / controller queries

    // ── Cargo ─────────────────────────────────────────────────────────────────
    private final Map<CargoType, Integer> cargoLoaded;
    private int currentCapacity;

    // ── Pixel / sub-tile position ─────────────────────────────────────────────
    private float x;                   // current world-x (tile units, fractional)
    private float y;                   // current world-y (tile units, fractional)
    private float movementProgress;    // 0.0 → 1.0 progress between two adjacent path tiles

    // ── Continuous movement: visual interpolation ─────────────────────────────
    /** Logical position at the start of the current tile-to-tile step (for smooth rendering). */
    private float visualX;
    private float visualY;
    /** Direction vector for the current movement step (for rotation rendering). */
    private float dirDx;
    private float dirDy;

    // ── Economy ───────────────────────────────────────────────────────────────
    private double totalIncome;
    private double totalMaintenancePaid;
    private int ticksSinceGarageMaintenance;
    private float maintenanceElapsedSeconds;
    private float operationalAgeSeconds;
    private boolean maintenanceRequired;
    private boolean underGarageMaintenance;
    private int garageMaintenanceTicksRemaining;
    private Route routeBeforeMaintenance;

    // ─────────────────────────────────────────────────────────────────────────
    public Vehicle(VehicleType type, int purchaseYear) {
        this.id = nextId++;
        this.type = type;
        this.purchaseYear = purchaseYear;
        this.cargoLoaded = new HashMap<>();
        this.currentCapacity = 0;
        this.isMoving = false;
        this.atStop = false;
        this.state = VehicleState.IDLE;
        this.movementProgress = 0.0f;
        this.pathIndex = 0;
        this.visualX = 0f;
        this.visualY = 0f;
        this.dirDx = 0f;
        this.dirDy = 0f;
        this.totalIncome = 0;
        this.totalMaintenancePaid = 0;
        this.ticksSinceGarageMaintenance = 0;
        this.maintenanceElapsedSeconds = 0f;
        this.operationalAgeSeconds = 0f;
        this.maintenanceRequired = false;
        this.underGarageMaintenance = false;
        this.garageMaintenanceTicksRemaining = 0;
        this.routeBeforeMaintenance = null;

        for (CargoType ct : CargoType.values()) {
            cargoLoaded.put(ct, 0);
        }
    }

    // ── Route assignment ──────────────────────────────────────────────────────

    /**
     * Assigns a route to this vehicle and positions it at the first stop.
     */
    public void setAssignedRoute(Route route) {
        this.assignedRoute = route;
        this.currentPath = null;
        this.pathIndex = 0;
        if (route != null && !route.getStops().isEmpty()) {
            this.currentStop = route.getStop(0);
            this.targetStop = null;
            this.x = currentStop.getX();
            this.y = currentStop.getY();
            this.isMoving = false;
            this.atStop = true;
            this.state = VehicleState.AT_STOP;
        } else {
            this.state = VehicleState.IDLE;
        }
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    public void setCurrentPath(List<int[]> path) {
        this.currentPath = path;
        this.pathIndex = 0;
        this.movementProgress = 0.0f;
        this.waiting = false;
        // Determine travel direction from the path vector (first → last tile)
        if (path != null && path.size() >= 2) {
            int[] first = path.get(0);
            int[] last  = path.get(path.size() - 1);
            // POSITIVE = net movement in +x or +y direction
            travelDirection = (last[0] + last[1]) >= (first[0] + first[1])
                    ? Tile.TrafficDirection.POSITIVE
                    : Tile.TrafficDirection.NEGATIVE;

            // Seed visual position at the first tile so rendering starts smoothly
            visualX = first[0];
            visualY = first[1];
            int[] second = path.get(1);
            dirDx = second[0] - first[0];
            dirDy = second[1] - first[1];
        }
    }

    /**
     * Core update called every game tick.
     * <ul>
     *   <li>While at a stop: performs load/unload then departs.</li>
     *   <li>While moving: advances along the tile path at vehicle speed,
     *       honouring the one-vehicle-per-direction-per-tile rule.</li>
     * </ul>
     *
     * @param deltaTime seconds elapsed this tick (already scaled by time-speed multiplier)
     * @param map       game map – used for tile occupancy checks
     * @return income earned this tick (cargo delivered)
     */
    public double update(float deltaTime, GameMap map, TrafficLightManager trafficLightManager) {
        if (underGarageMaintenance) {
            return 0;
        }

        if (assignedRoute == null || assignedRoute.getStopCount() < 2) {
            return 0;
        }

        double incomeThisTick = 0;

        if (atStop && currentStop != null) {
            // ── At a stop: unload everything, then load what this stop has ──
            incomeThisTick += performUnload();
            performLoad();
            atStop = false;
            state = VehicleState.MOVING;

            // Choose next stop and request a path via the route's circular list
            targetStop = assignedRoute.getNextStop(currentStop);
            isMoving = true;
            waiting = false;
            System.out.printf("[Vehicle %d] Departing from '%s' -> targeting '%s'%n",
                    id, currentStop.getName(), targetStop.getName());
            // Path will be assigned by VehicleManager after this call

        } else if (isMoving && currentPath != null && !currentPath.isEmpty()) {
            // ── Moving: advance along path with collision detection ──
            float speedTilesPerSec = type.getSpeed() / 60.0f;
            float distanceThisTick = speedTilesPerSec * deltaTime;

            movementProgress += distanceThisTick;

            while (movementProgress >= 1.0f && pathIndex < currentPath.size() - 1) {
                int nextIndex = pathIndex + 1;
                int[] nextTileCoord = currentPath.get(nextIndex);
                Tile nextTile = map.getTile(nextTileCoord[0], nextTileCoord[1]);

                // ── Traffic light check: must stop at red light ──────────
                TrafficLight trafficLight = trafficLightManager.getTrafficLightAt(nextTileCoord[0], nextTileCoord[1]);
                if (trafficLight != null) {
                    TrafficLight.Direction approachDir = getApproachDirection(currentPath.get(pathIndex), nextTileCoord);
                    if (trafficLight.shouldStop(approachDir)) {
                        // Red light – stop and wait
                        waiting = true;
                        movementProgress = 1.0f; // sit at the boundary
                        break;
                    }
                }

                // ── Collision check: can we enter the next tile? ──────────
                if (nextTile != null && !nextTile.enter(this, travelDirection)) {
                    // Lane occupied – stop and wait
                    waiting = true;
                    movementProgress = 1.0f; // sit at the boundary
                    break;
                }

                // Release previous tile occupancy
                if (occupiedTile != null) {
                    occupiedTile.leave(this, travelDirection);
                }
                occupiedTile = nextTile;
                waiting = false;

                movementProgress -= 1.0f;
                pathIndex = nextIndex;
            }

            int[] currentTileCoord = currentPath.get(pathIndex);
            float tx = currentTileCoord[0];
            float ty = currentTileCoord[1];

            if (pathIndex < currentPath.size() - 1 && !waiting) {
                int[] nextTile = currentPath.get(pathIndex + 1);
                x = tx + (nextTile[0] - tx) * movementProgress;
                y = ty + (nextTile[1] - ty) * movementProgress;
                // Keep visual anchor and direction in sync with logical step
                visualX = tx;
                visualY = ty;
                dirDx = nextTile[0] - tx;
                dirDy = nextTile[1] - ty;
            } else if (!waiting) {
                // Reached end of path
                x = tx;
                y = ty;
                movementProgress = 0.0f;
                // Release tile occupancy on arrival
                if (occupiedTile != null) {
                    occupiedTile.leave(this, travelDirection);
                    occupiedTile = null;
                }
                arriveAtStop(targetStop);
            } else {
                // Waiting: stay at current position
                x = tx;
                y = ty;
            }
        } else if (isMoving) {
            abortMovementNoPath();
        }

        totalIncome += incomeThisTick;
        return incomeThisTick;
    }
    
    /**
     * Determines the approach direction based on current and next tile coordinates
     */
    private TrafficLight.Direction getApproachDirection(int[] currentTile, int[] nextTile) {
        int dx = nextTile[0] - currentTile[0];
        int dy = nextTile[1] - currentTile[1];
        
        if (dx > 0) return TrafficLight.Direction.EAST;
        if (dx < 0) return TrafficLight.Direction.WEST;
        if (dy > 0) return TrafficLight.Direction.NORTH;
        if (dy < 0) return TrafficLight.Direction.SOUTH;
        
        return TrafficLight.Direction.NORTH; // default
    }

    /** Returns true if this vehicle is currently waiting for a tile to clear. */
    public boolean isWaiting() { return waiting; }

    /** Returns the direction this vehicle is currently travelling. */
    public Tile.TrafficDirection getTravelDirection() { return travelDirection; }

    /**
     * Releases any tile currently held by this vehicle.
     * Called by VehicleManager when a route is cleared or game is stopped.
     */
    public void releaseTileOccupancy() {
        if (occupiedTile != null) {
            occupiedTile.leave(this, travelDirection);
            occupiedTile = null;
        }
    }

    /**
     * Stops movement when no valid road path exists to the target stop.
     */
    public void abortMovementNoPath() {
        isMoving = false;
        atStop = true;
        waiting = false;
        state = VehicleState.AT_STOP;
        currentPath = null;
        pathIndex = 0;
        targetStop = null;
        releaseTileOccupancy();
    }

    /**
     * Called when the vehicle physically reaches its target stop.
     */
    private void arriveAtStop(Stop stop) {
        currentStop = stop;
        targetStop = null;
        isMoving = false;
        atStop = true;
        waiting = false;
        state = VehicleState.AT_STOP;
        currentPath = null;
        pathIndex = 0;
        System.out.printf("[Vehicle %d %s] Arrived at '%s'%n",
                id, type.getDisplayName(), stop.getName());
    }

    // ── Cargo operations ──────────────────────────────────────────────────────

    /**
     * Unloads all cargo still on the vehicle onto the current stop.
     * <p>
     * Direct (bonus) deliveries to nearby cities/facilities are handled by
     * {@code VehicleManager.attemptDirectDelivery()} <em>before</em> this method
     * is called, so {@code cargoLoaded} may already be partially depleted.
     * Whatever remains is offloaded to the stop at the standard rate.
     *
     * @return standard income earned from offloading to the stop
     */
    private double performUnload() {
        double income = 0;

        for (CargoType ct : CargoType.values()) {
            int amount = cargoLoaded.getOrDefault(ct, 0);
            if (amount > 0) {
                unloadCargo(currentStop, ct, amount);
                int ratePerUnit = (ct == CargoType.PASSENGER)
                        ? GameConstants.PASSENGER_INCOME_PER_UNIT
                        : GameConstants.CARGO_INCOME_PER_UNIT;
                income += (double) amount * ratePerUnit;
                System.out.printf("[Vehicle %d] Unloaded %d x %s at '%s' (+$%d)%n",
                        id, amount, ct.getDisplayName(), currentStop.getName(),
                        amount * ratePerUnit);
            }
        }
        return income;
    }
    
    /**
     * Delivers cargo to a nearby city (called by VehicleManager)
     */
    public int deliverToCity(City city, CargoType cargoType, int amount) {
        if (cargoType == CargoType.PASSENGER) {
            int delivered = city.deliverPassengers(amount);
            if (delivered > 0) {
                cargoLoaded.put(cargoType, cargoLoaded.get(cargoType) - delivered);
                currentCapacity -= delivered;
            }
            return delivered;
        } else {
            int delivered = city.deliverCargo(cargoType, amount);
            if (delivered > 0) {
                cargoLoaded.put(cargoType, cargoLoaded.get(cargoType) - delivered);
                currentCapacity -= delivered;
            }
            return delivered;
        }
    }
    
    /**
     * Delivers cargo to a nearby facility (called by VehicleManager)
     */
    public int deliverToFacility(IndustrialFacility facility, CargoType cargoType, int amount) {
        if (facility.deliverCargo(cargoType, amount)) {
            int delivered = Math.min(amount, cargoLoaded.getOrDefault(cargoType, 0));
            cargoLoaded.put(cargoType, cargoLoaded.get(cargoType) - delivered);
            currentCapacity -= delivered;
            return delivered;
        }
        return 0;
    }

    /**
     * Loads as much compatible cargo as possible from the current stop.
     */
    private void performLoad() {
        for (CargoType ct : CargoType.values()) {
            if (!type.canCarry(ct)) continue;
            int waiting = currentStop.getCargoAmount(ct);
            if (waiting > 0) {
                int loaded = loadCargo(currentStop, ct, waiting);
                if (loaded > 0) {
                    System.out.printf("[Vehicle %d] Loaded %d x %s from '%s'%n",
                            id, loaded, ct.getDisplayName(), currentStop.getName());
                }
            }
        }
    }

    /**
     * Load cargo from a stop into this vehicle.
     *
     * @return units actually loaded
     */
    public int loadCargo(Stop stop, CargoType cargoType, int amount) {
        if (!type.canCarry(cargoType)) return 0;

        int availableCapacity = type.getCapacity() - currentCapacity;
        int toLoad = Math.min(availableCapacity, amount);
        toLoad = stop.removeCargo(cargoType, toLoad);

        cargoLoaded.put(cargoType, cargoLoaded.get(cargoType) + toLoad);
        currentCapacity += toLoad;
        return toLoad;
    }

    /**
     * Unload cargo at a stop.
     */
    public void unloadCargo(Stop stop, CargoType cargoType, int amount) {
        int loaded = cargoLoaded.getOrDefault(cargoType, 0);
        int toUnload = Math.min(loaded, amount);

        cargoLoaded.put(cargoType, loaded - toUnload);
        currentCapacity -= toUnload;
        stop.addCargo(cargoType, toUnload);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public VehicleType getType() { return type; }
    public int getPurchaseYear() { return purchaseYear; }
    public Route getAssignedRoute() { return assignedRoute; }
    public Stop getCurrentStop() { return currentStop; }
    public Stop getTargetStop() { return targetStop; }
    public List<int[]> getCurrentPath() { return currentPath; }

    public Stop getNextStop() {
        if (assignedRoute != null && currentStop != null) {
            return assignedRoute.getNextStop(currentStop);
        }
        return null;
    }

    public boolean isMoving() { return isMoving; }
    public boolean isAtStop() { return atStop; }
    public VehicleState getState() { return state; }

    public float getX() { return x; }
    public float getY() { return y; }

    /** Visual anchor tile X (start of current tile-to-tile step). Used for smooth rendering. */
    public float getVisualX() { return visualX; }
    /** Visual anchor tile Y (start of current tile-to-tile step). Used for smooth rendering. */
    public float getVisualY() { return visualY; }
    /** Direction X component of current movement step (-1, 0, or 1). */
    public float getDirDx() { return dirDx; }
    /** Direction Y component of current movement step (-1, 0, or 1). */
    public float getDirDy() { return dirDy; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.visualX = x;
        this.visualY = y;
    }

    public float getMovementProgress() { return movementProgress; }

    public void setMovementProgress(float progress) {
        this.movementProgress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    public int getPathIndex() { return pathIndex; }

    // ── Persistence direct-setters (used by PersistenceManager on load) ───────

    /** Sets the current stop without triggering route/state logic. */
    public void setCurrentStopDirect(Stop stop) {
        this.currentStop = stop;
    }

    /** Sets the target stop without triggering pathfinding. */
    public void setTargetStopDirect(Stop stop) {
        this.targetStop = stop;
        if (stop != null) {
            this.isMoving = true;
            this.atStop   = false;
        }
    }

    /** Sets the vehicle state directly (used on load). */
    public void setStateDirect(VehicleState state) {
        this.state = state;
        if (state == VehicleState.MOVING) {
            this.isMoving = true;
            this.atStop   = false;
        } else if (state == VehicleState.AT_STOP) {
            this.isMoving = false;
            this.atStop   = true;
        } else {
            this.isMoving = false;
            this.atStop   = false;
        }
    }

    public int getCargoLoaded(CargoType type) {
        return cargoLoaded.getOrDefault(type, 0);
    }

    public int getCurrentCapacity() { return currentCapacity; }
    public int getRemainingCapacity() { return type.getCapacity() - currentCapacity; }
    public int getMaintenanceCost() { return type.getMaintenanceCost(); }
    public double getTotalIncome() { return totalIncome; }
    public double getTotalMaintenancePaid() { return totalMaintenancePaid; }

    public int getVehicleAge(int currentYear) {
        return Math.max(0, currentYear - purchaseYear);
    }

    public float getEffectiveVehicleAgeYears(int currentYear) {
        float calendarYears = getVehicleAge(currentYear);
        float operationalYears = operationalAgeSeconds / GameConstants.VEHICLE_OPERATIONAL_SECONDS_PER_AGE_YEAR;
        return calendarYears + operationalYears;
    }

    public void addOperationalAgeTime(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        operationalAgeSeconds += deltaSeconds;
    }

    public float getOperationalAgeDays() {
        return operationalAgeSeconds / 60f;
    }

    public void assignHomeGarage(IndustrialFacility garage, int roadX, int roadY) {
        this.homeGarage = garage;
        this.homeGarageRoadX = roadX;
        this.homeGarageRoadY = roadY;
    }

    public IndustrialFacility getHomeGarage() {
        return homeGarage;
    }

    public boolean hasHomeGarage() {
        return homeGarage != null && homeGarageRoadX >= 0 && homeGarageRoadY >= 0;
    }

    public int getTicksSinceGarageMaintenance() {
        return ticksSinceGarageMaintenance;
    }

    public float getMaintenanceElapsedSeconds() {
        return maintenanceElapsedSeconds;
    }

    public void incrementMaintenanceTicks() {
        ticksSinceGarageMaintenance++;
    }

    public void addMaintenanceElapsedSeconds(float deltaSeconds) {
        if (deltaSeconds <= 0f) return;
        maintenanceElapsedSeconds += deltaSeconds;
    }

    public void resetMaintenanceTicks() {
        ticksSinceGarageMaintenance = 0;
        maintenanceElapsedSeconds = 0f;
    }

    public boolean isMaintenanceRequired() {
        return maintenanceRequired;
    }

    public void setMaintenanceRequired(boolean required) {
        this.maintenanceRequired = required;
    }

    public boolean isUnderGarageMaintenance() {
        return underGarageMaintenance;
    }

    public void beginGarageMaintenance(int durationTicks) {
        // If maintenance starts mid-move, release any claimed lane tile to avoid ghost occupancy.
        releaseTileOccupancy();
        routeBeforeMaintenance = assignedRoute;
        setAssignedRoute(null);
        setPosition(homeGarageRoadX, homeGarageRoadY);
        underGarageMaintenance = true;
        garageMaintenanceTicksRemaining = Math.max(1, durationTicks);
        state = VehicleState.IDLE;
    }

    public boolean tickGarageMaintenance() {
        if (!underGarageMaintenance) return false;
        garageMaintenanceTicksRemaining--;
        return garageMaintenanceTicksRemaining <= 0;
    }

    public void finishGarageMaintenance() {
        underGarageMaintenance = false;
        maintenanceRequired = false;
        resetMaintenanceTicks();
        Route previous = routeBeforeMaintenance;
        routeBeforeMaintenance = null;
        if (previous != null) {
            setAssignedRoute(previous);
        }
    }

    public void recordMaintenancePaid(double amount) {
        this.totalMaintenancePaid += amount;
    }

    @Override
    public String toString() {
        return String.format("Vehicle#%d [%s] route='%s' cargo=%d/%d moving=%b",
                id, type.getDisplayName(),
                assignedRoute != null ? assignedRoute.getName() : "none",
                currentCapacity, type.getCapacity(), isMoving);
    }
    
    /** Convenience alias used by developer-branch Game code. */
    public String getName() {
        return "Vehicle-" + id;
    }

    /** Alias for getAssignedRoute() */
    public Route getRoute() {
        return assignedRoute;
    }

    /** Alias for setAssignedRoute(Route) */
    public void setRoute(Route route) {
        setAssignedRoute(route);
    }

    public boolean hasRoute() {
        return assignedRoute != null;
    }
}