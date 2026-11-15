package com.transporttycoon;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Desktop launcher for Transport Tycoon
 */
public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        
        // Window configuration
        config.setTitle("Transport Tycoon");
        config.setWindowedMode(1024, 768);
        config.useVsync(true);
        config.setForegroundFPS(60);
        
        // Window icon and other settings
        config.setResizable(true);
        // config.setWindowIcon("icon.png"); // Commented out - no icon needed for now
        
        // Launch the game
        new Lwjgl3Application(new TransportTycoonGame(), config);
    }
}