package com.example.arangkada.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.arangkada.R;

public class TermsAndConditionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        setupNavigation();

        // Inflate our custom content into BaseActivity’s content_frame
        ViewGroup contentFrame = findViewById(R.id.content_frame);
        View inflated = LayoutInflater.from(this).inflate(R.layout.activity_terms_conditions, contentFrame, false);
        contentFrame.removeAllViews();
        contentFrame.addView(inflated);

        onNavigationSetup();
    }

    @Override
    protected void onNavigationSetup() {
        showBackButton();
        setToolbarTitle("Terms & Conditions");
    }
}
