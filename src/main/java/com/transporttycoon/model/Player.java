package com.transporttycoon.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the player with capital management, ledger tracking, and bankruptcy detection
 */
public class Player {
    private double capital;
    private String name;
    private List<Transaction> ledger;
    private double monthlyIncome;
    private double monthlyExpenses;
    private static final double BANKRUPTCY_THRESHOLD = 0.0;
    
    public Player(double initialCapital) {
        this.capital = initialCapital;
        this.name = "Player";
        this.ledger = new ArrayList<>();
        this.monthlyIncome = 0;
        this.monthlyExpenses = 0;
        // Starting capital is set directly, not added through transaction
        ledger.add(new Transaction("Starting Capital", initialCapital, TransactionType.INCOME, System.currentTimeMillis()));
    }
    
    public Player(String name, double initialCapital) {
        this.name = name;
        this.capital = initialCapital;
        this.ledger = new ArrayList<>();
        this.monthlyIncome = 0;
        this.monthlyExpenses = 0;
        // Starting capital is set directly, not added through transaction
        ledger.add(new Transaction("Starting Capital", initialCapital, TransactionType.INCOME, System.currentTimeMillis()));
    }
    
    /**
     * Records a transaction in the ledger
     */
    public void recordTransaction(String description, double amount, TransactionType type) {
        ledger.add(new Transaction(description, amount, type, System.currentTimeMillis()));
        
        if (type == TransactionType.INCOME) {
            monthlyIncome += amount;
            capital += amount;
        } else {
            monthlyExpenses += amount;
            capital -= amount;
        }
    }
    
    /**
     * Adds funds to the player's capital
     */
    public void addFunds(double amount) {
        if (amount > 0) {
            recordTransaction("Income", amount, TransactionType.INCOME);
        }
    }
    
    /**
     * Deducts funds without adding a ledger entry.
     * Used for high-frequency per-tick costs (e.g. vehicle maintenance) to
     * prevent the ledger from growing to millions of entries over a long game.
     * Monthly totals are still tracked via {@code monthlyExpenses}.
     */
    public boolean deductFundsQuiet(double amount) {
        if (amount <= 0) return false;
        monthlyExpenses += amount;
        capital -= amount;
        return true;
    }

    /**
     * Deducts funds from the player's capital
     * @param amount Amount to deduct
     * @return true if transaction was recorded
     */
    public boolean deductFunds(double amount) {
        if (amount <= 0) {
            return false;
        }
        
        recordTransaction("Expense", amount, TransactionType.EXPENSE);
        return true;
    }
    
    /**
     * Checks if the player is bankrupt
     */
    public boolean isBankrupt() {
        return capital <= BANKRUPTCY_THRESHOLD;
    }
    
    /**
     * Resets monthly income/expenses for a new month
     */
    public void resetMonthlyStats() {
        monthlyIncome = 0;
        monthlyExpenses = 0;
    }
    
    /**
     * Gets the net profit for the current month
     */
    public double getMonthlyProfit() {
        return monthlyIncome - monthlyExpenses;
    }
    
    /**
     * Gets recent transactions (last N transactions)
     */
    public List<Transaction> getRecentTransactions(int count) {
        int startIndex = Math.max(0, ledger.size() - count);
        return new ArrayList<>(ledger.subList(startIndex, ledger.size()));
    }
    
    // Getters
    public double getCapital() {
        return capital;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getMonthlyIncome() {
        return monthlyIncome;
    }
    
    public double getMonthlyExpenses() {
        return monthlyExpenses;
    }
    
    public List<Transaction> getLedger() {
        return new ArrayList<>(ledger);
    }
    
    /**
     * Directly sets capital without recording a transaction.
     * Used by PersistenceManager when restoring a saved game.
     */
    public void setCapitalDirect(double capital) {
        this.capital = capital;
    }

    /**
     * Formats the capital as a currency string
     */
    public String getFormattedCapital() {
        return String.format("$%.2f", capital);
    }
    
    @Override
    public String toString() {
        return String.format("Player '%s' - Capital: %s (Income: %.2f, Expenses: %.2f)", 
                           name, getFormattedCapital(), monthlyIncome, monthlyExpenses);
    }
    
    /**
     * Inner class representing a transaction
     */
    public static class Transaction {
        public String description;
        public double amount;
        public TransactionType type;
        public long timestamp;
        
        public Transaction(String description, double amount, TransactionType type, long timestamp) {
            this.description = description;
            this.amount = amount;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: $%.2f", type, description, amount);
        }
    }
    
    /**
     * Enum for transaction types
     */
    public enum TransactionType {
        INCOME, EXPENSE
    }
}