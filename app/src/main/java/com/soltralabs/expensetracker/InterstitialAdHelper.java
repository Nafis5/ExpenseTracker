package com.soltralabs.expensetracker;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class InterstitialAdHelper {
    private static InterstitialAd mInterstitialAd;
    private static final String AD_UNIT_ID = "ca-app-pub-3103198316569371/5453363926"; // Test Ad Unit ID
    private static final String TAG = "InterstitialAdHelper";

    public static void loadAd(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    Log.i(TAG, "onAdLoaded");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.d(TAG, loadAdError.toString());
                    mInterstitialAd = null;
                }
            });
    }

    public static void showAd(Activity activity) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "The ad was dismissed.");
                    mInterstitialAd = null;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when ad fails to show.
                    Log.d(TAG, "The ad failed to show.");
                    mInterstitialAd = null;
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "The ad was shown.");
                }
            });
            mInterstitialAd.show(activity);
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
        }
    }
}
