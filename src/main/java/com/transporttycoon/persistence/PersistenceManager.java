package com.transporttycoon.persistence;

import com.transporttycoon.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles saving and loading complete game states to/from the Neon PostgreSQL database.
 *
 * <p>Spec requirements satisfied:</p>
 * <ul>
 *   <li>Save a given game state.</li>
 *   <li>Load a selected saved game state to continue playing.</li>
 *   <li>Vehicles that were in motion at save time resume their journey from
 *       where they were located.</li>
 *   <li>Multiple save files (save slots) are supported.</li>
 * </ul>
 *
 * <p>All database operations are transactional — a failed save never leaves
 * the database in a partial state.</p>
 */
public class PersistenceManager {

    private PersistenceManager() {}

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Saves the complete game state under the given slot name.
     * If a save with that name already exists it is overwritten atomically.
     *
     * @param game     the live game to save
     * @param slotName a human-readable name for this save (e.g. "Slot 1")
     * @throws RuntimeException on any database error
     */
    public static void save(Game game, String slotName) {
        System.out.println("[Persistence] Saving game to slot: " + slotName);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete existing save with same name (overwrite)
                deleteSaveByName(conn, slotName);

                // ── 1. Save slot header ───────────────────────────────────────
                int saveId = insertSaveSlot(conn, slotName, game);

                // ── 2. Save road/forest tiles ─────────────────────────────────
                saveTiles(conn, saveId, game.getMap());

                // ── 3. Save stops ─────────────────────────────────────────────
                // Map from Stop object → its new DB id (needed for routes/vehicles)
                Map<Stop, Integer> stopIdMap = saveStops(conn, saveId, game);

                // ── 4. Save routes ────────────────────────────────────────────
                Map<Route, Integer> routeIdMap = saveRoutes(conn, saveId, game, stopIdMap);

                // ── 5. Save vehicles ──────────────────────────────────────────
                saveVehicles(conn, saveId, game, stopIdMap, routeIdMap);

                conn.commit();
                System.out.println("[Persistence] Save complete. Slot id=" + saveId);

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("[Persistence] Save failed, rolled back: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("[Persistence] Database error during save: " + e.getMessage(), e);
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Loads a saved game state by slot name and returns a fully reconstructed
     * {@link Game} object ready to resume.
     *
     * <p>Vehicles that were in motion at save time have their position and
     * movement progress restored so they resume from exactly where they were.</p>
     *
     * @param slotName the save slot name to load
     * @return a reconstructed Game, or {@code null} if the slot does not exist
     * @throws RuntimeException on any database error
     */
    public static Game load(String slotName) {
        System.out.println("[Persistence] Loading game from slot: " + slotName);

        try (Connection conn = DatabaseManager.getConnection()) {

            // ── 1. Load slot header ───────────────────────────────────────────
            int saveId;
            Game game;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM save_slots WHERE slot_name = ?")) {
                ps.setString(1, slotName);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    System.out.println("[Persistence] No save found with name: " + slotName);
                    return null;
                }
                saveId = rs.getInt("id");
                game   = reconstructGameHeader(rs);
            }

            // ── 2. Restore road/forest tiles ──────────────────────────────────
            loadTiles(conn, saveId, game.getMap());

            // ── 3. Restore stops ──────────────────────────────────────────────
            // Map from DB stop id → Stop object (needed to wire routes/vehicles)
            Map<Integer, Stop> stopById = loadStops(conn, saveId, game);

            // ── 4. Restore routes ─────────────────────────────────────────────
            Map<Integer, Route> routeById = loadRoutes(conn, saveId, game, stopById);

            // ── 5. Restore vehicles (with in-motion state) ────────────────────
            loadVehicles(conn, saveId, game, stopById, routeById);

            System.out.println("[Persistence] Load complete from slot: " + slotName);
            return game;

        } catch (SQLException e) {
            throw new RuntimeException("[Persistence] Database error during load: " + e.getMessage(), e);
        }
    }

    // ── List / Delete ─────────────────────────────────────────────────────────

    /**
     * Returns metadata for all save slots, ordered by most recent first.
     */
    public static List<SaveSlotInfo> listSaves() {
        List<SaveSlotInfo> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT slot_name, saved_at, game_time, capital " +
                 "FROM save_slots ORDER BY saved_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new SaveSlotInfo(
                    rs.getString("slot_name"),
                    rs.getTimestamp("saved_at").toString(),
                    rs.getFloat("game_time"),
                    rs.getDouble("capital")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[Persistence] Could not list saves: " + e.getMessage());
        }
        return result;
    }

    /**
     * Deletes a save slot and all its associated data (cascades via FK).
     */
    public static void deleteSave(String slotName) {
        try (Connection conn = DatabaseManager.getConnection()) {
            deleteSaveByName(conn, slotName);
            System.out.println("[Persistence] Deleted save: " + slotName);
        } catch (SQLException e) {
            throw new RuntimeException("[Persistence] Delete failed: " + e.getMessage(), e);
        }
    }

    // ── Private helpers: Save ─────────────────────────────────────────────────

    private static void deleteSaveByName(Connection conn, String slotName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM save_slots WHERE slot_name = ?")) {
            ps.setString(1, slotName);
            ps.executeUpdate();
        }
    }

    private static int insertSaveSlot(Connection conn, String slotName, Game game)
            throws SQLException {
        String sql = "INSERT INTO save_slots " +
                     "(slot_name, game_time, game_year, game_month, time_speed, capital) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotName);
            ps.setFloat (2, game.getGameTime());
            ps.setInt   (3, game.getGameYear());
            ps.setInt   (4, game.getGameMonth());
            ps.setString(5, game.getTimeSpeed().name());
            ps.setDouble(6, game.getPlayer().getCapital());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void saveTiles(Connection conn, int saveId, GameMap map)
            throws SQLException {
        String sql = "INSERT INTO save_tiles (save_id, x, y, has_road, tree_count) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    Tile tile = map.getTile(x, y);
                    if (tile == null) continue;
                    // Only persist tiles that differ from default (road or trees)
                    if (!tile.hasRoad() && tile.getTreeCount() == 0) continue;
                    ps.setInt    (1, saveId);
                    ps.setInt    (2, x);
                    ps.setInt    (3, y);
                    ps.setBoolean(4, tile.hasRoad());
                    ps.setInt    (5, tile.getTreeCount());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private static Map<Stop, Integer> saveStops(Connection conn, int saveId, Game game)
            throws SQLException {
        Map<Stop, Integer> stopIdMap = new HashMap<>();
        String sql = "INSERT INTO save_stops (save_id, stop_name, x, y, is_active) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";
        List<Stop> allStops = game.getStopManager().getAllStops();
        for (Stop stop : allStops) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt    (1, saveId);
                ps.setString (2, stop.getName());
                ps.setInt    (3, stop.getX());
                ps.setInt    (4, stop.getY());
                ps.setBoolean(5, stop.isActive());
                ResultSet rs = ps.executeQuery();
                rs.next();
                stopIdMap.put(stop, rs.getInt(1));
            }
        }
        return stopIdMap;
    }

    private static Map<Route, Integer> saveRoutes(Connection conn, int saveId, Game game,
                                                   Map<Stop, Integer> stopIdMap)
            throws SQLException {
        Map<Route, Integer> routeIdMap = new HashMap<>();
        String routeSql = "INSERT INTO save_routes (save_id, route_name) VALUES (?, ?) RETURNING id";
        String orderSql = "INSERT INTO save_route_stops (route_id, stop_order, stop_id) VALUES (?, ?, ?)";

        for (Route route : game.getRoutes()) {
            int routeDbId;
            try (PreparedStatement ps = conn.prepareStatement(routeSql)) {
                ps.setInt   (1, saveId);
                ps.setString(2, route.getName());
                ResultSet rs = ps.executeQuery();
                rs.next();
                routeDbId = rs.getInt(1);
            }
            routeIdMap.put(route, routeDbId);

            // Save stop order
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                List<Stop> stops = route.getStops();
                for (int i = 0; i < stops.size(); i++) {
                    Integer stopDbId = stopIdMap.get(stops.get(i));
                    if (stopDbId == null) continue;
                    ps.setInt(1, routeDbId);
                    ps.setInt(2, i);
                    ps.setInt(3, stopDbId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        return routeIdMap;
    }

    private static void saveVehicles(Connection conn, int saveId, Game game,
                                      Map<Stop, Integer> stopIdMap,
                                      Map<Route, Integer> routeIdMap)
            throws SQLException {
        String sql = "INSERT INTO save_vehicles " +
                     "(save_id, vehicle_type, purchase_year, pos_x, pos_y, state, " +
                     " route_id, current_stop_id, target_stop_id, " +
                     " path_index, movement_progress, total_income, operational_age_sec) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        for (Vehicle v : game.getVehicles()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt   (1,  saveId);
                ps.setString(2,  v.getType().name());
                ps.setInt   (3,  v.getPurchaseYear());
                ps.setFloat (4,  v.getX());
                ps.setFloat (5,  v.getY());
                ps.setString(6,  v.getState().name());

                // Route (nullable)
                Route route = v.getAssignedRoute();
                Integer routeDbId = (route != null) ? routeIdMap.get(route) : null;
                if (routeDbId != null) ps.setInt(7, routeDbId);
                else                   ps.setNull(7, Types.INTEGER);

                // Current stop (nullable)
                Stop cur = v.getCurrentStop();
                Integer curDbId = (cur != null) ? stopIdMap.get(cur) : null;
                if (curDbId != null) ps.setInt(8, curDbId);
                else                 ps.setNull(8, Types.INTEGER);

                // Target stop (nullable) — key for resuming in-motion vehicles
                Stop tgt = v.getTargetStop();
                Integer tgtDbId = (tgt != null) ? stopIdMap.get(tgt) : null;
                if (tgtDbId != null) ps.setInt(9, tgtDbId);
                else                 ps.setNull(9, Types.INTEGER);

                ps.setInt  (10, v.getPathIndex());
                ps.setFloat(11, v.getMovementProgress());
                ps.setDouble(12, v.getTotalIncome());
                ps.setFloat(13, v.getOperationalAgeDays() * 60f); // convert days→seconds

                ps.executeUpdate();
            }
        }
    }

    // ── Private helpers: Load ─────────────────────────────────────────────────

    private static Game reconstructGameHeader(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setTimeSpeed(TimeSpeed.valueOf(rs.getString("time_speed")));
        game.getPlayer().setCapitalDirect(rs.getDouble("capital"));
        game.setGameTimeDirect(rs.getFloat("game_time"));
        game.setGameYearDirect(rs.getInt("game_year"));
        game.setGameMonthDirect(rs.getInt("game_month"));
        return game;
    }

    private static void loadTiles(Connection conn, int saveId, GameMap map)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT x, y, has_road, tree_count FROM save_tiles WHERE save_id = ?")) {
            ps.setInt(1, saveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;
                if (rs.getBoolean("has_road")) tile.setRoad(true);
                tile.setTreeCount(rs.getInt("tree_count"));
            }
        }
    }

    private static Map<Integer, Stop> loadStops(Connection conn, int saveId, Game game)
            throws SQLException {
        Map<Integer, Stop> stopById = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, stop_name, x, y, is_active FROM save_stops WHERE save_id = ?")) {
            ps.setInt(1, saveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int dbId = rs.getInt("id");
                Stop stop = game.getStopManager().placeStopFree(
                    rs.getString("stop_name"),
                    rs.getInt("x"),
                    rs.getInt("y")
                );
                if (stop != null) {
                    stop.setActive(rs.getBoolean("is_active"));
                    stopById.put(dbId, stop);
                }
            }
        }
        return stopById;
    }

    private static Map<Integer, Route> loadRoutes(Connection conn, int saveId, Game game,
                                                   Map<Integer, Stop> stopById)
            throws SQLException {
        Map<Integer, Route> routeById = new HashMap<>();

        // Load route headers
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, route_name FROM save_routes WHERE save_id = ?")) {
            ps.setInt(1, saveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int dbId = rs.getInt("id");
                Route route = game.getRouteManager().createRoute(rs.getString("route_name"));
                routeById.put(dbId, route);
            }
        }

        // Load stop order for each route
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT rs.route_id, rs.stop_order, rs.stop_id " +
                "FROM save_route_stops rs " +
                "JOIN save_routes r ON r.id = rs.route_id " +
                "WHERE r.save_id = ? ORDER BY rs.route_id, rs.stop_order")) {
            ps.setInt(1, saveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Route route = routeById.get(rs.getInt("route_id"));
                Stop  stop  = stopById.get(rs.getInt("stop_id"));
                if (route != null && stop != null) {
                    route.addStop(stop);
                }
            }
        }

        // Register routes in the game
        for (Route route : routeById.values()) {
            game.addRoute(route);
        }

        return routeById;
    }

    private static void loadVehicles(Connection conn, int saveId, Game game,
                                      Map<Integer, Stop> stopById,
                                      Map<Integer, Route> routeById)
            throws SQLException {
        String sql = "SELECT * FROM save_vehicles WHERE save_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                VehicleType type = VehicleType.valueOf(rs.getString("vehicle_type"));
                int purchaseYear = rs.getInt("purchase_year");

                // Purchase vehicle (bypasses cost deduction on load)
                Vehicle v = game.getVehicleManager().purchaseVehicleFree(type, purchaseYear);
                if (v == null) continue;

                // Restore position (fractional — preserves in-motion location)
                v.setPosition(rs.getFloat("pos_x"), rs.getFloat("pos_y"));

                // Restore route
                int routeDbId = rs.getInt("route_id");
                if (!rs.wasNull()) {
                    Route route = routeById.get(routeDbId);
                    if (route != null) {
                        v.setAssignedRoute(route);
                        route.assignVehicle(v);
                    }
                }

                // Restore current stop
                int curStopId = rs.getInt("current_stop_id");
                if (!rs.wasNull()) {
                    v.setCurrentStopDirect(stopById.get(curStopId));
                }

                // Restore target stop (vehicle was in motion at save time)
                int tgtStopId = rs.getInt("target_stop_id");
                if (!rs.wasNull()) {
                    Stop targetStop = stopById.get(tgtStopId);
                    v.setTargetStopDirect(targetStop);
                    // Re-request path so vehicle resumes movement immediately
                    if (targetStop != null) {
                        game.getVehicleManager().refreshPath(v);
                    }
                }

                // Restore movement progress (sub-tile position)
                v.setMovementProgress(rs.getFloat("movement_progress"));

                // Restore state
                VehicleState state = VehicleState.valueOf(rs.getString("state"));
                v.setStateDirect(state);

                game.addVehicle(v);
            }
        }
    }

    // ── SaveSlotInfo ──────────────────────────────────────────────────────────

    /** Lightweight DTO for displaying save slot metadata in the UI. */
    public static class SaveSlotInfo {
        public final String slotName;
        public final String savedAt;
        public final float  gameTime;
        public final double capital;

        public SaveSlotInfo(String slotName, String savedAt, float gameTime, double capital) {
            this.slotName  = slotName;
            this.savedAt   = savedAt;
            this.gameTime  = gameTime;
            this.capital   = capital;
        }

        @Override
        public String toString() {
            return String.format("%s  |  Day %.1f  |  $%.0f  |  %s",
                slotName, gameTime / 60f, capital, savedAt);
        }
    }
}
