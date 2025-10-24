package com.example.arangkada.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;
import com.example.arangkada.utils.LocaleHelper;

public class InfoActivity extends AppCompatActivity {

    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView catchphraseTextView;
    private TextView descriptionTextView;
    private Button getStartedButton;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
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


        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);


        finish();
    }

    @Override
    public void onBackPressed() {
        // Override back button to exit app instead of going to previous activity
        super.onBackPressed();
        finishAffinity(); // This will close the entire app
    }
}