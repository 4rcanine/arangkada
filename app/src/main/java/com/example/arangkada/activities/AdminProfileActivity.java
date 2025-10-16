package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.AdminActivity;
import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private CardView editProfileCard, notificationSettingsCard, helpSupportCard, aboutAppCard;
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
        notificationSettingsCard = findViewById(R.id.card_notification_settings);
        helpSupportCard = findViewById(R.id.card_help_support);
        aboutAppCard = findViewById(R.id.card_about_app);

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
        notificationSettingsCard.setOnClickListener(v -> openNotificationSettings());
        helpSupportCard.setOnClickListener(v -> openHelpSupport());
        aboutAppCard.setOnClickListener(v -> openAboutApp());
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void openEditProfile() {
        // Inflate dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_name);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_email);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_phone);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);

        // Pre-fill existing data
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

            // Update Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("number", newPhone);

            db.collection("accounts").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Update email in FirebaseAuth
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
        mAuth.signOut();
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AdminProfileActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AdminProfileActivity.this, AdminActivity.class);
        startActivity(intent);
        finish();
    }
}
