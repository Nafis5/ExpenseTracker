package com.soltralabs.expensetracker;

import android.app.Application;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize AdMob
        AdManager.initialize(this);
        // Initialize PremiumManager
        PremiumManager.getInstance(this);
    }
} 