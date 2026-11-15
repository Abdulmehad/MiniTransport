package com.transporttycoon.model;

/**
 * Represents the possible operational states of a {@link Vehicle}.
 *
 * <p>State transitions:</p>
 * <pre>
 *   IDLE ──assign route──▶ AT_STOP ──depart──▶ MOVING ──arrive──▶ AT_STOP
 *   AT_STOP ──remove route──▶ IDLE
 * </pre>
 */
public enum VehicleState {

    /** Vehicle has no route assigned and is stationary. */
    IDLE("Idle"),

    /** Vehicle is stopped at a route stop (loading/unloading). */
    AT_STOP("At Stop"),

    /** Vehicle is travelling between two stops along a computed path. */
    MOVING("Moving");

    private final String displayName;

    VehicleState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
