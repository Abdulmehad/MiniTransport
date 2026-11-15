package com.transporttycoon.model;

import java.util.List;

/**
 * Abstract base class for all content that can be placed on tiles
 */
public abstract class TileContent {
    protected String name;
    
    public TileContent(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the width in tiles that this content occupies
     */
    public abstract int getWidth();
    
    /**
     * Gets the height in tiles that this content occupies
     */
    public abstract int getHeight();
    
    /**
     * Gets the list of tiles occupied by this content
     */
    public abstract List<Tile> getOccupiedTiles();
    
    /**
     * Gets the visual representation character for debugging
     */
    public abstract char getDisplayChar();
}