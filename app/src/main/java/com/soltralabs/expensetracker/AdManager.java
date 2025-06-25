package com.soltralabs.expensetracker;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class AdManager {
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3103198316569371/9864790801"; // Test ID

    public static void initialize(Context context) {
        MobileAds.initialize(context, initializationStatus -> {});
    }

    public static void loadBannerAd(Context context, LinearLayout adContainer) {
        if (PremiumManager.getInstance(context).shouldShowAds()) {
            adContainer.setVisibility(View.VISIBLE);
            AdView adView = new AdView(context);
            adView.setAdUnitId(BANNER_AD_UNIT_ID);
            adView.setAdSize(AdSize.BANNER);
            adContainer.addView(adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            adContainer.setVisibility(View.GONE);
        }
    }
} 