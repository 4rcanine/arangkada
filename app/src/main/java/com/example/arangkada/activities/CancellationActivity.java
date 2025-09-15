package com.example.arangkada.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;

public class CancellationActivity extends AppCompatActivity {

    private TextView tripNumberText;
    private TextView fromLocationText;
    private TextView toLocationText;
    private TextView timeText;
    private TextView dateText;
    private TextView paymentTypeText;
    private TextView paymentMethodText;
    private TextView passengersText;
    private TextView totalPriceText;
    private TextView tripStatusText;
    private Button cancelRideButton;
    private Button backToTripsButton;
    private ImageView vanImage; // optional, if you still want a vehicle image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation);

        initializeViews();
        setupClickListeners();
        loadDummyData(); // replace later with actual Firestore or intent extras
    }

    private void initializeViews() {
        // Bind views from XML
        tripNumberText = findViewById(R.id.tv_trip_status); // renamed status
        fromLocationText = findViewById(R.id.fromLocationText);
        toLocationText = null; // Removed in new XML, route combined into one text
        timeText = null;       // Removed, we now have date+time together
        dateText = findViewById(R.id.dateText);
        paymentTypeText = null; // Removed, only "paymentMethodText" exists now
        paymentMethodText = findViewById(R.id.paymentMethodText);
        passengersText = null;  // Removed in new XML
        totalPriceText = findViewById(R.id.totalPriceText);
        tripStatusText = findViewById(R.id.tv_trip_status);
        cancelRideButton = findViewById(R.id.cancelRideButton);
        backToTripsButton = findViewById(R.id.btn_back_to_trips);
        vanImage = null; // removed, unless you add it to XML
    }

    private void setupClickListeners() {
        // Cancel Ride button
        cancelRideButton.setOnClickListener(v -> showCancelConfirmationDialog());

        // Back to Trips button
        backToTripsButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(CancellationActivity.this, MyTripsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this,
                        "MyTripsActivity not found. Please create MyTripsActivity.java",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadDummyData() {
        // In real app, get data from intent or Firestore
        fromLocationText.setText("Baguio Terminal → Cervantes");
        dateText.setText("Thu, Aug 17 - 5:00 AM");
        paymentMethodText.setText("Cash on hand");
        totalPriceText.setText("₱700.00");
        tripStatusText.setText("ACTIVE");
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel this ride?")
                .setPositiveButton("Yes", (DialogInterface dialog, int which) -> {
                    Toast.makeText(this, "Ride Cancelled", Toast.LENGTH_SHORT).show();
                    tripStatusText.setText("CANCELLED");
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
