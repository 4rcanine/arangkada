package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;

public class MyTripsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        // Back button logic
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // closes MyTripsActivity
        });
    }
}
