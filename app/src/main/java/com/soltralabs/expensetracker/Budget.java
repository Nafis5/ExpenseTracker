package com.soltralabs.expensetracker;

public class Budget {
    private int id;
    private String category;
    private double monthlyLimit;
    private String monthYear;

    public Budget() {
    }

    public Budget(int id, String category, double monthlyLimit, String monthYear) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthYear = monthYear;
    }

    public Budget(String category, double monthlyLimit, String monthYear) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.monthYear = monthYear;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }
} 