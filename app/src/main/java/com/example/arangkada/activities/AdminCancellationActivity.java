package com.example.arangkada.activities;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminCancellationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CancellationAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_cancellation);

        recyclerView = findViewById(R.id.recyclerCancellations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CancellationAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);
        db = FirebaseFirestore.getInstance();

        loadCancelledBookings();
    }

    private void loadCancelledBookings() {
        db.collection("bookings")
                .whereEqualTo("status", "Cancelled")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(AdminCancellationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Booking model
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

    // Recycler Adapter
    private class CancellationAdapter extends RecyclerView.Adapter<CancellationAdapter.CancellationViewHolder> {
        private final List<Booking> bookings;

        public CancellationAdapter(List<Booking> bookings) {
            this.bookings = bookings;
        }

        @NonNull
        @Override
        public CancellationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
            return new CancellationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CancellationViewHolder holder, int position) {
            Booking booking = bookings.get(position);

            holder.tvStatus.setText("Cancelled");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);

            // Fetch username
            db.collection("accounts").document(booking.getUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            holder.tvUsername.setText(userDoc.getString("name"));
                        } else {
                            holder.tvUsername.setText("Unknown User");
                        }
                    });

            // Fetch trip details
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

            // Created At
            if (booking.getCreatedAt() != null) {
                String createdStr = DateFormat.format("MMM dd, yyyy hh:mm a", booking.getCreatedAt().toDate()).toString();
                holder.tvCreatedAt.setText("Booked at: " + createdStr);
            } else {
                holder.tvCreatedAt.setText("Booked at: N/A");
            }

            // Passengers and reason
            db.collection("bookings").document(booking.getBookingId()).get()
                    .addOnSuccessListener(seatDoc -> {
                        if (seatDoc.exists()) {
                            long seats = seatDoc.getLong("seats") != null ? seatDoc.getLong("seats") : 0;
                            holder.tvPassengers.setText(String.valueOf(seats));

                            String reason = seatDoc.getString("reason");
                            if (reason != null && !reason.trim().isEmpty()) {
                                holder.tvReason.setVisibility(View.VISIBLE);
                                holder.tvReason.setText("Reason: " + reason);
                            } else {
                                holder.tvReason.setVisibility(View.GONE);
                            }
                        } else {
                            holder.tvPassengers.setText("N/A");
                            holder.tvReason.setVisibility(View.GONE);
                        }
                    });

            // Fare
            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class CancellationViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare,
                    tvStatus, tvCreatedAt, tvUsername, tvReason;

            public CancellationViewHolder(@NonNull View itemView) {
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
