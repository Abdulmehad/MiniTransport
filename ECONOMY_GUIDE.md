# Transport Tycoon - Economy Guide

## Starting Capital
**Initial Balance**: $50,000

This is your starting money to build your transport empire!

---

## Income Sources

### 1. Standard Deliveries (Unloading at Stops)
When vehicles unload cargo at stops:

| Cargo Type | Income per Unit |
|------------|----------------|
| Passengers | $5 |
| Iron | $10 |
| Wood | $10 |
| Steel | $10 |
| Paper | $10 |

**Example**: Truck unloads 20 units of Iron at a stop = $200 income

### 2. Bonus Deliveries (Direct to Cities/Facilities)
When vehicles deliver directly to nearby cities or facilities (within 3 tiles):

| Delivery Type | Income per Unit | Multiplier |
|---------------|----------------|------------|
| Passengers to Cities | $10 | 2x bonus |
| Cargo to Cities | $20 | 2x bonus |
| Raw Materials to Factories | $15 | 1.5x bonus |

**Example**: Truck delivers 20 units of Steel directly to a city = $400 income (2x bonus!)

**Tip**: Place stops very close to cities and facilities to maximize bonus income!

---

## Expenses

### 1. Road Construction
**Cost**: $100 per tile

Roads are permanent once built (no maintenance cost).

**Example**: Building a 10-tile road = $1,000

### 2. Stop Placement
**Cost**: $500 per stop

Stops are permanent once placed (no maintenance cost).

**Example**: Placing 4 stops = $2,000

### 3. Vehicle Purchase

| Vehicle Type | Purchase Cost | Capacity | Speed | Best For |
|-------------|---------------|----------|-------|----------|
| Minibus | $2,500 | 20 passengers | 150 | Small passenger routes |
| Bus | $4,000 | 40 passengers | 120 | Large passenger routes |
| Truck | $5,000 | 50 cargo | 80 | General cargo |
| Cargo Truck | $6,000 | 80 cargo | 60 | Heavy cargo routes |

### 4. Vehicle Maintenance (Per Tick)

| Vehicle Type | Maintenance Cost per Tick | Monthly Cost (approx) |
|-------------|---------------------------|----------------------|
| Minibus | $10 | ~$600 |
| Bus | $15 | ~$900 |
| Truck | $20 | ~$1,200 |
| Cargo Truck | $30 | ~$1,800 |

**Note**: Maintenance is deducted every game tick (60 ticks per second at normal speed).

---

## Profitability Analysis

### Break-Even Analysis

**Minibus** (Passengers):
- Purchase: $2,500
- Monthly maintenance: ~$600
- Capacity: 20 passengers
- Income per trip (bonus): 20 × $10 = $200
- **Break-even**: ~13 trips to recover purchase cost
- **Monthly profit**: ~$3,400 (assuming 20 trips/month)

**Truck** (Cargo):
- Purchase: $5,000
- Monthly maintenance: ~$1,200
- Capacity: 50 cargo
- Income per trip (bonus): 50 × $20 = $1,000
- **Break-even**: ~5 trips to recover purchase cost
- **Monthly profit**: ~$18,800 (assuming 20 trips/month)

**Cargo Truck** (Heavy Cargo):
- Purchase: $6,000
- Monthly maintenance: ~$1,800
- Capacity: 80 cargo
- Income per trip (bonus): 80 × $20 = $1,600
- **Break-even**: ~4 trips to recover purchase cost
- **Monthly profit**: ~$30,200 (assuming 20 trips/month)

### ROI (Return on Investment)

| Vehicle | Purchase Cost | Monthly Profit | ROI Period |
|---------|--------------|----------------|------------|
| Minibus | $2,500 | ~$3,400 | < 1 month |
| Bus | $4,000 | ~$7,100 | < 1 month |
| Truck | $5,000 | ~$18,800 | < 1 month |
| Cargo Truck | $6,000 | ~$30,200 | < 1 month |

**All vehicles pay for themselves in less than one game month!**

---

## Sample Budget Scenarios

### Scenario 1: Starting Small
**Goal**: One simple cargo route

**Expenses**:
- Road construction (20 tiles): $2,000
- 2 stops: $1,000
- 1 Truck: $5,000
- **Total**: $8,000

**Remaining capital**: $42,000

**Expected monthly income**: ~$18,800
**Expected monthly expenses**: ~$1,200
**Net monthly profit**: ~$17,600

### Scenario 2: Passenger Service
**Goal**: Connect two cities with bus service

**Expenses**:
- Road construction (30 tiles): $3,000
- 2 stops: $1,000
- 2 Buses: $8,000
- **Total**: $12,000

**Remaining capital**: $38,000

**Expected monthly income**: ~$14,200
**Expected monthly expenses**: ~$1,800
**Net monthly profit**: ~$12,400

### Scenario 3: Full Supply Chain
**Goal**: Mine → Factory → City

**Expenses**:
- Road construction (50 tiles): $5,000
- 4 stops: $2,000
- 2 Trucks: $10,000
- **Total**: $17,000

**Remaining capital**: $33,000

**Expected monthly income**: ~$37,600
**Expected monthly expenses**: ~$2,400
**Net monthly profit**: ~$35,200

---

## Income Tracking

### In-Game UI (Right Panel)

**Top Section**:
- Current capital (gold color)
- Monthly profit/loss (green = profit, red = loss)
- Recent transactions (last 3)

**Recent Transactions Format**:
- Green text = Income (e.g., "+$200 BONUS Delivery by Vehicle #1")
- Red text = Expense (e.g., "-$20 Maintenance: Vehicle-1")

### Console Output

**Delivery Messages**:
```
[Vehicle 1] ✓ Delivered 10 x Iron to Steel Works Beta (+$300 BONUS)
[Vehicle 2] Unloaded 20 passengers at 'Stop-2-2' (+$100)
```

**Monthly Report**:
```
╔════════════════════════════════════════════════════════════╗
║              MONTHLY FINANCIAL REPORT                      ║
╠════════════════════════════════════════════════════════════╣
║ Current Balance:     $48,500.00                            ║
║ Monthly Income:      +$5,200.00                            ║
║ Monthly Expenses:    -$1,500.00                            ║
╠════════════════════════════════════════════════════════════╣
║ NET PROFIT:          +$3,700.00                            ║
╚════════════════════════════════════════════════════════════╝

--- INCOME BREAKDOWN ---
Standard Deliveries: 15 trips = $1,500.00
Bonus Deliveries:    12 trips = $3,700.00
Total Delivery Income: $5,200.00
```

---

## Tips for Maximizing Profit

### 1. Prioritize Bonus Deliveries
- Place stops within 3 tiles of cities/facilities
- Direct deliveries earn 1.5x-2x more income
- **Best practice**: Stop right next to destination

### 2. Use Appropriate Vehicle Types
- **Passengers**: Use buses (higher capacity = more income per trip)
- **Light cargo**: Use trucks (good balance of speed and capacity)
- **Heavy cargo**: Use cargo trucks (maximum capacity)

### 3. Optimize Routes
- **Short routes** = More trips per hour = More income
- **Circular routes** = Continuous operation
- **Multiple stops** = Pick up from multiple sources

### 4. Monitor Demand
- Check left panel for city demands
- Red progress bars = high demand = opportunity for profit
- Green progress bars = satisfied demand = consider other routes

### 5. Scale Gradually
- Start with 1-2 vehicles
- Reinvest profits into more vehicles
- Expand to new routes as capital grows

### 6. Minimize Idle Time
- Ensure roads connect all stops
- Check for traffic congestion (vehicles waiting)
- Add more vehicles to busy routes

---

## Bankruptcy Warning

**You go bankrupt when capital reaches $0 or below!**

**Common causes**:
- Too many vehicles without enough routes
- High maintenance costs without income
- Expensive infrastructure without vehicles to use it

**Prevention**:
- Always keep at least $5,000 in reserve
- Monitor monthly profit/loss
- Don't buy vehicles you can't afford to maintain

---

## Advanced Strategies

### Strategy 1: Hub-and-Spoke
- Central hub stop with multiple routes
- Efficient for complex supply chains
- Requires more vehicles but higher throughput

### Strategy 2: Point-to-Point
- Direct routes between source and destination
- Simple and efficient
- Best for starting out

### Strategy 3: Circular Supply Chain
- Mine → Factory → City → Mine
- Vehicles always carrying cargo
- Maximum efficiency

### Strategy 4: Dedicated Fleets
- Separate vehicles for each cargo type
- Passenger fleet (buses)
- Cargo fleet (trucks)
- Prevents capacity waste

---

## Financial Goals

### Short-term (First Month)
- **Goal**: Positive monthly profit
- **Target**: $10,000+ profit per month
- **Strategy**: 2-3 efficient routes with bonus deliveries

### Medium-term (3 Months)
- **Goal**: Double starting capital
- **Target**: $100,000 total capital
- **Strategy**: 5-10 vehicles, multiple supply chains

### Long-term (6+ Months)
- **Goal**: Transport empire
- **Target**: $500,000+ capital
- **Strategy**: Full city coverage, all supply chains active

---

## Summary

**Key Takeaways**:
1. ✅ Bonus deliveries are 1.5x-2x more profitable
2. ✅ All vehicles pay for themselves in < 1 month
3. ✅ Maintenance costs are small compared to income
4. ✅ Infrastructure (roads/stops) is a one-time cost
5. ✅ Scale gradually and reinvest profits

**Starting capital of $50,000 is enough to build a profitable transport company!**

---

For gameplay instructions, see `QUICK_START_TRANSPORT.md`
For demand system details, see `DEMAND_SYSTEM_GUIDE.md`
