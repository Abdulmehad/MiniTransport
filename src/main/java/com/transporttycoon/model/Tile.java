package com.transporttycoon.model;

/**
 * Represents a single tile on the game map.
 *
 * <p>Per the game specification: <em>"Only one vehicle travelling in a given
 * direction can occupy a road tile at a time (therefore a maximum of 2 vehicles
 * can be on one tile in total)."</em>
 *
 * <p>Occupancy is tracked per direction: a vehicle moving in the POSITIVE
 * direction (increasing path index) and one moving in the NEGATIVE direction
 * may both occupy this tile simultaneously.</p>
 */
public class Tile {

    public static final int MAX_TREES_PER_TILE = 4;

    /** Direction a vehicle is travelling through this tile. */
    public enum TrafficDirection { POSITIVE, NEGATIVE }

    private final int x;
    private final int y;
    private TileContent content;
    private boolean hasRoad;
    private int treeCount;

    // Occupancy per direction (spec: max 1 vehicle per direction = max 2 total)
    private Vehicle occupantPositive = null;
    private Vehicle occupantNegative = null;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.content = null;
        this.hasRoad = false;
        this.treeCount = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public TileContent getContent() { return content; }
    public void setContent(TileContent content) {
        this.content = content;
        if (content != null) {
            this.treeCount = 0;
        }
    }

    public boolean isEmpty() { return content == null; }

    public boolean hasBuilding() {
        return content instanceof City || content instanceof IndustrialFacility;
    }

    public boolean hasRoad() { return hasRoad; }
    public void setRoad(boolean hasRoad) {
        this.hasRoad = hasRoad;
        if (hasRoad) {
            this.treeCount = 0;
        }
    }

    public int getTreeCount() {
        return treeCount;
    }

    public boolean hasTrees() {
        return treeCount > 0;
    }

    public void setTreeCount(int treeCount) {
        if (hasRoad || !isEmpty()) {
            this.treeCount = 0;
            return;
        }

        if (treeCount < 0) {
            this.treeCount = 0;
            return;
        }

        this.treeCount = Math.min(treeCount, MAX_TREES_PER_TILE);
    }

    public void addTrees(int treesToAdd) {
        if (treesToAdd <= 0) return;
        setTreeCount(this.treeCount + treesToAdd);
    }

    // ── Occupancy API ─────────────────────────────────────────────────────────

    /**
     * Returns true if the given direction lane is free (no vehicle currently
     * travelling in that direction is on this tile).
     */
    public boolean isDirectionFree(TrafficDirection dir) {
        return dir == TrafficDirection.POSITIVE
                ? occupantPositive == null
                : occupantNegative == null;
    }

    /**
     * Attempts to enter this tile in the given direction.
     *
     * @param vehicle the vehicle wanting to enter
     * @param dir     direction the vehicle is travelling
     * @return {@code true} if entry was granted; {@code false} if the lane is blocked
     */
    public boolean enter(Vehicle vehicle, TrafficDirection dir) {
        if (!isDirectionFree(dir)) return false;
        if (dir == TrafficDirection.POSITIVE) occupantPositive = vehicle;
        else                                  occupantNegative = vehicle;
        return true;
    }

    /**
     * Releases this tile's lane for the given vehicle.
     * Safe to call even if the vehicle is not the current occupant.
     */
    public void leave(Vehicle vehicle, TrafficDirection dir) {
        if (dir == TrafficDirection.POSITIVE && occupantPositive == vehicle) {
            occupantPositive = null;
        } else if (dir == TrafficDirection.NEGATIVE && occupantNegative == vehicle) {
            occupantNegative = null;
        }
    }

    /** Returns the vehicle in the POSITIVE lane, or {@code null}. */
    public Vehicle getOccupantPositive() { return occupantPositive; }
    /** Returns the vehicle in the NEGATIVE lane, or {@code null}. */
    public Vehicle getOccupantNegative() { return occupantNegative; }

    @Override
    public String toString() {
        return String.format("Tile(%d,%d)[road=%b trees=%d content=%s]", x, y, hasRoad, treeCount,
                content != null ? content.getClass().getSimpleName() : "Empty");
    }
}
