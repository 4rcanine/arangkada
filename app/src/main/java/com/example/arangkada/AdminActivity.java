package com.example.arangkada;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.example.arangkada.activities.AuthActivity;
import com.example.arangkada.activities.BaseActivity;
import com.example.arangkada.activities.ManageReservationsActivity;
import com.example.arangkada.activities.ManageVansActivity;
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

        // Set card UI (icons + titles)
        setAdminCard(cardReservations, R.drawable.ic_reservations, "Manage Reservations");
        setAdminCard(cardSchedule, R.drawable.ic_schedule, "Manage Van Schedules");
        setAdminCard(cardTerminals, R.drawable.ic_terminal, "Manage Terminals");
        setAdminCard(cardUsers, R.drawable.ic_users, "Manage Users");
        setAdminCard(cardCancelled, R.drawable.ic_cancelled, "Cancelled Trips");
        setAdminCard(cardQR, R.drawable.ic_qr, "QR Scanner");
        setAdminCard(cardSettings, R.drawable.ic_settings, "Settings");
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
        // TODO: Uncomment when activities are ready
        cardReservations.setOnClickListener(v -> startActivity(new Intent(this, ManageReservationsActivity.class)));
        cardSchedule.setOnClickListener(v -> startActivity(new Intent(this, ManageVansActivity.class)));
//        cardTerminals.setOnClickListener(v -> startActivity(new Intent(this, TerminalManagementActivity.class)));
//        cardUsers.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
//        cardCancelled.setOnClickListener(v -> startActivity(new Intent(this, CancelledTripsActivity.class)));
//        cardQR.setOnClickListener(v -> startActivity(new Intent(this, QRScannerActivity.class)));
//        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
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

    private void setAdminCard(View cardView, int iconRes, String title) {
        ImageView icon = cardView.findViewById(R.id.admin_icon);
        TextView text = cardView.findViewById(R.id.admin_title);

        icon.setImageResource(iconRes);
        text.setText(title);
    }
}
