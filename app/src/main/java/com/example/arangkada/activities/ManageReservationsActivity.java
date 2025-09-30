package com.example.arangkada.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arangkada.R;
import com.example.arangkada.models.Booking;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageReservationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ManageReservationsAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_reservations);

        recyclerView = findViewById(R.id.recyclerBookings);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageReservationsAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        swipeRefresh.setOnRefreshListener(this::loadBookings);

        loadBookings();
    }

    private void loadBookings() {
        swipeRefresh.setRefreshing(true);

        db.collection("bookings")
                .whereEqualTo("status", "Pending")
                .orderBy("departure", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                        return;
                    }

                    bookingList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId()); // Firestore doc ID
                                bookingList.add(booking);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    swipeRefresh.setRefreshing(false);
                });
    }

    // ✅ Updated Cancel logic to restore seats
    private void updateBookingStatus(String bookingId, String status, int seats, String tripId) {
        if ("Cancelled".equals(status)) {
            // First restore seats to the trip
            db.collection("trips")
                    .document(tripId)
                    .update("availableSeats", FieldValue.increment(seats))
                    .addOnSuccessListener(unused -> {
                        // Then update booking status
                        db.collection("bookings")
                                .document(bookingId)
                                .update("status", status)
                                .addOnSuccessListener(unused2 ->
                                        Toast.makeText(this, "Booking Cancelled & seats restored", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error restoring seats: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            // For Confirm, only update status
            db.collection("bookings")
                    .document(bookingId)
                    .update("status", status)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Booking " + status, Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    // =====================
    // ADAPTER CLASS INSIDE
    // =====================
    private class ManageReservationsAdapter extends RecyclerView.Adapter<ManageReservationsAdapter.BookingViewHolder> {

        private List<Booking> bookings;
        private FirebaseFirestore db;

        public ManageReservationsAdapter(List<Booking> bookings) {
            this.bookings = bookings;
            this.db = FirebaseFirestore.getInstance();
        }

        @NonNull
        @Override
        public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
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

            // Fetch trip → destination → van
            db.collection("trips")
                    .document(booking.getTripId())
                    .get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            String destinationId = tripDoc.getString("destinationId");
                            String vanId = tripDoc.getString("vanId");

                            // Show Van (no prefix)
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

            // ✅ Fetch seats
            db.collection("bookings")
                    .document(booking.getBookingId())
                    .get()
                    .addOnSuccessListener(seatDoc -> {
                        if (seatDoc.exists() && seatDoc.contains("seats")) {
                            long seats = seatDoc.getLong("seats");
                            holder.tvPassengers.setText(String.valueOf(seats));

                            // Confirm button
                            holder.btnConfirm.setOnClickListener(v ->
                                    updateBookingStatus(booking.getBookingId(), "Confirmed", (int) seats, booking.getTripId())
                            );

                            // Cancel button (restore seats)
                            holder.btnCancel.setOnClickListener(v ->
                                    updateBookingStatus(booking.getBookingId(), "Cancelled", (int) seats, booking.getTripId())
                            );
                        } else {
                            holder.tvPassengers.setText("N/A");

                            holder.btnConfirm.setOnClickListener(v ->
                                    updateBookingStatus(booking.getBookingId(), "Confirmed", 0, booking.getTripId())
                            );
                            holder.btnCancel.setOnClickListener(v ->
                                    updateBookingStatus(booking.getBookingId(), "Cancelled", 0, booking.getTripId())
                            );
                        }
                    });

            // Format date (no prefix)
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            String dateStr = booking.getDeparture() != null ? sdf.format(booking.getDeparture().toDate()) : "N/A";
            holder.tvDeparture.setText(dateStr);

            // Show total fare (only value with ₱)
            holder.tvTotalFare.setText("₱" + booking.getTotalFare());

            // ✅ Status with background color
            holder.tvStatus.setText(booking.getStatus());

            switch (booking.getStatus()) {
                case "Confirmed":
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    break;
                case "Completed":
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case "Cancelled":
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                    break;
                case "Pending":
                default:
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
            }
        }


        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            TextView txtUser, tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare, tvStatus;
            Button btnConfirm, btnCancel;

            public BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                txtUser = itemView.findViewById(R.id.txtUser);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnConfirm = itemView.findViewById(R.id.btnConfirm);
                btnCancel = itemView.findViewById(R.id.btnCancel);
            }
        }
    }
}
