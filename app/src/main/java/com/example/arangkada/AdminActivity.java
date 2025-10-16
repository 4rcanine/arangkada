package com.example.arangkada;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends BaseActivity {

    private CardView cardReservations, cardSchedule, cardTerminals, cardUsers, cardCancelled, cardQR, cardSettings;
    private TextView tvAdminName;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Button logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // logout from Firebase
            Intent intent = new Intent(AdminActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        setupNavigation();
        onNavigationSetup();

        initializeViews();
        setupClickListeners();

        // Firestore init
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadAdminName(user.getUid());
        }

        // REMOVED: setAdminCard() calls - cards are now fully defined in XML
        // The new layout has all icons and text built-in, no need to set them dynamically
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
    }

    private void setupClickListeners() {
        cardReservations.setOnClickListener(v -> startActivity(new Intent(this, ManageReservationsActivity.class)));
        cardSchedule.setOnClickListener(v -> startActivity(new Intent(this, CurrentVanScheduleActivity.class)));
        cardTerminals.setOnClickListener(v -> startActivity(new Intent(this, NewTerminalActivity.class)));
        cardUsers.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        cardCancelled.setOnClickListener(v -> startActivity(new Intent(this, AdminCancellationActivity.class)));
        cardQR.setOnClickListener(v -> startActivity(new Intent(this, QRScannerActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, AdminProfileActivity.class)));
    }

    private void loadAdminName(String userId) {
        db.collection("accounts").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name != null) {
                            tvAdminName.setText(name);
                        } else {
                            tvAdminName.setText("Admin");
                        }
                    } else {
                        tvAdminName.setText("Admin");
                    }
                })
                .addOnFailureListener(e -> tvAdminName.setText("Admin"));
    }

    @Override
    protected void onNavigationSetup() {
        showMenuButton();
    }

    // REMOVED: setAdminCard() method - no longer needed with new XML layout
    // The cards now have their icons and text defined directly in activity_admin.xml
}