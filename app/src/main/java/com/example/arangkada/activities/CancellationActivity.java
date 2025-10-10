package com.example.arangkada.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class CancellationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private Button btnBackToTrips;
    private TextView tvNoBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation);

        recyclerView = findViewById(R.id.recyclerBookings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        btnBackToTrips = findViewById(R.id.btn_back_to_trips);
        tvNoBooking = findViewById(R.id.tvNoBooking);

        db = FirebaseFirestore.getInstance();

        btnBackToTrips.setOnClickListener(v -> {
            Intent intent = new Intent(CancellationActivity.this, MyTripsActivity.class);
            startActivity(intent);
        });

        loadBookings();
    }

    private void loadBookings() {
        db.collection("bookings")
                .whereIn("status", List.of("Pending", "Confirmed"))
                .addSnapshotListener((@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) -> {
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

                    if (bookingList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvNoBooking.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvNoBooking.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // ================================
    // Booking Model
    // ================================
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

    // ================================
    // Adapter
    // ================================
    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

        private List<Booking> bookings;

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

            db.collection("accounts")
                    .document(booking.getUserId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) holder.txtUser.setText(doc.getString("name"));
                        else holder.txtUser.setText("Unknown User");
                    });

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
                                            if (destDoc.exists())
                                                holder.tvRoute.setText(destDoc.getString("name"));
                                            else holder.tvRoute.setText("Unknown");
                                        });
                            }
                        }
                    });

            String dateStr = booking.getDeparture() != null
                    ? android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", booking.getDeparture().toDate()).toString()
                    : "N/A";
            holder.tvDeparture.setText(dateStr);

            if (booking.getCreatedAt() != null) {
                long now = System.currentTimeMillis();
                long createdMillis = booking.getCreatedAt().toDate().getTime();
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(createdMillis, now, DateUtils.MINUTE_IN_MILLIS);
                holder.tvCreatedAt.setText(relativeTime);
            } else holder.tvCreatedAt.setText("N/A");

            db.collection("bookings")
                    .document(booking.getBookingId())
                    .get()
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

                            if (breakdown.length() > 0)
                                holder.tvPassengers.setText(seats + " (" + breakdown + ")");
                            else holder.tvPassengers.setText(String.valueOf(seats));
                        } else holder.tvPassengers.setText("N/A");
                    });

            holder.tvTotalFare.setText("₱" + booking.getTotalFare());
            holder.tvStatus.setText(booking.getStatus());

            int bgRes;
            switch (booking.getStatus()) {
                case "Pending":
                    bgRes = R.drawable.bg_status_pending;
                    break;
                case "Confirmed":
                    bgRes = R.drawable.bg_status_confirmed;
                    break;
                case "Cancelled":
                    bgRes = R.drawable.bg_status_cancelled;
                    break;
                default:
                    bgRes = R.drawable.bg_status_completed;
            }
            holder.tvStatus.setBackgroundResource(bgRes);

            // ================================
            // Buttons
            // ================================
            holder.btnCancel.setOnClickListener(v -> showCancelConfirmationDialog(booking));

            if ("Confirmed".equals(booking.getStatus())) {
                holder.btnViewQR.setVisibility(View.VISIBLE);
                holder.btnViewQR.setOnClickListener(v -> showQrCodeDialog(booking.getBookingId()));
            } else {
                holder.btnViewQR.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            TextView txtUser, tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare, tvStatus, tvCreatedAt;
            Button btnCancel, btnViewQR;

            public BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                txtUser = itemView.findViewById(R.id.txtUser);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
                btnCancel = itemView.findViewById(R.id.btnCancel);
                btnViewQR = itemView.findViewById(R.id.btnViewQR);
            }
        }
    }

    // ================================
    // Cancel Confirmation
    // ================================
    private void showCancelConfirmationDialog(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes", (dialog, which) -> cancelBooking(booking))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelBooking(Booking booking) {
        db.collection("trips")
                .document(booking.getTripId())
                .update("availableSeats", FieldValue.increment(booking.getSeats()))
                .addOnSuccessListener(unused -> {
                    db.collection("bookings")
                            .document(booking.getBookingId())
                            .update("status", "Cancelled")
                            .addOnSuccessListener(unused2 ->
                                    Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error restoring seats: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================================
    // Show QR Code Dialog
    // ================================
    private void showQrCodeDialog(String bookingId) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            int size = 600;
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(bookingId, BarcodeFormat.QR_CODE, size, size);

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(24, 24, 24, 24);

            new AlertDialog.Builder(this)
                    .setTitle("Booking QR Code. Show this to the Dispatcher present.")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
