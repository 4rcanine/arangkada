package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.*;

public class ProfileActivity extends BaseActivity {

    private static final int PICK_PROFILE_IMAGE = 301;
    private static final String TAG = "ProfileActivity";

    private ImageView profileImage;
    private TextView userName, userEmail, userPhone;
    private CardView editProfileCard, bookingHistoryCard, notificationSettingsCard, helpSupportCard;
    private Button logoutButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentProfilePictureUrl = null;
    private Uri selectedImageUri = null;

    // ImageKit Configuration
    private static final String IMAGEKIT_PUBLIC_KEY = "public_aM1dq8aVaA7PBiP8Pdfo6mYpUsM=";
    private static final String IMAGEKIT_PRIVATE_KEY = "private_xix6Ergz3zAHuAwotsM7a+4WsdU=";
    private static final String IMAGEKIT_UPLOAD_URL = "https://upload.imagekit.io/api/v1/files/upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        getLayoutInflater().inflate(R.layout.activity_profile,
                findViewById(R.id.content_frame), true);
        setupNavigation();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    @Override
    protected void onNavigationSetup() {
        showBackButton();
        setToolbarTitle("Profile");
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
        progressBar = findViewById(R.id.progressBar);
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
                        String profilePicture = documentSnapshot.getString("profilePicture");

                        userName.setText(name != null ? name : "Unknown");
                        userEmail.setText(email != null ? email : "No email");
                        userPhone.setText(number != null ? number : "No number");

                        // Load profile picture or show placeholder
                        currentProfilePictureUrl = profilePicture;
                        if (profilePicture != null && !profilePicture.isEmpty()) {
                            loadImageWithGlide(profilePicture, profileImage);
                        } else {
                            // Explicitly set placeholder when no profile picture exists
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupClickListeners() {
        // Profile image click listener
        profileImage.setOnClickListener(v -> showProfilePictureDialog());

        editProfileCard.setOnClickListener(v -> openEditProfile());
        bookingHistoryCard.setOnClickListener(v -> openBookingHistory());
        notificationSettingsCard.setOnClickListener(v -> openNotificationSettings());
        helpSupportCard.setOnClickListener(v -> openHelpSupport());
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void showProfilePictureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Picture");

        String[] options;
        if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty()) {
            options = new String[]{"View Profile Picture", "Change Profile Picture", "Remove Profile Picture"};
        } else {
            options = new String[]{"Add Profile Picture"};
        }

        builder.setItems(options, (dialog, which) -> {
            if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty()) {
                switch (which) {
                    case 0: // View
                        showFullScreenImage(currentProfilePictureUrl);
                        break;
                    case 1: // Change
                        pickProfileImage();
                        break;
                    case 2: // Remove
                        removeProfilePicture();
                        break;
                }
            } else {
                // Add
                pickProfileImage();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PROFILE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PROFILE_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            uploadProfilePictureToImageKit(selectedImageUri);
        }
    }

    private void uploadProfilePictureToImageKit(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                inputStream.close();

                String token = generateToken();
                long expire = getExpireTimestamp();
                String signature = generateSignature(token, expire);

                if (signature == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to generate signature", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName,
                                RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .addFormDataPart("fileName", fileName)
                        .addFormDataPart("publicKey", IMAGEKIT_PUBLIC_KEY)
                        .addFormDataPart("signature", signature)
                        .addFormDataPart("expire", String.valueOf(expire))
                        .addFormDataPart("token", token)
                        .addFormDataPart("folder", "profile_pictures")
                        .addFormDataPart("useUniqueFileName", "true")
                        .build();

                Request request = new Request.Builder()
                        .url(IMAGEKIT_UPLOAD_URL)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    String uploadedUrl = parseUrlFromResponse(responseBody);

                    if (uploadedUrl != null) {
                        saveProfilePictureToFirestore(uploadedUrl);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to parse image URL", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + errorBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Upload error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void saveProfilePictureToFirestore(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        String userId = user.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePicture", imageUrl);

        db.collection("accounts").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        currentProfilePictureUrl = imageUrl;
                        loadImageWithGlide(imageUrl, profileImage);
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to save profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    });
                });
    }

    private void removeProfilePicture() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Profile Picture")
                .setMessage("Are you sure you want to remove your profile picture?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) return;

                    progressBar.setVisibility(View.VISIBLE);
                    String userId = user.getUid();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profilePicture", null);

                    db.collection("accounts").document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                currentProfilePictureUrl = null;
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                                Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to remove profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ImageKit Helper Methods
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private long getExpireTimestamp() {
        return (System.currentTimeMillis() / 1000) + 3600;
    }

    private String generateSignature(String token, long expire) {
        try {
            String stringToSign = token + expire;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret_key = new SecretKeySpec(IMAGEKIT_PRIVATE_KEY.getBytes("UTF-8"), "HmacSHA1");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(stringToSign.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error generating signature", e);
            return null;
        }
    }

    private String parseUrlFromResponse(String jsonResponse) {
        try {
            int urlStart = jsonResponse.indexOf("\"url\":\"") + 7;
            int urlEnd = jsonResponse.indexOf("\"", urlStart);
            return jsonResponse.substring(urlStart, urlEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL from response", e);
            return null;
        }
    }

    private void loadImageWithGlide(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(imageView);
        }
    }

    private void showFullScreenImage(String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);

        ImageView imgFullScreen = dialogView.findViewById(R.id.imgFullScreen);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(imgFullScreen);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        imgFullScreen.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
        Intent intent = new Intent(this, MyTripsActivity.class);
        startActivity(intent);
    }

    private void openNotificationSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notification Settings");
        builder.setMessage("Would you like to receive notifications from Arangkada?");

        builder.setPositiveButton("Turn On", (dialog, which) -> {
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("notifications_enabled", true)
                    .apply();

            Toast.makeText(this, "Notifications turned on ✅", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setNegativeButton("Turn Off", (dialog, which) -> {
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("notifications_enabled", false)
                    .apply();

            Toast.makeText(this, "Notifications turned off 🚫", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.create().show();
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
                "This agreement contains the terms and conditions (hereinafter, the Terms and Conditions) that govern access and use of the mobile application (ARANGKADA) that are either referenced or linked to the Terms and Conditions.\n\n" +
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
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}