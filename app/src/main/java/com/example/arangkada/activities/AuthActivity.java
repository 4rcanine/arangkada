package com.example.arangkada.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.AdminActivity;
import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
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

        switchModeTextView.setOnClickListener(v -> toggleMode());

        switchToAdminTextView.setOnClickListener(v -> showAdminSignupMode());

        // Login
        loginButton.setOnClickListener(v -> performLogin());

        // Signup - show Terms dialog first (always)
        signupButton.setOnClickListener(v -> showTermsDialog(false));

        // Admin signup - show Terms dialog first
        adminSignupButton.setOnClickListener(v -> showTermsDialog(true));

        // Forgot password
        forgotPasswordTextView.setOnClickListener(v -> showForgotPasswordDialog());
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
                        if (user != null) {
                            checkUserType(user.getUid());
                        }
                    } else {
                        Toast.makeText(AuthActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Terms dialog that always shows before any signup.
     * Has a clickable link to open the full Terms activity.
     */
    private void showTermsDialog(boolean isAdmin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Terms and Conditions");
        title.setTextSize(18f);
        title.setPadding(0, 0, 0, 12);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        // Short message + scrollable preview
        ScrollView sv = new ScrollView(this);
        LinearLayout svInner = new LinearLayout(this);
        svInner.setOrientation(LinearLayout.VERTICAL);
        svInner.setPadding(0, 6, 0, 6);

        TextView preview = new TextView(this);
        preview.setText("By signing up you agree to Arangkada's Terms & Conditions. Please read them before proceeding.");
        preview.setTextSize(14f);
        preview.setLineSpacing(1.1f, 1.1f);
        svInner.addView(preview);
        sv.addView(svInner);

        root.addView(sv);

        // Link to open full terms
        TextView readFull = new TextView(this);
        readFull.setText("📄 Read full Terms and Conditions");
        readFull.setTextColor(getResources().getColor(R.color.purple_500));
        readFull.setPadding(0, 12, 0, 12);
        readFull.setOnClickListener(v -> {
            // Open the activity that displays full T&C
            startActivity(new Intent(AuthActivity.this, TermsAndConditionsActivity.class));
        });
        root.addView(readFull);

        // Checkbox to require explicit accept
        CheckBox cb = new CheckBox(this);
        cb.setText("I agree to these terms and accept");
        root.addView(cb);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_HORIZONTAL);
        btnRow.setPadding(0, 18, 0, 0);

        Button decline = new Button(this);
        decline.setText("Decline");
        styleRoundedButton(decline, 0xFF888888); // gray

        Button accept = new Button(this);
        accept.setText("Accept");
        styleRoundedButton(accept, getResources().getColor(R.color.purple_500)); // primary

        // layout params so they share the row
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(12, 0, 12, 0);
        decline.setLayoutParams(p);
        accept.setLayoutParams(p);

        btnRow.addView(decline);
        btnRow.addView(accept);
        root.addView(btnRow);

        builder.setView(root);
        AlertDialog dialog = builder.create();

        decline.setOnClickListener(v -> {
            dialog.dismiss();
            // user declined — simply return to signup screen
            Toast.makeText(AuthActivity.this, "You must accept Terms and Conditions to create an account.", Toast.LENGTH_SHORT).show();
        });

        accept.setOnClickListener(v -> {
            if (!cb.isChecked()) {
                Toast.makeText(AuthActivity.this, "Please check the box to proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            // proceed with original signup flow
            performSignup(isAdmin);
        });

        dialog.show();
    }

    private void styleRoundedButton(Button btn, int colorInt) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(24f * getResources().getDisplayMetrics().density);
        gd.setColor(colorInt);
        btn.setBackground(gd);
        btn.setTextColor(getResources().getColor(android.R.color.white));
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

        // Validation (keeps your original checks)
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

        // Firebase create user (as original)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(AuthActivity.this, "Signup failed: user is null", Toast.LENGTH_LONG).show();
                            return;
                        }

                        final String uid = user.getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", uid);
                        userData.put("name", fullName);
                        userData.put("email", email);
                        userData.put("number", phone);
                        userData.put("userType", isAdmin ? "admin" : "user");
                        userData.put("isAdmin", isAdmin);

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
        // Original working implementation preserved exactly
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email address to receive a password reset link.");

        // Create an EditText for email input
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Email Address");

        // Add padding to the EditText
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);

        // Create a container for the EditText
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("Send Reset Link", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String email = input.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(AuthActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidEmail(email)) {
                    Toast.makeText(AuthActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send password reset email
                sendPasswordResetEmail(email);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this,
                                    "Password reset email sent! Please check your inbox.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Unknown error";
                            Toast.makeText(AuthActivity.this,
                                    "Failed to send reset email: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
            checkUserType(currentUser.getUid()); // 🔹 ensure redirect works after reopening app
        }
    }

    private void checkUserType(String userId) {
        db.collection("accounts").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        if (isAdmin != null && isAdmin) {
                            // Navigate to Admin Dashboard
                            Intent intent = new Intent(AuthActivity.this, AdminActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                            finish();
                        } else {
                            // Navigate to User Dashboard
                            navigateToMainActivity();
                        }
                    } else {
                        Toast.makeText(AuthActivity.this, "User record not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AuthActivity.this, "Error checking user type: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
