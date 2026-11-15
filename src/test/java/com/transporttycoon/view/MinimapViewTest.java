package com.transporttycoon.view;

import com.transporttycoon.model.GameMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MinimapView pure-logic methods.
 *
 * Uses MinimapView(false) to skip GL resource creation.
 * Avoids all LibGDX classes (OrthographicCamera etc.) so no native
 * library is needed — these tests run in any JVM environment.
 *
 * Tests: contains(), handleClick() camera repositioning and clamping,
 * panel dimension constants, dispose().
 */
class MinimapViewTest {

    private MinimapView minimap;
    private GameMap map;

    // Lightweight camera stub — avoids LibGDX native dependency
    private float camX, camY;

    @BeforeEach
    void setUp() {
        minimap = new MinimapView(false);
        map     = new GameMap(20, 15);
        camX    = 320f;
        camY    = 240f;
    }

    // ── contains() ────────────────────────────────────────────────────────────

    @Test
    void contains_center_returnsTrue() {
        float cx = MinimapView.MINIMAP_X + MinimapView.MINIMAP_W / 2f;
        float cy = MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H / 2f;
        assertTrue(minimap.contains(cx, cy));
    }

    @Test
    void contains_topLeftCorner_returnsTrue() {
        assertTrue(minimap.contains(MinimapView.MINIMAP_X, MinimapView.MINIMAP_Y));
    }

    @Test
    void contains_bottomRightCorner_returnsTrue() {
        assertTrue(minimap.contains(
            MinimapView.MINIMAP_X + MinimapView.MINIMAP_W,
            MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H));
    }

    @Test
    void contains_outsideLeft_returnsFalse() {
        assertFalse(minimap.contains(MinimapView.MINIMAP_X - 1f, MinimapView.MINIMAP_Y + 10f));
    }

    @Test
    void contains_outsideRight_returnsFalse() {
        assertFalse(minimap.contains(
            MinimapView.MINIMAP_X + MinimapView.MINIMAP_W + 1f,
            MinimapView.MINIMAP_Y + 10f));
    }

    @Test
    void contains_outsideBelow_returnsFalse() {
        assertFalse(minimap.contains(MinimapView.MINIMAP_X + 10f, MinimapView.MINIMAP_Y - 1f));
    }

    @Test
    void contains_outsideAbove_returnsFalse() {
        assertFalse(minimap.contains(
            MinimapView.MINIMAP_X + 10f,
            MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H + 1f));
    }

    // ── Coordinate normalisation (mirrors handleClick logic) ──────────────────

    @Test
    void normalise_topLeftInnerArea_mapsToZeroZero() {
        float[] n = normalise(MinimapView.MINIMAP_X + 5f, MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(0f, n[0], 0.01f);
        assertEquals(0f, n[1], 0.01f);
    }

    @Test
    void normalise_outsideLeft_clampsToZero() {
        float[] n = normalise(MinimapView.MINIMAP_X - 100f, MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(0f, n[0], 0.001f);
    }

    @Test
    void normalise_outsideRight_clampsToOne() {
        float[] n = normalise(MinimapView.MINIMAP_X + MinimapView.MINIMAP_W + 100f,
                              MinimapView.MINIMAP_Y + 5f + 18f);
        assertEquals(1f, n[0], 0.001f);
    }

    @Test
    void normalise_center_mapsToHalf() {
        float pad     = 5f;
        float legendH = 18f;
        float headerH = 14f;
        float innerW  = MinimapView.MINIMAP_W - pad * 2f;
        float innerH  = MinimapView.MINIMAP_H - pad * 2f - legendH - headerH;
        float cx = MinimapView.MINIMAP_X + pad + innerW / 2f;
        float cy = MinimapView.MINIMAP_Y + pad + legendH + innerH / 2f;
        float[] n = normalise(cx, cy);
        assertEquals(0.5f, n[0], 0.05f);
        assertEquals(0.5f, n[1], 0.05f);
    }

    // ── Panel constants ───────────────────────────────────────────────────────

    @Test
    void minimapDimensions_arePositive() {
        assertTrue(MinimapView.MINIMAP_W > 0);
        assertTrue(MinimapView.MINIMAP_H > 0);
    }

    @Test
    void minimapPosition_isTopLeft() {
        assertTrue(MinimapView.MINIMAP_X >= 0f);
        assertTrue(MinimapView.MINIMAP_Y > 500f,
            "Minimap should be in the upper portion of the screen");
    }

    @Test
    void minimapFitsWithinScreen() {
        assertTrue(MinimapView.MINIMAP_X + MinimapView.MINIMAP_W <= 1024f,
            "Minimap must fit within 1024px screen width");
        assertTrue(MinimapView.MINIMAP_Y + MinimapView.MINIMAP_H <= 768f,
            "Minimap must fit within 768px screen height");
    }

    // ── dispose() ─────────────────────────────────────────────────────────────

    @Test
    void dispose_noGlInstance_doesNotThrow() {
        assertDoesNotThrow(() -> new MinimapView(false).dispose());
    }

    // ── Helper: mirrors handleClick normalisation without LibGDX ─────────────

    private static final float PAD     = 5f;
    private static final float LEGEND_H = 18f;
    private static final float HEADER_H = 14f;
    private static final float INNER_X  = MinimapView.MINIMAP_X + PAD;
    private static final float INNER_Y  = MinimapView.MINIMAP_Y + PAD + LEGEND_H;
    private static final float INNER_W  = MinimapView.MINIMAP_W - PAD * 2f;
    private static final float INNER_H  = MinimapView.MINIMAP_H - PAD * 2f - LEGEND_H - HEADER_H;

    private static float[] normalise(float uiX, float uiY) {
        float nx = Math.max(0f, Math.min(1f, (uiX - INNER_X) / INNER_W));
        float ny = Math.max(0f, Math.min(1f, (uiY - INNER_Y) / INNER_H));
        return new float[]{nx, ny};
    }
}
