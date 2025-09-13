package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;

public class InfoActivity extends AppCompatActivity {

    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView catchphraseTextView;
    private TextView descriptionTextView;
    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        logoImageView = findViewById(R.id.iv_logo);
        appNameTextView = findViewById(R.id.tv_app_name);
        catchphraseTextView = findViewById(R.id.tv_catchphrase);
        descriptionTextView = findViewById(R.id.tv_description);
        getStartedButton = findViewById(R.id.btn_get_started);
    }

    private void setupClickListeners() {
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAuth();
            }
        });

        // Optional: Allow clicking on the logo or app name to proceed
        logoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAuth();
            }
        });

        appNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAuth();
            }
        });
    }

    private void navigateToAuth() {
        // Navigate to authentication activity (login/signup)
        Intent intent = new Intent(InfoActivity.this, AuthActivity.class);
        startActivity(intent);

        // Optional: Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Optional: Finish this activity so user can't go back with back button
        finish();
    }

    @Override
    public void onBackPressed() {
        // Override back button to exit app instead of going to previous activity
        super.onBackPressed();
        finishAffinity(); // This will close the entire app
    }
}