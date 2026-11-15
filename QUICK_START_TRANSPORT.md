# Quick Start: Setting Up Your First Transport Route

## Goal
Transport IRON from the mine to the steel mill and earn money!

## Steps

### 1. Build Roads (if not already connected)
```
1. Click "Build Road" button
2. Click on a tile near the Iron Mine (8,3)
3. Click on a tile near the Steel Mill (15,5)
4. Roads will be built horizontally/vertically
```

### 2. Place Stops
```
Stop A (near Iron Mine):
1. Click "Place Stop" button
2. Click on a road tile within 3 tiles of Iron Mine Alpha (around 8,3)
3. Cost: $500

Stop B (near Steel Mill):
1. Click "Place Stop" button  
2. Click on a road tile within 3 tiles of Steel Works Beta (around 15,5)
3. Cost: $500
```

### 3. Buy a Truck
```
1. Click "Buy Truck" button ($5,000)
2. Click on Stop A to place the truck there
```

### 4. Assign Route
```
1. Press R key (or click "Assign Route" button)
2. Click on the truck you just bought
3. Click on Stop A
4. Click on Stop B
5. Press R again to finalize the circular route: A → B → A
```

### 5. Watch It Work!

**What happens**:
1. Stop A accumulates IRON from the mine (every 10 ticks)
2. Truck loads IRON at Stop A (automatic)
3. Truck drives to Stop B
4. Truck delivers IRON to Steel Mill (automatic, +$300 bonus!)
5. Truck returns to Stop A
6. Repeat!

**Check the UI**:
- **Left panel**: Watch "Iron Mine Alpha" stock decrease as cargo is picked up
- **Left panel**: Watch "Steel Works Beta" consumption stock increase
- **On map**: Yellow dot appears on Stop A when cargo is waiting
- **On map**: Green bar on truck shows it's carrying cargo
- **Console**: See messages like "[Vehicle 1] ✓ Delivered 10 x Iron..."

## Expected Income

- **Per delivery**: ~$150-300 (depending on bonus)
- **Per minute**: ~$500-1000 (with one truck)
- **Monthly maintenance**: -$20 per truck

## Next Steps

Once your first route is working:

1. **Add more routes**:
   - Farm → Paper Mill (WOOD)
   - Steel Mill → New York (STEEL)
   - Paper Mill → Chicago (PAPER)

2. **Buy more vehicles**:
   - More trucks for cargo
   - Buses for passengers

3. **Monitor demands**:
   - Check left panel for city demands
   - Green bars = satisfied
   - Red bars = need more deliveries

## Troubleshooting

**Truck not loading cargo?**
- Wait a few seconds for cargo to accumulate at the stop
- Check console for "[StopManager] X x Iron → stop..." messages

**Truck not moving?**
- Ensure roads connect Stop A and Stop B
- Check that route was assigned (press R and verify)

**Not earning money?**
- Deliveries only earn money when cargo reaches destination
- Check console for "✓ Delivered" messages

**Stop placement failed?**
- Must be on a road tile
- Must be within 3 tiles of a facility/city
- Costs $500 (check your balance)

## Controls Reminder

- **WASD**: Move camera to see different parts of map
- **Mouse Wheel**: Zoom in/out
- **Space**: Pause/Resume game
- **T**: Change time speed (faster = more cargo accumulation)
- **G**: Toggle grid
- **Left Click**: Select tiles to see info

---

**That's it! You're now running a transport company!** 🚚💰

For more details, see `DEMAND_SYSTEM_GUIDE.md`
