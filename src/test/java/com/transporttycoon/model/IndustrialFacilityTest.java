package com.transporttycoon.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IndustrialFacility - production and consumption system
 */
class IndustrialFacilityTest {
    
    @Test
    void testCreateMine() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        assertEquals("Iron Mine", mine.getName());
        assertEquals(IndustrialFacility.FacilityType.MINE, mine.getFacilityType());
        assertEquals(CargoType.IRON, mine.getProduction());
        assertEquals(0, mine.getCurrentStock());
    }
    
    @Test
    void testCreateFarm() {
        IndustrialFacility farm = IndustrialFacility.createFarm("Green Farm");
        
        assertEquals("Green Farm", farm.getName());
        assertEquals(IndustrialFacility.FacilityType.FARM, farm.getFacilityType());
        assertEquals(CargoType.WOOD, farm.getProduction());
    }
    
    @Test
    void testCreateSteelFactory() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        assertEquals("Steel Works", factory.getName());
        assertEquals(IndustrialFacility.FacilityType.FACTORY, factory.getFacilityType());
        assertEquals(CargoType.STEEL, factory.getProduction());
        assertEquals(CargoType.IRON, factory.getConsumption());
    }
    
    @Test
    void testCreatePaperFactory() {
        IndustrialFacility factory = IndustrialFacility.createPaperFactory("Paper Factory");
        
        assertEquals("Paper Factory", factory.getName());
        assertEquals(IndustrialFacility.FacilityType.FACTORY, factory.getFacilityType());
        assertEquals(CargoType.PAPER, factory.getProduction());
        assertEquals(CargoType.WOOD, factory.getConsumption());
    }
    
    @Test
    void testMineProduction() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        // Produce iron
        mine.produce();
        
        assertTrue(mine.getCurrentStock() > 0);
        assertEquals(10, mine.getCurrentStock()); // Default production amount
    }
    
    @Test
    void testFarmProduction() {
        IndustrialFacility farm = IndustrialFacility.createFarm("Wood Farm");
        
        // Produce wood
        farm.produce();
        
        assertEquals(10, farm.getCurrentStock());
    }
    
    @Test
    void testProductionCapacity() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        // Produce until capacity (100 units)
        for (int i = 0; i < 15; i++) {
            mine.produce();
        }
        
        // Should not exceed capacity
        assertTrue(mine.getCurrentStock() <= 100);
    }
    
    @Test
    void testSteelFactoryProduction_WithSufficientInput() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // Add iron (input material)
        factory.deliverRawMaterial(CargoType.IRON, 10);
        
        // Produce steel
        factory.produce();
        
        assertTrue(factory.getCurrentStock() > 0);
        assertTrue(factory.getConsumptionStock() < 10); // Iron was consumed
    }
    
    @Test
    void testSteelFactoryProduction_WithoutSufficientInput() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // No iron available
        factory.produce();
        
        assertEquals(0, factory.getCurrentStock());
    }
    
    @Test
    void testPaperFactoryProduction_WithSufficientInput() {
        IndustrialFacility factory = IndustrialFacility.createPaperFactory("Paper Factory");
        
        // Add wood (input material)
        factory.deliverRawMaterial(CargoType.WOOD, 10);
        
        // Produce paper
        factory.produce();
        
        assertTrue(factory.getCurrentStock() > 0);
    }
    
    @Test
    void testConsumptionRatio() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // Add 10 units of iron
        factory.deliverRawMaterial(CargoType.IRON, 10);
        int initialConsumption = factory.getConsumptionStock();
        
        // Produce steel
        factory.produce();
        
        // Should consume 5 units of iron to produce 10 units of steel (5:10 ratio)
        assertEquals(initialConsumption - 5, factory.getConsumptionStock());
        assertEquals(10, factory.getCurrentStock());
    }
    
    @Test
    void testLoadCargo() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        // Produce some iron
        mine.produce();
        mine.produce();
        assertEquals(20, mine.getCurrentStock());
        
        // Load 15 units
        int loaded = mine.loadCargo(15);
        
        assertEquals(15, loaded);
        assertEquals(5, mine.getCurrentStock());
    }
    
    @Test
    void testLoadCargo_MoreThanAvailable() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        mine.produce(); // 10 units
        
        // Try to load 20 units (more than available)
        int loaded = mine.loadCargo(20);
        
        assertEquals(10, loaded); // Should only load what's available
        assertEquals(0, mine.getCurrentStock());
    }
    
    @Test
    void testDeliverRawMaterial() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        int delivered = factory.deliverRawMaterial(CargoType.IRON, 25);
        
        assertEquals(25, delivered);
        assertEquals(25, factory.getConsumptionStock());
    }
    
    @Test
    void testDeliverRawMaterial_WrongType() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // Try to deliver wood to steel factory (needs iron)
        int delivered = factory.deliverRawMaterial(CargoType.WOOD, 25);
        
        assertEquals(0, delivered);
        assertEquals(0, factory.getConsumptionStock());
    }
    
    @Test
    void testConsumptionStockCapacity() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // Add more than capacity
        int delivered = factory.deliverRawMaterial(CargoType.IRON, 150);
        
        // Should not exceed capacity (100 units)
        assertEquals(100, delivered);
        assertEquals(100, factory.getConsumptionStock());
    }
    
    @Test
    void testFacilitySize() {
        IndustrialFacility mine = IndustrialFacility.createMine("Iron Mine");
        
        assertEquals(2, mine.getWidth());
        assertEquals(2, mine.getHeight());
    }
    
    @Test
    void testMultipleProductionCycles() {
        IndustrialFacility factory = IndustrialFacility.createSteelFactory("Steel Works");
        
        // Add enough iron for 3 production cycles
        factory.deliverRawMaterial(CargoType.IRON, 30);
        
        // Produce 3 times
        factory.produce();
        factory.produce();
        factory.produce();
        
        assertEquals(30, factory.getCurrentStock()); // 3 cycles × 10 units
        assertEquals(15, factory.getConsumptionStock()); // 30 - (3 × 5)
    }
    
    @Test
    void testCreateGarage() {
        IndustrialFacility garage = IndustrialFacility.createGarage("Main Garage");
        
        assertEquals("Main Garage", garage.getName());
        assertEquals(IndustrialFacility.FacilityType.GARAGE, garage.getFacilityType());
        assertNull(garage.getProduction());
        assertNull(garage.getConsumption());
    }
    
    @Test
    void testDeliverCargo() {
        IndustrialFacility factory = IndustrialFacility.createPaperFactory("Paper Factory");
        
        boolean delivered = factory.deliverCargo(CargoType.WOOD, 20);
        
        assertTrue(delivered);
        assertEquals(20, factory.getConsumptionStock());
    }
}
