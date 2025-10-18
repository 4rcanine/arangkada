package com.example.arangkada.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.adapters.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;
    private TextView tvHeader;
    private FrameLayout btnClear; // ✅ Correct type

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        setupNavigation();
        showBackButton();

        recyclerView = findViewById(R.id.recycler_notifications);
        tvHeader = findViewById(R.id.tv_header);
        btnClear = findViewById(R.id.btn_clear_notifications);
        tvHeader.setText("Notifications");


        notifications = loadNotifications();


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notifications);
        recyclerView.setAdapter(adapter);


        checkBookingStatus();


        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());


        btnClear.setOnClickListener(v -> {
            if (notifications.isEmpty()) {
                Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show();
                return;
            }
            clearAllNotifications();
            Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onNavigationSetup() {
        // No navigation changes needed
    }


    private void checkBookingStatus() {
        SharedPreferences prefs = getSharedPreferences("BookingData", MODE_PRIVATE);
        String bookingStatus = prefs.getString("latest_booking_status", "none");

        if (!"none".equals(bookingStatus)) {

            prefs.edit().putString("latest_booking_status", "none").apply();
        }
    }


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


    private void clearAllNotifications() {
        notifications.clear();
        adapter.notifyDataSetChanged();

        SharedPreferences prefs = getSharedPreferences("NotificationStorage", MODE_PRIVATE);
        prefs.edit().remove("notifications").apply();
    }
}
