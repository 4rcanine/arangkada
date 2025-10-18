package com.example.arangkada.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.adapters.NotificationAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;
    private TextView tvHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        setupNavigation();
        showBackButton();

        recyclerView = findViewById(R.id.recycler_notifications);
        tvHeader = findViewById(R.id.tv_header);
        tvHeader.setText("Notifications");

        // ✅ Load existing notifications first
        notifications = loadNotifications();
        if (notifications == null) {
            notifications = new ArrayList<>();
        }

        // ✅ Setup RecyclerView and adapter BEFORE checking for new notifications
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notifications);
        recyclerView.setAdapter(adapter);

        // ✅ Now it's safe to check booking status (adapter is ready)
        checkBookingStatus();

        // ✅ Back button click
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onNavigationSetup() {
        // No navigation changes needed
    }

    private void checkBookingStatus() {
        SharedPreferences prefs = getSharedPreferences("BookingData", MODE_PRIVATE);
        String bookingStatus = prefs.getString("latest_booking_status", "none");

        if (!"none".equals(bookingStatus)) {
            String timestamp = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                    .format(new Date());

            NotificationItem newItem = null;

            switch (bookingStatus) {
                case "confirmed":
                    newItem = new NotificationItem("Booking Confirmed",
                            "Your booking has been confirmed!", timestamp, "confirmed");
                    NotificationHelper.showBookingNotification(this,
                            "Booking Confirmed",
                            "Your booking has been confirmed!",
                            "confirmed");
                    break;
                case "rejected":
                    newItem = new NotificationItem("Booking Rejected",
                            "Sorry, your booking was rejected.", timestamp, "rejected");
                    NotificationHelper.showBookingNotification(this,
                            "Booking Rejected",
                            "Sorry, your booking was rejected.",
                            "rejected");
                    break;
                case "changed":
                    newItem = new NotificationItem("Booking Updated",
                            "Your booking details have changed.", timestamp, "changed");
                    NotificationHelper.showBookingNotification(this,
                            "Booking Updated",
                            "Your booking details have changed.",
                            "changed");
                    break;
            }

            if (newItem != null) {
                // Add to top of list
                notifications.add(0, newItem);
                saveNotifications(notifications);
                adapter.notifyItemInserted(0);
            }

            // Reset status
            prefs.edit().putString("latest_booking_status", "none").apply();
        }
    }

    // ✅ Save notification list to SharedPreferences
    private void saveNotifications(List<NotificationItem> list) {
        SharedPreferences prefs = getSharedPreferences("NotificationStorage", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder sb = new StringBuilder();
        for (NotificationItem item : list) {
            sb.append(item.getTitle()).append("|")
                    .append(item.getMessage()).append("|")
                    .append(item.getTimestamp()).append("|")
                    .append(item.getType()).append(";");
        }

        editor.putString("notifications", sb.toString());
        editor.apply();
    }

    // ✅ Load saved notifications
    private List<NotificationItem> loadNotifications() {
        SharedPreferences prefs = getSharedPreferences("NotificationStorage", MODE_PRIVATE);
        String savedData = prefs.getString("notifications", "");

        List<NotificationItem> list = new ArrayList<>();
        if (!savedData.isEmpty()) {
            String[] items = savedData.split(";");
            for (String data : items) {
                String[] parts = data.split("\\|");
                if (parts.length >= 4) {
                    list.add(new NotificationItem(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        }
        return list;
    }
}
