package com.example.arangkada.activities;

import android.os.Bundle;
import com.example.arangkada.R;

public class TermsAndConditionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_base);


        getLayoutInflater().inflate(R.layout.activity_terms_conditions,
                findViewById(R.id.content_frame), true);

        setupNavigation();
        showBackButton();
        setToolbarTitle("Terms & Conditions");
    }

    @Override
    protected void onNavigationSetup() {

    }
}
