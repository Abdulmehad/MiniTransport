# Demand & Supply System Guide

## Overview

The Transport Tycoon game now includes a complete demand and supply system with:
- **4 cargo types**: IRON, WOOD, STEEL, PAPER + PASSENGERS
- **Dynamic city demands** that change over time based on satisfaction
- **Industrial production chains**: Raw materials → Finished products
- **Real-time UI visualization** showing all demands and supplies

---

## How It Works

### 1. Industrial Facilities (Supply Side)

**Mines** (produce IRON):
- Automatically produce 10 units every production cycle
- Stock capacity: 100 units
- No consumption requirements

**Farms** (produce WOOD):
- Automatically produce 10 units every production cycle
- Stock capacity: 100 units
- No consumption requirements

**Steel Mills** (consume IRON → produce STEEL):
- Require 5 units of IRON to produce
- Produce 10 units of STEEL per cycle
- Stock capacity: 100 units for STEEL, separate for IRON

**Paper Mills** (consume WOOD → produce PAPER):
- Require 5 units of WOOD to produce
- Produce 10 units of PAPER per cycle
- Stock capacity: 100 units for PAPER, separate for WOOD

### 2. Cities (Demand Side)

**Cities demand**:
- **Passengers**: Based on population (1 per 50 people)
- **Paper**: Based on population (1 per 5 people)
- **Steel**: Based on population (1 per 8 people)

**City Happiness**:
- Calculated monthly based on delivery satisfaction
- High satisfaction (>80%) = Happy city = Population growth
- Low satisfaction (<30%) = Unhappy city = Population decline
- Happiness affects demand fluctuations

**Demand Changes Over Time**:
- Every 5 game seconds, demands fluctuate slightly
- Happy cities (>60% happiness): Demands increase by ~1-2%
- Unhappy cities (<40% happiness): Demands decrease by ~1%
- Random variation: ±5% for natural fluctuation

---

## How to Transport Cargo

### Step 1: Place Stops Near Sources and Destinations

**Near Industrial Facilities** (for pickup):
```
1. Click "Place Stop" button
2. Click on a road tile within 3 tiles of a facility
3. Stop will automatically accumulate cargo from the facility
```

**Near Cities** (for delivery):
```
1. Click "Place Stop" button
2. Click on a road tile within 3 tiles of a city
3. Vehicles can deliver passengers and goods here
```

### Step 2: Buy Vehicles

**For Passengers**:
- Buy Bus (capacity: 40, speed: 120) - $4,000
- Buy Minibus (capacity: 20, speed: 150) - $2,500

**For Cargo** (IRON, WOOD, STEEL, PAPER):
- Buy Truck (capacity: 50, speed: 80) - $5,000
- Buy Cargo Truck (capacity: 80, speed: 60) - $6,000

### Step 3: Assign Routes

**Method 1: Using R key**:
```
1. Press R to enter route assignment mode
2. Click on a vehicle
3. Click on stops in order (minimum 2)
4. Press R again to finalize the circular route
```

**Method 2: Using buttons**:
```
1. Click "Assign Route" button
2. Click on a vehicle
3. Click on stops in order
4. Click "Assign Route" again to finalize
```

### Step 4: Watch the Magic Happen

The vehicle will:
1. **Load cargo** at stops near facilities (automatic)
2. **Travel** along the road network to next stop
3. **Deliver cargo** to nearby cities/facilities (automatic)
4. **Earn income** for successful deliveries
5. **Repeat** the circular route indefinitely

---

## Example Supply Chains

### Chain 1: Iron → Steel → City
```
[Iron Mine] → Stop A → [Truck] → Stop B → [Steel Mill]
[Steel Mill] → Stop C → [Truck] → Stop D → [City]
```

**Setup**:
1. Place Stop A near Iron Mine (on road)
2. Place Stop B near Steel Mill (on road)
3. Build road connecting A → B
4. Buy Truck, assign route: A → B → A
5. Truck picks up IRON at A, delivers to Steel Mill at B
6. Place Stop C near Steel Mill, Stop D near City
7. Buy another Truck, assign route: C → D → C
8. Second truck picks up STEEL at C, delivers to City at D

### Chain 2: Wood → Paper → City
```
[Farm] → Stop X → [Truck] → Stop Y → [Paper Mill]
[Paper Mill] → Stop Z → [Truck] → Stop W → [City]
```

### Chain 3: Passenger Transport
```
[City A] → Stop 1 → [Bus] → Stop 2 → [City B]
```

**Setup**:
1. Place Stop 1 near City A
2. Place Stop 2 near City B
3. Build road connecting stops
4. Buy Bus, assign route: Stop 1 → Stop 2 → Stop 1
5. Bus picks up passengers at Stop 1, delivers to City B at Stop 2

---

## UI Visualization

### Left Panel: DEMAND & SUPPLY

**Cities Section**:
- City name and population
- Happiness bar (green = happy, yellow = neutral, red = unhappy)
- Passenger demand: 👤 delivered/total
- Cargo demands: 📦 delivered/total (Paper, Steel)
- Progress bars show satisfaction level
- **Green bar** = >80% satisfied
- **Yellow bar** = 40-80% satisfied
- **Red bar** = <40% satisfied

**Facilities Section**:
- Facility name
- Production stock: 📦 current/100
- Stock bar (red = overstocked, yellow = medium, green = low/good)
- Consumption stock (for factories): 📦 current amount
- Consumption bar shows raw material availability

### Right Panel: FLEET STATUS

Shows each vehicle:
- **Green dot** = Moving
- **Yellow dot** = At stop
- **Orange dot** = Waiting (blocked)
- **Gray dot** = Idle
- Vehicle ID, type, state, cargo capacity
- Target stop coordinates

### On-Map Labels

**Cities**:
- Name and population
- 👤 passengers delivered/demand
- 📦 cargo delivered/demand
- ♥ happiness percentage

**Facilities**:
- Name
- 📦 production stock/100
- 📦 consumption stock (if applicable)

### Delivery Notifications

When a vehicle delivers cargo:
- **Green floating text** appears above the destination
- Shows "+X 📦" where X is the amount delivered
- Floats upward and fades out over 2 seconds
- Different colors for different cargo types

---

## Monitoring Cargo Flow

### In Console Output

Watch for these messages:

**Cargo Accumulation**:
```
[StopManager] 10 x Iron → stop 'Stop-8-3' (waiting: 10/50)
[StopManager] 5 passengers → stop 'Stop-2-2' from New York (waiting: 5/50)
```

**Vehicle Loading**:
```
[Vehicle 1] Loaded 10 x Iron from 'Stop-8-3'
```

**Vehicle Delivery**:
```
[Vehicle 1] ✓ Delivered 10 x Iron to Steel Works Beta (+$300 BONUS)
```

**Monthly Reports**:
```
--- MONTHLY REPORT ---
Total Balance: $48,500.00
Monthly Income: $2,500.00
Monthly Expenses: $1,000.00
Net Profit: $1,500.00

--- CITY STATUS ---
City 'New York' (Pop: 100000, Happiness: 65%, Cargo Demand: 32500 units, Passenger Demand: 2000)
```

### In UI

**Watch the numbers change**:
1. **Stop cargo indicators**: Yellow dots appear when cargo is waiting
2. **Vehicle cargo bars**: Green bar on top of vehicle shows capacity used
3. **Demand panel**: Numbers update in real-time as deliveries happen
4. **Progress bars**: Fill up as demands are satisfied
5. **Happiness bars**: Change color based on satisfaction

---

## Tips for Efficient Transport

1. **Place stops strategically**:
   - Within 3 tiles of facilities/cities
   - On road networks
   - Consider multiple stops per city for different cargo types

2. **Match vehicle types to cargo**:
   - Buses/Minibuses ONLY carry passengers
   - Trucks/Cargo Trucks carry industrial goods (IRON, WOOD, STEEL, PAPER)

3. **Create supply chains**:
   - Raw materials (IRON, WOOD) → Factories → Finished goods (STEEL, PAPER) → Cities
   - Each step needs its own vehicle and route

4. **Monitor satisfaction**:
   - Green progress bars = good service
   - Red progress bars = need more vehicles or better routes
   - City happiness affects population growth

5. **Watch for bottlenecks**:
   - High facility stock = not enough pickup vehicles
   - Low facility consumption stock = not enough raw material delivery
   - Low city satisfaction = not enough delivery vehicles

---

## Income System

**Standard Income** (unloading at stops):
- Passengers: $5 per unit
- Cargo: $10 per unit

**Bonus Income** (direct delivery to cities/facilities):
- Passengers to cities: $10 per unit (2x bonus)
- Cargo to cities: $20 per unit (2x bonus)
- Raw materials to factories: $15 per unit (1.5x bonus)

**Tip**: Place stops very close to destinations for bonus income!

---

## Troubleshooting

**Problem**: Vehicles not loading cargo
- **Solution**: Ensure stop is within 3 tiles of a facility/city
- **Solution**: Wait for cargo to accumulate (happens every 10 ticks)

**Problem**: City happiness declining
- **Solution**: Add more vehicles to meet demands
- **Solution**: Check if routes are delivering to the right cities

**Problem**: Factory not producing
- **Solution**: Ensure factory has raw materials (check consumption stock)
- **Solution**: Set up supply chain from mines/farms to factory

**Problem**: Demands not changing
- **Solution**: Demands update every 5 game seconds (watch console)
- **Solution**: Monthly updates recalculate based on population changes

---

## Current Game State

**Pre-placed Content**:
- **New York** (100K pop) at (2,2) - Demands passengers, paper, steel
- **Chicago** (75K pop) at (12,8) - Demands passengers, paper, steel
- **Iron Mine Alpha** at (8,3) - Produces IRON
- **Steel Works Beta** at (15,5) - Consumes IRON, produces STEEL
- **Green Valley Farm** at (5,10) - Produces WOOD
- **Paper Mill Delta** at (10,12) - Consumes WOOD, produces PAPER

**Suggested First Routes**:
1. Iron Mine → Steel Mill (deliver IRON)
2. Farm → Paper Mill (deliver WOOD)
3. Steel Mill → New York (deliver STEEL)
4. Paper Mill → Chicago (deliver PAPER)
5. New York → Chicago (transport PASSENGERS)

---

## Visual Indicators Summary

| Indicator | Meaning |
|-----------|---------|
| Yellow dot on stop | Cargo waiting at stop |
| Green bar on vehicle | Vehicle carrying cargo |
| Green progress bar | Demand >80% satisfied |
| Yellow progress bar | Demand 40-80% satisfied |
| Red progress bar | Demand <40% satisfied |
| Green happiness bar | City happy (>60%) |
| Yellow happiness bar | City neutral (30-60%) |
| Red happiness bar | City unhappy (<30%) |
| Floating "+X 📦" | Successful delivery |

---

**Enjoy building your transport empire!** 🚚🏭🏙️
