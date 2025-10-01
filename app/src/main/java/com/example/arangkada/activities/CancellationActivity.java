package com.example.arangkada.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

public class CancellationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation);

        recyclerView = findViewById(R.id.recyclerBookings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadBookings();
    }

    private void loadBookings() {
        db.collection("bookings")
                .whereIn("status", List.of("Pending", "Confirmed"))
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(CancellationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        bookingList.clear();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Booking booking = doc.toObject(Booking.class);
                                if (booking != null) {
                                    booking.setBookingId(doc.getId());
                                    bookingList.add(booking);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // ================================
    // Booking Model (inner class)
    // ================================
    public static class Booking {
        private String bookingId;
        private String userId;
        private String tripId;
        private String status;
        private double totalFare;
        private com.google.firebase.Timestamp departure;
        private int seats;

        public Booking() {} // Needed for Firestore

        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }

        public String getUserId() { return userId; }
        public String getTripId() { return tripId; }
        public String getStatus() { return status; }
        public double getTotalFare() { return totalFare; }
        public com.google.firebase.Timestamp getDeparture() { return departure; }
        public int getSeats() { return seats; }
    }

    // ================================
    // Adapter (inner class)
    // ================================
    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

        private List<Booking> bookings;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        public BookingAdapter(List<Booking> bookings) {
            this.bookings = bookings;
        }

        @NonNull
        @Override
        public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cancellation_card, parent, false);
            return new BookingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
            Booking booking = bookings.get(position);

            // Fetch user name
            db.collection("accounts")
                    .document(booking.getUserId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            holder.txtUser.setText(doc.getString("name"));
                        } else {
                            holder.txtUser.setText("Unknown User");
                        }
                    });

            // Fetch trip details
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
            String dateStr = booking.getDeparture() != null ? sdf.format(booking.getDeparture().toDate()) : "N/A";
            holder.tvDeparture.setText(dateStr);

            // Passengers (seats count)
            holder.tvPassengers.setText(String.valueOf(booking.getSeats()));

            // Fare
            holder.tvTotalFare.setText("₱" + booking.getTotalFare());

            // Status
            holder.tvStatus.setText(booking.getStatus());

            // Cancel button
            holder.btnCancel.setOnClickListener(v -> cancelBooking(booking));
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            TextView txtUser, tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare, tvStatus;
            Button btnCancel;

            public BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                txtUser = itemView.findViewById(R.id.txtUser);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnCancel = itemView.findViewById(R.id.btnCancel);
            }
        }
    }

    // ================================
    // Cancel booking + seat restoration
    // ================================
    private void cancelBooking(Booking booking) {
        // Step 1: Restore seats in trip
        db.collection("trips")
                .document(booking.getTripId())
                .update("availableSeats", FieldValue.increment(booking.getSeats()))
                .addOnSuccessListener(unused -> {
                    // Step 2: Mark booking as Cancelled
                    db.collection("bookings")
                            .document(booking.getBookingId())
                            .update("status", "Cancelled")
                            .addOnSuccessListener(unused2 ->
                                    Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error restoring seats: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
