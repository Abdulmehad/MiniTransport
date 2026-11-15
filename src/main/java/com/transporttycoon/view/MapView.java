package com.transporttycoon.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.transporttycoon.model.GameMap;
import com.transporttycoon.model.Tile;
import com.transporttycoon.model.City;
import com.transporttycoon.model.CargoType;
import com.transporttycoon.model.IndustrialFacility;
import com.transporttycoon.model.TileContent;
import com.transporttycoon.model.TrafficLight;
import com.transporttycoon.model.TrafficLightManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders the game map using LibGDX with enhanced visual assets
 * Falls back to colored shapes if assets are missing
 */
public class MapView {
    private static final int TILE_SIZE = 32;
    private static final Color GRID_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.2f);
    private static final Color SELECTION_COLOR = new Color(1.0f, 1.0f, 0.0f, 0.5f);
    
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private OrthographicCamera camera;
    private AssetManager assetManager;
    private BitmapFont labelFont;
    private boolean showGrid;
    private Set<TileContent> renderedBuildings;
    private Set<TileContent> renderedLabels;
    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private java.util.List<DeliveryNotification> notifications = new java.util.ArrayList<>();
    
    public MapView(OrthographicCamera camera, AssetManager assetManager) {
        this.camera = camera;
        this.assetManager = assetManager;
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        this.labelFont = new BitmapFont();
        this.labelFont.setColor(Color.WHITE);
        this.labelFont.getData().setScale(0.9f);
        this.showGrid = true;
        this.renderedBuildings = new HashSet<>();
        this.renderedLabels = new HashSet<>();
    }
    
    /**
     * Renders the game map with enhanced visuals including vehicles and stops
     */
    public void renderMap(GameMap map) {
        if (map == null) return;
        
        // Clear the set of rendered buildings for this frame
        renderedBuildings.clear();
        renderedLabels.clear();
        
        // Render terrain tiles using textures
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        renderTerrainTiles(map);
        spriteBatch.end();

        // Ensure roads are visually readable even when road textures are subtle.
        renderRoadHighlights(map);
        
        // Render buildings (cities and facilities) as larger sprites
        spriteBatch.begin();
        renderBuildings(map);
        spriteBatch.end();
        
        // Render building labels
        spriteBatch.begin();
        renderLabels(map);
        spriteBatch.end();
        
        // Render delivery notifications
        spriteBatch.begin();
        renderNotifications();
        spriteBatch.end();
        
        // Render grid lines if enabled
        if (showGrid) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            renderGrid(map);
            shapeRenderer.end();
        }
    }

    private void renderRoadHighlights(GameMap map) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null || !tile.hasRoad()) {
                    continue;
                }

                float sx = x * TILE_SIZE;
                float sy = y * TILE_SIZE;

                // Asphalt tint layer for clear contrast against grass.
                shapeRenderer.setColor(new Color(0.17f, 0.20f, 0.24f, 0.55f));
                shapeRenderer.rect(sx + 2f, sy + 2f, TILE_SIZE - 4f, TILE_SIZE - 4f);

                // Center lane marker for readability while zoomed out.
                shapeRenderer.setColor(new Color(0.95f, 0.92f, 0.55f, 0.75f));
                shapeRenderer.rect(sx + 14f, sy + 8f, 4f, TILE_SIZE - 16f);
            }
        }
        shapeRenderer.end();
    }
    
    /**
     * Renders the map with traffic lights
     */
    public void renderMapWithTrafficLights(GameMap map, TrafficLightManager trafficLightManager) {
        renderMap(map);
        
        // Render traffic lights on top
        if (trafficLightManager != null) {
            renderTrafficLights(trafficLightManager);
        }
    }
    
    /**
     * Renders floating delivery notifications
     */
    private void renderNotifications() {
        java.util.Iterator<DeliveryNotification> iter = notifications.iterator();
        while (iter.hasNext()) {
            DeliveryNotification notif = iter.next();
            notif.update(com.badlogic.gdx.Gdx.graphics.getDeltaTime());
            
            if (notif.isExpired()) {
                iter.remove();
            } else {
                Color color = notif.getColor();
                labelFont.setColor(color.r, color.g, color.b, notif.getAlpha());
                labelFont.getData().setScale(0.8f);
                labelFont.draw(spriteBatch, notif.getMessage(), notif.getX(), notif.getY());
                labelFont.getData().setScale(0.9f);
            }
        }
    }
    
    /**
     * Adds a delivery notification at the specified world position
     */
    public void addDeliveryNotification(String message, float worldX, float worldY, Color color) {
        notifications.add(new DeliveryNotification(message, worldX * TILE_SIZE, worldY * TILE_SIZE + 20, 2.0f, color));
    }
    
    /**
     * Renders stops and vehicles on the map (called from GameView)
     */
    public void renderStopsAndVehicles(java.util.List<com.transporttycoon.model.Vehicle> vehicles, 
                                       java.util.List<com.transporttycoon.model.Stop> stops) {
        try {
        
        // Set up shapeRenderer for stops and vehicles
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Render stops as small red circles with cargo indicators
        if (stops != null && !stops.isEmpty()) {
            for (com.transporttycoon.model.Stop stop : stops) {
                float screenX = stop.getX() * TILE_SIZE + TILE_SIZE / 2;
                float screenY = stop.getY() * TILE_SIZE + TILE_SIZE / 2;
                
                // Draw stop as a red circle with outline for better visibility
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.circle(screenX, screenY, 9);
                shapeRenderer.setColor(new Color(1.0f, 0.15f, 0.15f, 0.95f));
                shapeRenderer.circle(screenX, screenY, 7);
                
                // Draw cargo indicator if stop has waiting cargo
                int totalCargo = stop.getTotalCargoUnits();
                if (totalCargo > 0) {
                    shapeRenderer.setColor(new Color(1f, 0.8f, 0.2f, 0.9f));
                    shapeRenderer.circle(screenX + 8, screenY + 8, 4);
                }
            }
        }
        
        // Render vehicles as colored squares with smooth continuous movement
        if (vehicles != null && !vehicles.isEmpty()) {
            for (com.transporttycoon.model.Vehicle vehicle : vehicles) {
                // Use the logical interpolated position (x/y are already fractional tile coords)
                float screenX = vehicle.getX() * TILE_SIZE + TILE_SIZE / 2f;
                float screenY = vehicle.getY() * TILE_SIZE + TILE_SIZE / 2f;

                // Shadow
                shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.65f));
                shapeRenderer.rect(screenX - 8, screenY - 8, 16, 16);

                // Vehicle body
                shapeRenderer.setColor(getVehicleColor(vehicle.getType()));
                shapeRenderer.rect(screenX - 6, screenY - 6, 12, 12);

                // Direction arrow – only when moving
                if (vehicle.isMoving() && !vehicle.isWaiting()) {
                    float dx = vehicle.getDirDx();
                    float dy = vehicle.getDirDy();
                    if (dx != 0 || dy != 0) {
                        float len = (float) Math.sqrt(dx * dx + dy * dy);
                        float nx = dx / len;
                        float ny = dy / len;
                        // Arrow tip
                        float tipX = screenX + nx * 9f;
                        float tipY = screenY + ny * 9f;
                        // Arrow base perpendicular
                        float perpX = -ny * 3f;
                        float perpY =  nx * 3f;
                        float baseX = screenX + nx * 4f;
                        float baseY = screenY + ny * 4f;
                        shapeRenderer.setColor(Color.WHITE);
                        shapeRenderer.triangle(
                            tipX, tipY,
                            baseX + perpX, baseY + perpY,
                            baseX - perpX, baseY - perpY
                        );
                    }
                }

                // Cargo bar
                int cargo = vehicle.getCurrentCapacity();
                if (cargo > 0) {
                    float cargoRatio = (float) cargo / vehicle.getType().getCapacity();
                    shapeRenderer.setColor(new Color(0.2f, 1f, 0.2f, 0.9f));
                    shapeRenderer.rect(screenX - 6, screenY + 7, 12 * cargoRatio, 2);
                }
            }
        }
        
        shapeRenderer.end();
        
        // Render stop labels with cargo info
        spriteBatch.begin();
        if (stops != null && !stops.isEmpty()) {
            for (com.transporttycoon.model.Stop stop : stops) {
                float screenX = stop.getX() * TILE_SIZE;
                float screenY = stop.getY() * TILE_SIZE;
                
                // Show cargo waiting at stop
                int totalCargo = stop.getTotalCargoUnits();
                if (totalCargo > 0) {
                    labelFont.getData().setScale(0.6f);
                    labelFont.setColor(new Color(0f, 0f, 0f, 0.8f));
                    labelFont.draw(spriteBatch, String.valueOf(totalCargo), screenX + 18, screenY + 20);
                    labelFont.setColor(new Color(1f, 1f, 0.2f, 1f));
                    labelFont.draw(spriteBatch, String.valueOf(totalCargo), screenX + 17, screenY + 21);
                    labelFont.getData().setScale(0.9f);
                }
            }
        }
        spriteBatch.end();
        
        } catch (Exception e) {
            System.err.println("Error rendering stops/vehicles: " + e.getMessage());
        }
    }
    
    /**
     * Returns a color based on vehicle type
     */
    private Color getVehicleColor(com.transporttycoon.model.VehicleType type) {
        switch (type) {
            case BUS:
                return new Color(1.0f, 1.0f, 0.0f, 0.8f); // Yellow
            case TRUCK:
                return new Color(1.0f, 0.5f, 0.0f, 0.8f); // Orange
            case CARGO_TRUCK:
                return new Color(0.8f, 0.2f, 0.0f, 0.8f); // Dark orange
            default:
                return new Color(0.5f, 0.5f, 0.5f, 0.8f); // Gray
        }
    }
    
    /**
     * Renders terrain tiles (grass, dirt, water, roads)
     */
    private void renderTerrainTiles(GameMap map) {
        Texture grassTexture = assetManager.getTexture("grass");
        
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null) {
                    float screenX = x * TILE_SIZE;
                    float screenY = y * TILE_SIZE;
                    
                    // Draw grass base
                    spriteBatch.draw(grassTexture, screenX, screenY, TILE_SIZE, TILE_SIZE);

                    if (!tile.hasRoad() && tile.hasTrees()) {
                        int treeCount = Math.max(1, Math.min(4, tile.getTreeCount()));
                        Texture forestTexture = assetManager.getTexture("forest_" + treeCount);
                        spriteBatch.draw(forestTexture, screenX, screenY, TILE_SIZE, TILE_SIZE);
                    }
                    
                    // Draw road if this tile has one
                    if (tile.hasRoad()) {
                        Texture roadTexture = getRoadTextureForTile(map, x, y);
                        // Add slight overlap (0.5 pixels) to prevent gaps at junctions
                        spriteBatch.draw(roadTexture, screenX - 0.5f, screenY - 0.5f, TILE_SIZE + 1f, TILE_SIZE + 1f);
                    }
                }
            }
        }
    }

    private Texture getRoadTextureForTile(GameMap map, int x, int y) {
        boolean north = hasRoadAt(map, x, y + 1);
        boolean east = hasRoadAt(map, x + 1, y);
        boolean south = hasRoadAt(map, x, y - 1);
        boolean west = hasRoadAt(map, x - 1, y);

        int connections = 0;
        if (north) connections++;
        if (east) connections++;
        if (south) connections++;
        if (west) connections++;

        if (connections == 4) {
            return assetManager.getTexture("road_4way");
        }

        if (connections == 3) {
            if (!north) return assetManager.getTexture("road_3way_n");
            if (!east) return assetManager.getTexture("road_3way_e");
            if (!south) return assetManager.getTexture("road_3way_s");
            return assetManager.getTexture("road_3way_w");
        }

        if (connections == 2) {
            if (north && south) return assetManager.getTexture("road_v");
            if (east && west) return assetManager.getTexture("road_h");
            if (north && east) return assetManager.getTexture("road_corner_ne");
            if (north && west) return assetManager.getTexture("road_corner_nw");
            if (south && east) return assetManager.getTexture("road_corner_se");
            if (south && west) return assetManager.getTexture("road_corner_sw");
        }

        if (connections == 1) {
            if (north || south) {
                return assetManager.getTexture("road_v");
            }
            return assetManager.getTexture("road_h");
        }

        return assetManager.getTexture("road");
    }

    private boolean hasRoadAt(GameMap map, int x, int y) {
        Tile neighbor = map.getTile(x, y);
        return neighbor != null && neighbor.hasRoad();
    }
    
    /**
     * Renders buildings (cities and industrial facilities) as multi-tile structures
     */
    private void renderBuildings(GameMap map) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && !tile.isEmpty()) {
                    TileContent content = tile.getContent();
                    if (!renderedBuildings.contains(content)) {
                        renderBuilding(tile, x, y);
                        renderedBuildings.add(content);
                    }
                }
            }
        }
    }
    
    /**
     * Renders a single building (city or facility)
     */
    private void renderBuilding(Tile tile, int mapX, int mapY) {
        float screenX = mapX * TILE_SIZE;
        float screenY = mapY * TILE_SIZE;
        
        if (tile.getContent() instanceof City) {
            City city = (City) tile.getContent();
            Texture cityTexture = getCityTexture(city);
            int size = city.getWidth() * TILE_SIZE;
            
            // Draw city building with slight shadow effect
            spriteBatch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            spriteBatch.draw(cityTexture, screenX + 2, screenY - 2, size, size);
            
            spriteBatch.setColor(Color.WHITE);
            spriteBatch.draw(cityTexture, screenX, screenY, size, size);
            
        } else if (tile.getContent() instanceof IndustrialFacility) {
            IndustrialFacility facility = (IndustrialFacility) tile.getContent();
            Texture facilityTexture = getFacilityTexture(facility);
            int size = facility.getWidth() * TILE_SIZE;
            
            // Draw facility with slight shadow
            spriteBatch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            spriteBatch.draw(facilityTexture, screenX + 2, screenY - 2, size, size);
            
            spriteBatch.setColor(Color.WHITE);
            spriteBatch.draw(facilityTexture, screenX, screenY, size, size);
        }
    }
    
    /**
     * Renders labels for buildings with demand information
     */
    private void renderLabels(GameMap map) {
        labelFont.setColor(Color.WHITE);
        
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && !tile.isEmpty()) {
                    TileContent content = tile.getContent();
                    if (renderedLabels.contains(content)) {
                        continue;
                    }

                    float screenX = x * TILE_SIZE;
                    float screenY = y * TILE_SIZE;
                    
                    if (tile.getContent() instanceof City) {
                        City city = (City) tile.getContent();
                        
                        // Always show basic info
                        String label = city.getName() + "\n" + formatNumber(city.getPopulation());
                        
                        // Show demand summary always (not just when selected)
                        int passDelivered = city.getPassengersDelivered();
                        int passDemand = city.getPassengerDemandMonthly();
                        label += String.format("\n%s %d/%d", 
                            CargoType.PASSENGER.getIcon(), passDelivered, passDemand);
                        
                        // Show cargo demands
                        for (CargoType cargo : city.getDemands()) {
                            int delivered = city.getCargoDelivered(cargo);
                            int demand = city.getCargoDemandMonthly(cargo);
                            label += String.format("\n%s %d/%d", 
                                cargo.getIcon(), delivered, demand);
                        }
                        
                        // Show happiness
                        label += String.format("\n♥ %.0f%%", city.getCityHappiness() * 100);

                        drawShadowedLabel(label, screenX + 4, screenY + city.getHeight() * TILE_SIZE - 4);
                        renderedLabels.add(content);
                        
                    } else if (tile.getContent() instanceof IndustrialFacility) {
                        IndustrialFacility facility = (IndustrialFacility) tile.getContent();

                        if (facility.getFacilityType() == IndustrialFacility.FacilityType.GARAGE) {
                            String label = facility.getName() + "\nService Hub";
                            drawShadowedLabel(label, screenX + 4, screenY + facility.getHeight() * TILE_SIZE - 4);
                            renderedLabels.add(content);
                            continue;
                        }
                        
                        // Always show production info
                        String label = facility.getName();
                        label += String.format("\n%s: %d/100", 
                            facility.getProduces().getIcon(), facility.getCurrentStock());
                        
                        if (facility.getConsumes() != null) {
                            label += String.format("\n%s: %d", 
                                facility.getConsumes().getIcon(), facility.getConsumptionStock());
                        }

                        drawShadowedLabel(label, screenX + 4, screenY + facility.getHeight() * TILE_SIZE - 4);
                        renderedLabels.add(content);
                    }
                }
            }
        }
    }

    private void drawShadowedLabel(String text, float x, float y) {
        labelFont.setColor(new Color(0f, 0f, 0f, 0.9f));
        labelFont.draw(spriteBatch, text, x + 1.5f, y - 1.5f);
        labelFont.setColor(Color.WHITE);
        labelFont.draw(spriteBatch, text, x, y);
    }
    
    /**
     * Gets the appropriate city texture based on population
     */
    private Texture getCityTexture(City city) {
        int population = city.getPopulation();
        
        if (population > 150000) {
            return assetManager.getTexture("city_large");
        } else if (population > 50000) {
            return assetManager.getTexture("city_medium");
        } else {
            return assetManager.getTexture("city_small");
        }
    }
    
    /**
     * Gets the appropriate facility texture based on type
     */
    private Texture getFacilityTexture(IndustrialFacility facility) {
        IndustrialFacility.FacilityType type = facility.getFacilityType();
        
        switch (type) {
            case MINE:
                return assetManager.getTexture("mine");
            case FARM:
                return assetManager.getTexture("farm");
            case FACTORY:
                if (facility.getProduction() == CargoType.STEEL) {
                    return assetManager.getTexture("steel_mill");
                }
                return assetManager.getTexture("paper_mill");
            case GARAGE:
                return assetManager.getTexture("garage");
            default:
                return assetManager.getTexture("mine");
        }
    }
    
    /**
     * Formats large numbers for display (e.g., 100000 -> 100K)
     */
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.valueOf(number);
        }
    }
    
    /**
     * Renders grid lines
     */
    private void renderGrid(GameMap map) {
        shapeRenderer.setColor(GRID_COLOR);
        
        // Vertical lines
        for (int x = 0; x <= map.getWidth(); x++) {
            float lineX = x * TILE_SIZE;
            shapeRenderer.line(lineX, 0, lineX, map.getHeight() * TILE_SIZE);
        }
        
        // Horizontal lines
        for (int y = 0; y <= map.getHeight(); y++) {
            float lineY = y * TILE_SIZE;
            shapeRenderer.line(0, lineY, map.getWidth() * TILE_SIZE, lineY);
        }
    }
    
    /**
     * Converts screen coordinates to map coordinates
     */
    public Vector3 screenToMap(float screenX, float screenY) {
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        int mapX = (int) (worldCoords.x / TILE_SIZE);
        int mapY = (int) (worldCoords.y / TILE_SIZE);
        return new Vector3(mapX, mapY, 0);
    }
    
    /**
     * Converts map coordinates to screen coordinates
     */
    public Vector3 mapToScreen(int mapX, int mapY) {
        return new Vector3(mapX * TILE_SIZE, mapY * TILE_SIZE, 0);
    }
    
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }
    
    public boolean isShowGrid() {
        return showGrid;
    }
    
    /**
     * Sets the selected tile for detailed information display
     */
    public void setSelectedTile(int x, int y) {
        this.selectedTileX = x;
        this.selectedTileY = y;
    }
    
    /**
     * Clears the selected tile
     */
    public void clearSelectedTile() {
        this.selectedTileX = -1;
        this.selectedTileY = -1;
    }
    
    /**
     * Renders traffic lights on the map
     */
    public void renderTrafficLights(TrafficLightManager trafficLightManager) {
        if (trafficLightManager == null) return;
        
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (TrafficLight light : trafficLightManager.getAllTrafficLights()) {
            float screenX = light.getX() * TILE_SIZE;
            float screenY = light.getY() * TILE_SIZE;
            float lightSize = 6f;
            float offset = 4f;
            
            // Draw lights for each direction
            if (light.hasNorthSouth()) {
                // North light
                TrafficLight.LightColor northColor = light.getLightColor(TrafficLight.Direction.NORTH);
                shapeRenderer.setColor(northColor == TrafficLight.LightColor.GREEN ? Color.GREEN : Color.RED);
                shapeRenderer.circle(screenX + TILE_SIZE/2, screenY + TILE_SIZE - offset, lightSize);
                
                // South light
                TrafficLight.LightColor southColor = light.getLightColor(TrafficLight.Direction.SOUTH);
                shapeRenderer.setColor(southColor == TrafficLight.LightColor.GREEN ? Color.GREEN : Color.RED);
                shapeRenderer.circle(screenX + TILE_SIZE/2, screenY + offset, lightSize);
            }
            
            if (light.hasEastWest()) {
                // East light
                TrafficLight.LightColor eastColor = light.getLightColor(TrafficLight.Direction.EAST);
                shapeRenderer.setColor(eastColor == TrafficLight.LightColor.GREEN ? Color.GREEN : Color.RED);
                shapeRenderer.circle(screenX + TILE_SIZE - offset, screenY + TILE_SIZE/2, lightSize);
                
                // West light
                TrafficLight.LightColor westColor = light.getLightColor(TrafficLight.Direction.WEST);
                shapeRenderer.setColor(westColor == TrafficLight.LightColor.GREEN ? Color.GREEN : Color.RED);
                shapeRenderer.circle(screenX + offset, screenY + TILE_SIZE/2, lightSize);
            }
        }
        
        shapeRenderer.end();
    }
    
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (labelFont != null) {
            labelFont.dispose();
        }
    }
}