# Tile Assets

This directory contains terrain and road tile textures (32x32 pixels each).

## Required Assets

All files should be 32x32 pixel PNG images with transparency support.

### Terrain Tiles
- `grass.png` - Light green (#5fb85f) grass texture
- `dirt.png` - Brown earth (#8b6f47) 
- `water.png` - Blue water (#3498db) with subtle waves

### Road Tiles
- `road_straight_h.png` - Horizontal straight road
- `road_straight_v.png` - Vertical straight road
- `road_corner_ne.png` - Northeast corner
- `road_corner_se.png` - Southeast corner
- `road_corner_sw.png` - Southwest corner
- `road_corner_nw.png` - Northwest corner
- `road_intersection_3way_n.png` - T-intersection (North)
- `road_intersection_3way_e.png` - T-intersection (East)
- `road_intersection_3way_s.png` - T-intersection (South)
- `road_intersection_3way_w.png` - T-intersection (West)
- `road_intersection_4way.png` - 4-way crossroads

## Asset Sources
- **Kenney.nl**: https://kenney.nl/assets/road-textures (Free, CC0)
- **OpenGameArt**: Search "road tiles 32x32"
- **Create your own** using Aseprite, Piskel, or GIMP

## Current Status
- ✅ Procedural fallback textures active
- ⏳ Awaiting real assets for enhanced visuals

When you add asset files, the game will automatically use them instead of the procedural fallbacks.
