package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private CardView editProfileCard, bookingHistoryCard, notificationSettingsCard, helpSupportCard;
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
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.iv_profile_image);
        userName = findViewById(R.id.tv_user_name);
        userEmail = findViewById(R.id.tv_user_email);
        userPhone = findViewById(R.id.tv_user_phone);

        editProfileCard = findViewById(R.id.card_edit_profile);
        bookingHistoryCard = findViewById(R.id.card_booking_history);
        notificationSettingsCard = findViewById(R.id.card_notification_settings);
        helpSupportCard = findViewById(R.id.card_help_support);
        logoutButton = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("accounts").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String number = documentSnapshot.getString("number");

                        userName.setText(name != null ? name : "Unknown");
                        userEmail.setText(email != null ? email : "No email");
                        userPhone.setText(number != null ? number : "No number");
                    } else {
                        Toast.makeText(ProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupClickListeners() {
        editProfileCard.setOnClickListener(v -> openEditProfile());
        bookingHistoryCard.setOnClickListener(v -> openBookingHistory());
        notificationSettingsCard.setOnClickListener(v -> openNotificationSettings());
        helpSupportCard.setOnClickListener(v -> openHelpSupport());
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void openEditProfile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etEmail = dialogView.findViewById(R.id.et_email);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        EditText etPassword = dialogView.findViewById(R.id.et_password);

        // Pre-fill current values
        etName.setText(userName.getText());
        etEmail.setText(userEmail.getText());
        etPhone.setText(userPhone.getText());

        AlertDialog dialog = builder.create();

        // Create Save button programmatically
        Button btnSave = new Button(this);
        btnSave.setText("Save");
        btnSave.setAllCaps(false);
        btnSave.setBackgroundColor(getResources().getColor(R.color.purple_500));
        btnSave.setTextColor(getResources().getColor(android.R.color.white));

    // Add the button to the root layout
        ((ViewGroup) dialogView).addView(btnSave);


        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) return;

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("number", newPhone);

            // Update Firestore
            db.collection("accounts").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        userName.setText(newName);
                        userEmail.setText(newEmail);
                        userPhone.setText(newPhone);
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());

            // Update Firebase Auth
            if (!newEmail.equals(currentUser.getEmail())) {
                currentUser.updateEmail(newEmail)
                        .addOnFailureListener(e -> Toast.makeText(this, "Email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            if (!newPassword.isEmpty()) {
                currentUser.updatePassword(newPassword)
                        .addOnFailureListener(e -> Toast.makeText(this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            dialog.dismiss();
        });

        dialog.show();
    }


    private void openBookingHistory() {
        Toast.makeText(this, "Booking history coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openNotificationSettings() {
        Toast.makeText(this, "Notification settings coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openHelpSupport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions");

        // Create a scrollable TextView
        TextView message = new TextView(this);
        message.setPadding(50, 30, 50, 30);
        message.setTextSize(14f);
        message.setTextColor(getResources().getColor(android.R.color.black));
        message.setText(
                "This agreement contains the terms and conditions (hereinafter, the “Terms and Conditions”) that govern access and use of the mobile application (ARANGKADA) that are either referenced or linked to the Terms and Conditions.\n\n" +
                        "Any individual who wishes to access or use the mobile application may do so subject to the Terms and Conditions. Anyone who does not accept the Terms and Conditions, which are binding and mandatory, should refrain from using the mobile application.\n\n" +
                        "These Terms and Conditions represent a data message as defined in RA 8792 (E-Commerce Act). Such data messages are generated by a computer system and do not require any physical or digital signature.\n\n" +
                        "Definitions:\n\n" +
                        "Transportation Contract: This refers to the agreement between the User and the Van Operator for the provision of van transportation services, established upon successful reservation.\n\n" +
                        "Payment Facilities: Payment is required upon confirmation of reservation through the application or directly at the terminal, depending on the setup of the Van Operator.\n\n" +
                        "Van Operator: The company or authorized individual responsible for providing van services. This operator is recognized by law to transport passengers and is a party to the Transportation Contract with the User.\n\n" +
                        "Fare/Ticket: This is the official travel document issued by the Van Operator upon confirmed reservation. It includes trip details and proof of payment. The app allows the User to view upcoming and previous reservations.\n\n" +
                        "Person: This may refer to an individual, organization, association, government unit, or other entity utilizing the Arangkada system to make a reservation or enter a contract with the Van Operator.\n\n" +
                        "Transaction: The process carried out via the mobile application to secure a reservation or complete a payment, whether digitally or physically at the terminal, in compliance with the Transportation Contract.\n\n" +
                        "User: Any individual who makes a reservation or performs any transaction using the Arangkada mobile application."
        );

        // Make it scrollable
        final ScrollView scrollView = new ScrollView(this);
        scrollView.addView(message);

        builder.setView(scrollView);

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}
