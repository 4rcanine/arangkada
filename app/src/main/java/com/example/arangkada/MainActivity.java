package com.example.arangkada;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.cardview.widget.CardView;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.arangkada.activities.BookRideActivity;
import com.example.arangkada.activities.CancellationActivity;
import com.example.arangkada.activities.MyTripsActivity;
import com.example.arangkada.activities.NotificationsActivity;
import com.example.arangkada.activities.ProfileActivity;
import com.example.arangkada.activities.BaseActivity;
import com.example.arangkada.activities.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

import javax.annotation.Nullable;

public class MainActivity extends BaseActivity {

    private TextView userNameTextView;
    private CardView bookRideCard, myTripsCard, notificationsCard, profileCard;
    private ImageView notificationBadge;
    private TextView tvNextTripTitle, tvNextTripRoute, tvNextTripDate, tvTotalFare, tvTripStatus;
    private Button btnViewQR;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListenerRegistration bookingListener;

    private static final String CHANNEL_ID = "booking_updates_channel";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation();
        onNavigationSetup();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupSwipeRefresh();
        setupUserInfo();
        fetchUpcomingOrRecentTrip();
        createNotificationChannel();

        // 🔹 Ask for notification permission on Android 13+
        requestNotificationPermission();

        // 🔹 Start listening for Firestore booking changes
        listenToBookingUpdates();
    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.tv_user_name);
        bookRideCard = findViewById(R.id.card_book_ride);
        myTripsCard = findViewById(R.id.card_my_trips);
        notificationsCard = findViewById(R.id.card_notifications);
        profileCard = findViewById(R.id.card_profile);

        tvNextTripTitle = findViewById(R.id.tv_next_trip_title);
        tvNextTripRoute = findViewById(R.id.tv_next_trip_route);
        tvNextTripDate = findViewById(R.id.tv_next_trip_date);
        tvTotalFare = findViewById(R.id.tv_total_fare);
        tvTripStatus = findViewById(R.id.tv_trip_status);
        btnViewQR = findViewById(R.id.btn_view_qr);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
    }

    private void setupClickListeners() {
        bookRideCard.setOnClickListener(v -> startActivity(new Intent(this, BookRideActivity.class)));
        myTripsCard.setOnClickListener(v -> startActivity(new Intent(this, MyTripsActivity.class)));
        notificationsCard.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        profileCard.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            setupUserInfo();
            fetchUpcomingOrRecentTrip();
            swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });
    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("accounts").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists())
                            userNameTextView.setText(document.getString("name"));
                        else userNameTextView.setText("Unknown User");
                    })
                    .addOnFailureListener(e -> userNameTextView.setText("Error loading user"));
        } else userNameTextView.setText("Guest");
    }

    private void fetchUpcomingOrRecentTrip() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        CollectionReference bookingsRef = db.collection("bookings");

        bookingsRef
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("Pending", "Confirmed"))
                .orderBy("departure", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) showTrip(task.getDocuments().get(0), true);
                    else fetchRecentTrip(userId);
                })
                .addOnFailureListener(e -> showNoTripFound());
    }

    private void fetchRecentTrip(String userId) {
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("Completed", "Cancelled"))
                .orderBy("departure", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) showTrip(task.getDocuments().get(0), false);
                    else showNoTripFound();
                })
                .addOnFailureListener(e -> showNoTripFound());
    }

    private void showTrip(DocumentSnapshot bookingDoc, boolean isUpcoming) {
        String tripId = bookingDoc.getString("tripId");
        String status = bookingDoc.getString("status");
        Timestamp departureTime = bookingDoc.getTimestamp("departure");
        Double totalFare = bookingDoc.getDouble("totalFare");

        if (tripId == null || departureTime == null) {
            showNoTripFound();
            return;
        }

        db.collection("trips").document(tripId)
                .get()
                .addOnSuccessListener(tripDoc -> {
                    if (tripDoc.exists()) {
                        String destinationId = tripDoc.getString("destinationId");
                        if (destinationId != null) {
                            db.collection("destinations").document(destinationId)
                                    .get()
                                    .addOnSuccessListener(destDoc -> {
                                        String destinationName = destDoc.exists()
                                                ? destDoc.getString("name")
                                                : "Unknown destination";
                                        updateTripUI(destinationName, departureTime, totalFare, status, isUpcoming);
                                    })
                                    .addOnFailureListener(e -> updateTripUI("Error loading destination", departureTime, totalFare, status, isUpcoming));
                        } else updateTripUI("Unknown destination", departureTime, totalFare, status, isUpcoming);
                    } else updateTripUI("Trip not found", departureTime, totalFare, status, isUpcoming);
                })
                .addOnFailureListener(e -> updateTripUI("Error loading trip", departureTime, totalFare, status, isUpcoming));
    }

    private void updateTripUI(String destinationName, Timestamp departureTime, Double totalFare, String status, boolean isUpcoming) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault());
        String formattedDate = dateFormat.format(departureTime.toDate());

        tvNextTripTitle.setText(isUpcoming ? "Upcoming Trip" : "Recent Trip");
        tvNextTripRoute.setText(destinationName);
        tvNextTripDate.setText(formattedDate);

        if (totalFare != null) {
            tvTotalFare.setVisibility(View.VISIBLE);
            tvTotalFare.setText(String.format(Locale.getDefault(), "Total Fare: ₱%.2f", totalFare));
        } else tvTotalFare.setVisibility(View.GONE);

        if (status != null) {
            tvTripStatus.setVisibility(View.VISIBLE);
            tvTripStatus.setText(status);

            switch (status) {
                case "Pending":
                    tvTripStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    btnViewQR.setVisibility(View.GONE);
                    break;
                case "Confirmed":
                    tvTripStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    btnViewQR.setVisibility(View.VISIBLE);
                    btnViewQR.setOnClickListener(v ->
                            startActivity(new Intent(this, CancellationActivity.class)));
                    break;
                case "Completed":
                    tvTripStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    btnViewQR.setVisibility(View.GONE);
                    break;
                case "Cancelled":
                    tvTripStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                    btnViewQR.setVisibility(View.GONE);
                    break;
            }
        } else {
            tvTripStatus.setVisibility(View.GONE);
            btnViewQR.setVisibility(View.GONE);
        }
    }

    private void showNoTripFound() {
        tvNextTripTitle.setText("No trips found");
        tvNextTripRoute.setText("Book your first trip now!");
        tvNextTripDate.setText("");
        tvTotalFare.setVisibility(View.GONE);
        tvTripStatus.setVisibility(View.GONE);
        btnViewQR.setVisibility(View.GONE);
    }

    // 🔹 LISTEN TO BOOKING STATUS CHANGES
    private void listenToBookingUpdates() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        bookingListener = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            DocumentSnapshot doc = dc.getDocument();
                            String status = doc.getString("status");

                            SharedPreferences prefs = getSharedPreferences("BookingData", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();

                            if ("Confirmed".equalsIgnoreCase(status)) {
                                editor.putString("latest_booking_status", "confirmed").apply();
                                NotificationHelper.showBookingNotification(
                                        this,
                                        "Booking Confirmed",
                                        "Your UV Express booking has been confirmed!",
                                        "confirmed"
                                );
                            } else if ("Rejected".equalsIgnoreCase(status)) {
                                editor.putString("latest_booking_status", "rejected").apply();
                                NotificationHelper.showBookingNotification(
                                        this,
                                        "Booking Rejected",
                                        "Sorry, your booking was rejected.",
                                        "rejected"
                                );
                            } else {
                                editor.putString("latest_booking_status", "changed").apply();
                                NotificationHelper.showBookingNotification(
                                        this,
                                        "Booking Update",
                                        "Your booking details have been updated.",
                                        "info"
                                );
                            }
                        }
                    }
                });
    }


    // 🔹 Create notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Booking Updates";
            String description = "Notifies users about booking status updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 🔹 Ask notification permission on Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) bookingListener.remove();
    }

    @Override
    protected void onNavigationSetup() {
        showMenuButton();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
    }
}
