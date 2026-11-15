package com.transporttycoon.view;

import com.badlogic.gdx.graphics.Color;

/**
 * Represents a temporary notification shown when cargo is delivered
 */
public class DeliveryNotification {
    private String message;
    private float x;
    private float y;
    private float lifetime;
    private float maxLifetime;
    private Color color;
    
    public DeliveryNotification(String message, float x, float y, float lifetime, Color color) {
        this.message = message;
        this.x = x;
        this.y = y;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.color = color;
    }
    
    public void update(float deltaTime) {
        lifetime -= deltaTime;
        // Float upward
        y += 20f * deltaTime;
    }
    
    public boolean isExpired() {
        return lifetime <= 0;
    }
    
    public String getMessage() {
        return message;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getAlpha() {
        return Math.max(0f, lifetime / maxLifetime);
    }
    
    public Color getColor() {
        return color;
    }
}
