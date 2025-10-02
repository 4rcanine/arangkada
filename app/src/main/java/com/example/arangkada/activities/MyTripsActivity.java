package com.example.arangkada.activities;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

public class MyTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser user;

    private Button btnClearHistory;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TripAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        btnClearHistory = findViewById(R.id.btnClearHistory);
        tvEmpty = findViewById(R.id.tvEmpty);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadUserBookings();
        }

        btnClearHistory.setOnClickListener(v -> showClearConfirmation());
    }

    private void loadUserBookings() {
        db.collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(MyTripsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    bookingList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                bookingList.add(booking);
                            }
                        }

                        // Auto-delete if more than 10
                        if (bookingList.size() > 10) {
                            Booking oldest = bookingList.get(bookingList.size() - 1);
                            db.collection("bookings").document(oldest.getBookingId()).delete();
                            bookingList.remove(bookingList.size() - 1);
                        }

                        tvEmpty.setVisibility(View.GONE);
                        btnClearHistory.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.VISIBLE);
                        btnClearHistory.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showClearConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all history?")
                .setPositiveButton("Yes", (dialog, which) -> clearUserHistory())
                .setNegativeButton("No", null)
                .show();
    }

    private void clearUserHistory() {
        db.collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }
                    Toast.makeText(MyTripsActivity.this, "History cleared.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Error clearing history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Booking Model
    public static class Booking {
        private String bookingId;
        private String userId;
        private String tripId;
        private String status;
        private double totalFare;
        private Timestamp departure;
        private Timestamp createdAt;
        private int seats;

        public Booking() {}

        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
        public String getUserId() { return userId; }
        public String getTripId() { return tripId; }
        public String getStatus() { return status; }
        public double getTotalFare() { return totalFare; }
        public Timestamp getDeparture() { return departure; }
        public Timestamp getCreatedAt() { return createdAt; }
        public int getSeats() { return seats; }
    }

    // Adapter
    private class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
        private final List<Booking> bookings;

        public TripAdapter(List<Booking> bookings) {
            this.bookings = bookings;
        }

        @NonNull
        @Override
        public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
            return new TripViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
            Booking booking = bookings.get(position);

            // Status
            if (holder.tvStatus != null) {
                holder.tvStatus.setText(booking.getStatus());
            }

            // 🔹 Fetch username from accounts/{userId}
            db.collection("accounts")
                    .document(booking.getUserId())
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            holder.tvUsername.setText(userDoc.getString("name"));
                        } else {
                            holder.tvUsername.setText("Unknown User");
                        }
                    });

            // Fetch trip details (Route & Van)
            db.collection("trips")
                    .document(booking.getTripId())
                    .get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            String destinationId = tripDoc.getString("destinationId");
                            String vanId = tripDoc.getString("vanId");

                            holder.tvVan.setText(vanId != null ? vanId : "Unknown");

                            if (destinationId != null) {
                                db.collection("destinations")
                                        .document(destinationId)
                                        .get()
                                        .addOnSuccessListener(destDoc -> {
                                            if (destDoc.exists()) {
                                                holder.tvRoute.setText(destDoc.getString("name"));
                                            } else {
                                                holder.tvRoute.setText("Unknown");
                                            }
                                        });
                            }
                        }
                    });

            // Departure
            String departureStr = booking.getDeparture() != null
                    ? DateFormat.format("MMM dd, yyyy hh:mm a", booking.getDeparture().toDate()).toString()
                    : "N/A";
            holder.tvDeparture.setText(departureStr);

            // 🔹 Created At → fixed timestamp format
            if (booking.getCreatedAt() != null) {
                String createdStr = DateFormat.format("MMM dd, yyyy hh:mm a", booking.getCreatedAt().toDate()).toString();
                holder.tvCreatedAt.setText(createdStr);
            } else {
                holder.tvCreatedAt.setText("N/A");
            }

            // Passengers with breakdown
            db.collection("bookings")
                    .document(booking.getBookingId())
                    .get()
                    .addOnSuccessListener(seatDoc -> {
                        if (seatDoc.exists()) {
                            long seats = seatDoc.getLong("seats") != null ? seatDoc.getLong("seats") : 0;
                            long regularCount = seatDoc.getLong("regularCount") != null ? seatDoc.getLong("regularCount") : 0;
                            long studentCount = seatDoc.getLong("studentCount") != null ? seatDoc.getLong("studentCount") : 0;
                            long seniorCount = seatDoc.getLong("seniorCount") != null ? seatDoc.getLong("seniorCount") : 0;

                            // Build breakdown string
                            StringBuilder breakdown = new StringBuilder();
                            if (regularCount > 0) breakdown.append("Regular-").append(regularCount);
                            if (studentCount > 0) {
                                if (breakdown.length() > 0) breakdown.append(", ");
                                breakdown.append("Student-").append(studentCount);
                            }
                            if (seniorCount > 0) {
                                if (breakdown.length() > 0) breakdown.append(", ");
                                breakdown.append("Senior-").append(seniorCount);
                            }

                            if (breakdown.length() > 0) {
                                holder.tvPassengers.setText(seats + " (" + breakdown.toString() + ")");
                            } else {
                                holder.tvPassengers.setText(String.valueOf(seats));
                            }
                        } else {
                            holder.tvPassengers.setText("N/A");
                        }
                    });

            // Fare
            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class TripViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare,
                    tvStatus, tvCreatedAt, tvPaymentMethod, tvUsername;

            public TripViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.txtUser);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
            }
        }
    }
}
