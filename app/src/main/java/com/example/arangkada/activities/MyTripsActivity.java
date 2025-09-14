package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;

public class MyTripsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);


        // "My trips" text logic (go to CancellationActivity)
        TextView myTripsText = findViewById(R.id.myTripsText); // <-- Make sure this ID exists
        myTripsText.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, CancellationActivity.class);
            startActivity(intent);
            finish(); // closes MyTripsActivity
        });
    }
}
