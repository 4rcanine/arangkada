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
import com.example.arangkada.activities.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            DocumentReference docRef = db.collection("accounts").document(userId);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("name");
                        userNameTextView.setText(name);
                    } else {
                        userNameTextView.setText("Unknown User");
                    }
                } else {
                    userNameTextView.setText("Error loading user");
                }
            });
        } else {
            // No user is logged in
            userNameTextView.setText("Guest");
        }
    }

    private void openBookingPage() {
        Toast.makeText(this, "Opening booking page...", Toast.LENGTH_SHORT).show();
        // If you want this to open BookingActivity, uncomment:
        // Intent intent = new Intent(this, BookingActivity.class);
        // startActivity(intent);
    }

    private void openMyTrips() {
        // Open MyTripsActivity
        Intent intent = new Intent(MainActivity.this, com.example.arangkada.activities.CancellationActivity.class);
        startActivity(intent);
    }



    private void openNotifications() {
        Toast.makeText(this, "2 new notifications", Toast.LENGTH_SHORT).show();
        // Hide notification badge when opened
        notificationBadge.setVisibility(View.GONE);
        // TODO: Navigate to NotificationsActivity
    }

    private void openProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void quickBookRide(String from, String to) {
        Toast.makeText(this, "Quick booking: " + from + " → " + to + "\nSearching available rides...", Toast.LENGTH_LONG).show();
        // TODO: Implement quick booking logic
    }



    @Override
    public void onBackPressed() {
        // Show confirmation dialog before exiting app
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        // TODO: Implement proper back button handling with exit confirmation
    }
}
