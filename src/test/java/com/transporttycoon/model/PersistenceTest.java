package com.transporttycoon.model;

import com.transporttycoon.persistence.DatabaseManager;
import com.transporttycoon.persistence.PersistenceManager;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Neon PostgreSQL persistence system (AAWT2S feature).
 *
 * <p>These tests require a live Neon database connection. If {@code db.properties}
 * is missing or the database is unreachable (e.g. in CI without credentials),
 * the entire class is skipped gracefully via {@code @BeforeAll} — the pipeline
 * does not fail.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersistenceTest {

    private static final String SLOT_A = "TEST_SLOT_A_" + System.currentTimeMillis();
    private static final String SLOT_B = "TEST_SLOT_B_" + System.currentTimeMillis();
    private static final String SLOT_C = "TEST_SLOT_C_" + System.currentTimeMillis();

    private static boolean dbAvailable = false;

    @BeforeAll
    static void initDb() {
        try {
            DatabaseManager.initialise();
            dbAvailable = true;
        } catch (Exception e) {
            System.out.println("[PersistenceTest] Database unavailable — skipping all tests. " + e.getMessage());
        }
    }

    /** Skip the test if the DB is not reachable (CI without credentials). */
    private void requireDb() {
        org.junit.jupiter.api.Assumptions.assumeTrue(dbAvailable,
            "Skipping: Neon database not available in this environment");
    }

    @AfterAll
    static void cleanup() {
        if (!dbAvailable) return;
        PersistenceManager.deleteSave(SLOT_A);
        PersistenceManager.deleteSave(SLOT_B);
        PersistenceManager.deleteSave(SLOT_C);
    }

    // ── Schema / connection ───────────────────────────────────────────────────

    @Test
    @Order(1)
    void databaseManager_initialises_withoutException() {
        requireDb();
        assertTrue(DatabaseManager.isInitialised());
    }

    @Test
    @Order(2)
    void save_doesNotThrow_forValidGame() {
        requireDb();
        Game game = buildTestGame();
        assertDoesNotThrow(() -> PersistenceManager.save(game, SLOT_A));
    }

    @Test
    @Order(3)
    void save_overwrite_replacesExistingSlot() {
        requireDb();
        Game game1 = buildTestGame();
        game1.getPlayer().setCapitalDirect(10000.0);
        PersistenceManager.save(game1, SLOT_B);
        Game game2 = buildTestGame();
        game2.getPlayer().setCapitalDirect(99999.0);
        assertDoesNotThrow(() -> PersistenceManager.save(game2, SLOT_B));
        Game loaded = PersistenceManager.load(SLOT_B);
        assertNotNull(loaded);
        assertEquals(99999.0, loaded.getPlayer().getCapital(), 0.01);
    }

    @Test
    @Order(4)
    void load_returnsNull_forNonExistentSlot() {
        requireDb();
        assertNull(PersistenceManager.load("SLOT_THAT_DOES_NOT_EXIST_XYZ"));
    }

    @Test
    @Order(5)
    void load_restoresCapital() {
        requireDb();
        Game game = buildTestGame();
        game.getPlayer().setCapitalDirect(42500.0);
        PersistenceManager.save(game, SLOT_A);
        Game loaded = PersistenceManager.load(SLOT_A);
        assertNotNull(loaded);
        assertEquals(42500.0, loaded.getPlayer().getCapital(), 0.01);
    }

    @Test
    @Order(6)
    void load_restoresTimeSpeed() {
        requireDb();
        Game game = buildTestGame();
        game.setTimeSpeed(TimeSpeed.FAST);
        PersistenceManager.save(game, SLOT_A);
        assertEquals(TimeSpeed.FAST, PersistenceManager.load(SLOT_A).getTimeSpeed());
    }

    @Test
    @Order(7)
    void load_restoresGameTime() {
        requireDb();
        Game game = buildTestGame();
        game.setGameTimeDirect(1234.5f);
        PersistenceManager.save(game, SLOT_A);
        assertEquals(1234.5f, PersistenceManager.load(SLOT_A).getGameTime(), 0.1f);
    }

    @Test
    @Order(8)
    void load_restoresRoads() {
        requireDb();
        Game game = buildTestGame();
        game.getMap().placeRoad(3, 3);
        game.getMap().placeRoad(4, 3);
        PersistenceManager.save(game, SLOT_A);
        Game loaded = PersistenceManager.load(SLOT_A);
        assertNotNull(loaded);
        assertTrue(loaded.getMap().isRoad(3, 3));
        assertTrue(loaded.getMap().isRoad(4, 3));
        assertFalse(loaded.getMap().isRoad(6, 3));
    }

    @Test
    @Order(9)
    void load_restoresStops() {
        requireDb();
        Game game = buildTestGame();
        game.getMap().placeRoad(5, 5);
        game.getStopManager().placeStopFree("TestStop", 5, 5);
        PersistenceManager.save(game, SLOT_A);
        List<Stop> stops = PersistenceManager.load(SLOT_A).getStopManager().getActiveStops();
        assertEquals(1, stops.size());
        assertEquals("TestStop", stops.get(0).getName());
    }

    @Test
    @Order(10)
    void load_restoresRouteWithStops() {
        requireDb();
        Game game = buildTestGame();
        game.getMap().placeRoad(2, 5);
        game.getMap().placeRoad(7, 5);
        Stop s1 = game.getStopManager().placeStopFree("Alpha", 2, 5);
        Stop s2 = game.getStopManager().placeStopFree("Beta",  7, 5);
        Route route = game.getRouteManager().createRoute("R1");
        route.addStop(s1); route.addStop(s2);
        game.addRoute(route);
        PersistenceManager.save(game, SLOT_A);
        List<Route> routes = PersistenceManager.load(SLOT_A).getRoutes();
        assertEquals(1, routes.size());
        assertEquals(2, routes.get(0).getStopCount());
    }

    @Test
    @Order(11)
    void load_restoresVehiclePosition() {
        requireDb();
        Game game = buildTestGame();
        game.getMap().placeRoad(4, 4);
        game.getStopManager().placeStopFree("S1", 4, 4);
        Vehicle v = game.getVehicleManager().purchaseVehicleFree(VehicleType.TRUCK, 1900);
        v.setPosition(4.5f, 4.0f);
        game.addVehicle(v);
        PersistenceManager.save(game, SLOT_A);
        Vehicle loaded = PersistenceManager.load(SLOT_A).getVehicles().get(0);
        assertEquals(4.5f, loaded.getX(), 0.01f);
        assertEquals(4.0f, loaded.getY(), 0.01f);
    }

    @Test
    @Order(12)
    void load_restoresVehicleType() {
        requireDb();
        Game game = buildTestGame();
        game.addVehicle(game.getVehicleManager().purchaseVehicleFree(VehicleType.BUS, 1900));
        PersistenceManager.save(game, SLOT_A);
        assertEquals(VehicleType.BUS, PersistenceManager.load(SLOT_A).getVehicles().get(0).getType());
    }

    @Test
    @Order(13)
    void listSaves_returnsMultipleSlots() {
        requireDb();
        PersistenceManager.save(buildTestGame(), SLOT_A);
        PersistenceManager.save(buildTestGame(), SLOT_C);
        long count = PersistenceManager.listSaves().stream()
                .filter(s -> s.slotName.equals(SLOT_A) || s.slotName.equals(SLOT_C))
                .count();
        assertEquals(2, count);
    }

    @Test
    @Order(14)
    void deleteSave_removesSlotFromList() {
        requireDb();
        PersistenceManager.save(buildTestGame(), SLOT_C);
        PersistenceManager.deleteSave(SLOT_C);
        assertNull(PersistenceManager.load(SLOT_C));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Game buildTestGame() {
        Game game = new Game();
        game.setGameTimeDirect(0f);
        game.setGameYearDirect(1900);
        game.setGameMonthDirect(1);
        game.setTimeSpeed(TimeSpeed.NORMAL);
        game.getPlayer().setCapitalDirect(50000.0);
        return game;
    }
}
