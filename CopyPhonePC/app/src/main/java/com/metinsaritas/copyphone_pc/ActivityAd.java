package com.metinsaritas.copyphone_pc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class ActivityAd extends AppCompatActivity {

    private AdView adViewAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        MobileAds.initialize(getApplicationContext(),
                "ca-app-pub-1030319386229981~3757619445");

        adViewAd = (AdView) findViewById(R.id.adViewAd);
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewAd.loadAd(adRequest);

    }
}
