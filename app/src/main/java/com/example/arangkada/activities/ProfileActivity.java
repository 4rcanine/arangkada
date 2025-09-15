package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private TextView totalTripsCount, favoriteRoute, memberSince;
    private CardView editProfileCard, bookingHistoryCard, paymentMethodsCard, notificationSettingsCard, helpSupportCard, aboutAppCard;
    private Button logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserData(); // <-- Load from Firestore
        setupClickListeners();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.iv_profile_image);
        userName = findViewById(R.id.tv_user_name);
        userEmail = findViewById(R.id.tv_user_email);
        userPhone = findViewById(R.id.tv_user_phone);

        editProfileCard = findViewById(R.id.card_edit_profile);
        bookingHistoryCard = findViewById(R.id.card_booking_history);
        paymentMethodsCard = findViewById(R.id.card_payment_methods);
        notificationSettingsCard = findViewById(R.id.card_notification_settings);
        helpSupportCard = findViewById(R.id.card_help_support);
        aboutAppCard = findViewById(R.id.card_about_app);

        totalTripsCount = findViewById(R.id.tv_total_trips);
        favoriteRoute = findViewById(R.id.tv_favorite_route);
        memberSince = findViewById(R.id.tv_member_since);

        logoutButton = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("accounts").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String number = documentSnapshot.getString("number");

                            userName.setText(name != null ? name : "Unknown");
                            userEmail.setText(email != null ? email : "No email");
                            userPhone.setText(number != null ? number : "No number");

                            // Dummy stats (replace later if you store them in Firestore)
                            totalTripsCount.setText("15");
                            favoriteRoute.setText("Cervantes → Baguio");
                            memberSince.setText("December 2024");
                        } else {
                            Toast.makeText(ProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupClickListeners() {
        editProfileCard.setOnClickListener(v -> openEditProfile());
        bookingHistoryCard.setOnClickListener(v -> openBookingHistory());
        paymentMethodsCard.setOnClickListener(v -> openPaymentMethods());
        notificationSettingsCard.setOnClickListener(v -> openNotificationSettings());
        helpSupportCard.setOnClickListener(v -> openHelpSupport());
        aboutAppCard.setOnClickListener(v -> openAboutApp());
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void openEditProfile() {
        Toast.makeText(this, "Edit Profile - Coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openBookingHistory() {
        Toast.makeText(this, "Booking history coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openPaymentMethods() {
        Toast.makeText(this, "Payment methods coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openNotificationSettings() {
        Toast.makeText(this, "Notification settings coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openHelpSupport() {
        Toast.makeText(this, "Help & Support coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openAboutApp() {
        Toast.makeText(this, "About App coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void performLogout() {
        // Sign out from FirebaseAuth
        mAuth.signOut();

        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Redirect to InfoActivity (or AuthActivity if you prefer login screen)
        Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
