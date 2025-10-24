package com.example.arangkada;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.arangkada.activities.AdminCancellationActivity;
import com.example.arangkada.activities.AdminProfileActivity;
import com.example.arangkada.activities.AuthActivity;
import com.example.arangkada.activities.BaseActivity;
import com.example.arangkada.activities.CurrentVanScheduleActivity;
import com.example.arangkada.activities.ManageReservationsActivity;
import com.example.arangkada.activities.ManageVansActivity;
import com.example.arangkada.activities.NewTerminalActivity;
import com.example.arangkada.activities.ProfileActivity;
import com.example.arangkada.activities.QRScannerActivity;
import com.example.arangkada.activities.UserManagementActivity;
import com.bumptech.glide.Glide;  // Add this import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends BaseActivity {

    private CardView cardReservations, cardSchedule, cardTerminals, cardUsers, cardCancelled, cardQR, cardSettings;
    private TextView tvAdminName;
    private ImageView adminProfileImageView;  // Add this
    private Button btnMakeAdmin;

    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Setup navigation FIRST
        setupNavigation();
        onNavigationSetup();

        initializeViews();
        setupClickListeners();

        // Firestore init
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadAdminInfo(user.getUid());  // Changed from loadAdminName
        }

        // Set card UI
        setAdminCard(cardReservations, R.drawable.ic_reservations, getString(R.string.card_manage_reservations));
        setAdminCard(cardSchedule, R.drawable.ic_cancelled, getString(R.string.card_manage_van_schedules));
        setAdminCard(cardTerminals, R.drawable.ic_terminal, getString(R.string.card_manage_terminals));
        setAdminCard(cardUsers, R.drawable.ic_users, getString(R.string.card_manage_all_users));
        setAdminCard(cardCancelled, R.drawable.ic_schedule, getString(R.string.card_booking_history));
        setAdminCard(cardQR, R.drawable.ic_qr, getString(R.string.card_qr_scanner));
        setAdminCard(cardSettings, R.drawable.ic_settings, getString(R.string.card_settings));
    }

    private void initializeViews() {
        cardReservations = findViewById(R.id.card_reservations);
        cardSchedule = findViewById(R.id.card_schedule);
        cardTerminals = findViewById(R.id.card_terminals);
        cardUsers = findViewById(R.id.card_users);
        cardCancelled = findViewById(R.id.card_cancelled);
        cardQR = findViewById(R.id.card_qr);
        cardSettings = findViewById(R.id.card_settings);

        tvAdminName = findViewById(R.id.tv_admin_name);
        adminProfileImageView = findViewById(R.id.iv_admin_profile_image);  // Add this
        btnMakeAdmin = findViewById(R.id.btn_make_admin);

        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupClickListeners() {
        cardReservations.setOnClickListener(v -> startActivity(new Intent(this, ManageReservationsActivity.class)));
        cardSchedule.setOnClickListener(v -> startActivity(new Intent(this, CurrentVanScheduleActivity.class)));
        cardTerminals.setOnClickListener(v -> startActivity(new Intent(this, NewTerminalActivity.class)));
        cardUsers.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        cardCancelled.setOnClickListener(v -> startActivity(new Intent(this, AdminCancellationActivity.class)));
        cardQR.setOnClickListener(v -> startActivity(new Intent(this, QRScannerActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, AdminProfileActivity.class)));

        // Make profile image clickable to go to settings/profile
        adminProfileImageView.setOnClickListener(v -> startActivity(new Intent(this, AdminProfileActivity.class)));

        btnMakeAdmin.setOnClickListener(v -> showCreateAdminDialog());
    }

    // Add this new method to load both name and profile picture
    private void loadAdminInfo(String userId) {
        db.collection("accounts").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Load name
                        String name = document.getString("name");
                        if (name != null) {
                            tvAdminName.setText(name);
                        } else {
                            tvAdminName.setText("Admin");
                        }

                        // Load profile picture
                        String profilePicture = document.getString("profilePicture");
                        if (profilePicture != null && !profilePicture.isEmpty()) {
                            loadProfileImage(profilePicture);
                        } else {
                            adminProfileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    } else {
                        tvAdminName.setText("Admin");
                        adminProfileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    tvAdminName.setText("Admin");
                    adminProfileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                });
    }

    // Add this method to load profile image using Glide
    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(adminProfileImageView);
    }

    private void showCreateAdminDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_admin, null);

        EditText etFullName = dialogView.findViewById(R.id.et_admin_fullname);
        EditText etEmail = dialogView.findViewById(R.id.et_admin_email);
        EditText etPhone = dialogView.findViewById(R.id.et_admin_phone);
        EditText etPassword = dialogView.findViewById(R.id.et_admin_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_admin_confirm_password);
        Button btnCreate = dialogView.findViewById(R.id.btn_create_admin);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_admin);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCreate.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validation
            if (fullName.isEmpty()) {
                etFullName.setError(getString(R.string.full_name_required));
                etFullName.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError(getString(R.string.email_required));
                etEmail.requestFocus();
                return;
            }
            if (!isValidEmail(email)) {
                etEmail.setError(getString(R.string.valid_email_required));
                etEmail.requestFocus();
                return;
            }
            if (!email.contains("@admin")) {
                etEmail.setError(getString(R.string.admin_email_must_contain));
                etEmail.requestFocus();
                return;
            }
            if (phone.isEmpty()) {
                etPhone.setError(getString(R.string.phone_required));
                etPhone.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError(getString(R.string.password_required));
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError(getString(R.string.password_min_length));
                etPassword.requestFocus();
                return;
            }
            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError(getString(R.string.passwords_do_not_match));
                etConfirmPassword.requestFocus();
                return;
            }

            // Create admin account
            createAdminAccount(fullName, email, phone, password, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void createAdminAccount(String fullName, String email, String phone, String password, AlertDialog dialog) {
        // Store current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Create new admin account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser newAdmin = mAuth.getCurrentUser();
                        if (newAdmin == null) {
                            Toast.makeText(this, "Failed to create admin account", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String uid = newAdmin.getUid();

                        Map<String, Object> adminData = new HashMap<>();
                        adminData.put("userId", uid);
                        adminData.put("name", fullName);
                        adminData.put("email", email);
                        adminData.put("number", phone);
                        adminData.put("userType", "admin");
                        adminData.put("isAdmin", true);

                        // Save to accounts collection
                        db.collection("accounts")
                                .document(uid)
                                .set(adminData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Admin account created successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();

                                    // Sign back in as the original admin
                                    if (currentUser != null) {
                                        mAuth.updateCurrentUser(currentUser)
                                                .addOnFailureListener(e -> {
                                                    // If re-authentication fails, force logout
                                                    Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
                                                    mAuth.signOut();
                                                    Intent intent = new Intent(AdminActivity.this, AuthActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    finish();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save admin data: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                    // Delete the partially created account
                                    if (newAdmin != null) {
                                        newAdmin.delete()
                                                .addOnCompleteListener(delTask -> {
                                                    if (delTask.isSuccessful()) {
                                                        Toast.makeText(this, "Cleaned up partially-created account", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }

                                    // Re-authenticate original admin
                                    if (currentUser != null) {
                                        mAuth.updateCurrentUser(currentUser);
                                    }
                                });

                    } else {
                        Toast.makeText(this, "Failed to create account: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    @Override
    protected void onNavigationSetup() {
        // Show menu button for admin dashboard (main screen)
        showMenuButton();
        // Optionally set custom title
        setToolbarTitle(getString(R.string.admin_dashboard));
    }

    private void setAdminCard(View cardView, int iconRes, String title) {
        ImageView icon = cardView.findViewById(R.id.admin_icon);
        TextView text = cardView.findViewById(R.id.admin_title);

        icon.setImageResource(iconRes);
        text.setText(title);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
    }
}