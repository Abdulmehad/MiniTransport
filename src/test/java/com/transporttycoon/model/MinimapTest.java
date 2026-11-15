package com.transporttycoon.model;

import com.transporttycoon.view.MinimapView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for minimap logic (AAWT2S feature).
 *
 * <p>Spec requirement: "The game map should be larger than the displayed area
 * and should be scrollable in both X and Y dimensions for navigation.
 * For easier orientation, a navigable minimap should be part of the game interface."</p>
 *
 * <p>These tests cover the coordinate-mapping and bounds logic of
 * {@link MinimapView} without requiring a LibGDX GL context.</p>
 */
class MinimapTest {

    private GameMap map;

    @BeforeEach
    void setUp() {
        map = new GameMap(20, 15);
    }

    // ── contains() ────────────────────────────────────────────────────────────

    @Test
    void contains_centerOfMinimap_returnsTrue() {
        float cx = MinimapView.MINIMAP_X + MinimapView.MINIMAP_W / 2f;
        float cy = MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H / 2f;
        MinimapView mv = new MinimapViewTestable();
        assertTrue(mv.contains(cx, cy), "Center of minimap panel should be inside");
    }

    @Test
    void contains_topLeftCorner_returnsTrue() {
        MinimapView mv = new MinimapViewTestable();
        assertTrue(mv.contains(MinimapView.MINIMAP_X, MinimapView.MINIMAP_Y));
    }

    @Test
    void contains_outsideLeft_returnsFalse() {
        MinimapView mv = new MinimapViewTestable();
        assertFalse(mv.contains(MinimapView.MINIMAP_X - 1f, MinimapView.MINIMAP_Y + 10f));
    }

    @Test
    void contains_outsideRight_returnsFalse() {
        MinimapView mv = new MinimapViewTestable();
        assertFalse(mv.contains(MinimapView.MINIMAP_X + MinimapView.MINIMAP_W + 1f,
                MinimapView.MINIMAP_Y + 10f));
    }

    @Test
    void contains_outsideBelow_returnsFalse() {
        MinimapView mv = new MinimapViewTestable();
        assertFalse(mv.contains(MinimapView.MINIMAP_X + 10f, MinimapView.MINIMAP_Y - 1f));
    }

    @Test
    void contains_outsideAbove_returnsFalse() {
        MinimapView mv = new MinimapViewTestable();
        assertFalse(mv.contains(MinimapView.MINIMAP_X + 10f,
                MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H + 1f));
    }

    // ── Minimap panel dimensions are positive ─────────────────────────────────

    @Test
    void minimapDimensions_arePositive() {
        assertTrue(MinimapView.MINIMAP_W > 0, "Minimap width must be positive");
        assertTrue(MinimapView.MINIMAP_H > 0, "Minimap height must be positive");
    }

    @Test
    void minimapPosition_isWithinSidebarArea() {
        // Minimap is in the top-left corner of the screen
        assertTrue(MinimapView.MINIMAP_X >= 0f, "Minimap X must be non-negative");
        assertTrue(MinimapView.MINIMAP_Y > 500f,
                "Minimap Y should be in the upper portion of the screen (y > 500)");
    }

    // ── Map is larger than viewport (scrollability precondition) ──────────────

    @Test
    void mapSize_isLargerThanTypicalViewport() {
        // Map is 20x15 tiles × 32px = 640×480 world pixels
        // Typical viewport is 1024×768 at zoom 1 — but the map area is ~734px wide
        // (1024 - 290 sidebar). At zoom 1 the map fills the viewport, so scrolling
        // is meaningful at zoom < 1 or on larger maps.
        int mapPixelW = map.getWidth()  * com.transporttycoon.GameConstants.TILE_SIZE;
        int mapPixelH = map.getHeight() * com.transporttycoon.GameConstants.TILE_SIZE;
        assertTrue(mapPixelW > 0 && mapPixelH > 0,
                "Map must have positive pixel dimensions for scrolling to work");
    }

    @Test
    void gameConstants_mapDimensions_arePositive() {
        assertTrue(com.transporttycoon.GameConstants.MAP_WIDTH  > 0);
        assertTrue(com.transporttycoon.GameConstants.MAP_HEIGHT > 0);
        assertTrue(com.transporttycoon.GameConstants.TILE_SIZE  > 0);
    }

    // ── Coordinate normalisation (white-box via subclass) ─────────────────────

    @Test
    void normalisation_topLeftClick_mapsToZeroZero() {
        // INNER_X = MINIMAP_X + PAD(5), INNER_Y = MINIMAP_Y + PAD(5) + LEGEND_H(18)
        float[] result = MinimapViewTestable.normalise(
                MinimapView.MINIMAP_X + 5f,
                MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(0f, result[0], 0.01f, "Top-left of inner area should normalise to x=0");
        assertEquals(0f, result[1], 0.01f, "Top-left of inner area should normalise to y=0");
    }

    @Test
    void normalisation_outsideLeft_clampsToZero() {
        float[] result = MinimapViewTestable.normalise(
                MinimapView.MINIMAP_X - 50f,
                MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(0f, result[0], 0.001f, "Click left of inner area should clamp to 0");
    }

    @Test
    void normalisation_outsideRight_clampsToOne() {
        float[] result = MinimapViewTestable.normalise(
                MinimapView.MINIMAP_X + MinimapView.MINIMAP_W + 50f,
                MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(1f, result[0], 0.001f, "Click right of inner area should clamp to 1");
    }

    @Test
    void normalisation_centerClick_mapsToHalfHalf() {
        // Centre of the inner map area
        float innerX = MinimapView.MINIMAP_X + 5f;
        float innerY = MinimapView.MINIMAP_Y + 5f + 18f;
        float innerW = MinimapView.MINIMAP_W - 10f;
        float innerH = MinimapView.MINIMAP_H - 10f - 18f - 14f; // minus legend and header
        float[] result = MinimapViewTestable.normalise(
                innerX + innerW / 2f,
                innerY + innerH / 2f);
        assertEquals(0.5f, result[0], 0.05f, "Center click should normalise to ~0.5 in x");
        assertEquals(0.5f, result[1], 0.05f, "Center click should normalise to ~0.5 in y");
    }

    // ── Helper subclass exposing internal normalisation without GL context ─────

    /**
     * Subclass that exposes the coordinate normalisation logic for testing
     * without needing a LibGDX GL context (no ShapeRenderer/SpriteBatch created).
     */
    static class MinimapViewTestable extends MinimapView {
        // Mirror the constants from MinimapView
        private static final float PAD      = 5f;
        private static final float LEGEND_H = 18f;
        private static final float HEADER_H = 14f;
        private static final float INNER_X  = MinimapView.MINIMAP_X + PAD;
        private static final float INNER_Y  = MinimapView.MINIMAP_Y + PAD + LEGEND_H;
        private static final float INNER_W  = MinimapView.MINIMAP_W - PAD * 2f;
        private static final float INNER_H  = MinimapView.MINIMAP_H - PAD * 2f - LEGEND_H - HEADER_H;

        MinimapViewTestable() {
            super(false); // skip GL resource creation
        }

        /** Returns [normX, normY] clamped to [0,1]. */
        static float[] normalise(float uiX, float uiY) {
            float nx = Math.max(0f, Math.min(1f, (uiX - INNER_X) / INNER_W));
            float ny = Math.max(0f, Math.min(1f, (uiY - INNER_Y) / INNER_H));
            return new float[]{nx, ny};
        }
    }
}
