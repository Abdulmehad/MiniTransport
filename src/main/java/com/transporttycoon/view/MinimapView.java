package com.transporttycoon.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.transporttycoon.GameConstants;
import com.transporttycoon.model.City;
import com.transporttycoon.model.GameMap;
import com.transporttycoon.model.IndustrialFacility;
import com.transporttycoon.model.Stop;
import com.transporttycoon.model.Tile;
import com.transporttycoon.model.Vehicle;

import java.util.List;

/**
 * Renders a high-quality navigable minimap overlay in the UI.
 *
 * <p>Positioned on the left side below the demand panel, the minimap shows a
 * scaled-down top-down view of the entire live game map. It reads directly from
 * the {@link GameMap} object every frame, so any change (road built, stop placed,
 * building added) is reflected immediately with no caching.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Terrain: grass, forest (darker green), road (grey-brown)</li>
 *   <li>Cities: blue-grey blocks with a subtle highlight border</li>
 *   <li>Industrial facilities: amber blocks</li>
 *   <li>Stops: bright red circles with white outline</li>
 *   <li>Vehicles: yellow dots with type-colour fill</li>
 *   <li>Viewport rectangle: animated white outline showing current camera view</li>
 *   <li>Legend: colour key at the bottom of the panel</li>
 *   <li>Click-to-navigate: clicking anywhere moves the world camera there</li>
 * </ul>
 *
 * <p>Spec: "The game map should be larger than the displayed area and should be
 * scrollable in both X and Y dimensions for navigation. For easier orientation,
 * a navigable minimap should be part of the game interface."</p>
 */
public class MinimapView {

    // ── Panel position – top-left corner, above the demand panel ─────────────
    /** X position of the minimap panel in UI space. */
    public static final float MINIMAP_X = 0f;
    /** Y position of the minimap panel in UI space. */
    public static final float MINIMAP_Y = 560f;   // top-left: 768 - 200 - 8
    /** Width of the minimap panel. */
    public static final float MINIMAP_W = 290f;
    /** Height of the minimap panel (map area + legend strip). */
    public static final float MINIMAP_H = 200f;

    // Internal layout
    private static final float PAD        = 5f;
    private static final float LEGEND_H   = 18f;   // legend strip at bottom
    private static final float HEADER_H   = 14f;   // title strip at top

    // Derived inner map-drawing area
    private static final float INNER_X = MINIMAP_X + PAD;
    private static final float INNER_Y = MINIMAP_Y + PAD + LEGEND_H;
    private static final float INNER_W = MINIMAP_W - PAD * 2f;
    private static final float INNER_H = MINIMAP_H - PAD * 2f - LEGEND_H - HEADER_H;

    // ── Terrain / feature colours ─────────────────────────────────────────────
    private static final Color COL_GRASS      = new Color(0.22f, 0.52f, 0.18f, 1f);
    private static final Color COL_GRASS_ALT  = new Color(0.20f, 0.48f, 0.16f, 1f); // checker variation
    private static final Color COL_FOREST     = new Color(0.08f, 0.32f, 0.08f, 1f);
    private static final Color COL_ROAD       = new Color(0.58f, 0.52f, 0.40f, 1f);
    private static final Color COL_ROAD_LINE  = new Color(0.72f, 0.66f, 0.52f, 1f); // centre-line hint
    private static final Color COL_CITY       = new Color(0.62f, 0.68f, 0.82f, 1f);
    private static final Color COL_CITY_HL    = new Color(0.80f, 0.85f, 1.00f, 1f); // city highlight
    private static final Color COL_FACILITY   = new Color(0.85f, 0.62f, 0.22f, 1f);
    private static final Color COL_FACILITY_HL= new Color(1.00f, 0.80f, 0.40f, 1f);
    private static final Color COL_STOP_OUTER = new Color(1.00f, 1.00f, 1.00f, 1f);
    private static final Color COL_STOP_INNER = new Color(0.95f, 0.15f, 0.15f, 1f);
    private static final Color COL_VEHICLE    = new Color(1.00f, 0.95f, 0.10f, 1f);
    private static final Color COL_VIEWPORT   = new Color(1.00f, 1.00f, 1.00f, 0.90f);
    private static final Color COL_BORDER     = new Color(0.45f, 0.65f, 1.00f, 0.95f);
    private static final Color COL_BG         = new Color(0.04f, 0.07f, 0.11f, 1.00f);
    private static final Color COL_HEADER_BG  = new Color(0.08f, 0.13f, 0.22f, 1.00f);
    private static final Color COL_TITLE      = new Color(0.70f, 0.88f, 1.00f, 1.00f);

    // Legend colours (match tile colours)
    private static final Color COL_LEG_ROAD  = COL_ROAD;
    private static final Color COL_LEG_CITY  = COL_CITY;
    private static final Color COL_LEG_FAC   = COL_FACILITY;
    private static final Color COL_LEG_STOP  = COL_STOP_INNER;
    private static final Color COL_LEG_VEH   = COL_VEHICLE;

    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch   spriteBatch;
    private final BitmapFont    labelFont;

    public MinimapView() {
        this(true);
    }

    /** Protected constructor — skips GL resource creation for unit tests. */
    protected MinimapView(boolean initGl) {
        if (initGl) {
            this.shapeRenderer = new ShapeRenderer();
            this.spriteBatch   = new SpriteBatch();
            this.labelFont     = new BitmapFont();
            this.labelFont.getData().setScale(0.52f);
        } else {
            this.shapeRenderer = null;
            this.spriteBatch   = null;
            this.labelFont     = null;
        }
    }

    /**
     * Renders the minimap. Called every frame from {@link GameView}.
     * Reads the live {@link GameMap} directly — all map changes are reflected immediately.
     *
     * @param uiCamera    UI orthographic camera
     * @param worldCamera main world camera (used to draw the viewport rect)
     * @param map         the live game map
     * @param vehicles    all vehicles currently in the game
     * @param stops       all active stops
     */
    public void render(OrthographicCamera uiCamera,
                       OrthographicCamera worldCamera,
                       GameMap map,
                       List<Vehicle> vehicles,
                       List<Stop> stops) {
        if (map == null) return;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        float tileW = INNER_W / map.getWidth();
        float tileH = INNER_H / map.getHeight();

        // ── Background ────────────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COL_BG);
        shapeRenderer.rect(MINIMAP_X, MINIMAP_Y, MINIMAP_W, MINIMAP_H);

        // Header bar
        shapeRenderer.setColor(COL_HEADER_BG);
        shapeRenderer.rect(MINIMAP_X, MINIMAP_Y + MINIMAP_H - HEADER_H, MINIMAP_W, HEADER_H);
        shapeRenderer.end();

        // ── Terrain tiles ─────────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;

                float px = INNER_X + x * tileW;
                float py = INNER_Y + y * tileH;
                float tw  = Math.max(1f, tileW);
                float th  = Math.max(1f, tileH);

                if (tile.getContent() instanceof City) {
                    // City: solid blue-grey fill
                    shapeRenderer.setColor(COL_CITY);
                    shapeRenderer.rect(px, py, tw, th);
                } else if (tile.getContent() instanceof IndustrialFacility) {
                    // Facility: amber fill
                    shapeRenderer.setColor(COL_FACILITY);
                    shapeRenderer.rect(px, py, tw, th);
                } else if (tile.hasRoad()) {
                    // Road: grey-brown base
                    shapeRenderer.setColor(COL_ROAD);
                    shapeRenderer.rect(px, py, tw, th);
                } else if (tile.hasTrees()) {
                    // Forest: dark green, density-tinted
                    float shade = 0.6f + tile.getTreeCount() * 0.1f;
                    shapeRenderer.setColor(
                        COL_FOREST.r * shade,
                        COL_FOREST.g * shade,
                        COL_FOREST.b * shade,
                        1f);
                    shapeRenderer.rect(px, py, tw, th);
                } else {
                    // Grass: subtle checkerboard for texture
                    Color g = ((x + y) % 2 == 0) ? COL_GRASS : COL_GRASS_ALT;
                    shapeRenderer.setColor(g);
                    shapeRenderer.rect(px, py, tw, th);
                }
            }
        }

        // ── Building highlight borders (1px inner border) ─────────────────────
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;
                float px = INNER_X + x * tileW;
                float py = INNER_Y + y * tileH;
                float tw  = Math.max(1f, tileW);
                float th  = Math.max(1f, tileH);

                if (tile.getContent() instanceof City) {
                    // Top-right highlight pixel
                    shapeRenderer.setColor(COL_CITY_HL);
                    shapeRenderer.rect(px + tw * 0.6f, py + th * 0.6f, tw * 0.35f, th * 0.35f);
                } else if (tile.getContent() instanceof IndustrialFacility) {
                    shapeRenderer.setColor(COL_FACILITY_HL);
                    shapeRenderer.rect(px + tw * 0.6f, py + th * 0.6f, tw * 0.35f, th * 0.35f);
                }
            }
        }

        // ── Road centre-line dots (only if tiles are large enough) ────────────
        if (tileW >= 3f) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    Tile tile = map.getTile(x, y);
                    if (tile == null || !tile.hasRoad()) continue;
                    float px = INNER_X + x * tileW + tileW / 2f;
                    float py = INNER_Y + y * tileH + tileH / 2f;
                    shapeRenderer.setColor(COL_ROAD_LINE);
                    shapeRenderer.rect(px - 0.5f, py - 0.5f, 1f, 1f);
                }
            }
        }

        // ── Stops ─────────────────────────────────────────────────────────────
        if (stops != null) {
            for (Stop stop : stops) {
                float px = INNER_X + stop.getX() * tileW + tileW / 2f;
                float py = INNER_Y + stop.getY() * tileH + tileH / 2f;
                float r  = Math.max(2.2f, tileW * 0.9f);
                // White outline
                shapeRenderer.setColor(COL_STOP_OUTER);
                shapeRenderer.circle(px, py, r + 1f, 8);
                // Red fill
                shapeRenderer.setColor(COL_STOP_INNER);
                shapeRenderer.circle(px, py, r, 8);
            }
        }

        // ── Vehicles ──────────────────────────────────────────────────────────
        if (vehicles != null) {
            for (Vehicle v : vehicles) {
                float px = INNER_X + v.getX() * tileW + tileW / 2f;
                float py = INNER_Y + v.getY() * tileH + tileH / 2f;
                float r  = Math.max(2f, tileW * 0.8f);
                // Dark outline
                shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
                shapeRenderer.circle(px, py, r + 1f, 6);
                // Type-coloured fill
                shapeRenderer.setColor(getVehicleMinimapColor(v));
                shapeRenderer.circle(px, py, r, 6);
            }
        }

        // ── Viewport: corner-bracket markers only (no fill, no solid rect) ─────
        float[] vp = getViewportMiniCoords(worldCamera, map);
        float vl = vp[0], vb = vp[1], vr = vp[2], vt = vp[3];
        float vpW = vr - vl;
        float vpH = vt - vb;
        // Corner arm length: 25% of the shorter side, capped at 8px
        float arm = Math.min(8f, Math.min(vpW, vpH) * 0.25f);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COL_VIEWPORT);

        // Bottom-left corner
        shapeRenderer.line(vl,       vb,       vl + arm, vb);
        shapeRenderer.line(vl,       vb,       vl,       vb + arm);
        // Bottom-right corner
        shapeRenderer.line(vr,       vb,       vr - arm, vb);
        shapeRenderer.line(vr,       vb,       vr,       vb + arm);
        // Top-left corner
        shapeRenderer.line(vl,       vt,       vl + arm, vt);
        shapeRenderer.line(vl,       vt,       vl,       vt - arm);
        // Top-right corner
        shapeRenderer.line(vr,       vt,       vr - arm, vt);
        shapeRenderer.line(vr,       vt,       vr,       vt - arm);

        // Thin centre crosshair dot at viewport centre
        float vcx = (vl + vr) / 2f;
        float vcy = (vb + vt) / 2f;
        shapeRenderer.line(vcx - 2f, vcy,      vcx + 2f, vcy);
        shapeRenderer.line(vcx,      vcy - 2f, vcx,      vcy + 2f);

        shapeRenderer.end();

        // ── Panel border ──────────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COL_BORDER);
        shapeRenderer.rect(MINIMAP_X, MINIMAP_Y, MINIMAP_W, MINIMAP_H);
        shapeRenderer.end();

        // ── Legend ────────────────────────────────────────────────────────────
        renderLegend(uiCamera);

        // ── Title ─────────────────────────────────────────────────────────────
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        labelFont.setColor(COL_TITLE);
        labelFont.draw(spriteBatch, "MINIMAP  [click to navigate]",
                MINIMAP_X + PAD, MINIMAP_Y + MINIMAP_H - 3f);
        spriteBatch.end();
    }

    /** Renders the colour legend strip at the bottom of the panel. */
    private void renderLegend(OrthographicCamera uiCamera) {
        float ly   = MINIMAP_Y + PAD;
        float lh   = LEGEND_H - 2f;
        float dotR = 3.5f;
        float step = MINIMAP_W / 5f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Road swatch
        shapeRenderer.setColor(COL_LEG_ROAD);
        shapeRenderer.rect(MINIMAP_X + step * 0f + PAD, ly + lh / 2f - 3f, 8f, 6f);

        // City swatch
        shapeRenderer.setColor(COL_LEG_CITY);
        shapeRenderer.rect(MINIMAP_X + step * 1f + PAD, ly + lh / 2f - 3f, 8f, 6f);

        // Facility swatch
        shapeRenderer.setColor(COL_LEG_FAC);
        shapeRenderer.rect(MINIMAP_X + step * 2f + PAD, ly + lh / 2f - 3f, 8f, 6f);

        // Stop dot
        shapeRenderer.setColor(COL_STOP_OUTER);
        shapeRenderer.circle(MINIMAP_X + step * 3f + PAD + dotR, ly + lh / 2f, dotR + 1f, 8);
        shapeRenderer.setColor(COL_LEG_STOP);
        shapeRenderer.circle(MINIMAP_X + step * 3f + PAD + dotR, ly + lh / 2f, dotR, 8);

        // Vehicle dot
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.circle(MINIMAP_X + step * 4f + PAD + dotR, ly + lh / 2f, dotR + 1f, 6);
        shapeRenderer.setColor(COL_LEG_VEH);
        shapeRenderer.circle(MINIMAP_X + step * 4f + PAD + dotR, ly + lh / 2f, dotR, 6);

        shapeRenderer.end();

        // Legend labels
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        labelFont.setColor(new Color(0.75f, 0.75f, 0.75f, 1f));
        labelFont.draw(spriteBatch, "Road",  MINIMAP_X + step * 0f + PAD + 10f, ly + lh - 1f);
        labelFont.draw(spriteBatch, "City",  MINIMAP_X + step * 1f + PAD + 10f, ly + lh - 1f);
        labelFont.draw(spriteBatch, "Fac",   MINIMAP_X + step * 2f + PAD + 10f, ly + lh - 1f);
        labelFont.draw(spriteBatch, "Stop",  MINIMAP_X + step * 3f + PAD + 10f, ly + lh - 1f);
        labelFont.draw(spriteBatch, "Veh",   MINIMAP_X + step * 4f + PAD + 10f, ly + lh - 1f);
        spriteBatch.end();
    }

    /** Returns the vehicle colour by type for the minimap dot. */
    private Color getVehicleMinimapColor(Vehicle v) {
        switch (v.getType()) {
            case BUS:        return new Color(1.0f, 1.0f, 0.1f, 1f); // yellow
            case MINIBUS:    return new Color(0.9f, 0.9f, 0.3f, 1f); // pale yellow
            case TRUCK:      return new Color(1.0f, 0.5f, 0.1f, 1f); // orange
            case CARGO_TRUCK:return new Color(0.8f, 0.3f, 0.1f, 1f); // dark orange
            default:         return COL_VEHICLE;
        }
    }

    /**
     * Computes the minimap pixel coordinates of the world camera viewport.
     * @return float[4] = { left, bottom, right, top } in minimap UI coords
     */
    private float[] getViewportMiniCoords(OrthographicCamera worldCamera, GameMap map) {
        float worldMapW = map.getWidth()  * GameConstants.TILE_SIZE;
        float worldMapH = map.getHeight() * GameConstants.TILE_SIZE;

        float vpHalfW = worldCamera.viewportWidth  * worldCamera.zoom / 2f;
        float vpHalfH = worldCamera.viewportHeight * worldCamera.zoom / 2f;

        float vpLeft   = worldCamera.position.x - vpHalfW;
        float vpBottom = worldCamera.position.y - vpHalfH;
        float vpRight  = worldCamera.position.x + vpHalfW;
        float vpTop    = worldCamera.position.y + vpHalfH;

        float mmLeft   = INNER_X + (vpLeft   / worldMapW) * INNER_W;
        float mmBottom = INNER_Y + (vpBottom / worldMapH) * INNER_H;
        float mmRight  = INNER_X + (vpRight  / worldMapW) * INNER_W;
        float mmTop    = INNER_Y + (vpTop    / worldMapH) * INNER_H;

        // Clamp to inner area
        mmLeft   = Math.max(INNER_X,          mmLeft);
        mmBottom = Math.max(INNER_Y,          mmBottom);
        mmRight  = Math.min(INNER_X + INNER_W, mmRight);
        mmTop    = Math.min(INNER_Y + INNER_H, mmTop);

        return new float[]{ mmLeft, mmBottom, mmRight, mmTop };
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Returns true if the given UI-space point is inside the minimap panel.
     */
    public boolean contains(float uiX, float uiY) {
        return uiX >= MINIMAP_X && uiX <= MINIMAP_X + MINIMAP_W
            && uiY >= MINIMAP_Y && uiY <= MINIMAP_Y + MINIMAP_H;
    }

    /**
     * Converts a minimap click to world coordinates and repositions the camera.
     * Only acts if the click is within the inner map area (not legend/header).
     */
    public void handleClick(float uiX, float uiY,
                            OrthographicCamera worldCamera,
                            GameMap map) {
        // Only navigate when clicking the actual map area, not legend/header
        if (uiX < INNER_X || uiX > INNER_X + INNER_W
         || uiY < INNER_Y || uiY > INNER_Y + INNER_H) return;

        float normX = Math.max(0f, Math.min(1f, (uiX - INNER_X) / INNER_W));
        float normY = Math.max(0f, Math.min(1f, (uiY - INNER_Y) / INNER_H));

        float worldMapW = map.getWidth()  * GameConstants.TILE_SIZE;
        float worldMapH = map.getHeight() * GameConstants.TILE_SIZE;

        float targetX = normX * worldMapW;
        float targetY = normY * worldMapH;

        // Clamp so camera doesn't show outside the map
        float halfW = worldCamera.viewportWidth  * worldCamera.zoom / 2f;
        float halfH = worldCamera.viewportHeight * worldCamera.zoom / 2f;

        targetX = Math.max(halfW, Math.min(worldMapW - halfW, targetX));
        targetY = Math.max(halfH, Math.min(worldMapH - halfH, targetY));

        worldCamera.position.set(targetX, targetY, 0);
        worldCamera.update();
    }

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (spriteBatch   != null) spriteBatch.dispose();
        if (labelFont     != null) labelFont.dispose();
    }
}
