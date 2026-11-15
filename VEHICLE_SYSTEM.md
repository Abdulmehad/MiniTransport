# Vehicle & Transportation System

## Overview

This document describes the design and interaction of all classes added in the
`feature/vehicle-management-system` branch.

---

## Class Diagram (simplified)

```
Game
 ├── VehicleManager  ──uses──▶  Pathfinder
 │    └── Vehicle[]             GameMap
 ├── RouteManager               Player
 │    └── Route[]
 │         └── Stop[]
 └── StopManager
      └── Stop[]
```

---

## Classes

### `Vehicle.java` (completed)
| Responsibility | Details |
|---|---|
| Route following | Moves along a circular list of `Stop` objects via a tile-path |
| Cargo loading | Loads compatible cargo from the current `Stop` up to capacity |
| Cargo unloading | Unloads all cargo at arrival; income is credited to `Player` |
| State tracking | `VehicleState` enum: `IDLE` → `AT_STOP` → `MOVING` → `AT_STOP` |
| Path consumption | Iterates `List<int[]>` path provided by `VehicleManager` |

### `Route.java` (unchanged, used as-is)
* Circular linked list of `Stop` objects via `getNextStop(current)`.
* Tracks assigned `Vehicle` instances.

### `Stop.java` (unchanged, used as-is)
* Holds tile position and a `Map<CargoType, Integer>` of waiting cargo.
* Cargo is added by `StopManager` and consumed by `Vehicle.loadCargo()`.

### `Pathfinder.java` (NEW)
| Feature | Details |
|---|---|
| A* algorithm | Manhattan-distance heuristic, uniform tile cost |
| BFS algorithm | Switchable at runtime via `setAlgorithm(Algorithm.BFS)` |
| Walkability | Road tiles > empty tiles; buildings are obstacles |
| Fallback | Bresenham straight-line when no path exists |
| Overloads | Accepts tile coords `(x,y)` or `Stop` objects directly |

### `VehicleManager.java` (NEW)
| Responsibility | Details |
|---|---|
| Fleet registry | Tracks all purchased `Vehicle` objects |
| Vehicle purchase | Deducts cost from `Player`, creates `Vehicle`, adds to fleet |
| Route assignment | Calls `Pathfinder` to pre-compute path on assignment |
| Tick update | Calls `Vehicle.update()` each tick; credits income, deducts maintenance |
| Path refresh | Automatically re-computes path when a vehicle departs a stop |

### `RouteManager.java` (NEW)
* CRUD operations for `Route` objects.
* Queries: by name, by stop membership.

### `StopManager.java` (NEW)
* Places `Stop` objects on the map (with optional cost deduction).
* Every `CARGO_ACCUMULATION_INTERVAL` ticks, scans nearby `IndustrialFacility`
  tiles within a 3-tile radius and transfers produced goods to the stop's queue.

### `VehicleState.java` (NEW)
* Enum: `IDLE`, `AT_STOP`, `MOVING`.

---

## Game Loop Integration

```
Game.update(deltaTime)
 ├── vehicleManager.update(scaledDelta)
 │    └── for each vehicle:
 │         ├── vehicle.update()   → load/unload/move
 │         ├── player.addFunds(income)
 │         ├── player.deductFunds(maintenance)
 │         └── if departed: refreshPath(vehicle) via Pathfinder
 └── stopManager.update()
      └── every N ticks: accumulateCargo() from nearby facilities
```

---

## How to buy a vehicle and assign a route

```java
// Via GameController (recommended)
Stop mine  = game.getStopManager().placeStopFree("Mine Stop",  8, 3);
Stop depot = game.getStopManager().placeStopFree("Depot",     15, 5);
Route r    = controller.createRoute("Mine → Depot", mine, depot);
Vehicle v  = controller.buyVehicle(VehicleType.CARGO_TRUCK, r);

// Or directly via managers
Vehicle v2 = game.getVehicleManager().purchaseVehicle(VehicleType.TRUCK, game.getGameYear());
game.getVehicleManager().assignRoute(v2, r);
```

---

## Pathfinding

Switch between A* and BFS at runtime:

```java
game.getVehicleManager().getPathfinder().setAlgorithm(Pathfinder.Algorithm.BFS);
```

A* is the default and is recommended for large maps.
