# 🚌 Mini Transport Tycoon

[![pipeline status](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/badges/developer/pipeline.svg)](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/-/pipelines)
[![coverage](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/badges/developer/coverage.svg?job=test&key_text=coverage&key_width=70)](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/-/jobs)
[![line coverage](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/badges/developer/coverage.svg?job=verify_coverage&key_text=line+coverage&key_width=90)](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/-/jobs)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![LibGDX](https://img.shields.io/badge/LibGDX-1.12.1-red?logo=libgdx&logoColor=white)](https://libgdx.com/)
[![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/Neon-PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://neon.tech/)
[![JUnit5](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![JaCoCo](https://img.shields.io/badge/coverage-JaCoCo-brightgreen?logo=java&logoColor=white)](https://www.jacoco.org/)
[![License](https://img.shields.io/badge/License-Academic-lightgrey)](LICENSE)
[![branch](https://img.shields.io/badge/branch-developer-blueviolet?logo=git&logoColor=white)](https://szofttech.inf.elte.hu/software-technology-2026/group-09/super-mario/-/tree/developer)

A grid-based transportation and economic simulation game built with **LibGDX**, developed as a university software technology project at ELTE Faculty of Informatics (2025/2026 Spring).

---

## 📋 Table of Contents

- [🚌 Mini Transport Tycoon](#-mini-transport-tycoon)
  - [📋 Table of Contents](#-table-of-contents)
  - [Overview](#overview)
  - [Features](#features)
    - [Core Game](#core-game)
    - [Sub-task Features (Implemented)](#sub-task-features-implemented)
    - [Visual](#visual)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Build \& Run](#build--run)
    - [Netlify Preview](#netlify-preview)
    - [Building a Windows .exe (native launcher)](#building-a-windows-exe-native-launcher)
    - [Database Setup (Persistence)](#database-setup-persistence)
  - [Controls](#controls)
  - [Project Structure](#project-structure)
  - [Architecture](#architecture)
  - [Testing](#testing)
  - [Team](#team)

---

## Overview

Mini Transport Tycoon is a simplified transportation-economic simulation inspired by the classic Transport Tycoon. Players organise road freight and passenger transport between cities and industrial facilities by purchasing vehicles, building roads, and establishing routes — all in real time.

---

## Features

### Core Game
- **Grid-based map** (20×15 tiles) with cities, industrial facilities, and road network
- **4 vehicle types**: Bus, Minibus, Truck, Cargo Truck — each with distinct speed, capacity, and cost
- **Circular routes**: assign vehicles to stop sequences that repeat automatically
- **Economy**: income from deliveries, costs for roads/vehicles/maintenance, bankruptcy detection
- **Time control**: Pause / Normal / Fast (2×) / Very Fast (4×)
- **Traffic system**: one vehicle per direction per tile, traffic light support at intersections

### Sub-task Features (Implemented)
| Feature | Complexity | Status |
|---|---|---|
| Forests | 0.5 | ✅ Done |
| Garage | 0.5 | ✅ Done |
| Traffic Lights | 1.0 | ✅ Done |
| Minimap | 0.5 | ✅ Done |
| Continuous Movement | 0.5 | ✅ Done |
| Persistence | 0.5 | ✅ Done |

### Visual
- Texture-based rendering with procedural fallback graphics
- Multi-tile buildings (cities 3×3, facilities 2×2) with shadow effects
- Direction arrows on moving vehicles
- High-quality minimap with legend, viewport indicator, and click-to-navigate
- Demand & Supply panel, Fleet Status panel, real-time financial reporting

---

## Getting Started

### Prerequisites
- Java 21+
- A `db.properties` file in the project root (for save/load — see below)

### Build & Run

```bash
# Windows
gradlew.bat clean build
gradlew.bat runGame

# Linux / macOS
./gradlew clean build
./gradlew runGame
```

Or use the provided script:
```bash
run.bat
```

### Netlify Preview

This project is a desktop LibGDX game, so the full Java build still runs locally with the commands above. For a Netlify-hostable version, the repository includes a static preview site in `netlify-site/`.

Deploy settings for Netlify:

- Publish directory: `netlify-site`
- Build command: none

That preview is browser-safe and static, so it can be published directly without a JVM or database.

### Building a Windows .exe (native launcher)

This project is primarily a desktop LibGDX application. You can create a Windows-native launcher using `jpackage` (requires JDK 14+ with `jpackage`; JDK 21 recommended).

Steps:

1. Build a fat JAR (includes dependencies):

```powershell
.\gradlew.bat shadowJar
```

2. Package to an .exe using `jpackage` (helper script provided):

```powershell
.\create-exe.bat
```

Output: `build\jpackage` will contain the generated installer/launcher depending on your `jpackage` configuration.

Notes:
- `jpackage` bundles a private runtime by default; ensure your JDK has the tool available.
- If you prefer a different wrapper (launch4j, Inno Setup installer, or a signed installer), adapt `create-exe.bat` accordingly.

### Database Setup (Persistence)

Create a `db.properties` file in the project root (this file is gitignored):

```properties
db.url=jdbc:postgresql://<host>/<dbname>?sslmode=require&channel_binding=require
db.user=<username>
db.password=<password>
```

The game connects to a **Neon serverless PostgreSQL** database. If `db.properties` is missing or the database is unreachable, the game starts normally — save/load buttons will log a warning but will not crash the game.

---

## Controls

| Key / Action | Function |
|---|---|
| `WASD` | Move camera |
| Mouse Wheel | Zoom in / out |
| `Space` | Pause / Resume |
| `T` | Cycle time speed |
| `G` | Toggle grid |
| `R` | Route assignment mode |
| `ESC` | Exit |
| Left Click (map) | Select tile / place road / stop / vehicle |
| Left Click (minimap) | Jump camera to that position |

---

## Project Structure

```
src/
├── main/java/com/transporttycoon/
│   ├── model/              # Game logic and data
│   │   ├── Game.java           — main game state & update loop
│   │   ├── GameMap.java        — tile grid
│   │   ├── Vehicle.java        — vehicle movement & cargo
│   │   ├── VehicleManager.java — fleet management & pathfinding
│   │   ├── Route.java          — circular stop sequences
│   │   ├── Stop.java           — cargo transfer points
│   │   ├── City.java           — city demand & growth
│   │   ├── IndustrialFacility.java — production & consumption
│   │   ├── TrafficLight.java   — intersection control
│   │   └── ...
│   ├── view/               # LibGDX rendering
│   │   ├── GameView.java       — main render loop & UI
│   │   ├── MapView.java        — tile & entity rendering
│   │   ├── MinimapView.java    — navigable minimap overlay
│   │   └── AssetManager.java   — texture loading & fallbacks
│   ├── controller/         # Input handling
│   │   └── GameController.java
│   ├── persistence/        # Save / Load system
│   │   ├── DatabaseManager.java    — Neon connection & schema
│   │   └── PersistenceManager.java — save / load / list / delete
│   └── GameConstants.java  — shared configuration values
│
└── test/java/com/transporttycoon/model/
    ├── ContinuousMovementTest.java  — 12 tests
    ├── MinimapTest.java             — 13 tests
    ├── PersistenceTest.java         — 14 integration tests
    ├── VehicleTest.java
    ├── VehicleManagerTest.java
    ├── PathfinderTest.java
    ├── TileOccupancyTest.java
    ├── GameMapForestTest.java
    └── VehicleTypeTest.java

assets/
├── tiles/      # 32×32 terrain and road textures
├── buildings/  # City and facility sprites
├── ui/         # UI elements and icons
└── fonts/      # Custom fonts
```

---

## Architecture

The project follows a strict **MVC** pattern:

```
Model  ──▶  Game, GameMap, Vehicle, Route, Stop, City, IndustrialFacility
View   ──▶  GameView, MapView, MinimapView, AssetManager
Controller ──▶  GameController (InputProcessor)
Persistence ──▶  DatabaseManager, PersistenceManager (Neon PostgreSQL)
```

Key design patterns:
- **Manager pattern**: VehicleManager, RouteManager, StopManager, TrafficLightManager
- **State machine**: GameAction enum for input modes; VehicleState for vehicle lifecycle
- **Circular routes**: vehicles loop through stops indefinitely
- **Tile occupancy**: one vehicle per direction per tile (max 2 total)
- **A\* pathfinding**: Pathfinder computes road paths between stops

---

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

Test suite overview:

| Test Class | Tests | What it covers |
|---|---|---|
| `VehicleTest` | 12 | Cargo loading/unloading, state transitions |
| `VehicleManagerTest` | 14 | Purchase, route assignment, maintenance, sell |
| `VehicleTypeTest` | 15 | Type properties and cargo compatibility |
| `PathfinderTest` | 7 | A* road pathfinding |
| `TileOccupancyTest` | 8 | One-vehicle-per-direction rule |
| `GameMapForestTest` | 3 | Forest growth and spread |
| `ContinuousMovementTest` | 12 | Smooth interpolation, visual fields, direction |
| `MinimapTest` | 14 | Bounds, coordinate normalisation, layout |
| `PersistenceTest` | 14 | Save/load/overwrite/delete against live Neon DB — skipped in CI if DB unavailable |
| `AssetManagerTest` | 3 | AssetManager construction (no GL required) |
| `MapViewTest` | 12 | Coordinate conversion, grid toggle, tile selection — pure logic, no GL |
| `MinimapViewTest` | 15 | contains(), normalisation, panel constants, dispose — pure logic, no GL |
| **Total** | **129** | **129 passing, 0 failing** |

---

## Team

**ELTE Faculty of Informatics — Software Technology Practice 2025/2026 Spring**
Group 09

---

*Built with LibGDX · Persisted on Neon PostgreSQL · Tested with JUnit 5*
