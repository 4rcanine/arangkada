package com.example.arangkada.activities;

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
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView menuButton;
    protected ImageView backButton;
    protected LinearLayout navigationContainer;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
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

    // 🔹 Helper to set toolbar text (optional: child activities can call this)
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
            openAccountDeletion();
        } else if (id == R.id.nav_logout) {
            performLogout();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
        return true;
    }

    private void openNotifications() {
        Intent intent = new Intent(this, NotificationsActivity.class);
        startActivity(intent);
    }

    private void openLanguageSettings() {
        Toast.makeText(this, "Language Settings: English/Filipino - Coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openTermsAndConditions() {
        Toast.makeText(this, "Terms and Conditions - Feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openAccountDeletion() {
        Toast.makeText(this, "Account Deletion - Feature coming soon!", Toast.LENGTH_SHORT).show();
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

    // 🔹 Abstract hook for child activities
    protected abstract void onNavigationSetup();
}
