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

public class AuthActivity extends AppCompatActivity {

    // Views
    private LinearLayout loginLayout;
    private LinearLayout signupLayout;
    private TextView titleTextView;
    private TextView switchModeTextView;

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

    // State
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        initializeViews();
        setupClickListeners();
        showLoginMode();
    }

    private void initializeViews() {
        // Common Views
        titleTextView = findViewById(R.id.tv_title);
        switchModeTextView = findViewById(R.id.tv_switch_mode);
        loginLayout = findViewById(R.id.layout_login);
        signupLayout = findViewById(R.id.layout_signup);

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
    }

    private void setupClickListeners() {
        // Switch between login and signup
        switchModeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAuthMode();
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
                performSignup();
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

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            showLoginMode();
        } else {
            showSignupMode();
        }
        clearFields();
    }

    private void showLoginMode() {
        isLoginMode = true;
        titleTextView.setText(R.string.login_title);
        loginLayout.setVisibility(View.VISIBLE);
        signupLayout.setVisibility(View.GONE);
        switchModeTextView.setText(R.string.switch_to_signup);
    }

    private void showSignupMode() {
        isLoginMode = false;
        titleTextView.setText(R.string.signup_title);
        loginLayout.setVisibility(View.GONE);
        signupLayout.setVisibility(View.VISIBLE);
        switchModeTextView.setText(R.string.switch_to_login);
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
    }

    private void performLogin() {
        String email = loginEmailEditText.getText().toString().trim();
        String password = loginPasswordEditText.getText().toString().trim();

        // Basic validation
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

        // Dummy login logic - Replace with actual authentication
        if (isValidDummyCredentials(email, password)) {
            Toast.makeText(this, "Login successful! Welcome back.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(this, "Invalid credentials. Try: user@test.com / password123", Toast.LENGTH_LONG).show();
        }
    }

    private void performSignup() {
        String fullName = signupFullNameEditText.getText().toString().trim();
        String email = signupEmailEditText.getText().toString().trim();
        String phone = signupPhoneEditText.getText().toString().trim();
        String password = signupPasswordEditText.getText().toString().trim();
        String confirmPassword = signupConfirmPasswordEditText.getText().toString().trim();

        // Basic validation
        if (fullName.isEmpty()) {
            signupFullNameEditText.setError("Full name is required");
            signupFullNameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            signupEmailEditText.setError("Email is required");
            signupEmailEditText.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            signupEmailEditText.setError("Please enter a valid email");
            signupEmailEditText.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            signupPhoneEditText.setError("Phone number is required");
            signupPhoneEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            signupPasswordEditText.setError("Password is required");
            signupPasswordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            signupPasswordEditText.setError("Password must be at least 6 characters");
            signupPasswordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            signupConfirmPasswordEditText.setError("Passwords do not match");
            signupConfirmPasswordEditText.requestFocus();
            return;
        }

        // Dummy signup logic - Replace with actual registration
        Toast.makeText(this, "Account created successfully! Welcome " + fullName + "!", Toast.LENGTH_SHORT).show();
        navigateToMainActivity();
    }

    private boolean isValidDummyCredentials(String email, String password) {
        // Dummy validation - accept any email with password "password123"
        return password.equals("password123") && email.contains("@");
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
        // Go back to InfoActivity
        Intent intent = new Intent(AuthActivity.this, InfoActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}