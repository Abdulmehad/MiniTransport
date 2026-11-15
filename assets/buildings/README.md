# Building Assets

This directory contains textures for cities and industrial facilities.

## Required Assets

All files should be PNG images with transparency (alpha channel).

### City Buildings (96x96 pixels for 3x3 tile coverage)
- `city_small.png` - Small city (pop < 50K)
  - Suggested: Low-rise buildings, simple architecture
- `city_medium.png` - Medium city (pop 50K-150K)
  - Suggested: Mid-rise buildings, more density
- `city_large.png` - Large city (pop > 150K)
  - Suggested: Skyscrapers, metropolitan appearance

### Industrial Facilities (64x64 pixels for 2x2 tile coverage)
- `mine.png` - Mining facility
  - Colors: Gray (#7f8c8d), metallic, dark tones
  - Features: Mining equipment, conveyor belts, excavation
  
- `farm.png` - Agricultural facility
  - Colors: Red barn (#c0392b), green fields (#27ae60)
  - Features: Barn, silo, fields
  
- `paper_mill.png` - Paper production
  - Colors: White/gray (#95a5a6), industrial
  - Features: Factory building, smokestacks
  
- `steel_mill.png` - Steel production
  - Colors: Dark gray (#34495e), orange glow (#e74c3c)
  - Features: Heavy industry, furnaces, smokestacks
  
- `garage.png` - Vehicle garage
  - Colors: Blue (#2980b9), modern
  - Features: Garage doors, vehicle bays

## Visual Style Guide
- **Cities**: Should show lit windows (yellow/orange) for nighttime feel
- **Factories**: Include smokestacks and industrial details
- **All buildings**: Add shadows and depth for 2.5D effect

## Asset Sources
- **Kenney.nl**: 
  - "City Kit" - https://kenney.nl/assets/city-kit
  - "Isometric City" - https://kenney.nl/assets/isometric-city
- **OpenGameArt**: Search "factory", "city", "industrial"
- **itch.io**: Filter by "tycoon" or "city builder" assets

## Current Status
- ✅ Procedurally generated fallback buildings active
- ⏳ Awaiting real assets for professional appearance

The fallback system generates:
- Cities: Multi-building blocks with lit windows
- Factories: Colored buildings with smokestacks and windows
- All buildings include labels and shadows

Replace these by placing PNG files with the exact names listed above.
