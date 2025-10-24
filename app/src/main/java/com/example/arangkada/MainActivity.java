package com.example.arangkada;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private TextView userNameTextView;
    private ImageView profileImageView;
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

        requestNotificationPermission();
        listenToBookingUpdates();
    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.tv_user_name);
        profileImageView = findViewById(R.id.iv_profile_image_dashboard);
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
        myTripsCard.setOnClickListener(v -> startActivity(new Intent(this, CancellationActivity.class)));
        notificationsCard.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        profileCard.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        // Make profile image clickable to go to profile
        profileImageView.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
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
                        if (document.exists()) {
                            // Set user name
                            String name = document.getString("name");
                            userNameTextView.setText(name != null ? name : "Unknown User");

                            // Load profile picture
                            String profilePicture = document.getString("profilePicture");
                            if (profilePicture != null && !profilePicture.isEmpty()) {
                                loadProfileImage(profilePicture);
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        } else {
                            userNameTextView.setText("Unknown User");
                            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNameTextView.setText("Error loading user");
                        profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                    });
        } else {
            userNameTextView.setText("Guest");
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(profileImageView);
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

    // ✅ FINAL FIXED LISTENER
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
                            final String bookingId = dc.getDocument().getId();
                            db.collection("bookings").document(bookingId)
                                    .get()
                                    .addOnSuccessListener(freshBookingDoc -> {
                                        if (freshBookingDoc == null || !freshBookingDoc.exists()) return;

                                        String status = freshBookingDoc.getString("status");
                                        String tripId = freshBookingDoc.getString("tripId");
                                        final com.google.firebase.Timestamp departureTs = freshBookingDoc.getTimestamp("departure");
                                        final Double fare;
                                        Object fareObj = freshBookingDoc.get("totalFare");
                                        fare = (fareObj instanceof Number) ? ((Number) fareObj).doubleValue() : 0.0;

                                        if (tripId == null) {
                                            saveAndShowNotification(userId, "Booking Updated",
                                                    "Your booking status changed.", "info",
                                                    "Unknown Trip", departureTs, fare);
                                            return;
                                        }

                                        db.collection("trips").document(tripId)
                                                .get()
                                                .addOnSuccessListener(tripDoc -> {
                                                    if (tripDoc == null || !tripDoc.exists()) {
                                                        saveAndShowNotification(userId, "Booking Updated",
                                                                "Your booking status changed.", "info",
                                                                "Unknown Trip", departureTs, fare);
                                                        return;
                                                    }

                                                    String destinationId = tripDoc.getString("destinationId");
                                                    String destinationNameFallback =
                                                            tripDoc.getString("destinationName") != null
                                                                    ? tripDoc.getString("destinationName")
                                                                    : tripDoc.getString("name");

                                                    if (destinationId != null) {
                                                        db.collection("destinations").document(destinationId)
                                                                .get()
                                                                .addOnSuccessListener(destDoc -> {
                                                                    String destinationName =
                                                                            (destDoc != null && destDoc.exists() && destDoc.getString("name") != null)
                                                                                    ? destDoc.getString("name")
                                                                                    : (destinationNameFallback != null ? destinationNameFallback : "Unknown Trip");

                                                                    showNotificationWithDetails(userId, status, destinationName, departureTs, fare);
                                                                })
                                                                .addOnFailureListener(err -> showNotificationWithDetails(
                                                                        userId, status,
                                                                        destinationNameFallback != null ? destinationNameFallback : "Unknown Trip",
                                                                        departureTs, fare
                                                                ));
                                                    } else {
                                                        showNotificationWithDetails(
                                                                userId, status,
                                                                destinationNameFallback != null ? destinationNameFallback : "Unknown Trip",
                                                                departureTs, fare
                                                        );
                                                    }
                                                });
                                    });
                        }
                    }
                });
    }

    private void showNotificationWithDetails(String userId, String status, String destinationName,
                                             com.google.firebase.Timestamp departureTs, Double fare) {
        String formattedDate = (departureTs != null)
                ? new SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(departureTs.toDate())
                : "N/A";

        String title, message, type;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "confirmed":
                title = "Booking Confirmed";
                message = "Your trip to " + destinationName + " on " + formattedDate +
                        " has been confirmed.\nFare: ₱" + String.format(Locale.getDefault(), "%.2f", fare);
                type = "confirmed";
                break;
            case "rejected":
                title = "Booking Rejected";
                message = "Your booking for " + destinationName + " on " + formattedDate + " was rejected.";
                type = "rejected";
                break;
            case "cancelled":
                title = "Booking Cancelled";
                message = "Your trip to " + destinationName + " on " + formattedDate + " was cancelled.";
                type = "cancelled";
                break;
            default:
                title = "Booking Updated";
                message = "Your booking for " + destinationName + " on " + formattedDate + " was updated.";
                type = "info";
                break;
        }

        saveAndShowNotification(userId, title, message, type, destinationName, departureTs, fare);
    }


    private void saveAndShowNotification(String userId, String title, String message, String type,
                                         String destinationName, com.google.firebase.Timestamp departureTs, Double fare) {
        // 🔒 Check user preference first
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true); // default: ON

        if (!notificationsEnabled) {
            // Skip notification if user turned it off
            return;
        }

        // Continue normal flow if enabled
        SharedPreferences bookingPrefs = getSharedPreferences("BookingData", MODE_PRIVATE);
        bookingPrefs.edit().putString("latest_booking_status", type).apply();

        NotificationHelper.showBookingNotification(this, title, message, type);
        saveNotificationLocally(title, message, type);

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("type", type);
        notifData.put("tripName", destinationName);
        notifData.put("tripDate", departureTs != null ? departureTs : com.google.firebase.Timestamp.now());
        notifData.put("fare", fare);
        notifData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .collection("notifications")
                .add(notifData);
    }


    private void saveNotificationLocally(String title, String message, String type) {
        String timestampNow = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(new java.util.Date());
        SharedPreferences notifPrefs = getSharedPreferences("NotificationStorage", MODE_PRIVATE);
        String existing = notifPrefs.getString("notifications", "");
        String newEntry = title + "|" + message + "|" + timestampNow + "|" + type + ";";
        notifPrefs.edit().putString("notifications", newEntry + existing).apply();
    }

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