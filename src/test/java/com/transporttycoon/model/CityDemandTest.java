package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for City demand system - population-based demands and happiness
 */
class CityDemandTest {
    
    private City city;
    
    @BeforeEach
    void setUp() {
        city = new City("Test City", 100000);
    }
    
    @Test
    void testInitialDemands() {
        // Passenger demand: 1 per 50 people
        assertEquals(2000, city.getPassengerDemandMonthly());
        
        // Cargo demands should be calculated based on population
        assertTrue(city.getCargoDemandMonthly(CargoType.PAPER) > 0);
        assertTrue(city.getCargoDemandMonthly(CargoType.STEEL) > 0);
    }
    
    @Test
    void testInitialHappiness() {
        // Initial happiness should be 70% (0.7f)
        assertEquals(0.7f, city.getCityHappiness(), 0.01f);
    }
    
    @Test
    void testDeliverPassengers() {
        int initialDemand = city.getPassengerDemandMonthly();
        
        city.deliverPassengers(100);
        
        assertEquals(100, city.getPassengersDelivered());
    }
    
    @Test
    void testDeliverCargo() {
        city.deliverCargo(CargoType.PAPER, 500);
        
        assertEquals(500, city.getCargoDelivered(CargoType.PAPER));
    }
    
    @Test
    void testCannotDeliverMoreThanDemand() {
        int demand = city.getPassengerDemandMonthly();
        
        int delivered = city.deliverPassengers(demand + 1000);
        
        // Should only deliver up to demand
        assertEquals(demand, delivered);
        assertEquals(demand, city.getPassengersDelivered());
    }
    
    @Test
    void testHappinessCalculation_HighSatisfaction() {
        // Deliver 90% of passenger demand
        int demand = city.getPassengerDemandMonthly();
        city.deliverPassengers((int)(demand * 0.9));
        
        // Deliver 90% of cargo demands
        city.deliverCargo(CargoType.PAPER, (int)(city.getCargoDemandMonthly(CargoType.PAPER) * 0.9));
        city.deliverCargo(CargoType.STEEL, (int)(city.getCargoDemandMonthly(CargoType.STEEL) * 0.9));
        
        city.endMonth();
        
        // Happiness should be high (>0.7f = 70%)
        assertTrue(city.getCityHappiness() > 0.7f);
    }
    
    @Test
    void testHappinessCalculation_LowSatisfaction() {
        // Deliver only 20% of demands
        city.deliverPassengers((int)(city.getPassengerDemandMonthly() * 0.2));
        city.deliverCargo(CargoType.PAPER, (int)(city.getCargoDemandMonthly(CargoType.PAPER) * 0.2));
        city.deliverCargo(CargoType.STEEL, (int)(city.getCargoDemandMonthly(CargoType.STEEL) * 0.2));
        
        city.endMonth();
        
        // Happiness should decrease from initial 0.7f
        // With smoothing: 0.7 * 0.7 + 0.3 * 0.2 = 0.49 + 0.06 = 0.55
        assertTrue(city.getCityHappiness() < 0.7f);
        assertTrue(city.getCityHappiness() > 0.5f);
    }
    
    @Test
    void testPopulationGrowth_HighHappiness() {
        int initialPopulation = city.getPopulation();
        
        // Deliver most demands to increase happiness
        city.deliverPassengers((int)(city.getPassengerDemandMonthly() * 0.9));
        city.deliverCargo(CargoType.PAPER, (int)(city.getCargoDemandMonthly(CargoType.PAPER) * 0.9));
        city.deliverCargo(CargoType.STEEL, (int)(city.getCargoDemandMonthly(CargoType.STEEL) * 0.9));
        
        city.endMonth();
        
        // Population should grow with high happiness
        assertTrue(city.getPopulation() >= initialPopulation);
    }
    
    @Test
    void testPopulationDecline_LowHappiness() {
        int initialPopulation = city.getPopulation();
        
        // Deliver very little to decrease happiness
        city.deliverPassengers((int)(city.getPassengerDemandMonthly() * 0.1));
        
        city.endMonth();
        
        // Population should decline with low happiness
        assertTrue(city.getPopulation() <= initialPopulation);
    }
    
    @Test
    void testMonthlyReset() {
        // Deliver some cargo
        city.deliverPassengers(100);
        city.deliverCargo(CargoType.PAPER, 500);
        
        assertEquals(100, city.getPassengersDelivered());
        assertEquals(500, city.getCargoDelivered(CargoType.PAPER));
        
        // Reset for new month
        city.endMonth();
        
        assertEquals(0, city.getPassengersDelivered());
        assertEquals(0, city.getCargoDelivered(CargoType.PAPER));
    }
    
    @Test
    void testRecalculateDemands_AfterPopulationChange() {
        int initialPopulation = city.getPopulation();
        int initialDemand = city.getPassengerDemandMonthly();
        
        // Simulate population growth
        city.setPopulation(initialPopulation * 2);
        
        // Demands should roughly double
        assertTrue(city.getPassengerDemandMonthly() > initialDemand * 1.8);
    }
    
    @Test
    void testGetAllCargoDemands() {
        var demands = city.getAllCargoDemands();
        
        assertNotNull(demands);
        assertTrue(demands.size() > 0);
        assertTrue(demands.containsKey(CargoType.PAPER));
    }
    
    @Test
    void testGetDemands() {
        var demands = city.getDemands();
        
        assertNotNull(demands);
        assertTrue(demands.size() > 0);
    }
    
    @Test
    void testUpdate() {
        // Test that update doesn't crash
        city.update(1.0f);
        
        // City should still be functional
        assertTrue(city.getPopulation() > 0);
    }
    
    @Test
    void testFacilitySize() {
        assertEquals(3, city.getWidth());
        assertEquals(3, city.getHeight());
    }
    
    @Test
    void testDeliverCargo_ReturnsAcceptedAmount() {
        int demand = city.getCargoDemandMonthly(CargoType.PAPER);
        
        int accepted = city.deliverCargo(CargoType.PAPER, demand / 2);
        
        assertEquals(demand / 2, accepted);
    }
    
    @Test
    void testMultipleDeliveries() {
        city.deliverPassengers(50);
        city.deliverPassengers(50);
        city.deliverCargo(CargoType.PAPER, 100);
        city.deliverCargo(CargoType.PAPER, 200);
        
        assertEquals(100, city.getPassengersDelivered());
        assertEquals(300, city.getCargoDelivered(CargoType.PAPER));
    }
}
