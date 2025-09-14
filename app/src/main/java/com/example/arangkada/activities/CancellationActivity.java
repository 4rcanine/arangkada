package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.arangkada.MainActivity;
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
    private Button cancelRideButton;
    private ImageView vanImage;
    private TextView historyText; // History button


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation);

        // Initialize views
        initializeViews();

        // Set hardcoded data
        setHardcodedData();

        // Set click listeners
        setupClickListeners();


    }

    private void initializeViews() {
        tripNumberText = findViewById(R.id.tripNumberText);
        fromLocationText = findViewById(R.id.fromLocationText);
        toLocationText = findViewById(R.id.toLocationText);
        timeText = findViewById(R.id.timeText);
        dateText = findViewById(R.id.dateText);
        paymentTypeText = findViewById(R.id.paymentTypeText);
        paymentMethodText = findViewById(R.id.paymentMethodText);
        passengersText = findViewById(R.id.passengersText);
        totalPriceText = findViewById(R.id.totalPriceText);
        cancelRideButton = findViewById(R.id.cancelRideButton);
        vanImage = findViewById(R.id.vanImage);
        historyText = findViewById(R.id.historyText); // Initialize history text
    }

    private void setHardcodedData() {
        tripNumberText.setText("1");
        fromLocationText.setText("Baguio Terminal");
        toLocationText.setText("Baguio");
        timeText.setText("5:00 - 6:00 AM");
        dateText.setText("Thu, Aug 17");
        paymentTypeText.setText("Type of Payment:");
        paymentMethodText.setText("Cash on hand");
        passengersText.setText("Regular (2)");
        totalPriceText.setText("₱ 700.00");

        // Replace with your van image
        vanImage.setImageResource(R.drawable.ic_van);
    }

    private void setupClickListeners() {
        // Cancel ride button
        if (cancelRideButton != null) {
            cancelRideButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCancellationDialog();
                }
            });
        }

        // History button
        if (historyText != null) {
            historyText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToMyTrips();
                }
            });
        }
    }

    private void showCancellationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Ride");
        builder.setMessage("Are you sure you want to cancel this ride? This action cannot be undone.");

        builder.setPositiveButton("Yes, Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelRide();
            }
        });

        builder.setNegativeButton("No, Keep Ride", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.red_accent));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.teal_primary));
    }

    private void cancelRide() {
        Toast.makeText(this, "Ride cancelled successfully", Toast.LENGTH_SHORT).show();
        finish(); // close activity
    }

    private void navigateToMyTrips() {
        try {
            Intent intent = new Intent(this, MyTripsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "MyTripsActivity not found. Please create MyTripsActivity.java",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
