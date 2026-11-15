package com.transporttycoon.model;

import com.transporttycoon.GameConstants;
import java.util.Random;

/**
 * Represents the game map containing a grid of tiles.
 * Tracks road tiles to support vehicle pathfinding.
 */
public class GameMap {
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];

        // Initialize all tiles
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(x, y);
            }
        }
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the tile at the specified coordinates
     * @param x X coordinate
     * @param y Y coordinate  
     * @return The tile, or null if coordinates are out of bounds
     */
    public Tile getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return tiles[x][y];
    }
    
    /**
     * Places a city on the map at the specified location
     * Cities occupy multiple tiles (minimum 3x3)
     */
    public boolean placeCity(City city, int startX, int startY) {
        int cityWidth = city.getWidth();
        int cityHeight = city.getHeight();
        
        // Check if area is available
        if (!isAreaFree(startX, startY, cityWidth, cityHeight)) {
            return false;
        }
        
        // Place the city
        for (int x = startX; x < startX + cityWidth; x++) {
            for (int y = startY; y < startY + cityHeight; y++) {
                Tile tile = getTile(x, y);
                tile.setContent(city);
                city.addOccupiedTile(tile);
            }
        }
        
        return true;
    }
    
    /**
     * Places an industrial facility on the map at the specified location
     * Facilities occupy multiple tiles (minimum 2x2)
     */
    public boolean placeFacility(IndustrialFacility facility, int startX, int startY) {
        int facilityWidth = facility.getWidth();
        int facilityHeight = facility.getHeight();
        
        // Check if area is available
        if (!isAreaFree(startX, startY, facilityWidth, facilityHeight)) {
            return false;
        }
        
        // Place the facility
        for (int x = startX; x < startX + facilityWidth; x++) {
            for (int y = startY; y < startY + facilityHeight; y++) {
                Tile tile = getTile(x, y);
                tile.setContent(facility);
                facility.addOccupiedTile(tile);
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a rectangular area is free for building
     */
    private boolean isAreaFree(int startX, int startY, int width, int height) {
        // Check bounds
        if (startX < 0 || startY < 0 || 
            startX + width > this.width || 
            startY + height > this.height) {
            return false;
        }
        
        // Check if all tiles in the area are empty and not road
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                Tile tile = tiles[x][y];
                if (!tile.isEmpty() || tile.hasRoad()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks whether a road can be built at the specified coordinate.
     * Road can only be built on a tile that is within bounds, empty (no building),
     * and not already a road.
     */
    public boolean canBuildRoad(int x, int y) {
        Tile tile = getTile(x, y);
        if (tile == null) return false;
        if (!tile.isEmpty() || tile.hasBuilding()) return false;
        return !tile.hasRoad();
    }

    /**
     * Builds a road on the given tile if allowed.
     * Preferred API from the map-world team.
     */
    public boolean buildRoad(int x, int y) {
        if (!canBuildRoad(x, y)) return false;
        getTile(x, y).setRoad(true);
        return true;
    }

    /**
     * Updates forest density across the map.
     * Trees only exist on empty non-road tiles. Existing forest can thicken,
     * and forest can spread to neighboring empty tiles.
     */
    public void updateForests(Random random) {
        int[][] nextTreeCounts = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = tiles[x][y];

                if (tile.hasRoad() || !tile.isEmpty()) {
                    nextTreeCounts[x][y] = 0;
                    continue;
                }

                int currentTrees = tile.getTreeCount();
                int nextTrees = currentTrees;

                if (currentTrees > 0) {
                    if (currentTrees < GameConstants.MAX_TREES_PER_TILE
                            && random.nextDouble() < GameConstants.FOREST_GROWTH_CHANCE) {
                        nextTrees++;
                    }
                } else {
                    int forestNeighbors = countForestedNeighbors(x, y);
                    if (forestNeighbors > 0) {
                        double spreadChance = 1.0 - Math.pow(
                                1.0 - GameConstants.FOREST_SPREAD_CHANCE_PER_NEIGHBOR,
                                forestNeighbors
                        );
                        if (random.nextDouble() < spreadChance) {
                            nextTrees = 1;
                        }
                    } else if (random.nextDouble() < GameConstants.FOREST_SPAWN_CHANCE) {
                        nextTrees = 1;
                    }
                }

                nextTreeCounts[x][y] = Math.min(nextTrees, GameConstants.MAX_TREES_PER_TILE);
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y].setTreeCount(nextTreeCounts[x][y]);
            }
        }
    }

    private int countForestedNeighbors(int x, int y) {
        int neighbors = 0;
        neighbors += hasTreesAt(x + 1, y) ? 1 : 0;
        neighbors += hasTreesAt(x - 1, y) ? 1 : 0;
        neighbors += hasTreesAt(x, y + 1) ? 1 : 0;
        neighbors += hasTreesAt(x, y - 1) ? 1 : 0;
        return neighbors;
    }

    private boolean hasTreesAt(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null && tile.hasTrees();
    }

    public boolean hasAdjacentRoad(int startX, int startY, int width, int height) {
        for (int x = startX; x < startX + width; x++) {
            if (isRoad(x, startY - 1) || isRoad(x, startY + height)) {
                return true;
            }
        }
        for (int y = startY; y < startY + height; y++) {
            if (isRoad(startX - 1, y) || isRoad(startX + width, y)) {
                return true;
            }
        }
        return false;
    }

    public int[] findAdjacentRoad(IndustrialFacility facility) {
        if (facility == null) return null;

        for (Tile occupiedTile : facility.getOccupiedTiles()) {
            int x = occupiedTile.getX();
            int y = occupiedTile.getY();

            if (isRoad(x + 1, y)) return new int[] {x + 1, y};
            if (isRoad(x - 1, y)) return new int[] {x - 1, y};
            if (isRoad(x, y + 1)) return new int[] {x, y + 1};
            if (isRoad(x, y - 1)) return new int[] {x, y - 1};
        }

        return null;
    }

    public boolean hasAnyTrees() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y].hasTrees()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean spawnRandomInitialTree(Random random) {
        int totalTiles = width * height;
        if (totalTiles == 0) return false;

        int startIndex = random.nextInt(totalTiles);
        for (int offset = 0; offset < totalTiles; offset++) {
            int index = (startIndex + offset) % totalTiles;
            int x = index % width;
            int y = index / width;

            Tile tile = tiles[x][y];
            if (!tile.hasRoad() && tile.isEmpty() && !tile.hasTrees()) {
                tile.setTreeCount(1);
                return true;
            }
        }

        return false;
    }

    /**
     * Marks a tile as a road (no building check – for internal/test use).
     * Delegates to {@link Tile#setRoad(boolean)}.
     */
    public boolean placeRoad(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        tiles[x][y].setRoad(true);
        return true;
    }

    /**
     * Returns whether a tile has a road on it.
     */
    public boolean isRoad(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null && tile.hasRoad();
    }

    /**
     * Lays a straight horizontal road from (x1,y) to (x2,y).
     */
    public void placeHorizontalRoad(int x1, int x2, int y) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        for (int x = minX; x <= maxX; x++) {
            placeRoad(x, y);
        }
    }

    /**
     * Lays a straight vertical road from (x,y1) to (x,y2).
     */
    public void placeVerticalRoad(int x, int y1, int y2) {
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        for (int y = minY; y <= maxY; y++) {
            placeRoad(x, y);
        }
    }

    /**
     * Gets a simple text representation of the map for debugging
     */
    public String getMapAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Map (").append(width).append("x").append(height).append("):\n");
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[x][y];
                if (tile.hasRoad()) {
                    sb.append('=');
                } else if (tile.hasTrees()) {
                    sb.append(tile.getTreeCount());
                } else if (tile.isEmpty()) {
                    sb.append('.');
                } else {
                    sb.append(tile.getContent().getDisplayChar());
                }
                sb.append(' ');
            }
            sb.append('\n');
        }
        
        return sb.toString();
    }
}