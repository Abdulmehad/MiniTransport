package com.transporttycoon.model;

/**
 * Enum representing different types of cargo that can be transported in the game
 */
public enum CargoType {
    PASSENGER("Passenger", "👥"),
    WOOD("Wood", "🪵"),
    PAPER("Paper", "📄"),
    IRON("Iron", "⚙️"),
    STEEL("Steel", "🏗️");
    
    private final String displayName;
    private final String icon;
    
    CargoType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getIcon() {
        return icon;
    }
}