package com.example.arangkada;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.arangkada.activities.BookRideActivity;
import com.example.arangkada.activities.CancellationActivity;
import com.example.arangkada.activities.InfoActivity;
import com.example.arangkada.activities.BookingActivity;
import com.example.arangkada.activities.ProfileActivity;
import com.example.arangkada.activities.BaseActivity;
import com.example.arangkada.activities.MyTripsActivity;
import com.example.arangkada.activities.NotificationsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

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

        // Wire BaseActivity's drawer/navigation into this activity
        setupNavigation();
        onNavigationSetup();

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

        cervantesToBaguioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                intent.putExtra("from_location", "Cervantes");
                intent.putExtra("to_location", "Baguio");
                startActivity(intent);
            }
        });

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
            userNameTextView.setText("Guest");
        }
    }

    private void openBookingPage() {
        Intent intent = new Intent(MainActivity.this, BookRideActivity.class);
        startActivity(intent);
    }

    private void openMyTrips() {
        Intent intent = new Intent(MainActivity.this, CancellationActivity.class);
        startActivity(intent);
    }

    private void openNotifications() {
        // Clear badge when user checks notifications
        notificationBadge.setVisibility(View.GONE);

        // Open the notifications activity
        Intent intent = new Intent(this, NotificationsActivity.class);
        startActivity(intent);
    }

    private void openProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void quickBookRide(String from, String to) {
        Toast.makeText(this, "Quick booking: " + from + " → " + to + "\nSearching available rides...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
    }

    // -----------------------------------------------------------------------------------------
    // Implement required abstract hook from BaseActivity
    // -----------------------------------------------------------------------------------------
    @Override
    protected void onNavigationSetup() {
        // Show menu (hamburger) button
        showMenuButton();
        // ❌ No toolbar title here → Dashboard stays clean
    }
}
