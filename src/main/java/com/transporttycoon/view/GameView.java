package com.transporttycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.transporttycoon.GameConstants;
import com.transporttycoon.model.Game;
import com.transporttycoon.model.GameMap;

/**
 * Main game view.
 *
 * Layout (left → right, fills the entire physical screen with NO letterboxing):
 *   ┌────────────┬──────────────────────────────┬────────────┐
 *   │ LEFT 290px │   MAP — fills remaining px   │ RIGHT 305px│
 *   │  Minimap   │   (ExtendViewport)           │  HUD/ctrl  │
 *   │  Demand &  │                              │  Fleet     │
 *   │  Supply    │                              │  Buttons   │
 *   └────────────┴──────────────────────────────┴────────────┘
 *
 * Key design decisions
 * ────────────────────
 * • UI uses ScreenViewport (1 unit = 1 physical pixel) → no black bars ever.
 * • Map uses ExtendViewport pinned between the two columns each frame.
 * • Fonts are generated via FreeTypeFontGenerator at proper pixel sizes.
 *   Three tiers:
 *     fontTitle  20 px – panel section headers
 *     fontBody   15 px – HUD values, city names
 *     fontSmall  12 px – dense data rows, transactions, controls hint
 *     fontButton 14 px – button labels
 */
public class GameView {

    // Side-column widths in PHYSICAL pixels (fixed, independent of resolution)
    private static final int LEFT_COL_W  = 290;
    private static final int RIGHT_COL_W = 290;

    // ── Rendering infrastructure ──────────────────────────────────────────────
    private OrthographicCamera worldCamera;
    private OrthographicCamera uiCamera;
    private ScreenViewport     worldViewport;
    private ScreenViewport     uiViewport;

    private SpriteBatch   spriteBatch;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout   glyphLayout;

    // Fonts
    private BitmapFont fontTitle;
    private BitmapFont fontBody;
    private BitmapFont fontSmall;
    private BitmapFont fontButton;

    private AssetManager assetManager;
    private Object       controller;

    private MapView     mapView;
    private MinimapView minimapView;

    // ── UIButton ──────────────────────────────────────────────────────────────
    public static class UIButton {
        public String label;
        public float x, y, width, height;
        public boolean hovered;

        public UIButton(String label, float x, float y, float w, float h) {
            this.label = label; this.x = x; this.y = y; this.width = w; this.height = h;
        }
        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    private java.util.List<UIButton> actionButtons;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GameView() {
        worldCamera = new OrthographicCamera();
        uiCamera    = new OrthographicCamera();

        float mapWorldW = GameConstants.MAP_WIDTH  * GameConstants.TILE_SIZE;
        float mapWorldH = GameConstants.MAP_HEIGHT * GameConstants.TILE_SIZE;
        // Keep world units in pixels; this avoids shrinking the full map into a tiny viewport.
        worldViewport = new ScreenViewport(worldCamera);
        uiViewport    = new ScreenViewport(uiCamera);

        spriteBatch   = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout   = new GlyphLayout();

        assetManager = new AssetManager();
        assetManager.loadAssets();

        initFonts();

        mapView     = new MapView(worldCamera, assetManager);
        minimapView = new MinimapView();

        worldCamera.position.set(mapWorldW / 2f, mapWorldH / 2f, 0);
        worldCamera.update();

        // Buttons with placeholder positions; repositioned each frame in renderButtons()
        actionButtons = new java.util.ArrayList<>();
        String[] labels = {
            "Build Road", "Build Garage",
            "Buy Bus",    "Buy Minibus",
            "Buy Truck",  "Buy C.Truck",
            "Place Stop", "Traffic Light",
            "Assign Route","Sell Old",
            "Save Game",  "Load Game"
        };
        for (String label : labels) actionButtons.add(new UIButton(label, 0, 0, 0, 0));
    }

    // ── Font initialisation ───────────────────────────────────────────────────
    private void initFonts() {
        com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("fonts/ui_font.ttf");
        if (fh.exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fh);
            fontTitle  = buildFont(gen, 20);
            fontBody   = buildFont(gen, 15);
            fontSmall  = buildFont(gen, 12);
            fontButton = buildFont(gen, 14);
            gen.dispose();
        } else {
            // Fallback: scale the built-in bitmap font
            fontTitle  = scaledDefault(1.40f);
            fontBody   = scaledDefault(1.00f);
            fontSmall  = scaledDefault(0.80f);
            fontButton = scaledDefault(0.92f);
        }
    }

    private BitmapFont buildFont(FreeTypeFontGenerator g, int size) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size          = size;
        p.borderWidth   = 0.6f;
        p.borderColor   = new Color(0f, 0f, 0f, 0.55f);
        p.shadowOffsetX = 1;
        p.shadowOffsetY = -1;
        p.shadowColor   = new Color(0f, 0f, 0f, 0.45f);
        p.minFilter     = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        p.magFilter     = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        return g.generateFont(p);
    }

    private BitmapFont scaledDefault(float scale) {
        BitmapFont f = new BitmapFont();
        f.getData().setScale(scale);
        return f;
    }

    // ── Main render ───────────────────────────────────────────────────────────
    public void render(Game game) {
        if (game == null) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Pin map viewport between the two side columns — fills 100 % height
        int mapX = LEFT_COL_W;
        int mapW = Math.max(1, sw - LEFT_COL_W - RIGHT_COL_W);
        worldViewport.setScreenBounds(mapX, 0, mapW, sh);
        worldViewport.apply();
        worldCamera.update();

        GameMap map = game.getMap();
        if (map != null) {
            mapView.renderMapWithTrafficLights(map, game.getTrafficLightManager());
            spriteBatch.setProjectionMatrix(worldCamera.combined);
            mapView.renderStopsAndVehicles(game.getVehicles(), getAllStopsFromRoutes(game));
        }

        renderUI(game, sw, sh);
    }

    // ── Full UI layer ─────────────────────────────────────────────────────────
    private void renderUI(Game game, int sw, int sh) {
        uiViewport.update(sw, sh, true);
        uiViewport.apply();
        uiCamera.update();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        spriteBatch.begin();

        // Side column backgrounds
        drawSolidRect(0, 0, LEFT_COL_W, sh, new Color(0.038f, 0.065f, 0.105f, 1f));
        drawSolidRect(sw - RIGHT_COL_W, 0, RIGHT_COL_W, sh, new Color(0.038f, 0.065f, 0.105f, 1f));

        // Divider lines
        drawSolidRect(LEFT_COL_W - 1, 0, 2, sh, new Color(0.22f, 0.32f, 0.50f, 1f));
        drawSolidRect(sw - RIGHT_COL_W - 1, 0, 2, sh, new Color(0.22f, 0.32f, 0.50f, 1f));

        renderRightSidebar(game, sw, sh);
        renderLeftColumn(game, sw, sh);
        renderRouteAssignmentInstructions(sw, sh);

        if (game.checkBankruptcy()) {
            spriteBatch.end();
            renderBankruptcyOverlay(sw, sh);
            spriteBatch.begin();
        }

        spriteBatch.end();

        minimapView.render(uiCamera, worldCamera, game.getMap(),
                game.getVehicles(), getAllStopsFromRoutes(game));

        spriteBatch.begin();
        spriteBatch.end();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIGHT SIDEBAR  (HUD stats + fleet + buttons + controls hint)
    // ─────────────────────────────────────────────────────────────────────────
    private void renderRightSidebar(Game game, int sw, int sh) {
        float rx = sw - RIGHT_COL_W + 12f;   // content left edge

        // ── Header ──────────────────────────────────────────────────────────
        fontTitle.setColor(new Color(0.58f, 0.82f, 1f, 1f));
        fontTitle.draw(spriteBatch, "CONTROL CENTER", rx, sh - 10f);

        // Thin separator under header
        drawSolidRect(sw - RIGHT_COL_W + 6, sh - 36, RIGHT_COL_W - 12, 1,
                new Color(0.30f, 0.44f, 0.62f, 0.7f));

        float y = sh - 46f;

        // ── Financial data ───────────────────────────────────────────────────
        fontBody.setColor(Color.GOLD);
        fontBody.draw(spriteBatch, "$ " + game.getPlayer().getFormattedCapital(), rx, y);
        y -= 22f;

        fontBody.setColor(getTimeSpeedColor(game.getTimeSpeed().name()));
        fontBody.draw(spriteBatch, "Speed: " + game.getTimeSpeed().name(), rx, y);
        y -= 22f;

        fontBody.setColor(Color.CYAN);
        fontBody.draw(spriteBatch, String.format("Day %.1f", game.getGameTime() / 60f), rx, y);
        y -= 22f;

        double mp = game.getPlayer().getMonthlyProfit();
        if (mp >= 0) {
            fontBody.setColor(new Color(0.28f, 1f, 0.28f, 1f));
            fontBody.draw(spriteBatch, String.format("Profit: +$%.0f", mp), rx, y);
        } else {
            fontBody.setColor(new Color(1f, 0.28f, 0.28f, 1f));
            fontBody.draw(spriteBatch, String.format("Loss: -$%.0f", Math.abs(mp)), rx, y);
        }
        y -= 26f;

        // ── Recent transactions ──────────────────────────────────────────────
        drawSolidRect(sw - RIGHT_COL_W + 6, y + 14, RIGHT_COL_W - 12, 1,
                new Color(0.22f, 0.32f, 0.44f, 0.55f));
        y -= 4f;

        fontSmall.setColor(new Color(0.60f, 0.60f, 0.60f, 1f));
        fontSmall.draw(spriteBatch, "RECENT TRANSACTIONS", rx, y);
        y -= 15f;

        for (com.transporttycoon.model.Player.Transaction tx
                : game.getPlayer().getRecentTransactions(3)) {
            String desc = tx.description.length() > 23
                    ? tx.description.substring(0, 20) + "..." : tx.description;
            boolean income = tx.type == com.transporttycoon.model.Player.TransactionType.INCOME;
            fontSmall.setColor(income ? new Color(0.25f, 0.90f, 0.25f, 0.95f)
                                      : new Color(0.92f, 0.35f, 0.35f, 0.95f));
            fontSmall.draw(spriteBatch,
                    String.format("%s$%.0f %s", income ? "+" : "-", tx.amount, desc),
                    rx + 4, y);
            y -= 14f;
        }
        y -= 8f;

        // ── Fleet status ─────────────────────────────────────────────────────
        drawSolidRect(sw - RIGHT_COL_W + 6, y + 12, RIGHT_COL_W - 12, 1,
                new Color(0.22f, 0.32f, 0.44f, 0.55f));
        y -= 4f;

        fontSmall.setColor(new Color(0.52f, 0.80f, 1f, 0.88f));
        fontSmall.draw(spriteBatch, "FLEET STATUS", rx, y);
        y -= 15f;

        java.util.List<com.transporttycoon.model.Vehicle> vehicles = game.getVehicles();
        if (vehicles.isEmpty()) {
            fontSmall.setColor(new Color(0.48f, 0.48f, 0.48f, 0.9f));
            fontSmall.draw(spriteBatch, "No vehicles purchased yet.", rx + 4, y);
            y -= 14f;
        } else {
            // Compute button section top so fleet rows don't overlap buttons
            float btnTop = computeButtonSectionTop(sh);
            int maxRows  = Math.max(0, (int)((y - btnTop - 8) / 15));

            for (int i = 0; i < Math.min(vehicles.size(), maxRows); i++) {
                com.transporttycoon.model.Vehicle v = vehicles.get(i);

                // Coloured state dot
                spriteBatch.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(getFleetDotColor(v));
                shapeRenderer.circle(rx + 5, y - 4, 5);
                shapeRenderer.end();
                spriteBatch.begin();

                String st  = v.isWaiting() ? "WAIT" : v.isMoving() ? "MOVE"
                           : v.isAtStop()  ? "STOP" : "IDLE";
                String tp  = v.getType().name().replace("_", "");
                String cap = v.getCurrentCapacity() + "/" + v.getType().getCapacity();
                String tgt = "";
                com.transporttycoon.model.Stop ts = v.getTargetStop();
                if (ts != null) tgt = "\u2192(" + ts.getX() + "," + ts.getY() + ")";

                String maint = "";
                if (v.hasHomeGarage()) {
                    float age = v.getEffectiveVehicleAgeYears(game.getGameYear());
                    int itv = Math.max(GameConstants.VEHICLE_MIN_MAINTENANCE_INTERVAL_TICKS,
                            (int)(GameConstants.VEHICLE_BASE_MAINTENANCE_INTERVAL_TICKS
                                    - age * GameConstants.VEHICLE_MAINTENANCE_INTERVAL_AGE_PENALTY_TICKS));
                    float itvD  = (itv / 60f) / 60f;
                    float elapD = v.getMaintenanceElapsedSeconds() / 60f;
                    String ms   = v.isUnderGarageMaintenance() ? "SRV"
                            : v.isMaintenanceRequired() ? "DUE" : "OK";
                    maint = String.format(" M:%s %.0f/%.0fd", ms, elapD, itvD);
                }

                fontSmall.setColor(
                    "MOVE".equals(st) ? new Color(0.28f, 1f,   0.38f, 1f) :
                    "WAIT".equals(st) ? new Color(1f,   0.56f, 0.10f, 1f) :
                    "STOP".equals(st) ? new Color(1f,   0.88f, 0.16f, 1f) :
                                        new Color(0.62f, 0.62f, 0.62f, 1f));
                fontSmall.draw(spriteBatch,
                        String.format("#%d %s %s %s %s%s",
                                v.getId(), tp, st, cap, tgt, maint),
                        rx + 14, y);
                y -= 15f;
            }
            if (vehicles.size() > maxRows) {
                fontSmall.setColor(new Color(0.52f, 0.52f, 0.52f, 0.8f));
                fontSmall.draw(spriteBatch, "+" + (vehicles.size() - maxRows) + " more vehicles",
                        rx + 4, y);
            }
        }

        // ── Buttons ──────────────────────────────────────────────────────────
        renderButtons(sw, sh);

        // ── Controls hint (very bottom) ───────────────────────────────────────
        float cy = 56f;
        drawSolidRect(sw - RIGHT_COL_W + 6, cy + 14, RIGHT_COL_W - 12, 1,
                new Color(0.22f, 0.32f, 0.44f, 0.45f));
        fontSmall.setColor(new Color(0.48f, 0.48f, 0.48f, 0.9f));
        fontSmall.draw(spriteBatch, "WASD: move   Wheel: zoom", rx, cy);       cy -= 14f;
        fontSmall.draw(spriteBatch, "Space: pause/resume",      rx, cy);       cy -= 14f;
        fontSmall.draw(spriteBatch, "T: speed   G: toggle grid",rx, cy);
    }

    /**
     * Returns the Y coordinate of the top of the button section (bottom of fleet area).
     * 6 rows × (btnH + rowGap + catLabelH) + bottom controls height + padding.
     */
    private float computeButtonSectionTop(int sh) {
        float controlsH = 14f * 3 + 20f;   // 3 control lines + separator + gap
        float btnH = 36f, rowGap = 5f, catH = 18f;
        float sixRows = 6 * (btnH + rowGap + catH);
        return controlsH + sixRows + 8f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BUTTONS  (6 rows × 2 cols, built bottom-up)
    // ─────────────────────────────────────────────────────────────────────────
    private void renderButtons(int sw, int sh) {
        float rx   = sw - RIGHT_COL_W + 10f;
        float colW = (RIGHT_COL_W - 28f) / 2f;   // two equal columns, 8px gap
        float btnH = 36f;
        float rowGap = 5f;
        float catH   = 18f;
        float step   = btnH + rowGap + catH;      // total height per row slot

        // Anchor bottom of button stack just above controls summary
        float baseY = 56f + 14f * 3 + 18f;

        // Row slots (bottom to top):  slot 0 = Save/Load … slot 5 = Road/Garage
        String[] cats = {
            "PERSISTENCE", "ROUTE MANAGEMENT", "CARGO VEHICLES",
            "PASSENGER VEHICLES", "INFRASTRUCTURE", ""
        };
        // Button pairs in same order as actionButtons list (top-down in constructor)
        // We reverse to map them bottom-up:
        int[] slotMap = {4, 3, 2, 1, 5, 0}; // pair index → slot index
        //  pair 0 (Build Road/Garage)  → slot 4
        //  pair 1 (Bus/Minibus)        → slot 3
        //  pair 2 (Truck/CTruck)       → slot 2
        //  pair 3 (Stop/Light)         → slot 1
        //  pair 4 (Assign/Sell)        → slot 5
        //  pair 5 (Save/Load)          → slot 0

        float[] slotY = new float[6];
        for (int i = 0; i < 6; i++) slotY[i] = baseY + i * step;

        // Update button bounds
        for (int pair = 0; pair < 6; pair++) {
            int slot = slotMap[pair];
            UIButton bl = actionButtons.get(pair * 2);
            UIButton br = actionButtons.get(pair * 2 + 1);
            float by = slotY[slot];
            bl.x = rx;            bl.y = by; bl.width = colW; bl.height = btnH;
            br.x = rx + colW + 8; br.y = by; br.width = colW; br.height = btnH;
        }

        spriteBatch.end();

        // Draw category header strips
        for (int slot = 0; slot < 6; slot++) {
            if (cats[slot].isEmpty()) continue;
            float stripY = slotY[slot] + btnH + rowGap;
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0.07f, 0.11f, 0.18f, 1f));
            shapeRenderer.rect(rx - 2, stripY, RIGHT_COL_W - 16, catH - 2);
            shapeRenderer.end();
        }

        for (UIButton btn : actionButtons) drawButton(btn);

        spriteBatch.begin();

        // Category labels
        fontSmall.setColor(new Color(0.52f, 0.80f, 1f, 0.80f));
        for (int slot = 0; slot < 6; slot++) {
            if (!cats[slot].isEmpty()) {
                fontSmall.draw(spriteBatch, cats[slot],
                        rx, slotY[slot] + btnH + rowGap + catH - 3);
            }
        }
        // leave spriteBatch OPEN
    }

    private void drawButton(UIButton btn) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(btn.hovered
                ? new Color(0.28f, 0.55f, 0.92f, 1f)
                : new Color(0.08f, 0.15f, 0.26f, 1f));
        shapeRenderer.rect(btn.x, btn.y, btn.width, btn.height);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(btn.hovered
                ? new Color(0.55f, 0.80f, 1f, 1f)
                : new Color(0.28f, 0.45f, 0.70f, 0.85f));
        shapeRenderer.rect(btn.x, btn.y, btn.width, btn.height);
        shapeRenderer.end();

        spriteBatch.begin();
        // Shadow text
        fontButton.setColor(new Color(0f, 0f, 0f, 0.70f));
        fontButton.draw(spriteBatch, btn.label, btn.x + 8, btn.y + btn.height - 9);
        // Lit text
        fontButton.setColor(btn.hovered ? Color.YELLOW : Color.WHITE);
        fontButton.draw(spriteBatch, btn.label, btn.x + 7, btn.y + btn.height - 8);
        spriteBatch.end();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEFT COLUMN  (minimap header + demand/supply panel)
    // ─────────────────────────────────────────────────────────────────────────
    private void renderLeftColumn(Game game, int sw, int sh) {
        float mmX = MinimapView.MINIMAP_X;
        float mmY = MinimapView.MINIMAP_Y;
        float mmW = MinimapView.MINIMAP_W;
        float mmH = MinimapView.MINIMAP_H;
        float gap = 8f;

        // "MINIMAP" label at very top
        fontSmall.setColor(new Color(0.48f, 0.76f, 1f, 0.88f));
        fontSmall.draw(spriteBatch, "MINIMAP", mmX + 10f, mmY + mmH - 3f);

        // Frame for the minimap area
        drawOutlineRect(mmX + 4f, mmY, mmW - 8f, mmH,
                new Color(0.26f, 0.38f, 0.56f, 0.78f));

        // Demand & Supply panel below minimap
        renderDemandPanel(game, mmY - gap, sh);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEMAND & SUPPLY PANEL
    // ─────────────────────────────────────────────────────────────────────────
    private void renderDemandPanel(Game game, float topY, int sh) {
        float panelBot = 6f;
        float panelH   = topY - panelBot;

        // Panel background
        drawSolidRect(4, panelBot, LEFT_COL_W - 8, panelH, new Color(0.035f, 0.060f, 0.095f, 0.97f));
        drawOutlineRect(4, panelBot, LEFT_COL_W - 8, panelH, new Color(0.24f, 0.36f, 0.52f, 0.75f));

        float y = topY - 18f;

        fontTitle.setColor(new Color(0.58f, 0.82f, 1f, 1f));
        fontTitle.draw(spriteBatch, "DEMAND & SUPPLY", 12, y);
        y -= 24f;

        // ── Cities ──────────────────────────────────────────────────────────
        fontSmall.setColor(new Color(1f, 0.84f, 0.28f, 1f));
        fontSmall.draw(spriteBatch, "\u2550\u2550\u2550  CITIES  \u2550\u2550\u2550", 12, y);
        y -= 17f;

        java.util.List<com.transporttycoon.model.City> cities = new java.util.ArrayList<>();
        for (int cx = 0; cx < game.getMap().getWidth(); cx++) {
            for (int cy2 = 0; cy2 < game.getMap().getHeight(); cy2++) {
                com.transporttycoon.model.Tile tile = game.getMap().getTile(cx, cy2);
                if (tile != null && tile.getContent() instanceof com.transporttycoon.model.City) {
                    com.transporttycoon.model.City city = (com.transporttycoon.model.City) tile.getContent();
                    if (!cities.contains(city)) cities.add(city);
                }
            }
        }

        for (com.transporttycoon.model.City city : cities) {
            if (y < panelBot + 28) break;

            // City name + population
            fontBody.setColor(Color.WHITE);
            String cn = city.getName();
            if (cn.length() > 15) cn = cn.substring(0, 12) + "...";
            fontBody.draw(spriteBatch,
                    cn + "  (" + formatPopulation(city.getPopulation()) + ")", 14, y);
            y -= 18f;

            // Happiness bar
            float hap    = city.getCityHappiness();
            Color hapCol = hap > 0.6f ? new Color(0.16f, 0.82f, 0.16f, 1f)
                         : hap > 0.3f ? new Color(0.90f, 0.68f, 0.08f, 1f)
                         :              new Color(0.88f, 0.16f, 0.16f, 1f);
            fontSmall.setColor(new Color(0.68f, 0.68f, 0.68f, 1f));
            fontSmall.draw(spriteBatch, "Happy:", 18, y + 1);
            drawMiniBar(70, y - 7, LEFT_COL_W - 88, hap, hapCol);
            fontSmall.setColor(hapCol);
            fontSmall.draw(spriteBatch, String.format("%.0f%%", hap * 100),
                    LEFT_COL_W - 14 - measureText(fontSmall, "100%"), y + 1);
            y -= 14f;

            // Passenger demand row
            int pd = city.getPassengersDelivered(), pm = city.getPassengerDemandMonthly();
            float ps = pm > 0 ? (float) pd / pm : 0f;
            Color pc = demandColor(ps);
            fontSmall.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
            fontSmall.draw(spriteBatch, "Pass:", 18, y);
            fontSmall.setColor(pc);
            fontSmall.draw(spriteBatch,
                    String.format("%s %d/%d",
                            com.transporttycoon.model.CargoType.PASSENGER.getIcon(), pd, pm),
                    56, y);
            drawMiniBar(130, y - 7, LEFT_COL_W - 144, ps, pc);
            y -= 14f;

            // Cargo demand rows
            for (com.transporttycoon.model.CargoType cargo : city.getDemands()) {
                if (y < panelBot + 18) break;
                int cd = city.getCargoDelivered(cargo), cm = city.getCargoDemandMonthly(cargo);
                float cs = cm > 0 ? (float) cd / cm : 0f;
                Color cc = demandColor(cs);
                fontSmall.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
                fontSmall.draw(spriteBatch,
                        cargo.name().substring(0, Math.min(4, cargo.name().length())) + ":", 18, y);
                fontSmall.setColor(cc);
                fontSmall.draw(spriteBatch,
                        String.format("%s %d/%d", cargo.getIcon(), cd, cm), 56, y);
                drawMiniBar(130, y - 7, LEFT_COL_W - 144, cs, cc);
                y -= 14f;
            }
            y -= 8f;
        }

        // ── Facilities ───────────────────────────────────────────────────────
        if (y < panelBot + 45) return;

        y -= 4f;
        fontSmall.setColor(new Color(0.88f, 0.64f, 0.28f, 1f));
        fontSmall.draw(spriteBatch, "\u2550\u2550\u2550  FACILITIES  \u2550\u2550\u2550", 12, y);
        y -= 17f;

        java.util.List<com.transporttycoon.model.IndustrialFacility> facs =
                new java.util.ArrayList<>();
        for (int fx = 0; fx < game.getMap().getWidth(); fx++) {
            for (int fy = 0; fy < game.getMap().getHeight(); fy++) {
                com.transporttycoon.model.Tile tile = game.getMap().getTile(fx, fy);
                if (tile != null && tile.getContent() instanceof com.transporttycoon.model.IndustrialFacility) {
                    com.transporttycoon.model.IndustrialFacility fac =
                            (com.transporttycoon.model.IndustrialFacility) tile.getContent();
                    if (fac.getFacilityType() ==
                            com.transporttycoon.model.IndustrialFacility.FacilityType.GARAGE) continue;
                    if (!facs.contains(fac)) facs.add(fac);
                }
            }
        }

        int shown = 0;
        for (com.transporttycoon.model.IndustrialFacility fac : facs) {
            if (y < panelBot + 18 || shown >= 5) break;

            fontBody.setColor(Color.WHITE);
            String fn = fac.getName();
            if (fn.length() > 18) fn = fn.substring(0, 15) + "...";
            fontBody.draw(spriteBatch, fn, 14, y);
            y -= 16f;

            int   stk  = fac.getCurrentStock();
            float stkR = stk / 100f;
            Color stkC = stkR > 0.7f ? new Color(0.9f, 0.26f, 0.26f, 1f)
                       : stkR > 0.3f ? new Color(0.9f, 0.76f, 0.16f, 1f)
                       :               new Color(0.26f, 0.90f, 0.26f, 1f);
            fontSmall.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
            fontSmall.draw(spriteBatch, "Stock:", 18, y);
            fontSmall.setColor(stkC);
            fontSmall.draw(spriteBatch,
                    String.format("%s %d/100", fac.getProduces().getIcon(), stk), 56, y);
            drawMiniBar(130, y - 7, LEFT_COL_W - 144, stkR, stkC);
            y -= 14f;

            if (fac.getConsumes() != null) {
                int   cs  = fac.getConsumptionStock(), cn = fac.getConsumptionNeeded();
                float csR = (float) cs / Math.max(1, cn * 5);
                Color csC = csR > 0.6f ? new Color(0.26f, 0.90f, 0.26f, 1f)
                          : csR > 0.2f ? new Color(0.9f, 0.76f, 0.16f, 1f)
                          :              new Color(0.9f, 0.26f, 0.26f, 1f);
                fontSmall.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
                fontSmall.draw(spriteBatch, "Input:", 18, y);
                fontSmall.setColor(csC);
                fontSmall.draw(spriteBatch,
                        String.format("%s %d", fac.getConsumes().getIcon(), cs), 56, y);
                drawMiniBar(130, y - 7, LEFT_COL_W - 144, Math.min(1f, csR), csC);
                y -= 14f;
            }
            y -= 7f;
            shown++;
        }

        if (facs.size() > shown) {
            fontSmall.setColor(new Color(0.52f, 0.52f, 0.52f, 0.85f));
            fontSmall.draw(spriteBatch, "+" + (facs.size() - shown) + " more", 14, y);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTE ASSIGNMENT OVERLAY
    // ─────────────────────────────────────────────────────────────────────────
    private void renderRouteAssignmentInstructions(int sw, int sh) {
        if (controller == null) return;
        try {
            Object state = controller.getClass().getMethod("getCurrentActionState").invoke(controller);
            if (!"ASSIGN_ROUTE".equals(state)) return;

            float px = LEFT_COL_W + 12, py = sh - 110, pw = sw - LEFT_COL_W - RIGHT_COL_W - 24, ph = 98;
            spriteBatch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0.06f, 0.11f, 0.18f, 0.96f));
            shapeRenderer.rect(px, py, pw, ph);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(new Color(0.36f, 0.62f, 1f, 0.82f));
            shapeRenderer.rect(px, py, pw, ph);
            shapeRenderer.end();
            spriteBatch.begin();
            fontBody.setColor(new Color(1f, 1f, 0.70f, 1f));
            fontBody.draw(spriteBatch,
                    "ROUTE ASSIGNMENT \u2013 Click vehicle, then click stops in order (min 2)",
                    px + 12, py + ph - 10);
            fontBody.setColor(new Color(0.52f, 1f, 0.52f, 1f));
            fontBody.draw(spriteBatch,
                    "Press R or 'Assign Route' again to finalise circular route",
                    px + 12, py + ph - 34);
            fontBody.setColor(new Color(1f, 0.48f, 0.48f, 1f));
            fontBody.draw(spriteBatch, "Press ESC or 'Stop Route' to cancel",
                    px + 12, py + ph - 58);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BANKRUPTCY OVERLAY
    // ─────────────────────────────────────────────────────────────────────────
    private void renderBankruptcyOverlay(int sw, int sh) {
        uiViewport.apply();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.82f));
        shapeRenderer.rect(0, 0, sw, sh);
        shapeRenderer.end();

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        fontTitle.setColor(Color.RED);
        fontTitle.draw(spriteBatch, "BANKRUPT! Game Over", sw / 2f - 135, sh / 2f + 18);
        fontBody.setColor(Color.LIGHT_GRAY);
        fontBody.draw(spriteBatch, "Close the window to exit.", sw / 2f - 105, sh / 2f - 14);
        spriteBatch.end();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DRAWING HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    /** Draws a filled rectangle without toggling spriteBatch. */
    private void drawSolidRect(float x, float y, float w, float h, Color c) {
        spriteBatch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(c);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.end();
        spriteBatch.begin();
    }

    /** Draws an outlined (hollow) rectangle. */
    private void drawOutlineRect(float x, float y, float w, float h, Color c) {
        spriteBatch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(c);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.end();
        spriteBatch.begin();
    }

    /** Draws a horizontal progress bar; spriteBatch is OPEN on entry & return. */
    private void drawMiniBar(float x, float y, float w, float fill, Color col) {
        spriteBatch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.16f, 0.16f, 0.16f, 0.78f));
        shapeRenderer.rect(x, y, w, 7);
        shapeRenderer.setColor(col);
        shapeRenderer.rect(x, y, w * Math.min(1f, Math.max(0f, fill)), 7);
        shapeRenderer.end();
        spriteBatch.begin();
    }

    private Color demandColor(float sat) {
        return sat >= 0.8f ? new Color(0.26f, 0.90f, 0.26f, 1f)
             : sat >= 0.4f ? new Color(0.90f, 0.76f, 0.16f, 1f)
             :               new Color(0.90f, 0.26f, 0.26f, 1f);
    }

    private Color getFleetDotColor(com.transporttycoon.model.Vehicle v) {
        if (v.isWaiting()) return new Color(1.0f, 0.50f, 0.05f, 1f);
        if (v.isMoving())  return new Color(0.10f, 0.85f, 0.20f, 1f);
        if (v.isAtStop())  return new Color(1.0f, 0.88f, 0.10f, 1f);
        return new Color(0.50f, 0.50f, 0.50f, 0.9f);
    }

    private Color getTimeSpeedColor(String s) {
        switch (s) {
            case "PAUSE":     return Color.RED;
            case "NORMAL":    return Color.GREEN;
            case "FAST":      return Color.YELLOW;
            case "VERY_FAST": return Color.ORANGE;
            default:          return Color.WHITE;
        }
    }

    private String formatPopulation(int p) {
        if (p >= 1_000_000) return String.format("%.1fM", p / 1_000_000.0);
        if (p >= 1_000)     return String.format("%.0fK", p / 1_000.0);
        return String.valueOf(p);
    }

    private float measureText(BitmapFont font, String text) {
        glyphLayout.setText(font, text);
        return glyphLayout.width;
    }

    private java.util.List<com.transporttycoon.model.Stop> getAllStopsFromRoutes(Game game) {
        java.util.List<com.transporttycoon.model.Stop> all = new java.util.ArrayList<>();
        all.addAll(game.getStopManager().getActiveStops());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (com.transporttycoon.model.Stop s : all) seen.add(s.getX() + "," + s.getY());
        for (com.transporttycoon.model.Route r : game.getRoutes())
            for (com.transporttycoon.model.Stop s : r.getStops()) {
                String k = s.getX() + "," + s.getY();
                if (!seen.contains(k)) { all.add(s); seen.add(k); }
            }
        return all;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────
    public void updateButtonHovers(float mx, float my) {
        for (UIButton b : actionButtons) b.hovered = b.contains(mx, my);
    }

    public UIButton getButtonAtPosition(float mx, float my) {
        for (UIButton b : actionButtons) if (b.contains(mx, my)) return b;
        return null;
    }

    public void resize(int w, int h) {
        uiViewport.update(w, h, true);
        worldViewport.update(w, h, false);
    }

    public OrthographicCamera getCamera()        { return worldCamera; }
    public MinimapView        getMinimapView()   { return minimapView; }
    public MapView            getMapView()       { return mapView; }
    public Viewport           getViewport()      { return uiViewport; }
    public Viewport           getWorldViewport() { return worldViewport; }
    public void               setController(Object c) { this.controller = c; }

    public void dispose() {
        if (spriteBatch   != null) spriteBatch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (mapView       != null) mapView.dispose();
        if (minimapView   != null) minimapView.dispose();
        if (assetManager  != null) assetManager.dispose();
        disposeFont(fontTitle);
        disposeFont(fontBody);
        disposeFont(fontSmall);
        disposeFont(fontButton);
    }

    private void disposeFont(BitmapFont f) {
        try { if (f != null) f.dispose(); } catch (Exception ignored) {}
    }
}