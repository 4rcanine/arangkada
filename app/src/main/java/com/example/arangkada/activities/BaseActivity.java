package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.arangkada.R;
import com.example.arangkada.utils.LocaleHelper;
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
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
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, R.string.no_user_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Check if user is admin
        db.collection("accounts").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean isAdmin = document.getBoolean("isAdmin");

                        if (isAdmin != null && isAdmin) {
                            // User is admin - show toast
                            Toast.makeText(this, "This is a User Account Feature", Toast.LENGTH_SHORT).show();
                        } else {
                            // User is not admin - open NotificationsActivity
                            Intent intent = new Intent(this, NotificationsActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        // Document doesn't exist - assume regular user
                        Intent intent = new Intent(this, NotificationsActivity.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking user type: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openLanguageSettings() {
        String currentLanguage = LocaleHelper.getLanguage(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.language_settings);

        // Language options
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_filipino)
        };

        int checkedItem = currentLanguage.equals("fil") ? 1 : 0;

        builder.setSingleChoiceItems(languages, checkedItem, null);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
            String selectedLanguage = selectedPosition == 1 ? "fil" : "en";

            if (!selectedLanguage.equals(currentLanguage)) {
                LocaleHelper.setLocale(this, selectedLanguage);

                // Recreate activity to apply language immediately
                recreate();
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showRestartDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_settings)
                .setMessage(R.string.restart_required)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    // Restart the app
                    Intent intent = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void openTermsAndConditions() {
        Intent intent = new Intent(this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    performAccountDeletion();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performAccountDeletion() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, R.string.no_user_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        Toast.makeText(this, R.string.deleting_account, Toast.LENGTH_SHORT).show();

        db.collection("accounts").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(BaseActivity.this,
                                            R.string.account_deleted,
                                            Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(BaseActivity.this, AuthActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    finish();
                                } else {
                                    String errorMsg = getString(R.string.delete_failed,
                                            task.getException() != null ? task.getException().getMessage() : "Unknown error");
                                    Toast.makeText(BaseActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    String errorMsg = getString(R.string.delete_data_failed, e.getMessage());
                    Toast.makeText(BaseActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void performLogout() {
        if (mAuth != null) {
            mAuth.signOut();
        }

        Toast.makeText(this, R.string.logging_out, Toast.LENGTH_SHORT).show();

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