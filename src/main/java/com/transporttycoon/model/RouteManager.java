package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RouteManager – maintains the collection of all {@link Route} objects in the game.
 *
 * <p>Provides helpers to create named routes, add/remove stops, and look up
 * routes by name or by stop membership.  Routes are circular by design;
 * vehicles will cycle through all stops indefinitely.</p>
 */
public class RouteManager {

    private final List<Route> routes = new ArrayList<>();

    // ── Route lifecycle ───────────────────────────────────────────────────────

    /**
     * Creates a new, empty route with the given name and registers it.
     *
     * @param name the human-readable route name (e.g. "Mine → Steel Mill")
     * @return the new route
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Route createRoute(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Route name must not be blank");
        }
        Route route = new Route(name);
        routes.add(route);
        System.out.printf("[RouteManager] Created route '%s'%n", name);
        return route;
    }

    /**
     * Creates a route and immediately populates it with the provided stops.
     *
     * @param name  route name
     * @param stops ordered list of stops (minimum 2)
     * @return the new route
     * @throws IllegalArgumentException if fewer than 2 stops are supplied
     */
    public Route createRoute(String name, List<Stop> stops) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("A route must have at least 2 stops");
        }
        Route route = createRoute(name);
        for (Stop stop : stops) {
            route.addStop(stop);
        }
        return route;
    }

    /**
     * Removes a route from the registry.
     *
     * <p>Note: vehicles that were assigned this route will keep a reference to
     * it.  Call {@link VehicleManager#unassignRoute(Vehicle)} before removing
     * a route to avoid orphaned vehicles.</p>
     *
     * @param route the route to remove
     * @return {@code true} if the route was found and removed
     */
    public boolean removeRoute(Route route) {
        boolean removed = routes.remove(route);
        if (removed) {
            System.out.printf("[RouteManager] Removed route '%s'%n", route.getName());
        }
        return removed;
    }

    // ── Stop management ───────────────────────────────────────────────────────

    /**
     * Appends a stop to an existing route.
     *
     * @param route the route to modify
     * @param stop  the stop to add
     */
    public void addStopToRoute(Route route, Stop stop) {
        if (route == null || stop == null) return;
        route.addStop(stop);
        System.out.printf("[RouteManager] Stop '%s' added to route '%s' (total stops: %d)%n",
                stop.getName(), route.getName(), route.getStopCount());
    }

    /**
     * Removes a stop from an existing route.
     *
     * @param route the route to modify
     * @param stop  the stop to remove
     */
    public void removeStopFromRoute(Route route, Stop stop) {
        if (route == null || stop == null) return;
        route.removeStop(stop);
        System.out.printf("[RouteManager] Stop '%s' removed from route '%s'%n",
                stop.getName(), route.getName());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable snapshot of all registered routes.
     *
     * @return all routes
     */
    public List<Route> getAllRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * Finds a route by its exact name (case-sensitive).
     *
     * @param name the route name to search for
     * @return the matching route, or {@code null} if not found
     */
    public Route getRouteByName(String name) {
        for (Route r : routes) {
            if (r.getName().equals(name)) return r;
        }
        return null;
    }

    /**
     * Returns all routes that include the given stop.
     *
     * @param stop the stop to search for
     * @return list of routes containing {@code stop}
     */
    public List<Route> getRoutesContainingStop(Stop stop) {
        List<Route> result = new ArrayList<>();
        for (Route r : routes) {
            if (r.getStops().contains(stop)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Returns the total number of registered routes.
     *
     * @return route count
     */
    public int getRouteCount() {
        return routes.size();
    }

    /**
     * Prints all routes and their stops to standard output.
     */
    public void printAllRoutes() {
        System.out.println("=== Routes (" + routes.size() + ") ===");
        for (Route r : routes) {
            System.out.printf("  Route '%s' (%d stops, %d vehicles)%n",
                    r.getName(), r.getStopCount(), r.getAssignedVehicles().size());
            for (Stop s : r.getStops()) {
                System.out.printf("    → %s (%d,%d)%n", s.getName(), s.getX(), s.getY());
            }
        }
        System.out.println("=================");
    }
}
