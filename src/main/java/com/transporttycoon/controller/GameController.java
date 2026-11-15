package com.transporttycoon.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.transporttycoon.GameConstants;
import com.transporttycoon.model.Game;
import com.transporttycoon.model.IndustrialFacility;
import com.transporttycoon.model.Player;
import com.transporttycoon.model.Route;
import com.transporttycoon.model.Stop;
import com.transporttycoon.model.Tile;
import com.transporttycoon.model.TimeSpeed;
import com.transporttycoon.model.Vehicle;
import com.transporttycoon.model.VehicleType;
import com.transporttycoon.view.GameView;
import com.transporttycoon.persistence.DatabaseManager;
import com.transporttycoon.persistence.PersistenceManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Main game controller handling input and game flow
 */
public class GameController implements InputProcessor {
    private static final float CAMERA_SPEED = 300f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final int TILE_SIZE = 32;
    
    // Game action state machine
    private enum GameAction {
        NONE, BUILD_ROAD, PLACE_STOP, BUILD_GARAGE, BUY_VEHICLE, ASSIGN_ROUTE, STOP_ROUTE, PLACE_TRAFFIC_LIGHT
    }
    
    private Game game;
    private GameView gameView;
    private OrthographicCamera camera;
    /** Back-reference to the top-level application so that a loaded game can
     *  be propagated to the render loop.  Set via {@link #setApp}. */
    private com.transporttycoon.TransportTycoonGame app;
    
    // Input state
    private boolean wPressed, aPressed, sPressed, dPressed;
    
    // Game action state
    private GameAction currentAction = GameAction.NONE;
    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private Stop selectedStopForVehicle = null;
    private Vehicle selectedVehicleForRoute = null;
    private Route currentRouteBeingBuilt = null;
    private VehicleType selectedVehicleType = null;
    private List<Stop> selectedStopsForRoute = new ArrayList<>();
    private int windowedWidth;
    private int windowedHeight;
    
    public GameController(Game game, GameView gameView) {
        this.game = game;
        this.gameView = gameView;
        this.camera = gameView.getCamera();
        this.windowedWidth = Gdx.graphics.getWidth();
        this.windowedHeight = Gdx.graphics.getHeight();

        // Set up input processing
        Gdx.input.setInputProcessor(this);
    }

    /**
     * Registers the top-level application so that {@link #handleLoadGame()} can
     * push the newly loaded {@link Game} back into the render loop.
     */
    public void setApp(com.transporttycoon.TransportTycoonGame app) {
        this.app = app;
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
            return;
        }

        windowedWidth = Gdx.graphics.getWidth();
        windowedHeight = Gdx.graphics.getHeight();
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        if (displayMode != null) {
            Gdx.graphics.setFullscreenMode(displayMode);
        }
    }
    
    /**
     * Updates the controller (called every frame)
     */
    public void update(float deltaTime) {
        handleCameraMovement(deltaTime);
    }
    
    /**
     * Handles camera movement based on input, clamped to map bounds.
     */
    private void handleCameraMovement(float deltaTime) {
        float moveSpeed = CAMERA_SPEED * deltaTime * camera.zoom;

        if (wPressed) camera.position.y += moveSpeed;
        if (sPressed) camera.position.y -= moveSpeed;
        if (dPressed) camera.position.x += moveSpeed;
        if (aPressed) camera.position.x -= moveSpeed;

        // Clamp camera so it never scrolls outside the actual map bounds.
        // Use the live map dimensions rather than GameConstants.MAP_WIDTH/HEIGHT,
        // which are not guaranteed to match the map that was created at runtime.
        float mapPixelW = game.getMap().getWidth()  * TILE_SIZE;
        float mapPixelH = game.getMap().getHeight() * TILE_SIZE;
        float halfW = camera.viewportWidth  * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        // If the viewport is larger than the world on an axis, keep camera centered on that axis.
        if (halfW >= mapPixelW / 2f) {
            camera.position.x = mapPixelW / 2f;
        } else {
            camera.position.x = Math.max(halfW, Math.min(mapPixelW - halfW, camera.position.x));
        }

        if (halfH >= mapPixelH / 2f) {
            camera.position.y = mapPixelH / 2f;
        } else {
            camera.position.y = Math.max(halfH, Math.min(mapPixelH - halfH, camera.position.y));
        }

        camera.update();
    }
    
    /**
     * Purchases a vehicle of the given type via the VehicleManager and
     * optionally assigns it to an existing route.
     *
     * @param type  vehicle type to buy
     * @param route route to assign (may be {@code null})
     * @return the purchased vehicle, or {@code null} if funds are insufficient
     */
    public Vehicle buyVehicle(VehicleType type, Route route) {
        Vehicle vehicle = game.getVehicleManager().purchaseVehicle(type, game.getGameYear());
        if (vehicle != null && route != null) {
            game.getVehicleManager().assignRoute(vehicle, route);
        }
        return vehicle;
    }

    /**
     * Creates a new route via the RouteManager with two stops and returns it.
     *
     * @param name the route name
     * @param from origin stop
     * @param to   destination stop
     * @return the new route
     */
    public Route createRoute(String name, Stop from, Stop to) {
        Route route = game.getRouteManager().createRoute(name);
        game.getRouteManager().addStopToRoute(route, from);
        game.getRouteManager().addStopToRoute(route, to);
        return route;
    }

    /**
     * Returns the current game action state (used by GameView for UI display)
     */
    public String getCurrentActionState() {
        return currentAction.toString();
    }

    /**
     * Returns the active game instance. This may change after loading a save.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Cycles through time speeds
     */
    private void cycleTimeSpeed() {
        TimeSpeed currentSpeed = game.getTimeSpeed();
        TimeSpeed nextSpeed;
        
        switch (currentSpeed) {
            case PAUSE:
                nextSpeed = TimeSpeed.NORMAL;
                break;
            case NORMAL:
                nextSpeed = TimeSpeed.FAST;
                break;
            case FAST:
                nextSpeed = TimeSpeed.VERY_FAST;
                break;
            case VERY_FAST:
                nextSpeed = TimeSpeed.PAUSE;
                break;
            default:
                nextSpeed = TimeSpeed.NORMAL;
        }
        
        game.setTimeSpeed(nextSpeed);
    }
    
    // InputProcessor implementation
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F11 ||
            (keycode == Input.Keys.ENTER &&
             (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) ||
              Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)))) {
            toggleFullscreen();
            return true;
        }

        switch (keycode) {
            case Input.Keys.W:
                wPressed = true;
                return true;
            case Input.Keys.A:
                aPressed = true;
                return true;
            case Input.Keys.S:
                sPressed = true;
                return true;
            case Input.Keys.D:
                dPressed = true;
                return true;
            case Input.Keys.SPACE:
                if (game.getTimeSpeed() == TimeSpeed.PAUSE) {
                    game.setTimeSpeed(TimeSpeed.NORMAL);
                } else {
                    game.setTimeSpeed(TimeSpeed.PAUSE);
                }
                return true;
            case Input.Keys.G:
                gameView.getMapView().setShowGrid(!gameView.getMapView().isShowGrid());
                return true;
            case Input.Keys.T:
                cycleTimeSpeed();
                return true;
            case Input.Keys.ESCAPE:
                Gdx.app.exit();
                return true;
            case Input.Keys.R:
                startAssignRoute();
                return true;
        }
        return false;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
                wPressed = false;
                return true;
            case Input.Keys.A:
                aPressed = false;
                return true;
            case Input.Keys.S:
                sPressed = false;
                return true;
            case Input.Keys.D:
                dPressed = false;
                return true;
        }
        return false;
    }
    
    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            try {
                // Convert screen coordinates to viewport coordinates
                Vector3 viewportCoords = new Vector3(screenX, screenY, 0);
                gameView.getViewport().unproject(viewportCoords);

                // Check minimap click first – navigates the world camera
                if (gameView.getMinimapView().contains(viewportCoords.x, viewportCoords.y)) {
                    gameView.getMinimapView().handleClick(
                            viewportCoords.x, viewportCoords.y,
                            camera, game.getMap());
                    return true;
                }

                // First check if a button was clicked
                GameView.UIButton clickedButton = gameView.getButtonAtPosition(viewportCoords.x, viewportCoords.y);
                if (clickedButton != null) {
                    handleButtonClick(clickedButton);
                    return true;
                }
                
                // Otherwise handle map tile click
                Vector3 worldCoords = new Vector3(screenX, screenY, 0);
                gameView.getWorldViewport().unproject(worldCoords);
                int tileX = (int) (worldCoords.x / TILE_SIZE);
                int tileY = (int) (worldCoords.y / TILE_SIZE);
                Vector3 mapCoords = new Vector3(tileX, tileY, 0);
                if (mapCoords != null) {
                    handleMapClick((int) mapCoords.x, (int) mapCoords.y);
                }
            } catch (Exception e) {
                System.err.println("Error in touchDown: " + e.getMessage());
            }
        }
        return true;
    }
    
    /**
     * Handles button clicks from the UI
     */
    private void handleButtonClick(GameView.UIButton button) {
        switch (button.label) {
            case "Buy Bus":
                startBuyVehicle(VehicleType.BUS);
                break;
            case "Buy Minibus":
                startBuyVehicle(VehicleType.MINIBUS);
                break;
            case "Buy Truck":
                startBuyVehicle(VehicleType.TRUCK);
                break;
            case "Buy C.Truck":
                startBuyVehicle(VehicleType.CARGO_TRUCK);
                break;
            case "Build Road":
                startBuildRoad();
                break;
            case "Place Stop":
                startPlaceStop();
                break;
            case "Build Garage":
                startBuildGarage();
                break;
            case "Traffic Light":
                startPlaceTrafficLight();
                break;
            case "Sell Old":
                sellOverAgedVehicles();
                break;
            case "Save Game":
                handleSaveGame();
                break;
            case "Load Game":
                handleLoadGame();
                break;
            case "Assign Route":
                startAssignRoute();
                break;
            case "Stop Route":
                stopSelectedRoute();
                break;
        }
    }
    
    /**
     * Handles map tile clicks based on current game action
     */
    private void handleMapClick(int tileX, int tileY) {
        if (game.getMap() == null) return;
        
        Tile tile = game.getMap().getTile(tileX, tileY);
        if (tile == null) return;
        
        // Update selected tile in MapView for detailed info display
        gameView.getMapView().setSelectedTile(tileX, tileY);
        
        switch (currentAction) {
            case BUILD_ROAD:
                handleBuildRoadClick(tileX, tileY, tile);
                break;
            case PLACE_STOP:
                handlePlaceStopClick(tileX, tileY, tile);
                break;
            case BUILD_GARAGE:
                handleBuildGarageClick(tileX, tileY, tile);
                break;
            case BUY_VEHICLE:
                handleBuyVehicleClick(tileX, tileY, tile);
                break;
            case ASSIGN_ROUTE:
                handleAssignRouteClick(tileX, tileY, tile);
                break;
            case STOP_ROUTE:
                handleStopRouteClick(tileX, tileY, tile);
                break;
            case PLACE_TRAFFIC_LIGHT:
                handlePlaceTrafficLightClick(tileX, tileY, tile);
                break;
            default:
                // Show tile information
                if (!tile.isEmpty()) {
                    System.out.println("Selected: " + tile.getContent().toString());
                } else {
                    System.out.println("Clicked tile: (" + tileX + ", " + tileY + ") - Empty");
                }
        }
    }
    
    /**
     * Initiates road building mode
     */
    private void startBuildRoad() {
        if (currentAction == GameAction.BUILD_ROAD) {
            // Cancel road building
            currentAction = GameAction.NONE;
            selectedTileX = -1;
            selectedTileY = -1;
            System.out.println("Road building cancelled.");
        } else {
            currentAction = GameAction.BUILD_ROAD;
            selectedTileX = -1;
            selectedTileY = -1;
            System.out.println("Road building mode: Click first tile.");
        }
    }
    
    /**
     * Handles clicks during road building
     */
    private void handleBuildRoadClick(int tileX, int tileY, Tile tile) {
        try {
            if (selectedTileX == -1) {
                // First tile selection
                selectedTileX = tileX;
                selectedTileY = tileY;
                System.out.println("First road tile selected: (" + tileX + ", " + tileY + ")");
                System.out.println("Road building mode: Click second tile to build.");
            } else {
                // Second tile selection - build road
                int x1 = selectedTileX;
                int y1 = selectedTileY;
                int x2 = tileX;
                int y2 = tileY;
                
                if (buildRoadBetween(x1, y1, x2, y2)) {
                    System.out.println("Road built from (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
                } else {
                    System.out.println("Failed to build road. Check tile availability and funds.");
                }
                
                currentAction = GameAction.NONE;
                selectedTileX = -1;
                selectedTileY = -1;
            }
        } catch (Exception e) {
            System.err.println("Error in road building: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Builds a road between two tiles (simple pathfinding - straight line only for now)
     */
    private boolean buildRoadBetween(int x1, int y1, int x2, int y2) {
        try {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            
            // Only allow horizontal or vertical roads for simplicity
            if (dx > 0 && dy > 0) {
                System.out.println("Roads must be placed horizontally or vertically.");
                return false;
            }
            
            int roadLength = dx + dy;
            if (roadLength == 0) {
                System.out.println("Must select two different tiles.");
                return false;
            }

            int startX = Math.min(x1, x2);
            int endX = Math.max(x1, x2);
            int startY = Math.min(y1, y2);
            int endY = Math.max(y1, y2);

            int newRoadTiles = 0;
            int treesToClear = 0;
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    Tile currentTile = game.getMap().getTile(x, y);
                    if (currentTile == null) {
                        System.out.println("Road path goes out of bounds.");
                        return false;
                    }
                    if (currentTile.hasBuilding()) {
                        System.out.println("Cannot build road on a building at (" + x + ", " + y + ")");
                        return false;
                    }
                    if (!currentTile.hasRoad()) {
                        newRoadTiles++;
                        treesToClear += currentTile.getTreeCount();
                    }
                }
            }

            if (newRoadTiles == 0) {
                System.out.println("All selected tiles already have roads.");
                return true;
            }

            double roadCost = (newRoadTiles * GameConstants.ROAD_COST_PER_TILE)
                    + (treesToClear * GameConstants.FOREST_CLEARING_COST_PER_TREE);

            if (game.getPlayer().getCapital() < roadCost) {
                System.out.println("Insufficient funds to build " + newRoadTiles + " road tiles. Cost: $" + roadCost);
                return false;
            }
            
            // Build the road
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    Tile currentTile = game.getMap().getTile(x, y);
                    if (currentTile != null && currentTile.hasRoad()) {
                        continue;
                    }
                    if (!game.getMap().buildRoad(x, y)) {
                        System.out.println("Could not build road at (" + x + ", " + y + ")");
                        return false;
                    }
                }
            }
            
            // Deduct cost
            game.getPlayer().recordTransaction(
                "Road construction",
                roadCost,
                Player.TransactionType.EXPENSE
            );

            if (treesToClear > 0) {
                System.out.println("Clearing surcharge applied for " + treesToClear + " trees.");
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error building road: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initiates stop placement mode
     */
    private void startPlaceStop() {
        if (currentAction == GameAction.PLACE_STOP) {
            // Cancel stop placement
            currentAction = GameAction.NONE;
            System.out.println("Stop placement cancelled.");
        } else {
            currentAction = GameAction.PLACE_STOP;
            System.out.println("Stop placement mode: Click on a road tile.");
        }
    }

    private void startBuildGarage() {
        if (currentAction == GameAction.BUILD_GARAGE) {
            currentAction = GameAction.NONE;
            System.out.println("Garage building cancelled.");
        } else {
            currentAction = GameAction.BUILD_GARAGE;
            System.out.println("Garage building mode: Click top-left tile of a 2x2 garage next to a road.");
        }
    }

    private void handleBuildGarageClick(int tileX, int tileY, Tile tile) {
        try {
            IndustrialFacility garage = IndustrialFacility.createGarage("Garage-" + System.nanoTime());

            if (!game.getMap().hasAdjacentRoad(tileX, tileY, garage.getWidth(), garage.getHeight())) {
                System.out.println("Garage must be adjacent to an existing road.");
                currentAction = GameAction.NONE;
                return;
            }

            if (game.getPlayer().getCapital() < GameConstants.GARAGE_BUILD_COST) {
                System.out.println("Insufficient funds to build garage. Cost: $" + GameConstants.GARAGE_BUILD_COST);
                currentAction = GameAction.NONE;
                return;
            }

            if (!game.getMap().placeFacility(garage, tileX, tileY)) {
                System.out.println("Cannot place garage at that location. Ensure a free 2x2 area.");
                currentAction = GameAction.NONE;
                return;
            }

            game.getPlayer().recordTransaction(
                "Garage construction",
                GameConstants.GARAGE_BUILD_COST,
                Player.TransactionType.EXPENSE
            );
            System.out.println("Garage built at (" + tileX + ", " + tileY + ").");
        } catch (Exception e) {
            System.err.println("Error building garage: " + e.getMessage());
        }

        currentAction = GameAction.NONE;
    }
    
    /**
     * Handles clicks during stop placement
     */
    private void handlePlaceStopClick(int tileX, int tileY, Tile tile) {
        try {
            // Must place on a road
            if (!tile.hasRoad()) {
                System.out.println("Stops must be placed on roads. Use Build Road first.");
                return;
            }

            if (!isNearCityOrFacility(tileX, tileY, GameConstants.STOP_FACILITY_SEARCH_RADIUS)) {
                System.out.println("Stops must be near a city or industrial facility.");
                return;
            }

            // Prevent duplicate stops on the same tile
            for (Stop existingStop : game.getStopManager().getAllStops()) {
                if (existingStop.getX() == tileX && existingStop.getY() == tileY && existingStop.isActive()) {
                    System.out.println("A stop already exists at (" + tileX + ", " + tileY + ")");
                    return;
                }
            }
            
            // Check cost
            int stopCost = GameConstants.STOP_COST;
            if (game.getPlayer().getCapital() < stopCost) {
                System.out.println("Insufficient funds to place stop. Cost: $" + stopCost);
                return;
            }
            
            Stop newStop = game.getStopManager().placeStop("Stop-" + tileX + "-" + tileY, tileX, tileY, stopCost);
            if (newStop != null) {
                System.out.println("Stop placed at (" + tileX + ", " + tileY + ")");
            } else {
                System.out.println("Failed to place stop at (" + tileX + ", " + tileY + ")");
            }
        } catch (Exception e) {
            System.err.println("Error placing stop: " + e.getMessage());
        }
        
        currentAction = GameAction.NONE;
    }
    
    /**
     * Initiates vehicle purchase mode
     */
    private void startBuyVehicle(VehicleType vehicleType) {
        if (vehicleType == null) {
            System.err.println("Vehicle type is null!");
            return;
        }
        
        double cost = vehicleType.getCost();
        
        // Check if player can afford it
        if (game.getPlayer().getCapital() < cost) {
            System.out.println("Insufficient funds to buy " + vehicleType.getDisplayName() + ". Cost: $" + cost);
            return;
        }
        
        // Store vehicle type for use in handler
        selectedVehicleType = vehicleType;
        currentAction = GameAction.BUY_VEHICLE;
        System.out.println("Vehicle purchase mode: Click on a STOP to buy " + vehicleType.getDisplayName() + " (or click on a garage).");
    }

    private boolean isNearCityOrFacility(int tileX, int tileY, int radius) {
        for (int x = tileX - radius; x <= tileX + radius; x++) {
            for (int y = tileY - radius; y <= tileY + radius; y++) {
                Tile nearbyTile = game.getMap().getTile(x, y);
                if (nearbyTile != null && nearbyTile.hasBuilding()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handles clicks during vehicle purchase
     */
    private void handleBuyVehicleClick(int tileX, int tileY, Tile tile) {
        if (selectedVehicleType == null) {
            System.err.println("No vehicle type selected!");
            currentAction = GameAction.NONE;
            return;
        }

        // First, try to find a stop at this location
        Stop stop = findActiveStopAtTile(tileX, tileY);
        if (stop != null) {
            // Buy vehicle at stop (simpler method)
            if (game.buyVehicle(selectedVehicleType, stop)) {
                System.out.println("Vehicle purchased at stop: " + stop.getName());
                currentAction = GameAction.NONE;
                selectedVehicleType = null;
                return;
            } else {
                System.out.println("Failed to purchase vehicle.");
                currentAction = GameAction.NONE;
                selectedVehicleType = null;
                return;
            }
        }

        // If no stop, try garage method
        if (!(tile.getContent() instanceof IndustrialFacility)) {
            System.out.println("Vehicles must be purchased at a stop or from a garage tile.");
            currentAction = GameAction.NONE;
            selectedVehicleType = null;
            return;
        }

        IndustrialFacility facility = (IndustrialFacility) tile.getContent();
        if (facility.getFacilityType() != IndustrialFacility.FacilityType.GARAGE) {
            System.out.println("Selected building is not a garage. Try clicking on a stop instead.");
            currentAction = GameAction.NONE;
            selectedVehicleType = null;
            return;
        }

        int[] garageRoad = game.getMap().findAdjacentRoad(facility);
        if (garageRoad == null) {
            System.out.println("Garage is not connected to the road network.");
            currentAction = GameAction.NONE;
            selectedVehicleType = null;
            return;
        }
        
        try {
            if (game.buyVehicleFromGarage(selectedVehicleType, facility, garageRoad[0], garageRoad[1])) {
                System.out.println("Vehicle purchased from garage at (" + tileX + ", " + tileY + ")");
            } else {
                System.out.println("Failed to purchase vehicle.");
            }
        } catch (Exception e) {
            System.err.println("Error purchasing vehicle: " + e.getMessage());
        }
        
        selectedVehicleType = null;
        currentAction = GameAction.NONE;
    }

    private void sellOverAgedVehicles() {
        int soldCount = game.sellOverAgedVehicles();
        if (soldCount > 0) {
            System.out.println("Sold " + soldCount + " over-aged vehicles.");
        } else {
            System.out.println("No over-aged vehicles available for sale.");
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Save counter so each quick-save gets a unique default slot name. */
    private int saveCounter = 1;

    private void handleSaveGame() {
        try {
            if (!DatabaseManager.isInitialised()) {
                DatabaseManager.initialise();
            }
            String slotName = "Slot " + saveCounter++;
            PersistenceManager.save(game, slotName);
            System.out.println("[Save] Game saved to: " + slotName);

            // Print all available saves
            System.out.println("[Save] Available saves:");
            for (PersistenceManager.SaveSlotInfo info : PersistenceManager.listSaves()) {
                System.out.println("  " + info);
            }
        } catch (Exception e) {
            System.err.println("[Save] Failed: " + e.getMessage());
        }
    }

    private void handleLoadGame() {
        try {
            if (!DatabaseManager.isInitialised()) {
                DatabaseManager.initialise();
            }
            java.util.List<PersistenceManager.SaveSlotInfo> saves = PersistenceManager.listSaves();
            if (saves.isEmpty()) {
                System.out.println("[Load] No saves found.");
                return;
            }
            // Load the most recent save
            String slotName = saves.get(0).slotName;
            com.transporttycoon.model.Game loaded = PersistenceManager.load(slotName);
            if (loaded != null) {
                game = loaded;
                loaded.start();
                game.getVehicleManager().setMapView(gameView.getMapView());
                // Propagate the new Game instance to the render loop so that
                // TransportTycoonGame.render() updates and renders the loaded state
                // rather than the stale pre-load instance.
                if (app != null) {
                    app.setGame(loaded);
                }
                System.out.println("[Load] Loaded: " + slotName);
            }
        } catch (Exception e) {
            System.err.println("[Load] Failed: " + e.getMessage());
        }
    }
    
    /**
     * Initiates route assignment mode
     */
    private void startAssignRoute() {
        if (currentAction != GameAction.ASSIGN_ROUTE) {
            currentAction = GameAction.ASSIGN_ROUTE;
            selectedVehicleForRoute = null;
            selectedStopsForRoute.clear();
            System.out.println("Route mode: Click a vehicle first, then click stops in order. Press R again to finalize.");
            return;
        }

        if (selectedVehicleForRoute != null && selectedStopsForRoute.size() >= 2) {
            finalizeRouteAssignment();
        } else {
            System.out.println("Route assignment cancelled.");
        }

        currentAction = GameAction.NONE;
        selectedVehicleForRoute = null;
        selectedStopsForRoute.clear();
    }
    
    /**
     * Handles clicks during route assignment
     */
    private void handleAssignRouteClick(int tileX, int tileY, Tile tile) {
        try {
            if (selectedVehicleForRoute == null) {
                Vehicle vehicle = findVehicleAtTile(tileX, tileY);
                if (vehicle == null) {
                    System.out.println("Click on a vehicle tile to select a vehicle first.");
                    return;
                }
                selectedVehicleForRoute = vehicle;
                System.out.println("Selected " + vehicle.getName() + ". Now click stops in order (minimum 2). Press R to finalize.");
                return;
            }

            Stop stop = findActiveStopAtTile(tileX, tileY);
            if (stop == null) {
                System.out.println("Click on an existing stop tile to add it to route.");
                return;
            }

            if (selectedStopsForRoute.contains(stop)) {
                System.out.println("Stop '" + stop.getName() + "' is already in this route draft.");
                return;
            }

            selectedStopsForRoute.add(stop);
            System.out.println("Added stop " + stop.getName() + " (" + selectedStopsForRoute.size() + " selected)");

            if (selectedStopsForRoute.size() >= 2) {
                System.out.println("Route draft is valid. Press R to finalize circular route.");
            }
        } catch (Exception e) {
            System.err.println("Error assigning route: " + e.getMessage());
        }
    }

    private Vehicle findVehicleAtTile(int tileX, int tileY) {
        for (Vehicle vehicle : game.getVehicles()) {
            int vx = Math.round(vehicle.getX());
            int vy = Math.round(vehicle.getY());
            if (vx == tileX && vy == tileY) {
                return vehicle;
            }
        }
        return null;
    }

    private Stop findActiveStopAtTile(int tileX, int tileY) {
        for (Stop stop : game.getStopManager().getActiveStops()) {
            if (stop.getX() == tileX && stop.getY() == tileY) {
                return stop;
            }
        }
        return null;
    }

    private void finalizeRouteAssignment() {
        if (selectedVehicleForRoute == null || selectedStopsForRoute.size() < 2) {
            System.out.println("Need one vehicle and at least two stops to create a route.");
            return;
        }

        try {
            String routeName = "Route-" + (game.getRouteManager().getRouteCount() + 1);
            Route route = game.getRouteManager().createRoute(routeName, selectedStopsForRoute);
            game.assignRoute(selectedVehicleForRoute, route);
            System.out.println("Assigned circular route " + routeName + " with " + selectedStopsForRoute.size() + " stops to " + selectedVehicleForRoute.getName());
            System.out.println("Vehicle will repeat: A → B → ... → A");
        } catch (Exception e) {
            System.err.println("Failed to finalize route: " + e.getMessage());
        }
    }

    /**
     * Initiates route stopping mode
     */
    private void stopSelectedRoute() {
        if (currentAction != GameAction.STOP_ROUTE) {
            currentAction = GameAction.STOP_ROUTE;
            System.out.println("Stop Route mode: Click on a vehicle to stop its route.");
            return;
        }

        // Already in STOP_ROUTE mode; cancel it
        currentAction = GameAction.NONE;
        System.out.println("Stop Route mode cancelled.");
    }

    /**
     * Handles clicks during stop route mode
     */
    private void handleStopRouteClick(int tileX, int tileY, Tile tile) {
        try {
            Vehicle vehicle = findVehicleAtTile(tileX, tileY);
            if (vehicle == null) {
                System.out.println("Click on a vehicle tile to stop its route.");
                return;
            }

            // Clear the vehicle's route
            vehicle.setAssignedRoute(null);
            System.out.println("Cleared route from " + vehicle.getName() + ". Vehicle stopped.");

            // Exit stop route mode
            currentAction = GameAction.NONE;
        } catch (Exception e) {
            System.err.println("Error stopping route: " + e.getMessage());
        }
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        Vector3 viewportCoords = new Vector3(screenX, screenY, 0);
        gameView.getViewport().unproject(viewportCoords);
        gameView.updateButtonHovers(viewportCoords.x, viewportCoords.y);
        return false;
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Zoom in/out
        float newZoom = camera.zoom + amountY * ZOOM_SPEED;
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        return true;
    }
    
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    
    /**
     * Initiates traffic light placement mode
     */
    private void startPlaceTrafficLight() {
        if (currentAction == GameAction.PLACE_TRAFFIC_LIGHT) {
            currentAction = GameAction.NONE;
            System.out.println("Cancelled traffic light placement");
        } else {
            currentAction = GameAction.PLACE_TRAFFIC_LIGHT;
            System.out.println("Click on a 3-way or 4-way intersection to place a traffic light");
        }
    }
    
    /**
     * Handles traffic light placement click
     */
    private void handlePlaceTrafficLightClick(int tileX, int tileY, Tile tile) {
        if (!tile.hasRoad()) {
            System.out.println("Cannot place traffic light: no road at this location");
            return;
        }
        
        boolean success = game.getTrafficLightManager().placeTrafficLight(tileX, tileY);
        
        if (success) {
            System.out.println("Traffic light placed at (" + tileX + ", " + tileY + ")");
            currentAction = GameAction.NONE;
        }
    }
}
