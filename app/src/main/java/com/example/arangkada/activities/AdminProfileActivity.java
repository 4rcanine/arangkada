package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.AdminActivity;
import com.example.arangkada.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private CardView editProfileCard, termsServicesCard;
    private Button logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

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
        termsServicesCard = findViewById(R.id.card_terms_services);
        logoutButton = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

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
                        Toast.makeText(AdminProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AdminProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupClickListeners() {
        editProfileCard.setOnClickListener(v -> openEditProfile());
        termsServicesCard.setOnClickListener(v -> openTermsAndServicesDialog());
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void openEditProfile() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_name);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_email);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_phone);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);

        etName.setText(userName.getText().toString());
        etEmail.setText(userEmail.getText().toString());
        etPhone.setText(userPhone.getText().toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            String userId = currentUser.getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("number", newPhone);

            db.collection("accounts").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.updateEmail(newEmail)
                                .addOnSuccessListener(aVoid1 -> {
                                    if (!newPassword.isEmpty()) {
                                        currentUser.updatePassword(newPassword)
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                                    loadUserData();
                                                    dialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                    } else {
                                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        loadUserData();
                                        dialog.dismiss();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void openTermsAndServicesDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminProfileActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AdminProfileActivity.this, AdminActivity.class);
        startActivity(intent);
        finish();
    }
}
