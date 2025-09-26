package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    // Views
    private LinearLayout loginLayout;
    private LinearLayout signupLayout;
    private LinearLayout adminSignupLayout;
    private TextView titleTextView;
    private TextView switchModeTextView;
    private TextView switchToAdminTextView;

    // Login Views
    private EditText loginEmailEditText;
    private EditText loginPasswordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;

    // Signup Views
    private EditText signupFullNameEditText;
    private EditText signupEmailEditText;
    private EditText signupPhoneEditText;
    private EditText signupPasswordEditText;
    private EditText signupConfirmPasswordEditText;
    private Button signupButton;

    // Admin Signup Views
    private EditText adminFullNameEditText;
    private EditText adminEmailEditText;
    private EditText adminPhoneEditText;
    private EditText adminPasswordEditText;
    private EditText adminConfirmPasswordEditText;
    private Button adminSignupButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // State
    private int currentMode = 0; // 0 = login, 1 = signup, 2 = admin signup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        initializeViews();
        setupClickListeners();
        showLoginMode();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        // Common Views
        titleTextView = findViewById(R.id.tv_title);
        switchModeTextView = findViewById(R.id.tv_switch_mode);
        switchToAdminTextView = findViewById(R.id.tv_switch_to_admin);
        loginLayout = findViewById(R.id.layout_login);
        signupLayout = findViewById(R.id.layout_signup);
        adminSignupLayout = findViewById(R.id.layout_admin_signup);

        // Login Views
        loginEmailEditText = findViewById(R.id.et_login_email);
        loginPasswordEditText = findViewById(R.id.et_login_password);
        loginButton = findViewById(R.id.btn_login);
        forgotPasswordTextView = findViewById(R.id.tv_forgot_password);

        // Signup Views
        signupFullNameEditText = findViewById(R.id.et_signup_fullname);
        signupEmailEditText = findViewById(R.id.et_signup_email);
        signupPhoneEditText = findViewById(R.id.et_signup_phone);
        signupPasswordEditText = findViewById(R.id.et_signup_password);
        signupConfirmPasswordEditText = findViewById(R.id.et_signup_confirm_password);
        signupButton = findViewById(R.id.btn_signup);

        // Admin Signup Views
        adminFullNameEditText = findViewById(R.id.et_admin_fullname);
        adminEmailEditText = findViewById(R.id.et_admin_email);
        adminPhoneEditText = findViewById(R.id.et_admin_phone);
        adminPasswordEditText = findViewById(R.id.et_admin_password);
        adminConfirmPasswordEditText = findViewById(R.id.et_admin_confirm_password);
        adminSignupButton = findViewById(R.id.btn_admin_signup);
    }

    private void setupClickListeners() {
        // Switch between modes
        switchModeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });

        switchToAdminTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdminSignupMode();
            }
        });

        // Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // Signup button
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignup(false);
            }
        });

        // Admin Signup button
        adminSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignup(true);
            }
        });

        // Forgot password
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void toggleMode() {
        if (currentMode == 0) {
            showSignupMode();
        } else {
            showLoginMode();
        }
        clearFields();
    }

    private void showLoginMode() {
        currentMode = 0;
        titleTextView.setText("Welcome Back");
        loginLayout.setVisibility(View.VISIBLE);
        signupLayout.setVisibility(View.GONE);
        adminSignupLayout.setVisibility(View.GONE);
        switchModeTextView.setText("Don't have an account? Sign up");
        switchToAdminTextView.setVisibility(View.VISIBLE);
    }

    private void showSignupMode() {
        currentMode = 1;
        titleTextView.setText("Create Account");
        loginLayout.setVisibility(View.GONE);
        signupLayout.setVisibility(View.VISIBLE);
        adminSignupLayout.setVisibility(View.GONE);
        switchModeTextView.setText("Already have an account? Log in");
        switchToAdminTextView.setVisibility(View.VISIBLE);
    }

    private void showAdminSignupMode() {
        currentMode = 2;
        titleTextView.setText("Admin Registration");
        loginLayout.setVisibility(View.GONE);
        signupLayout.setVisibility(View.GONE);
        adminSignupLayout.setVisibility(View.VISIBLE);
        switchModeTextView.setText("Back to regular signup");
        switchToAdminTextView.setVisibility(View.GONE);
    }

    private void clearFields() {
        // Clear login fields
        loginEmailEditText.setText("");
        loginPasswordEditText.setText("");

        // Clear signup fields
        signupFullNameEditText.setText("");
        signupEmailEditText.setText("");
        signupPhoneEditText.setText("");
        signupPasswordEditText.setText("");
        signupConfirmPasswordEditText.setText("");

        // Clear admin signup fields
        adminFullNameEditText.setText("");
        adminEmailEditText.setText("");
        adminPhoneEditText.setText("");
        adminPasswordEditText.setText("");
        adminConfirmPasswordEditText.setText("");
    }

    private void performLogin() {
        String email = loginEmailEditText.getText().toString().trim();
        String password = loginPasswordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            loginEmailEditText.setError("Email is required");
            loginEmailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            loginPasswordEditText.setError("Password is required");
            loginPasswordEditText.requestFocus();
            return;
        }

        // Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(AuthActivity.this, "Login successful! Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(AuthActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performSignup(boolean isAdmin) {
        String fullName, email, phone, password, confirmPassword;

        if (isAdmin) {
            fullName = adminFullNameEditText.getText().toString().trim();
            email = adminEmailEditText.getText().toString().trim();
            phone = adminPhoneEditText.getText().toString().trim();
            password = adminPasswordEditText.getText().toString().trim();
            confirmPassword = adminConfirmPasswordEditText.getText().toString().trim();
        } else {
            fullName = signupFullNameEditText.getText().toString().trim();
            email = signupEmailEditText.getText().toString().trim();
            phone = signupPhoneEditText.getText().toString().trim();
            password = signupPasswordEditText.getText().toString().trim();
            confirmPassword = signupConfirmPasswordEditText.getText().toString().trim();
        }

        // Validation
        if (fullName.isEmpty()) {
            setError(isAdmin ? adminFullNameEditText : signupFullNameEditText, "Full name is required");
            return;
        }
        if (email.isEmpty()) {
            setError(isAdmin ? adminEmailEditText : signupEmailEditText, "Email is required");
            return;
        }
        if (!isValidEmail(email)) {
            setError(isAdmin ? adminEmailEditText : signupEmailEditText, "Please enter a valid email");
            return;
        }

        // Admin email validation
        if (isAdmin && !email.contains("@admin")) {
            setError(adminEmailEditText, "Admin email must contain '@admin'");
            return;
        }

        if (phone.isEmpty()) {
            setError(isAdmin ? adminPhoneEditText : signupPhoneEditText, "Phone number is required");
            return;
        }
        if (password.isEmpty()) {
            setError(isAdmin ? adminPasswordEditText : signupPasswordEditText, "Password is required");
            return;
        }
        if (password.length() < 6) {
            setError(isAdmin ? adminPasswordEditText : signupPasswordEditText, "Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            setError(isAdmin ? adminConfirmPasswordEditText : signupConfirmPasswordEditText, "Passwords do not match");
            return;
        }

        // Create Firebase Authentication user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(AuthActivity.this, "Signup failed: user is null", Toast.LENGTH_LONG).show();
                            return;
                        }

                        final String uid = user.getUid();

                        // Prepare user data to save in Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", uid);
                        userData.put("name", fullName);
                        userData.put("email", email);
                        userData.put("number", phone);
                        userData.put("userType", isAdmin ? "admin" : "user");
                        userData.put("isAdmin", isAdmin);

                        // Choose collection based on user type
                        String collection = isAdmin ? "admins" : "accounts";

                        db.collection(collection)
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    String welcomeMessage = isAdmin ?
                                            "Admin account created! Welcome " + fullName :
                                            "Account created! Welcome " + fullName;
                                    Toast.makeText(AuthActivity.this, welcomeMessage, Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AuthActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                    if (mAuth.getCurrentUser() != null) {
                                        mAuth.getCurrentUser().delete()
                                                .addOnCompleteListener(delTask -> {
                                                    if (delTask.isSuccessful()) {
                                                        Toast.makeText(AuthActivity.this, "Cleaned up partially-created account due to error.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });

                    } else {
                        Toast.makeText(AuthActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showForgotPasswordDialog() {
        Toast.makeText(this, "Forgot password feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentMode == 2) {
            showSignupMode();
        } else if (currentMode == 1) {
            showLoginMode();
        } else {
            Intent intent = new Intent(AuthActivity.this, InfoActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }
    }
}