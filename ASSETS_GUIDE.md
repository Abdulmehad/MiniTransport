# Asset Requirements for Transport Tycoon

This document outlines all the visual assets needed for the game. Assets should be placed in the `assets/` directory.

## 🎨 Required Assets

### Tile Assets (32x32 pixels, PNG format)
Place in: `assets/tiles/`

1. **grass.png** - Base grass terrain tile
   - Recommended: Light green (#5fb85f), natural grass texture
   - Source: OpenGameArt.org, Kenney.nl assets, or create with Aseprite/Piskel

2. **dirt.png** - Dirt/earth terrain
   - Recommended: Brown earth tone (#8b6f47)
   
3. **water.png** (Future: for rivers/lakes)
   - Recommended: Blue with subtle wave animation frames (#3498db)

### Building Assets (PNG format with transparency)
Place in: `assets/buildings/`

#### Cities (96x96 pixels minimum - for 3x3 tile coverage)
4. **city_small.png** - Small city buildings
   - Multi-story buildings, yellow/orange windows
   - Source: Kenney.nl "City Pack" or similar
   
5. **city_medium.png** - Medium city (alternative texture)
   - More dense urban appearance
   
6. **city_large.png** - Large city (for future city growth)
   - Skyscrapers, dense metropolitan feel

#### Industrial Facilities (64x64 pixels - for 2x2 tile coverage)
7. **mine.png** - Mining facility
   - Dark colors, mining equipment, conveyor belts
   - Colors: Gray (#7f8c8d), brown, metallic
   
8. **farm.png** - Farm/agricultural facility
   - Green fields, barn, silo
   - Colors: Red barn (#c0392b), green (#27ae60), brown
   
9. **paper_mill.png** - Paper production facility
   - Industrial building with smokestacks
   - Colors: White/gray (#95a5a6), blue accents
   
10. **steel_mill.png** - Steel production facility
    - Heavy industry appearance, furnaces, smokestacks
    - Colors: Dark gray (#34495e), orange glow (#e74c3c)

### Road Assets (32x32 pixels)
Place in: `assets/tiles/`

11. **road_straight_h.png** - Horizontal straight road
    - Dark gray asphalt (#36454f), white lane markings
    
12. **road_straight_v.png** - Vertical straight road
    
13. **road_corner_ne.png** - Corner road (North-East)
14. **road_corner_se.png** - Corner road (South-East)
15. **road_corner_sw.png** - Corner road (South-West)
16. **road_corner_nw.png** - Corner road (North-West)

17. **road_intersection_3way_n.png** - T-intersection (North exit)
18. **road_intersection_3way_e.png** - T-intersection (East exit)
19. **road_intersection_3way_s.png** - T-intersection (South exit)
20. **road_intersection_3way_w.png** - T-intersection (West exit)

21. **road_intersection_4way.png** - 4-way intersection

### UI Assets
Place in: `assets/ui/`

22. **panel_bg.png** - UI panel background (9-patch)
    - Semi-transparent dark panel
    
23. **button_normal.png** - Button normal state
24. **button_hover.png** - Button hover state
25. **button_pressed.png** - Button pressed state

26. **icon_money.png** - Money/capital icon (16x16)
27. **icon_time.png** - Time icon (16x16)
28. **icon_pause.png** - Pause icon (16x16)
29. **icon_play.png** - Play icon (16x16)

### Font Assets
Place in: `assets/fonts/`

30. **game_font.fnt** + **game_font.png** - Bitmap font
    - Recommended: Use Hiero (LibGDX tool) to generate from system fonts
    - Suggested fonts: "Roboto", "Arial", "Press Start 2P" (retro style)

### Particle Effects (Optional for polish)
Place in: `assets/particles/`

31. **smoke.png** - Smoke particle for factories
32. **spark.png** - Sparkle effect for successful deliveries

## 🎯 Asset Sources (Free & Legal)

### Recommended Free Sources:
1. **Kenney.nl** - https://kenney.nl/assets
   - City Kit, Isometric City, Road Textures
   - CC0 License (Public Domain)
   
2. **OpenGameArt.org**
   - Search: "city", "industrial", "road", "tile"
   - Check individual licenses (many CC-BY 3.0)
   
3. **itch.io Free Assets**
   - https://itch.io/game-assets/free
   - Filter by "Top Rated" and "Tycoon" or "City"

4. **Pixel Art Tools** (create your own):
   - Aseprite (paid, $20)
   - Piskel (free, web-based)
   - GIMP (free)

### Color Palette Recommendation:
```
Grass:         #5fb85f (Light Green)
City:          #f4d03f (Golden Yellow)
Road:          #36454f (Dark Slate)
Mine:          #7f8c8d (Gray)
Farm:          #27ae60 (Green)
Steel Mill:    #34495e (Dark Gray)
Paper Mill:    #95a5a6 (Silver)
Water:         #3498db (Blue)
UI Background: #2c3e50 (Dark Blue-Gray)
```

## 📐 Asset Specifications

- **Format**: PNG with transparency (alpha channel)
- **Tile Size**: 32x32 pixels (for grid tiles)
- **Building Sizes**: 
  - Industrial: 64x64 (2x2 tiles)
  - City: 96x96 or larger (3x3+ tiles)
- **DPI**: 72 or 96 DPI
- **Color Depth**: 32-bit RGBA

## 🔄 Fallback System

The game implements a fallback rendering system:
- If asset file is missing → renders colored rectangle with label
- Allows development to continue without all assets
- Assets can be added incrementally

## 📝 Current Status

- ✅ Directory structure created
- ⏳ Awaiting asset files (or using fallback rendering)
- ⏳ Texture atlas optimization (future improvement)

## 🚀 Quick Start

1. Download asset pack from Kenney.nl (e.g., "City Kit" + "Road Textures")
2. Extract and rename files to match the names above
3. Place in appropriate `assets/` subdirectories
4. Restart the game - assets will load automatically

---

**Note**: All asset loading is handled by `AssetManager.java`. Missing assets automatically fall back to procedurally generated textures.
