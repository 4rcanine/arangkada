package com.example.arangkada.activities;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;

    private TextView tvEmpty;
    private String userId; // user ID passed from UserManagementActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips); // reuse same layout

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TripAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.btnClearHistory).setVisibility(View.GONE); // hide clear button

        db = FirebaseFirestore.getInstance();

        // get userId passed from UserManagementActivity
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: No user selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadBookingsForUser();
    }

    private void loadBookingsForUser() {
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // -------------------------
    // Booking Model
    // -------------------------
    public static class Booking {
        private String bookingId;
        private String userId;
        private String tripId;
        private String status;
        private String reason;
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
        public String getReason() { return reason; }
        public double getTotalFare() { return totalFare; }
        public Timestamp getDeparture() { return departure; }
        public Timestamp getCreatedAt() { return createdAt; }
        public int getSeats() { return seats; }
    }

    // -------------------------
    // Adapter Class
    // -------------------------
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

            // STATUS
            String status = booking.getStatus();
            holder.tvStatus.setText(status);
            if ("Completed".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
            } else if ("Confirmed".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
            } else if ("Pending".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            } else {
                holder.tvStatus.setBackgroundResource(0);
            }

            // USERNAME
            db.collection("accounts").document(booking.getUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            holder.tvUsername.setText(userDoc.getString("name"));
                        } else {
                            holder.tvUsername.setText("Unknown User");
                        }
                    });

            // TRIP DETAILS
            db.collection("trips").document(booking.getTripId()).get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            String destinationId = tripDoc.getString("destinationId");
                            String vanId = tripDoc.getString("vanId");
                            holder.tvVan.setText(vanId != null ? vanId : "Unknown");

                            if (destinationId != null) {
                                db.collection("destinations").document(destinationId).get()
                                        .addOnSuccessListener(destDoc -> {
                                            if (destDoc.exists()) {
                                                holder.tvRoute.setText(destDoc.getString("name"));
                                            } else {
                                                holder.tvRoute.setText("Unknown Route");
                                            }
                                        });
                            }
                        }
                    });

            // DEPARTURE
            String departureStr = booking.getDeparture() != null
                    ? DateFormat.format("MMM dd, yyyy hh:mm a", booking.getDeparture().toDate()).toString()
                    : "N/A";
            holder.tvDeparture.setText(departureStr);

            // CREATED AT
            if (booking.getCreatedAt() != null) {
                holder.tvCreatedAt.setText(
                        DateFormat.format("MMM dd, yyyy hh:mm a", booking.getCreatedAt().toDate()).toString()
                );
            } else {
                holder.tvCreatedAt.setText("N/A");
            }

            // PASSENGERS
            db.collection("bookings").document(booking.getBookingId()).get()
                    .addOnSuccessListener(seatDoc -> {
                        if (seatDoc.exists()) {
                            long seats = seatDoc.getLong("seats") != null ? seatDoc.getLong("seats") : 0;
                            long regularCount = seatDoc.getLong("regularCount") != null ? seatDoc.getLong("regularCount") : 0;
                            long studentCount = seatDoc.getLong("studentCount") != null ? seatDoc.getLong("studentCount") : 0;
                            long seniorCount = seatDoc.getLong("seniorCount") != null ? seatDoc.getLong("seniorCount") : 0;

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
                                holder.tvPassengers.setText(seats + " (" + breakdown + ")");
                            } else {
                                holder.tvPassengers.setText(String.valueOf(seats));
                            }

                            // REASON (if cancelled)
                            if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
                                String reason = seatDoc.getString("reason");
                                if (reason != null && !reason.trim().isEmpty()) {
                                    holder.tvReason.setVisibility(View.VISIBLE);
                                    holder.tvReason.setText("Reason: " + reason);
                                } else {
                                    holder.tvReason.setVisibility(View.GONE);
                                }
                            } else {
                                holder.tvReason.setVisibility(View.GONE);
                            }
                        }
                    });

            // TOTAL FARE
            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class TripViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare,
                    tvStatus, tvCreatedAt, tvUsername, tvReason;

            public TripViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.txtUser);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
                tvReason = itemView.findViewById(R.id.tv_reason);
            }
        }
    }
}
