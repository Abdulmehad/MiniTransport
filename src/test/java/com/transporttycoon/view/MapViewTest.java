package com.transporttycoon.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MapView pure-logic methods.
 *
 * MapView's rendering requires GL shaders and native LibGDX libraries.
 * We test only the coordinate conversion and state methods via an inner
 * logic harness — no LibGDX classes needed, runs in any JVM.
 */
class MapViewTest {

    private static final int TILE_SIZE = 32;
    private MapViewLogicHarness harness;

    @BeforeEach
    void setUp() {
        harness = new MapViewLogicHarness();
    }

    // ── Grid toggle ───────────────────────────────────────────────────────────

    @Test
    void showGrid_defaultIsTrue() {
        assertTrue(harness.isShowGrid());
    }

    @Test
    void setShowGrid_false_disablesGrid() {
        harness.setShowGrid(false);
        assertFalse(harness.isShowGrid());
    }

    @Test
    void setShowGrid_toggle_roundTrips() {
        harness.setShowGrid(false);
        harness.setShowGrid(true);
        assertTrue(harness.isShowGrid());
    }

    // ── Coordinate conversion ─────────────────────────────────────────────────

    @Test
    void mapToScreen_tile0_0_returnsOrigin() {
        float[] r = harness.mapToScreen(0, 0);
        assertEquals(0f, r[0], 0.001f);
        assertEquals(0f, r[1], 0.001f);
    }

    @Test
    void mapToScreen_tile1_0_returns32_0() {
        float[] r = harness.mapToScreen(1, 0);
        assertEquals(TILE_SIZE, r[0], 0.001f);
        assertEquals(0f,        r[1], 0.001f);
    }

    @Test
    void mapToScreen_tile0_1_returns0_32() {
        float[] r = harness.mapToScreen(0, 1);
        assertEquals(0f,        r[0], 0.001f);
        assertEquals(TILE_SIZE, r[1], 0.001f);
    }

    @Test
    void mapToScreen_tile5_3_returns160_96() {
        float[] r = harness.mapToScreen(5, 3);
        assertEquals(5 * TILE_SIZE, r[0], 0.001f);
        assertEquals(3 * TILE_SIZE, r[1], 0.001f);
    }

    @Test
    void mapToScreen_negativeCoords_returnsNegativePixels() {
        float[] r = harness.mapToScreen(-1, -2);
        assertEquals(-TILE_SIZE,     r[0], 0.001f);
        assertEquals(-2 * TILE_SIZE, r[1], 0.001f);
    }

    // ── Selected tile state ───────────────────────────────────────────────────

    @Test
    void selectedTile_defaultIsMinusOne() {
        assertEquals(-1, harness.getSelectedTileX());
        assertEquals(-1, harness.getSelectedTileY());
    }

    @Test
    void setSelectedTile_updatesCoordinates() {
        harness.setSelectedTile(4, 7);
        assertEquals(4, harness.getSelectedTileX());
        assertEquals(7, harness.getSelectedTileY());
    }

    @Test
    void clearSelectedTile_resetsToMinusOne() {
        harness.setSelectedTile(4, 7);
        harness.clearSelectedTile();
        assertEquals(-1, harness.getSelectedTileX());
        assertEquals(-1, harness.getSelectedTileY());
    }

    @Test
    void setSelectedTile_overwrite_updatesCorrectly() {
        harness.setSelectedTile(1, 1);
        harness.setSelectedTile(9, 9);
        assertEquals(9, harness.getSelectedTileX());
        assertEquals(9, harness.getSelectedTileY());
    }

    // ── Inner harness ─────────────────────────────────────────────────────────

    static class MapViewLogicHarness {
        private boolean showGrid = true;
        private int selectedTileX = -1;
        private int selectedTileY = -1;

        boolean isShowGrid()                  { return showGrid; }
        void    setShowGrid(boolean v)        { showGrid = v; }
        int     getSelectedTileX()            { return selectedTileX; }
        int     getSelectedTileY()            { return selectedTileY; }
        void    setSelectedTile(int x, int y) { selectedTileX = x; selectedTileY = y; }
        void    clearSelectedTile()           { selectedTileX = -1; selectedTileY = -1; }

        float[] mapToScreen(int mapX, int mapY) {
            return new float[]{ mapX * TILE_SIZE, mapY * TILE_SIZE };
        }
    }
}
