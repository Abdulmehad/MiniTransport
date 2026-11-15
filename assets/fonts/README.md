# Asset Placeholder

This file indicates where font assets should be placed.

## Required Fonts

### Option 1: Bitmap Font (Generated)
1. Download Hiero (LibGDX bitmap font generator): https://libgdx.com/wiki/tools/hiero
2. Generate a bitmap font with:
   - Font: Roboto, Arial, or any clear sans-serif font
   - Size: 16-24 pixels
   - Effects: Add a 1px black border and subtle shadow
3. Export as `game_font.fnt` and `game_font.png`
4. Place both files here in `assets/fonts/`

### Option 2: TrueType Font
1. Download a free font like Roboto: https://fonts.google.com/specimen/Roboto
2. Place `Roboto-Regular.ttf` in this directory
3. The game will automatically generate a bitmap font at runtime

### Option 3: Use Default
- If no font files are present, the game uses LibGDX's default font (scaled for better visibility)

## Current Status
- ✅ Fallback system active (using default scaled font)
- ⏳ Waiting for custom font assets for better visual quality
