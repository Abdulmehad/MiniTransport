package com.transporttycoon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.transporttycoon.controller.GameController;
import com.transporttycoon.model.Game;
import com.transporttycoon.persistence.DatabaseManager;
import com.transporttycoon.view.GameView;

/**
 * Main Transport Tycoon game application
 */
public class TransportTycoonGame extends ApplicationAdapter {
    private Game game;
    private GameView gameView;
    private GameController gameController;
    
    @Override
    public void create() {
        // Initialise Neon database connection and schema
        try {
            DatabaseManager.initialise();
        } catch (Exception e) {
            System.err.println("[DB] Warning: Could not connect to database. " +
                               "Save/Load will be unavailable. " + e.getMessage());
        }

        // Initialize game components
        game = new Game();
        gameView = new GameView();
        gameController = new GameController(game, gameView);
        
        // Connect controller to view for state queries
        gameView.setController(gameController);

        // Give the controller a back-reference so a loaded game can be
        // propagated into this render loop (see handleLoadGame / setGame).
        gameController.setApp(this);

        // Connect MapView to VehicleManager for delivery notifications
        game.getVehicleManager().setMapView(gameView.getMapView());
        
        // Start the game
        game.start();
        
        System.out.println("=== TRANSPORT TYCOON STARTED ===");
        System.out.println("Controls:");
        System.out.println("- WASD: Move camera");
        System.out.println("- Mouse wheel: Zoom");
        System.out.println("- Space: Pause/Resume");
        System.out.println("- T: Cycle time speed");
        System.out.println("- G: Toggle grid");
        System.out.println("- F11 or Alt+Enter: Toggle fullscreen");
        System.out.println("- Left click: Select tile");
        System.out.println("- ESC: Exit");
        System.out.println("==============================");
    }
    
    /**
     * Replaces the active {@link Game} instance.  Called by
     * {@link com.transporttycoon.controller.GameController} after a successful
     * save-file load so that the render loop operates on the restored state.
     */
    public void setGame(Game g) {
        this.game = g;
        // Re-connect the delivery notification sink to the new VehicleManager.
        g.getVehicleManager().setMapView(gameView.getMapView());
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Keep app-level reference aligned with controller after load-game swaps.
        game = gameController.getGame();

        // Update game logic
        game.update(deltaTime);

        // Update controller
        gameController.update(deltaTime);

        // Render the game
        gameView.render(game);
    }
    
    @Override
    public void resize(int width, int height) {
        gameView.resize(width, height);
    }
    
    @Override
    public void dispose() {
        if (gameView != null) {
            gameView.dispose();
        }
    }
}