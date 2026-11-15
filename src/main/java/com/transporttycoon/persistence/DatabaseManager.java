package com.transporttycoon.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages the PostgreSQL (Neon) database connection and schema initialisation.
 *
 * <p>Credentials are loaded from {@code db.properties} on the classpath or
 * working directory — this file is listed in {@code .gitignore} and must
 * never be committed to version control.</p>
 *
 * <p>Call {@link #initialise()} once at application startup. All other
 * persistence classes obtain connections via {@link #getConnection()}.</p>
 */
public class DatabaseManager {

    private static final String PROPS_FILE = "db.properties";

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialised = false;

    private DatabaseManager() {}

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Loads credentials, tests the connection, and creates all required tables
     * if they do not already exist.
     *
     * @throws RuntimeException if credentials cannot be loaded or the database
     *                          is unreachable
     */
    public static void initialise() {
        if (initialised) return;
        loadCredentials();
        createSchema();
        initialised = true;
        System.out.println("[DB] Connected to Neon PostgreSQL and schema verified.");
    }

    private static void loadCredentials() {
        Properties props = new Properties();

        // Try classpath first, then working directory
        try (InputStream is = DatabaseManager.class.getClassLoader()
                .getResourceAsStream(PROPS_FILE)) {
            if (is != null) {
                props.load(is);
            } else {
                // Fall back to file system (working directory)
                try (InputStream fis = new java.io.FileInputStream(PROPS_FILE)) {
                    props.load(fis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                "[DB] Cannot load " + PROPS_FILE + ". " +
                "Create it with db.url, db.user, db.password.", e);
        }

        jdbcUrl   = props.getProperty("db.url");
        dbUser    = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");

        if (jdbcUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException(
                "[DB] db.properties is missing one or more required keys: " +
                "db.url, db.user, db.password");
        }
    }

    // ── Connection ────────────────────────────────────────────────────────────

    /**
     * Returns a new JDBC connection to the Neon database.
     * Callers are responsible for closing the connection (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    /**
     * Creates all tables required by the persistence system if they do not
     * already exist. Safe to call on every startup (idempotent).
     */
    private static void createSchema() {
        String[] ddl = {
            // ── Save slots ────────────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_slots (
                id          SERIAL PRIMARY KEY,
                slot_name   VARCHAR(100) NOT NULL UNIQUE,
                saved_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                game_time   FLOAT        NOT NULL,
                game_year   INT          NOT NULL,
                game_month  INT          NOT NULL,
                time_speed  VARCHAR(20)  NOT NULL,
                capital     FLOAT        NOT NULL
            )
            """,

            // ── Map tiles ─────────────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_tiles (
                save_id     INT          NOT NULL REFERENCES save_slots(id) ON DELETE CASCADE,
                x           INT          NOT NULL,
                y           INT          NOT NULL,
                has_road    BOOLEAN      NOT NULL DEFAULT FALSE,
                tree_count  INT          NOT NULL DEFAULT 0,
                PRIMARY KEY (save_id, x, y)
            )
            """,

            // ── Stops ─────────────────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_stops (
                id          SERIAL PRIMARY KEY,
                save_id     INT          NOT NULL REFERENCES save_slots(id) ON DELETE CASCADE,
                stop_name   VARCHAR(100) NOT NULL,
                x           INT          NOT NULL,
                y           INT          NOT NULL,
                is_active   BOOLEAN      NOT NULL DEFAULT TRUE
            )
            """,

            // ── Routes ────────────────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_routes (
                id          SERIAL PRIMARY KEY,
                save_id     INT          NOT NULL REFERENCES save_slots(id) ON DELETE CASCADE,
                route_name  VARCHAR(100) NOT NULL
            )
            """,

            // ── Route stop order ──────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_route_stops (
                route_id    INT NOT NULL REFERENCES save_routes(id) ON DELETE CASCADE,
                stop_order  INT NOT NULL,
                stop_id     INT NOT NULL REFERENCES save_stops(id) ON DELETE CASCADE,
                PRIMARY KEY (route_id, stop_order)
            )
            """,

            // ── Vehicles ──────────────────────────────────────────────────────
            """
            CREATE TABLE IF NOT EXISTS save_vehicles (
                id                  SERIAL PRIMARY KEY,
                save_id             INT          NOT NULL REFERENCES save_slots(id) ON DELETE CASCADE,
                vehicle_type        VARCHAR(30)  NOT NULL,
                purchase_year       INT          NOT NULL,
                pos_x               FLOAT        NOT NULL,
                pos_y               FLOAT        NOT NULL,
                state               VARCHAR(20)  NOT NULL,
                route_id            INT          REFERENCES save_routes(id) ON DELETE SET NULL,
                current_stop_id     INT          REFERENCES save_stops(id)  ON DELETE SET NULL,
                target_stop_id      INT          REFERENCES save_stops(id)  ON DELETE SET NULL,
                path_index          INT          NOT NULL DEFAULT 0,
                movement_progress   FLOAT        NOT NULL DEFAULT 0,
                total_income        FLOAT        NOT NULL DEFAULT 0,
                operational_age_sec FLOAT        NOT NULL DEFAULT 0
            )
            """
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Schema creation failed: " + e.getMessage(), e);
        }
    }

    /** Returns true if the database has been successfully initialised. */
    public static boolean isInitialised() {
        return initialised;
    }
}
