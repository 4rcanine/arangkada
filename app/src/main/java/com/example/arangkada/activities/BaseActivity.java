package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.arangkada.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView menuButton;
    protected ImageView backButton;
    protected LinearLayout navigationContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    protected void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        menuButton = findViewById(R.id.btn_menu);
        backButton = findViewById(R.id.btn_back);
        navigationContainer = findViewById(R.id.navigation_container);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (menuButton != null) {
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.END);
                    }
                }
            });
        }

        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }

    protected void showBackButton() {
        if (backButton != null && menuButton != null) {
            backButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.GONE);
        }
    }

    protected void showMenuButton() {
        if (backButton != null && menuButton != null) {
            backButton.setVisibility(View.GONE);
            menuButton.setVisibility(View.VISIBLE);
        }
    }

    // helper
    protected void setToolbarTitle(String title) {
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(title);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_notifications) {
            openNotifications();
        } else if (id == R.id.nav_language) {
            openLanguageSettings();
        } else if (id == R.id.nav_terms) {
            openTermsAndConditions();
        } else if (id == R.id.nav_delete_account) {
            showDeleteAccountConfirmation();
        } else if (id == R.id.nav_logout) {
            performLogout();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
        return true;
    }

    private void openNotifications() {
        Toast.makeText(this, "This feature is coming soon!", Toast.LENGTH_SHORT).show();

    }

    private void openLanguageSettings() {
        Toast.makeText(this, "Language Settings: English/Filipino - Coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openTermsAndConditions() {
        Intent intent = new Intent(this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performAccountDeletion();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performAccountDeletion() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Show progress
        Toast.makeText(this, "Deleting account...", Toast.LENGTH_SHORT).show();

        // First, delete Firestore document
        db.collection("accounts").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Firestore document deleted successfully, now delete auth account
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(BaseActivity.this,
                                            "Account deleted successfully",
                                            Toast.LENGTH_SHORT).show();

                                    // Redirect to auth activity
                                    Intent intent = new Intent(BaseActivity.this, AuthActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    finish();
                                } else {
                                    Toast.makeText(BaseActivity.this,
                                            "Failed to delete account: " + task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BaseActivity.this,
                            "Failed to delete account data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void performLogout() {
        if (mAuth != null) {
            mAuth.signOut();
        }

        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }


    protected abstract void onNavigationSetup();
}