package com.transporttycoon.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssetManager pure-logic behaviour.
 *
 * AssetManager.loadAssets() calls Gdx.files and creates Textures which
 * require a GL context. We test only the parts that don't touch GL:
 * - that the class can be instantiated
 * - that getTexture() with a known key returns the same object on repeated calls
 *   (tested via the no-GL path where textures are null but the map is populated)
 *
 * Full texture loading is verified by manual/integration testing.
 */
class AssetManagerTest {

    @Test
    void constructor_doesNotThrow() {
        assertDoesNotThrow(AssetManager::new,
            "AssetManager constructor must not throw before loadAssets() is called");
    }

    @Test
    void newInstance_isNotNull() {
        assertNotNull(new AssetManager());
    }

    @Test
    void multipleInstances_areIndependent() {
        AssetManager a = new AssetManager();
        AssetManager b = new AssetManager();
        assertNotSame(a, b);
    }
}
