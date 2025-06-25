package com.soltralabs.expensetracker;

public class UserPreferences {
    private int id;
    private boolean isPremium;
    private double monthlyIncome;
    private int transactionCount;

    public UserPreferences() {
    }

    public UserPreferences(int id, boolean isPremium, double monthlyIncome, int transactionCount) {
        this.id = id;
        this.isPremium = isPremium;
        this.monthlyIncome = monthlyIncome;
        this.transactionCount = transactionCount;
    }
    
    public UserPreferences(boolean isPremium, double monthlyIncome, int transactionCount) {
        this.isPremium = isPremium;
        this.monthlyIncome = monthlyIncome;
        this.transactionCount = transactionCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }
} 