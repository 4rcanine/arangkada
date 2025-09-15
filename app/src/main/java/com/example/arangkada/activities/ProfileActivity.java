package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.example.arangkada.activities.InfoActivity;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName;
    private TextView userEmail;
    private TextView userPhone;

    private CardView editProfileCard;
    private CardView bookingHistoryCard;
    private CardView paymentMethodsCard;
    private CardView notificationSettingsCard;
    private CardView helpSupportCard;
    private CardView aboutAppCard;

    private TextView totalTripsCount;
    private TextView favoriteRoute;
    private TextView memberSince;

    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.iv_profile_image);
        userName = findViewById(R.id.tv_user_name);
        userEmail = findViewById(R.id.tv_user_email);
        userPhone = findViewById(R.id.tv_user_phone);

        editProfileCard = findViewById(R.id.card_edit_profile);
        bookingHistoryCard = findViewById(R.id.card_booking_history);
        paymentMethodsCard = findViewById(R.id.card_payment_methods);
        notificationSettingsCard = findViewById(R.id.card_notification_settings);
        helpSupportCard = findViewById(R.id.card_help_support);
        aboutAppCard = findViewById(R.id.card_about_app);

        totalTripsCount = findViewById(R.id.tv_total_trips);
        favoriteRoute = findViewById(R.id.tv_favorite_route);
        memberSince = findViewById(R.id.tv_member_since);

        logoutButton = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        // In a real app, this would come from SharedPreferences or API
        userName.setText("Juan Dela Cruz");
        userEmail.setText("juan.delacruz@email.com");
        userPhone.setText("+63 912 345 6789");

        // Statistics (dummy data)
        totalTripsCount.setText("15");
        favoriteRoute.setText("Cervantes → Baguio");
        memberSince.setText("December 2024");
    }

    private void setupClickListeners() {
        editProfileCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditProfile();
            }
        });

        bookingHistoryCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBookingHistory();
            }
        });

        paymentMethodsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPaymentMethods();
            }
        });

        notificationSettingsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationSettings();
            }
        });

        helpSupportCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHelpSupport();
            }
        });

        aboutAppCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAboutApp();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });
    }

    private void openEditProfile() {
        Toast.makeText(this, "Edit Profile - Coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to EditProfileActivity
        // Intent intent = new Intent(this, EditProfileActivity.class);
        // startActivity(intent);
    }

    private void openBookingHistory() {
        String historyMessage = "Booking History:\n\n" +
                "Recent Trips:\n" +
                "• Dec 15, 2024 - Cervantes → Baguio (₱45)\n" +
                "• Dec 12, 2024 - Baguio → Cervantes (₱45)\n" +
                "• Dec 10, 2024 - Cervantes → Baguio (₱45)\n" +
                "• Dec 8, 2024 - Baguio → Cervantes (₱45)\n\n" +
                "Total Spent: ₱675.00";

        Toast.makeText(this, historyMessage, Toast.LENGTH_LONG).show();
        // TODO: Navigate to BookingHistoryActivity
    }

    private void openPaymentMethods() {
        String paymentInfo = "Payment Methods:\n\n" +
                "• Cash (Default)\n" +
                "• GCash (Coming Soon)\n" +
                "• Credit Card (Coming Soon)\n\n" +
                "Manage your payment preferences here.";

        Toast.makeText(this, paymentInfo, Toast.LENGTH_LONG).show();
        // TODO: Navigate to PaymentMethodsActivity
    }

    private void openNotificationSettings() {
        String notificationInfo = "Notification Settings:\n\n" +
                "Current Settings:\n" +
                "• Booking Confirmations: ON\n" +
                "• Trip Reminders: ON\n" +
                "• Promotional Offers: OFF\n" +
                "• App Updates: ON\n\n" +
                "Customize your notification preferences.";

        Toast.makeText(this, notificationInfo, Toast.LENGTH_LONG).show();
        // TODO: Navigate to NotificationSettingsActivity
    }

    private void openHelpSupport() {
        String helpInfo = "Help & Support:\n\n" +
                "Contact Information:\n" +
                "• Email: support@arangkada.ph\n" +
                "• Phone: +63 917 123 4567\n" +
                "• Hours: 6:00 AM - 10:00 PM\n\n" +
                "Frequently Asked Questions:\n" +
                "• How to cancel a booking?\n" +
                "• Payment methods available?\n" +
                "• UV Express schedule information?";

        Toast.makeText(this, helpInfo, Toast.LENGTH_LONG).show();
        // TODO: Navigate to HelpSupportActivity
    }

    private void openAboutApp() {
        String aboutInfo = "About Arangkada:\n\n" +
                "Version: 1.0.0\n" +
                "Build: 2024.12.15\n\n" +
                "Arangkada is your convenient UV Express booking companion for routes in Northern Luzon, specifically Cervantes to Baguio and vice versa.\n\n" +
                "\"Your Ride, Your Way!\"\n\n" +
                "© 2024 Arangkada Philippines";

        Toast.makeText(this, aboutInfo, Toast.LENGTH_LONG).show();
        // TODO: Navigate to AboutAppActivity
    }

    private void performLogout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Clear any stored user data here (SharedPreferences, etc.)
        // TODO: Clear user session data

        // Navigate back to InfoActivity
        Intent intent = new Intent(ProfileActivity.this, InfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Go back to dashboard
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
