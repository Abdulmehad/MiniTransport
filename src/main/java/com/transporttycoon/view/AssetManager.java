package com.transporttycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.files.FileHandle;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all game assets (textures, fonts, etc.) with automatic fallback generation
 * for missing assets. This allows development to continue even without all art assets.
 */
public class AssetManager {
    private static final int TILE_SIZE = 32;
    private static final int BUILDING_SIZE_2x2 = 64;
    private static final int BUILDING_SIZE_3x3 = 96;
    
    private Map<String, Texture> textures;
    private BitmapFont gameFont;
    private boolean assetsLoaded = false;
    
    public AssetManager() {
        textures = new HashMap<>();
    }
    
    /**
     * Loads all game assets. Creates fallback textures for missing files.
     */
    public void loadAssets() {
        System.out.println("Loading game assets...");
        
        // Load terrain tiles
        loadTexture("grass", "assets/tiles/grass.png", createGrassTile());
        loadTexture("dirt", "assets/tiles/dirt.png", createDirtTile());
        loadTexture("water", "assets/tiles/water.png", createWaterTile());
        loadTexture("forest_1", "assets/tiles/forest_1.png", createForestTile(1));
        loadTexture("forest_2", "assets/tiles/forest_2.png", createForestTile(2));
        loadTexture("forest_3", "assets/tiles/forest_3.png", createForestTile(3));
        loadTexture("forest_4", "assets/tiles/forest_4.png", createForestTile(4));
        
        // Load road tiles
        Texture roadFallback = createRoadTile();
        loadTexture("road", "assets/tiles/road_generic.png", roadFallback);
        loadTexture("road_h", "assets/tiles/road_straight_h.png", roadFallback);
        loadTexture("road_v", "assets/tiles/road_straight_v.png", roadFallback);
        loadTexture("road_corner_ne", "assets/tiles/road_corner_ne.png", roadFallback);
        loadTexture("road_corner_nw", "assets/tiles/road_corner_nw.png", roadFallback);
        loadTexture("road_corner_se", "assets/tiles/road_corner_se.png", roadFallback);
        loadTexture("road_corner_sw", "assets/tiles/road_corner_sw.png", roadFallback);
        loadTexture("road_3way_n", "assets/tiles/road_intersection_3way_n.png", createIntersectionTile());
        loadTexture("road_3way_e", "assets/tiles/road_intersection_3way_e.png", createIntersectionTile());
        loadTexture("road_3way_s", "assets/tiles/road_intersection_3way_s.png", createIntersectionTile());
        loadTexture("road_3way_w", "assets/tiles/road_intersection_3way_w.png", createIntersectionTile());
        loadTexture("road_4way", "assets/tiles/road_intersection_4way.png", createIntersectionTile());
        
        // Load city buildings from required PNG assets
        loadRequiredPngTextureFromAnyPath("city_small", new String[]{"assets/buildings/city_small.png", "buildings/city_small.png"});
        loadRequiredPngTextureFromAnyPath("city_medium", new String[]{"assets/buildings/city_medium.png", "buildings/city_medium.png"});
        loadRequiredPngTextureFromAnyPath("city_large", new String[]{"assets/buildings/city_large.png", "buildings/city_large.png"});
        
        // Load industrial facilities from required PNG assets
        loadRequiredPngTextureFromAnyPath("mine", new String[]{"assets/buildings/mine.png", "buildings/mine.png"});
        loadRequiredPngTextureFromAnyPath("farm", new String[]{"assets/buildings/farm.png", "buildings/farm.png"});
        loadRequiredPngTextureFromAnyPath("paper_mill", new String[]{"assets/buildings/paper_mill.png", "buildings/paper_mill.png"});
        loadRequiredPngTextureFromAnyPath("steel_mill", new String[]{"assets/buildings/steel_mill.png", "buildings/steel_mill.png"});
        
        // Load garage from required PNG asset
        loadRequiredPngTextureFromAnyPath("garage", new String[]{"assets/buildings/garage.png", "buildings/garage.png"});
        
        // Load font
        loadFont();
        
        assetsLoaded = true;
        System.out.println("Asset loading complete. Total textures: " + textures.size());
    }
    
    /**
     * Loads a texture from file or uses fallback if file doesn't exist
     */
    private void loadTexture(String key, String filepath, Texture fallback) {
        FileHandle file = Gdx.files.internal(filepath);
        
        if (file.exists()) {
            try {
                Texture texture = new Texture(file);
                // Set texture filtering to prevent gaps at junctions
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                textures.put(key, texture);
                System.out.println("✓ Loaded: " + filepath);
            } catch (Exception e) {
                System.out.println("✗ Failed to load " + filepath + ", using fallback");
                textures.put(key, fallback);
            }
        } else {
            System.out.println("✗ Missing: " + filepath + " (using procedural fallback)");
            textures.put(key, fallback);
        }
    }

    /**
     * Tries multiple internal paths for a texture and uses fallback only if all are missing.
     */
    private void loadTextureFromAnyPath(String key, String[] filepaths, Texture fallback) {
        for (String filepath : filepaths) {
            FileHandle file = Gdx.files.internal(filepath);
            if (!file.exists()) {
                continue;
            }

            try {
                Texture texture = new Texture(file);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                textures.put(key, texture);
                System.out.println("✓ Loaded: " + filepath);
                return;
            } catch (Exception e) {
                System.out.println("✗ Failed to load " + filepath + ", trying next path...");
            }
        }

        System.out.println("✗ Missing building asset for key '" + key + "' (using procedural fallback)");
        textures.put(key, fallback);
    }

    /**
     * Loads a required PNG texture from one of the provided internal paths.
     * Throws if not found so the game never silently falls back to generated art.
     */
    private void loadRequiredPngTextureFromAnyPath(String key, String[] filepaths) {
        for (String filepath : filepaths) {
            if (!filepath.toLowerCase().endsWith(".png")) {
                continue;
            }

            FileHandle file = Gdx.files.internal(filepath);
            if (!file.exists()) {
                continue;
            }

            try {
                Texture texture = new Texture(file);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                textures.put(key, texture);
                System.out.println("✓ Loaded PNG: " + filepath);
                return;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load required PNG for key '" + key + "' from " + filepath, e);
            }
        }

        throw new IllegalStateException("Missing required PNG asset for key '" + key + "'. Expected one of: " + java.util.Arrays.toString(filepaths));
    }
    
    /**
     * Creates a procedurally generated grass tile texture
     * ASSET REQUIREMENT: Replace with assets/tiles/grass.png (32x32)
     * Suggested: Light green (#5fb85f) with natural grass texture variation
     */
    private Texture createGrassTile() {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        
        // Base grass color
        pixmap.setColor(new Color(0.37f, 0.72f, 0.37f, 1f));
        pixmap.fill();
        
        // Add some variation/texture
        pixmap.setColor(new Color(0.32f, 0.67f, 0.32f, 1f));
        for (int i = 0; i < 20; i++) {
            int x = (int)(Math.random() * TILE_SIZE);
            int y = (int)(Math.random() * TILE_SIZE);
            pixmap.drawPixel(x, y);
        }
        
        // Darker grass accents
        pixmap.setColor(new Color(0.27f, 0.62f, 0.27f, 1f));
        for (int i = 0; i < 15; i++) {
            int x = (int)(Math.random() * TILE_SIZE);
            int y = (int)(Math.random() * TILE_SIZE);
            pixmap.fillCircle(x, y, 1);
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates a procedurally generated dirt tile
     * ASSET REQUIREMENT: Replace with assets/tiles/dirt.png (32x32)
     */
    private Texture createDirtTile() {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.55f, 0.44f, 0.28f, 1f));
        pixmap.fill();
        
        // Add dirt texture
        pixmap.setColor(new Color(0.50f, 0.39f, 0.23f, 1f));
        for (int i = 0; i < 30; i++) {
            int x = (int)(Math.random() * TILE_SIZE);
            int y = (int)(Math.random() * TILE_SIZE);
            pixmap.drawPixel(x, y);
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates water tile
     * ASSET REQUIREMENT: Replace with assets/tiles/water.png (32x32)
     */
    private Texture createWaterTile() {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.2f, 0.6f, 0.87f, 1f));
        pixmap.fill();
        
        // Add wave highlights
        pixmap.setColor(new Color(0.3f, 0.7f, 0.97f, 0.6f));
        for (int y = 0; y < TILE_SIZE; y += 8) {
            pixmap.drawLine(0, y, TILE_SIZE, y);
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates a road tile texture
     * ASSET REQUIREMENT: Replace with assets/tiles/road_straight_h.png (32x32)
     * Suggested: Dark gray asphalt (#36454f) with white lane markings
     */
    private Texture createRoadTile() {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        
        // Dark gray asphalt - fill completely to edges
        pixmap.setColor(new Color(0.21f, 0.27f, 0.31f, 1f));
        pixmap.fill();
        
        // Center white dashed line
        pixmap.setColor(Color.WHITE);
        for (int x = 2; x < TILE_SIZE; x += 8) {
            pixmap.fillRectangle(x, TILE_SIZE/2 - 1, 4, 2);
        }
        
        // Side lines
        pixmap.setColor(new Color(1f, 1f, 1f, 0.5f));
        pixmap.drawLine(0, 4, TILE_SIZE, 4);
        pixmap.drawLine(0, TILE_SIZE - 5, TILE_SIZE, TILE_SIZE - 5);
        
        Texture texture = new Texture(pixmap);
        // Set nearest neighbor filtering to prevent gaps
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private Texture createForestTile(int trees) {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0f, 0f, 0f, 0f));
        pixmap.fill();

        int[][] positions = new int[][] {
            {8, 8}, {22, 8}, {8, 22}, {22, 22}
        };

        for (int i = 0; i < Math.min(trees, positions.length); i++) {
            int cx = positions[i][0];
            int cy = positions[i][1];

            pixmap.setColor(new Color(0.30f, 0.18f, 0.10f, 1f));
            pixmap.fillRectangle(cx - 1, cy + 2, 3, 6);

            pixmap.setColor(new Color(0.12f, 0.48f, 0.14f, 0.95f));
            pixmap.fillCircle(cx, cy, 4);
            pixmap.setColor(new Color(0.18f, 0.58f, 0.20f, 0.85f));
            pixmap.fillCircle(cx + 1, cy + 1, 2);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates intersection tile
     * ASSET REQUIREMENT: Replace with assets/tiles/road_intersection_4way.png (32x32)
     */
    private Texture createIntersectionTile() {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        
        // Dark gray asphalt - fill completely to edges
        pixmap.setColor(new Color(0.21f, 0.27f, 0.31f, 1f));
        pixmap.fill();
        
        // White crossing lines
        pixmap.setColor(new Color(1f, 1f, 1f, 0.3f));
        pixmap.drawLine(TILE_SIZE/2, 0, TILE_SIZE/2, TILE_SIZE);
        pixmap.drawLine(0, TILE_SIZE/2, TILE_SIZE, TILE_SIZE/2);
        
        Texture texture = new Texture(pixmap);
        // Set nearest neighbor filtering to prevent gaps
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates a city building texture with modern styling
     * ASSET REQUIREMENT: Replace with assets/buildings/city_*.png (96x96)
     * Suggested: Multi-story buildings with yellow/orange lit windows
     */
    private Texture createCityTexture(String label, int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        
        // Building base color (concrete/brick)
        pixmap.setColor(new Color(0.75f, 0.7f, 0.65f, 1f));
        pixmap.fill();
        
        // Create multiple "buildings" in the city block
        int buildingCount = 3;
        int buildingWidth = size / buildingCount;
        
        for (int i = 0; i < buildingCount; i++) {
            int x = i * buildingWidth;
            int height = (int)(size * (0.6f + Math.random() * 0.4f));
            int y = size - height;
            
            // Building color variation
            float colorVar = 0.05f * (float)Math.random();
            pixmap.setColor(new Color(0.7f + colorVar, 0.65f + colorVar, 0.6f + colorVar, 1f));
            pixmap.fillRectangle(x, y, buildingWidth - 2, height);
            
            // Add lit windows (yellow/orange)
            pixmap.setColor(new Color(1f, 0.85f, 0.3f, 1f));
            for (int wy = y + 4; wy < size - 4; wy += 8) {
                for (int wx = x + 3; wx < x + buildingWidth - 4; wx += 6) {
                    if (Math.random() > 0.3) { // 70% windows are lit
                        pixmap.fillRectangle(wx, wy, 3, 4);
                    }
                }
            }
            
            // Building outline
            pixmap.setColor(new Color(0.3f, 0.3f, 0.3f, 1f));
            pixmap.drawRectangle(x, y, buildingWidth - 2, height);
        }
        
        // Add label at bottom
        pixmap.setColor(new Color(0.2f, 0.2f, 0.2f, 0.8f));
        pixmap.fillRectangle(0, 0, size, 12);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Creates an industrial facility texture
     * ASSET REQUIREMENT: Replace with assets/buildings/*.png (64x64)
     * Different facilities should have distinct visual styles
     */
    private Texture createIndustrialTexture(String label, int size, Color baseColor) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        
        // Main building
        pixmap.setColor(baseColor);
        pixmap.fillRectangle(4, 10, size - 8, size - 14);
        
        // Building outline
        pixmap.setColor(new Color(baseColor.r * 0.6f, baseColor.g * 0.6f, baseColor.b * 0.6f, 1f));
        pixmap.drawRectangle(4, 10, size - 8, size - 14);
        
        // Add smokestacks for factories
        if (label.contains("MILL")) {
            pixmap.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
            pixmap.fillRectangle(size/4, size/2, 6, size/2 - 10);
            pixmap.fillRectangle(size*3/4 - 6, size/2, 6, size/2 - 10);
            
            // Smoke effect (simple)
            pixmap.setColor(new Color(0.7f, 0.7f, 0.7f, 0.4f));
            pixmap.fillCircle(size/4 + 3, size - 8, 4);
            pixmap.fillCircle(size*3/4 - 3, size - 8, 4);
        }
        
        // Add windows
        pixmap.setColor(new Color(0.3f, 0.3f, 0.4f, 1f));
        for (int y = 16; y < size - 16; y += 10) {
            for (int x = 10; x < size - 10; x += 12) {
                pixmap.fillRectangle(x, y, 6, 6);
            }
        }
        
        // Label background
        pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 0.8f));
        pixmap.fillRectangle(0, 0, size, 10);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Loads game font
     * ASSET REQUIREMENT: assets/fonts/game_font.fnt + game_font.png
     * Or use FreeType to generate from TTF font
     */
    private void loadFont() {
        FileHandle fontFile = Gdx.files.internal("assets/fonts/game_font.fnt");
        
        if (fontFile.exists()) {
            try {
                gameFont = new BitmapFont(fontFile);
                System.out.println("✓ Loaded custom font");
                return;
            } catch (Exception e) {
                System.out.println("✗ Failed to load custom font, using default");
            }
        }
        
        // Try to use FreeType to generate a better font
        FileHandle ttfFile = Gdx.files.internal("assets/fonts/Roboto-Regular.ttf");
        if (ttfFile.exists()) {
            try {
                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);
                FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
                parameter.size = 16;
                parameter.color = Color.WHITE;
                parameter.borderWidth = 1;
                parameter.borderColor = Color.BLACK;
                parameter.shadowOffsetX = 1;
                parameter.shadowOffsetY = 1;
                parameter.shadowColor = new Color(0, 0, 0, 0.5f);
                
                gameFont = generator.generateFont(parameter);
                generator.dispose();
                System.out.println("✓ Generated font from TTF");
                return;
            } catch (Exception e) {
                System.out.println("✗ Failed to generate font from TTF");
            }
        }
        
        // Fallback to default LibGDX font
        gameFont = new BitmapFont();
        gameFont.setColor(Color.WHITE);
        gameFont.getData().setScale(1.2f);
        System.out.println("✓ Using default font (scaled)");
    }
    
    /**
     * Gets a texture by key
     */
    public Texture getTexture(String key) {
        return textures.getOrDefault(key, textures.get("grass"));
    }
    
    /**
     * Gets a texture region (useful for sprite batching)
     */
    public TextureRegion getTextureRegion(String key) {
        Texture texture = getTexture(key);
        return new TextureRegion(texture);
    }
    
    /**
     * Gets the game font
     */
    public BitmapFont getFont() {
        return gameFont;
    }
    
    /**
     * Checks if all assets have been loaded
     */
    public boolean isLoaded() {
        return assetsLoaded;
    }
    
    /**
     * Disposes of all loaded assets
     */
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        
        if (gameFont != null) {
            gameFont.dispose();
        }
        
        System.out.println("Assets disposed");
    }
}
