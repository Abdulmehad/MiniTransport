package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * StopManager – manages the creation and lifecycle of {@link Stop} objects.
 *
 * <p>Stops represent designated pick-up / drop-off points on the map.
 * This manager handles:</p>
 * <ul>
 *   <li>Placing new stops (with cost deduction from the {@link Player}).</li>
 *   <li>Deactivating stops when no longer needed.</li>
 *   <li>Periodically accumulating cargo at stops from nearby
 *       {@link IndustrialFacility} objects (called each game tick).</li>
 * </ul>
 */
public class StopManager {

    private static final int CARGO_ACCUMULATION_INTERVAL_TICKS = 10;

    private final GameMap map;
    private final Player player;
    private final List<Stop> stops = new ArrayList<>();
    private int ticksSinceLastAccumulation = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a new StopManager.
     *
     * @param map    the game map (used to check tile content near stops)
     * @param player the active player (stop purchase cost is deducted here)
     */
    public StopManager(GameMap map, Player player) {
        if (map == null)    throw new IllegalArgumentException("map must not be null");
        if (player == null) throw new IllegalArgumentException("player must not be null");
        this.map = map;
        this.player = player;
    }

    // ── Stop placement ────────────────────────────────────────────────────────

    /**
     * Places a new stop at tile ({@code x}, {@code y}) and deducts the stop
     * cost from the player's capital.
     *
     * @param name the stop name
     * @param x    tile x-coordinate
     * @param y    tile y-coordinate
     * @param cost cost in currency units (use {@link com.transporttycoon.GameConstants#STOP_COST})
     * @return the new stop, or {@code null} if the player cannot afford it or
     *         coordinates are out of bounds
     */
    public Stop placeStop(String name, int x, int y, int cost) {
        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) {
            System.out.printf("[StopManager] Cannot place stop '%s': coordinates (%d,%d) out of bounds%n",
                    name, x, y);
            return null;
        }
        if (player.getCapital() < cost) {
            System.out.printf("[StopManager] Cannot afford stop '%s' ($%d needed, $%.0f available)%n",
                    name, cost, player.getCapital());
            return null;
        }

        player.deductFunds(cost);
        Stop stop = new Stop(name, x, y);
        stops.add(stop);
        System.out.printf("[StopManager] Placed stop '%s' at (%d,%d)  cost=$%d  balance=$%.0f%n",
                name, x, y, cost, player.getCapital());
        return stop;
    }

    /**
     * Places a free stop (no cost) – useful for demo / test scenarios.
     *
     * @param name the stop name
     * @param x    tile x-coordinate
     * @param y    tile y-coordinate
     * @return the new stop, or {@code null} if coordinates are invalid
     */
    public Stop placeStopFree(String name, int x, int y) {
        return placeStop(name, x, y, 0);
    }

    /**
     * Deactivates a stop.  Deactivated stops are kept in the list but will
     * not accumulate cargo or accept vehicles.
     *
     * @param stop the stop to deactivate
     */
    public void deactivateStop(Stop stop) {
        if (stop != null) {
            stop.setActive(false);
            System.out.printf("[StopManager] Stop '%s' deactivated%n", stop.getName());
        }
    }

    // ── Tick update ───────────────────────────────────────────────────────────

    /**
     * Must be called every game tick.  Periodically accumulates cargo at each
     * active stop from the nearest producing facility within a 3-tile radius.
     */
    public void update() {
        ticksSinceLastAccumulation++;
        if (ticksSinceLastAccumulation >= CARGO_ACCUMULATION_INTERVAL_TICKS) {
            ticksSinceLastAccumulation = 0;
            accumulateCargo();
        }
    }

    /**
     * Scans each active stop and looks for an {@link IndustrialFacility}
     * within {@code SEARCH_RADIUS} tiles.  If one is found, it produces goods
     * which are then added to the stop's cargo waiting queue.
     * 
     * Also accumulates passengers from nearby cities.
     *
     * <p>Cargo is only transferred if the facility actually has stock after
     * producing, preventing infinite accumulation at a single stop.</p>
     */
    private void accumulateCargo() {
        final int SEARCH_RADIUS = 3;
        /** Maximum units allowed to wait at a single stop per cargo type. */
        final int MAX_STOP_CARGO = 50;

        for (Stop stop : stops) {
            if (!stop.isActive()) continue;

            int sx = stop.getX();
            int sy = stop.getY();

            // ── Pass 1: look for a producing IndustrialFacility ───────────────
            // (previously shared a single break with the City branch, so a stop
            //  near both a factory and a city would only ever accumulate one type)
            outerFacility:
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                    Tile tile = map.getTile(sx + dx, sy + dy);
                    if (tile == null || tile.isEmpty()) continue;

                    if (tile.getContent() instanceof IndustrialFacility) {
                        IndustrialFacility facility = (IndustrialFacility) tile.getContent();
                        if (facility.getFacilityType() == IndustrialFacility.FacilityType.GARAGE
                                || facility.getProduces() == null) {
                            continue;
                        }
                        // Stock is already updated by Game.updateProduction() via
                        // facility.update(deltaTime) — just read, no extra produce() here.
                        int stock = facility.getCurrentStock();
                        if (stock > 0) {
                            int waiting = stop.getCargoAmount(facility.getProduces());
                            int canAccept = Math.max(0, MAX_STOP_CARGO - waiting);
                            int toTransfer = Math.min(stock, canAccept);
                            if (toTransfer > 0) {
                                int transferred = facility.loadCargo(toTransfer);
                                stop.addCargo(facility.getProduces(), transferred);
                                System.out.printf("[StopManager] %d x %s → stop '%s' (waiting: %d/%d)%n",
                                        transferred, facility.getProduces().getDisplayName(),
                                        stop.getName(), waiting + transferred, MAX_STOP_CARGO);
                            }
                        }
                        break outerFacility; // one facility per stop per cycle
                    }
                }
            }

            // ── Pass 2: look for a nearby City to generate passengers ─────────
            // Runs independently of Pass 1 so a stop near both a factory and a
            // city correctly accumulates goods AND passengers each cycle.
            outerCity:
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                    Tile tile = map.getTile(sx + dx, sy + dy);
                    if (tile == null || tile.isEmpty()) continue;

                    if (tile.getContent() instanceof City) {
                        City city = (City) tile.getContent();
                        int waiting = stop.getCargoAmount(CargoType.PASSENGER);
                        int canAccept = Math.max(0, MAX_STOP_CARGO - waiting);

                        if (canAccept > 0) {
                            int passengersGenerated = Math.min(5, city.getPopulation() / 10000);
                            int toTransfer = Math.min(passengersGenerated, canAccept);

                            if (toTransfer > 0) {
                                stop.addCargo(CargoType.PASSENGER, toTransfer);
                                System.out.printf("[StopManager] %d passengers → stop '%s' from %s (waiting: %d/%d)%n",
                                        toTransfer, stop.getName(), city.getName(),
                                        waiting + toTransfer, MAX_STOP_CARGO);
                            }
                        }
                        break outerCity; // one city per stop per cycle
                    }
                }
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable view of all stops (active and inactive).
     *
     * @return all stops
     */
    public List<Stop> getAllStops() {
        return Collections.unmodifiableList(stops);
    }

    /**
     * Returns only the active stops.
     *
     * @return list of active stops
     */
    public List<Stop> getActiveStops() {
        List<Stop> active = new ArrayList<>();
        for (Stop s : stops) {
            if (s.isActive()) active.add(s);
        }
        return active;
    }

    /**
     * Finds a stop by name (case-insensitive).
     *
     * @param name the stop name
     * @return the stop, or {@code null} if not found
     */
    public Stop getStopByName(String name) {
        for (Stop s : stops) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    /**
     * Returns the total number of stops (active and inactive).
     *
     * @return stop count
     */
    public int getStopCount() {
        return stops.size();
    }

    /**
     * Prints a summary of every stop and its cargo queues.
     */
    public void printStopStatus() {
        System.out.println("=== Stops (" + stops.size() + ") ===");
        for (Stop s : stops) {
            System.out.printf("  [%s] '%s' (%d,%d) – waiting: %d units%n",
                    s.isActive() ? "ACTIVE" : "INACTIVE",
                    s.getName(), s.getX(), s.getY(), s.getTotalCargoUnits());
        }
        System.out.println("==================");
    }
}
