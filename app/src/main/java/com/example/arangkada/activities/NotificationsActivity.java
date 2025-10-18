package com.example.arangkada.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;

import com.example.arangkada.R;

public class NotificationsActivity extends BaseActivity {

    private TextView tvNotificationMessage;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        setupNavigation();
        showBackButton();
        setToolbarTitle("Notifications");

        tvNotificationMessage = findViewById(R.id.tv_notification_message);

        // 🔹 Check permission first (for Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION
            );
        } else {
            checkBookingStatus();
        }
    }

    @Override
    protected void onNavigationSetup() {
        // No extra setup for navigation
    }

    private void checkBookingStatus() {
        SharedPreferences prefs = getSharedPreferences("BookingData", MODE_PRIVATE);
        String bookingStatus = prefs.getString("latest_booking_status", "none");

        if (bookingStatus.equals("confirmed")) {
            tvNotificationMessage.setText("✅ Your booking has been confirmed!");
            NotificationHelper.showBookingNotification(
                    this,
                    "Booking Confirmed",
                    "Your UV Express ride has been confirmed!"
            );
        } else if (bookingStatus.equals("rejected")) {
            tvNotificationMessage.setText("❌ Sorry, your booking was rejected.");
            NotificationHelper.showBookingNotification(
                    this,
                    "Booking Rejected",
                    "Sorry, your booking was rejected. Please try another schedule."
            );
        } else {
            tvNotificationMessage.setText("No new notifications at the moment.");
        }

        // Reset to avoid duplicate alerts
        prefs.edit().putString("latest_booking_status", "none").apply();
    }

    // 🔹 Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                checkBookingStatus();
            } else {
                Toast.makeText(this, "Notifications permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
