package com.transporttycoon.model;

/**
 * Enum representing different game time speeds
 */
public enum TimeSpeed {
    PAUSE(0.0f, "⏸️"),
    NORMAL(1.0f, "▶️"),
    FAST(2.0f, "⏩"),
    VERY_FAST(4.0f, "⏭️");
    
    private final float multiplier;
    private final String icon;
    
    TimeSpeed(float multiplier, String icon) {
        this.multiplier = multiplier;
        this.icon = icon;
    }
    
    public float getMultiplier() {
        return multiplier;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public boolean isPaused() {
        return this == PAUSE;
    }
}