package com.example.arangkada;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.activities.InfoActivity;
import com.example.arangkada.activities.BookingActivity;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private TextView userNameTextView;
    private CardView bookRideCard;
    private CardView myTripsCard;
    private CardView notificationsCard;
    private CardView profileCard;
    private LinearLayout quickBookLayout;
    private Button cervantesToBaguioButton;
    private Button baguioToCervantesButton;
    private Button logoutButton;
    private ImageView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        setupUserInfo();
    }

    private void initializeViews() {
        welcomeTextView = findViewById(R.id.tv_welcome);
        userNameTextView = findViewById(R.id.tv_user_name);
        bookRideCard = findViewById(R.id.card_book_ride);
        myTripsCard = findViewById(R.id.card_my_trips);
        notificationsCard = findViewById(R.id.card_notifications);
        profileCard = findViewById(R.id.card_profile);
        quickBookLayout = findViewById(R.id.layout_quick_book);
        cervantesToBaguioButton = findViewById(R.id.btn_cervantes_to_baguio);
        baguioToCervantesButton = findViewById(R.id.btn_baguio_to_cervantes);
        logoutButton = findViewById(R.id.btn_logout);
        notificationBadge = findViewById(R.id.iv_notification_badge);
    }

    private void setupClickListeners() {
        // Main feature cards
        bookRideCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBookingPage();
            }
        });

        myTripsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMyTrips();
            }
        });

        notificationsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotifications();
            }
        });

        profileCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile();
            }
        });

        // Cervantes -> Baguio button (starts BookingActivity with route extras)
        cervantesToBaguioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                intent.putExtra("from_location", "Cervantes");
                intent.putExtra("to_location", "Baguio");
                startActivity(intent);
            }
        });

        // Baguio -> Cervantes button (starts BookingActivity with route extras)
        baguioToCervantesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                intent.putExtra("from_location", "Baguio");
                intent.putExtra("to_location", "Cervantes");
                startActivity(intent);
            }
        });

        // Logout button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void setupUserInfo() {
        // In a real app, you'd get this from SharedPreferences or user session
        userNameTextView.setText("Matthew Dela Cruz");

        // Show notification badge (dummy data)
        notificationBadge.setVisibility(View.VISIBLE);
    }

    private void openBookingPage() {
        Toast.makeText(this, "Opening booking page...", Toast.LENGTH_SHORT).show();
        // If you want this to open BookingActivity, uncomment:
        // Intent intent = new Intent(this, BookingActivity.class);
        // startActivity(intent);
    }

    private void openMyTrips() {
        Toast.makeText(this, "My trips: 3 upcoming, 12 completed", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to TripsActivity
    }

    private void openNotifications() {
        Toast.makeText(this, "2 new notifications", Toast.LENGTH_SHORT).show();
        // Hide notification badge when opened
        notificationBadge.setVisibility(View.GONE);
        // TODO: Navigate to NotificationsActivity
    }

    private void openProfile() {
        Toast.makeText(this, "Opening profile...", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to ProfileActivity
    }

    private void quickBookRide(String from, String to) {
        Toast.makeText(this, "Quick booking: " + from + " → " + to + "\nSearching available rides...", Toast.LENGTH_LONG).show();
        // TODO: Implement quick booking logic
    }

    private void logout() {
        // Clear any stored user data here (SharedPreferences, etc.)
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Navigate back to InfoActivity
        Intent intent = new Intent(MainActivity.this, InfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog before exiting app
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        // TODO: Implement proper back button handling with exit confirmation
    }
}
