package com.transporttycoon.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameMapForestTest {

    @Test
    void buildRoad_onForestedTile_clearsTrees() {
        GameMap map = new GameMap(4, 4);
        Tile tile = map.getTile(1, 1);
        tile.setTreeCount(3);

        assertTrue(map.canBuildRoad(1, 1));
        assertTrue(map.buildRoad(1, 1));
        assertTrue(tile.hasRoad());
        assertEquals(0, tile.getTreeCount());
    }

    @Test
    void updateForests_existingForestGrows_untilMax() {
        GameMap map = new GameMap(4, 4);
        Tile tile = map.getTile(1, 1);
        tile.setTreeCount(1);

        Random zeroRandom = new ZeroRandom();
        for (int i = 0; i < 10; i++) {
            map.updateForests(zeroRandom);
        }

        assertEquals(4, tile.getTreeCount());
    }

    @Test
    void updateForests_forestSpreads_toAdjacentEmptyTile() {
        GameMap map = new GameMap(5, 5);
        map.getTile(2, 2).setTreeCount(1);

        map.updateForests(new ZeroRandom());

        assertTrue(map.getTile(2, 3).hasTrees());
    }

    private static final class ZeroRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }
    }
}
