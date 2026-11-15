package com.transporttycoon.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the economic system - income, expenses, and transactions
 */
class EconomyTest {
    
    private Player player;
    
    @BeforeEach
    void setUp() {
        player = new Player(50000.0);
    }
    
    @Test
    void testInitialCapital() {
        assertEquals(50000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testAddFunds() {
        player.addFunds(1000.0);
        
        assertEquals(51000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testDeductFunds() {
        player.deductFunds(5000.0);
        
        assertEquals(45000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testRecordIncome() {
        player.recordTransaction("Cargo delivery", 500.0, Player.TransactionType.INCOME);
        
        assertEquals(50500.0, player.getCapital(), 0.01);
        assertEquals(500.0, player.getMonthlyIncome(), 0.01);
    }
    
    @Test
    void testRecordExpense() {
        player.recordTransaction("Vehicle purchase", 5000.0, Player.TransactionType.EXPENSE);
        
        assertEquals(45000.0, player.getCapital(), 0.01);
        assertEquals(5000.0, player.getMonthlyExpenses(), 0.01);
    }
    
    @Test
    void testMonthlyProfit_Positive() {
        player.recordTransaction("Delivery 1", 1000.0, Player.TransactionType.INCOME);
        player.recordTransaction("Delivery 2", 1500.0, Player.TransactionType.INCOME);
        player.recordTransaction("Maintenance", 500.0, Player.TransactionType.EXPENSE);
        
        double profit = player.getMonthlyProfit();
        
        assertEquals(2000.0, profit, 0.01); // 2500 income - 500 expense
    }
    
    @Test
    void testMonthlyProfit_Negative() {
        player.recordTransaction("Delivery", 500.0, Player.TransactionType.INCOME);
        player.recordTransaction("Vehicle purchase", 5000.0, Player.TransactionType.EXPENSE);
        player.recordTransaction("Road building", 2000.0, Player.TransactionType.EXPENSE);
        
        double profit = player.getMonthlyProfit();
        
        assertEquals(-6500.0, profit, 0.01); // 500 - 7000
    }
    
    @Test
    void testResetMonthlyStats() {
        player.recordTransaction("Delivery", 1000.0, Player.TransactionType.INCOME);
        player.recordTransaction("Expense", 500.0, Player.TransactionType.EXPENSE);
        
        assertEquals(1000.0, player.getMonthlyIncome(), 0.01);
        assertEquals(500.0, player.getMonthlyExpenses(), 0.01);
        
        player.resetMonthlyStats();
        
        assertEquals(0.0, player.getMonthlyIncome(), 0.01);
        assertEquals(0.0, player.getMonthlyExpenses(), 0.01);
    }
    
    @Test
    void testBankruptcy() {
        player.deductFunds(55000.0); // More than available
        
        assertTrue(player.isBankrupt());
        assertTrue(player.getCapital() < 0);
    }
    
    @Test
    void testNotBankrupt() {
        player.deductFunds(10000.0);
        
        assertFalse(player.isBankrupt());
        assertTrue(player.getCapital() > 0);
    }
    
    @Test
    void testRecentTransactions() {
        player.recordTransaction("Transaction 1", 100.0, Player.TransactionType.INCOME);
        player.recordTransaction("Transaction 2", 200.0, Player.TransactionType.EXPENSE);
        player.recordTransaction("Transaction 3", 300.0, Player.TransactionType.INCOME);
        
        var recent = player.getRecentTransactions(3);
        
        assertEquals(3, recent.size());
    }
    
    @Test
    void testTransactionLedger() {
        player.recordTransaction("Income 1", 1000.0, Player.TransactionType.INCOME);
        player.recordTransaction("Expense 1", 500.0, Player.TransactionType.EXPENSE);
        
        var ledger = player.getLedger();
        
        assertTrue(ledger.size() >= 2);
    }
    
    @Test
    void testStandardDeliveryIncome() {
        // Standard delivery: $10 per unit for cargo
        int units = 50;
        double income = units * 10.0;
        
        player.recordTransaction("Standard delivery", income, Player.TransactionType.INCOME);
        
        assertEquals(50500.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testBonusDeliveryIncome() {
        // Bonus delivery: $20 per unit (2x multiplier)
        int units = 50;
        double income = units * 20.0;
        
        player.recordTransaction("BONUS delivery", income, Player.TransactionType.INCOME);
        
        assertEquals(51000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testVehiclePurchaseCost() {
        // Truck costs $5,000
        double cost = 5000.0;
        
        player.recordTransaction("Buy Truck", cost, Player.TransactionType.EXPENSE);
        
        assertEquals(45000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testRoadBuildingCost() {
        // Road costs $100 per tile
        int tiles = 20;
        double cost = tiles * 100.0;
        
        player.recordTransaction("Build road", cost, Player.TransactionType.EXPENSE);
        
        assertEquals(48000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testStopPlacementCost() {
        // Stop costs $500
        double cost = 500.0;
        
        player.recordTransaction("Place stop", cost, Player.TransactionType.EXPENSE);
        
        assertEquals(49500.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testMaintenanceCost() {
        // Truck maintenance: $20 per tick
        double cost = 20.0;
        
        player.recordTransaction("Maintenance", cost, Player.TransactionType.EXPENSE);
        
        assertEquals(49980.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testMultipleVehiclesMaintenance() {
        // 3 trucks, each $20/tick
        for (int i = 0; i < 3; i++) {
            player.recordTransaction("Maintenance: Vehicle-" + i, 20.0, Player.TransactionType.EXPENSE);
        }
        
        assertEquals(49940.0, player.getCapital(), 0.01);
        assertEquals(60.0, player.getMonthlyExpenses(), 0.01);
    }
    
    @Test
    void testProfitability_SingleRoute() {
        // Simulate one month of operation with one truck
        // Income: 20 deliveries × $1000 = $20,000
        for (int i = 0; i < 20; i++) {
            player.recordTransaction("Delivery", 1000.0, Player.TransactionType.INCOME);
        }
        
        // Expenses: Maintenance $1,200
        player.recordTransaction("Monthly maintenance", 1200.0, Player.TransactionType.EXPENSE);
        
        double profit = player.getMonthlyProfit();
        
        assertEquals(18800.0, profit, 0.01);
        assertTrue(profit > 0); // Profitable!
    }
    
    @Test
    void testPlayerName() {
        Player namedPlayer = new Player("John", 10000.0);
        
        assertEquals("John", namedPlayer.getName());
        assertEquals(10000.0, namedPlayer.getCapital(), 0.01);
    }
    
    @Test
    void testSetCapitalDirect() {
        player.setCapitalDirect(75000.0);
        
        assertEquals(75000.0, player.getCapital(), 0.01);
    }
    
    @Test
    void testFormattedCapital() {
        String formatted = player.getFormattedCapital();
        
        assertTrue(formatted.contains("50000"));
        assertTrue(formatted.startsWith("$"));
    }
    
    @Test
    void testTransactionToString() {
        Player.Transaction transaction = new Player.Transaction(
            "Test", 100.0, Player.TransactionType.INCOME, System.currentTimeMillis()
        );
        
        String str = transaction.toString();
        
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("100"));
    }
}
