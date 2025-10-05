package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageReservationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ManageReservationsAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;

    private Spinner spinnerStatus, spinnerRoute;
    private Button btnDateFilter;

    private String selectedStatus = "All";
    private String selectedRoute = "All";
    private String selectedDate = null;

    private List<String> routeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_reservations);

        recyclerView = findViewById(R.id.recyclerBookings);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageReservationsAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerRoute = findViewById(R.id.spinnerRoute);
        btnDateFilter = findViewById(R.id.btnDateFilter);

        db = FirebaseFirestore.getInstance();

        setupFilters();

        swipeRefresh.setOnRefreshListener(this::loadBookings);
        loadBookings();
    }

    private void setupFilters() {
        // Status filter
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                this, R.array.status_filter_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = parent.getItemAtPosition(position).toString();
                loadBookings();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Load all routes into route spinner
        routeList.clear();
        routeList.add("All");
        db.collection("destinations").get().addOnSuccessListener(query -> {
            for (DocumentSnapshot doc : query) {
                String name = doc.getString("name");
                if (name != null) routeList.add(name);
            }

            ArrayAdapter<String> routeAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, routeList);
            routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRoute.setAdapter(routeAdapter);

            spinnerRoute.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedRoute = parent.getItemAtPosition(position).toString();
                    loadBookings();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        });

        // Date filter button
        btnDateFilter.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (DatePicker view, int year, int monthOfYear, int dayOfMonth) -> {
                    calendar.set(year, monthOfYear, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());
                    btnDateFilter.setText(selectedDate);
                    loadBookings();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadBookings() {
        swipeRefresh.setRefreshing(true);
        Query query = db.collection("bookings");

        // Apply filters dynamically
        if (!"All".equals(selectedStatus)) {
            query = query.whereEqualTo("status", selectedStatus);
        }

        query = query.orderBy("departure", Query.Direction.ASCENDING);

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
                return;
            }

            bookingList.clear();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking == null) continue;

                    // Filter by date
                    if (selectedDate != null && booking.getDeparture() != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String bookingDate = sdf.format(booking.getDeparture().toDate());
                        if (!bookingDate.equals(selectedDate)) continue;
                    }

                    // Filter by route
                    if (!"All".equals(selectedRoute)) {
                        String tripId = booking.getTripId();
                        if (tripId == null) continue;

                        // Check trip’s destination
                        DocumentSnapshot tripDoc = value.getDocuments().stream()
                                .filter(d -> d.getId().equals(tripId))
                                .findFirst().orElse(null);
                    }

                    booking.setBookingId(doc.getId());
                    bookingList.add(booking);
                }
            }

            adapter.notifyDataSetChanged();
            swipeRefresh.setRefreshing(false);
        });
    }

    // =====================
    // Update booking status
    // =====================
    private void updateBookingStatus(String bookingId, String status, int seats, String tripId, String reason) {
        if ("Cancelled".equals(status)) {
            db.collection("trips")
                    .document(tripId)
                    .update("availableSeats", FieldValue.increment(seats))
                    .addOnSuccessListener(unused -> {
                        db.collection("bookings")
                                .document(bookingId)
                                .update("status", status, "reason", reason)
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
    // Adapter class
    // =====================
    private class ManageReservationsAdapter extends RecyclerView.Adapter<ManageReservationsAdapter.BookingViewHolder> {

        private List<Booking> bookings;
        private FirebaseFirestore db;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

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

            // Fetch seats and passenger types
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

                            if (breakdown.length() > 0) {
                                holder.tvPassengers.setText(seats + " (" + breakdown.toString() + ")");
                            } else {
                                holder.tvPassengers.setText(String.valueOf(seats));
                            }

                            holder.btnConfirm.setOnClickListener(v ->
                                    updateBookingStatus(booking.getBookingId(), "Confirmed", (int) seats, booking.getTripId(), null)
                            );

                            holder.btnCancel.setOnClickListener(v -> showCancelReasonDialog(booking.getBookingId(), booking.getTripId(), (int) seats));
                        } else {
                            holder.tvPassengers.setText("N/A");
                        }
                    });

            String dateStr = booking.getDeparture() != null ? sdf.format(booking.getDeparture().toDate()) : "N/A";
            holder.tvDeparture.setText(dateStr);
            holder.tvTotalFare.setText("₱" + booking.getTotalFare());

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

        private void showCancelReasonDialog(String bookingId, String tripId, int seats) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ManageReservationsActivity.this);
            builder.setTitle("Reason for Cancellation");

            final EditText input = new EditText(ManageReservationsActivity.this);
            input.setHint("Enter reason...");
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);

            builder.setPositiveButton("Submit", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(ManageReservationsActivity.this, "Please provide a reason.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateBookingStatus(bookingId, "Cancelled", seats, tripId, reason);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
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
