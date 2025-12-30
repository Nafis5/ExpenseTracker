package com.soltralabs.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;

public class PremiumManager {
    private static PremiumManager instance;
    private boolean isPremium = false;
    private static final String PREFS_NAME = "premium_prefs";
    private static final String IS_PREMIUM_KEY = "is_premium";

    private PremiumManager(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isPremium = prefs.getBoolean(IS_PREMIUM_KEY, false);
    }

    public static synchronized PremiumManager getInstance(Context context) {
        if (instance == null) {
            instance = new PremiumManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isPremium() {
        return isPremium;

    }

    public void setIsPremium(boolean premium, Context context) {

        this.isPremium = premium;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(IS_PREMIUM_KEY, premium);
        editor.apply();
    }

    public boolean canAddTransaction(int currentTransactionCount) {
        if (isPremium) {
            return true;
        }
        return currentTransactionCount < 50;
    }

    public boolean canViewHistoricalData() {
        return isPremium;


    }

    public boolean shouldShowAds() {
       // return !isPremium;
        return true;
    }
} 