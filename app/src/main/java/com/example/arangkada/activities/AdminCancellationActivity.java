package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminCancellationActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private CancellationAdapter adapter;

    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private List<Booking> currentPageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentAdminId;

    private TextView tvEmpty, tvPageIndicator;
    private Button btnPrev, btnNext;
    private Spinner spinnerStatus, spinnerDestination;
    private EditText etDateFrom, etDateTo;

    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages = 1;

    private Date dateFrom = null;
    private Date dateTo = null;
    private String selectedStatus = "All";
    private String selectedDestinationId = "All";

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        View contentView = getLayoutInflater().inflate(
                R.layout.activity_admin_cancellation,
                findViewById(R.id.content_frame),
                true
        );

        setupNavigation();

        recyclerView = contentView.findViewById(R.id.recyclerCancellations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CancellationAdapter(currentPageList);
        recyclerView.setAdapter(adapter);

        tvEmpty = contentView.findViewById(R.id.tvEmpty);
        btnPrev = contentView.findViewById(R.id.btnPrev);
        btnNext = contentView.findViewById(R.id.btnNext);
        tvPageIndicator = contentView.findViewById(R.id.tvPageIndicator);
        spinnerStatus = contentView.findViewById(R.id.spinnerStatus);
        spinnerDestination = contentView.findViewById(R.id.spinnerDestination);
        etDateFrom = contentView.findViewById(R.id.etDateFrom);
        etDateTo = contentView.findViewById(R.id.etDateTo);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current admin ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentAdminId = currentUser.getUid();

        setupFilters();
        loadAllBookings();

        etDateFrom.setOnClickListener(v -> showDatePickerDialog(true));
        etDateTo.setOnClickListener(v -> showDatePickerDialog(false));

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });
    }
    @Override
    protected void onNavigationSetup() {
        // Optional: Add menu logic here if needed
    }


    private void setupFilters() {
        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("All", "Cancelled", "Completed"));
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedStatus = parent.getItemAtPosition(pos).toString();
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Destination Spinner
        List<String> destinationNames = new ArrayList<>();
        List<String> destinationIds = new ArrayList<>();
        destinationNames.add("All");
        destinationIds.add("All");

        db.collection("destinations").get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot) {
                destinationNames.add(doc.getString("name"));
                destinationIds.add(doc.getId());
            }

            ArrayAdapter<String> destAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    destinationNames);
            spinnerDestination.setAdapter(destAdapter);

            spinnerDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    selectedDestinationId = destinationIds.get(pos);
                    applyFilters();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void showDatePickerDialog(boolean isFrom) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isFrom) {
                        dateFrom = calendar.getTime();
                        etDateFrom.setText(dateFormatter.format(dateFrom));
                    } else {
                        dateTo = calendar.getTime();
                        etDateTo.setText(dateFormatter.format(dateTo));
                    }
                    applyFilters(); // Auto filter after picking date
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private void loadAllBookings() {
        db.collection("bookings")
                .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allBookings.clear();
                    if (value != null && !value.isEmpty()) {
                        List<Booking> tempBookings = new ArrayList<>();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                tempBookings.add(booking);
                            }
                        }

                        // Fetch trip details and filter by adminID
                        if (tempBookings.isEmpty()) {
                            applyFilters();
                            return;
                        }

                        final int totalBookings = tempBookings.size();
                        final int[] processedCount = {0};

                        for (Booking booking : tempBookings) {
                            db.collection("trips").document(booking.getTripId()).get()
                                    .addOnSuccessListener(tripDoc -> {
                                        processedCount[0]++;

                                        if (tripDoc.exists()) {
                                            String tripAdminId = tripDoc.getString("adminID");
                                            String destId = tripDoc.getString("destinationId");

                                            // Only add booking if trip belongs to current admin
                                            if (tripAdminId != null && tripAdminId.equals(currentAdminId)) {
                                                booking.setDestinationId(destId);
                                                allBookings.add(booking);
                                            }
                                        }

                                        // When all bookings are processed, apply filters
                                        if (processedCount[0] == totalBookings) {
                                            applyFilters();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processedCount[0]++;
                                        // Continue even if one fails
                                        if (processedCount[0] == totalBookings) {
                                            applyFilters();
                                        }
                                    });
                        }

                    } else {
                        tvEmpty.setVisibility(View.VISIBLE);
                        filteredBookings.clear();
                        currentPageList.clear();
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void applyFilters() {
        filteredBookings.clear();

        for (Booking b : allBookings) {
            boolean match = true;

            if (!selectedStatus.equals("All") && !b.getStatus().equalsIgnoreCase(selectedStatus))
                match = false;

            // ✅ Destination filtering logic
            if (!selectedDestinationId.equals("All")) {
                if (b.getDestinationId() == null || !b.getDestinationId().equals(selectedDestinationId)) {
                    match = false;
                }
            }

            if (dateFrom != null && b.getCreatedAt() != null && b.getCreatedAt().toDate().before(dateFrom))
                match = false;
            if (dateTo != null && b.getCreatedAt() != null && b.getCreatedAt().toDate().after(dateTo))
                match = false;

            if (match) filteredBookings.add(b);
        }

        filteredBookings.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        totalPages = Math.max(1, (int) Math.ceil((double) filteredBookings.size() / ITEMS_PER_PAGE));
        currentPage = 1;
        updatePagination();
    }

    private void updatePagination() {
        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredBookings.size());

        currentPageList.clear();
        if (!filteredBookings.isEmpty())
            currentPageList.addAll(filteredBookings.subList(start, end));

        adapter.notifyDataSetChanged();
        tvPageIndicator.setText(currentPage + " / " + totalPages);
        tvEmpty.setVisibility(filteredBookings.isEmpty() ? View.VISIBLE : View.GONE);

        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }


    public static class Booking {
        private String bookingId;
        private String userId;
        private String tripId;
        private String destinationId;
        private String status;
        private String reason;
        private double totalFare;
        private Timestamp departure;
        private Timestamp createdAt;
        private int seats;
        private int regularCount;
        private int seniorCount;
        private int studentCount;

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
        public int getRegularCount() { return regularCount; }
        public int getSeniorCount() { return seniorCount; }
        public int getStudentCount() { return studentCount; }

        public String getDestinationId() { return destinationId; }
        public void setDestinationId(String destinationId) { this.destinationId = destinationId; }
    }


    private class CancellationAdapter extends RecyclerView.Adapter<CancellationAdapter.CancellationViewHolder> {
        private final List<Booking> bookings;
        public CancellationAdapter(List<Booking> bookings) { this.bookings = bookings; }

        @NonNull
        @Override
        public CancellationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
            return new CancellationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CancellationViewHolder holder, int position) {
            Booking booking = bookings.get(position);
            String status = booking.getStatus();

            holder.tvStatus.setText(status);
            holder.tvStatus.setBackgroundResource(
                    "Cancelled".equalsIgnoreCase(status)
                            ? R.drawable.bg_status_cancelled
                            : R.drawable.bg_status_completed);

            // Username
            db.collection("accounts").document(booking.getUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists())
                            holder.tvUsername.setText(userDoc.getString("name"));
                        else holder.tvUsername.setText("Unknown User");
                    });

            // Trip
            db.collection("trips").document(booking.getTripId()).get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            holder.tvVan.setText(tripDoc.getString("vanId"));
                            String destinationId = tripDoc.getString("destinationId");
                            if (destinationId != null) {
                                db.collection("destinations").document(destinationId).get()
                                        .addOnSuccessListener(destDoc -> {
                                            holder.tvRoute.setText(destDoc.exists()
                                                    ? destDoc.getString("name")
                                                    : "Unknown");
                                        });
                            }
                        }
                    });

            String departureStr = booking.getDeparture() != null
                    ? DateFormat.format("MMM dd, yyyy hh:mm a", booking.getDeparture().toDate()).toString()
                    : "N/A";
            holder.tvDeparture.setText(departureStr);

            if (booking.getCreatedAt() != null) {
                holder.tvCreatedAt.setText("Booked at: " +
                        DateFormat.format("MMM dd, yyyy hh:mm a", booking.getCreatedAt().toDate()));
            } else holder.tvCreatedAt.setText("Booked at: N/A");

            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));


            StringBuilder passengersBreakdown = new StringBuilder();
            passengersBreakdown.append(booking.getSeats()).append(" (");

            boolean hasBreakdown = false;
            if (booking.getRegularCount() > 0) {
                passengersBreakdown.append("Regular: ").append(booking.getRegularCount());
                hasBreakdown = true;
            }
            if (booking.getStudentCount() > 0) {
                if (hasBreakdown) passengersBreakdown.append(", ");
                passengersBreakdown.append("Student: ").append(booking.getStudentCount());
                hasBreakdown = true;
            }
            if (booking.getSeniorCount() > 0) {
                if (hasBreakdown) passengersBreakdown.append(", ");
                passengersBreakdown.append("Senior: ").append(booking.getSeniorCount());
                hasBreakdown = true;
            }
            passengersBreakdown.append(")");

            holder.tvPassengers.setText(passengersBreakdown.toString());

            if ("Cancelled".equalsIgnoreCase(status) && booking.getReason() != null) {
                holder.tvReason.setVisibility(View.VISIBLE);
                holder.tvReason.setText("Reason: " + booking.getReason());
            } else holder.tvReason.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() { return bookings.size(); }

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