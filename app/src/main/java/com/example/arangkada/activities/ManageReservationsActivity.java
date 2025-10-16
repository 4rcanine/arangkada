package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arangkada.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ManageReservationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ManageReservationsAdapter adapter;

    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private List<Booking> currentPageList = new ArrayList<>();

    private FirebaseFirestore db;

    private Spinner spinnerStatus, spinnerDestination;
    private EditText etDateFrom, etDateTo;
    private TextView tvPageIndicator;
    private Button btnPrev, btnNext;

    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages = 1;

    private static final String PREFS_NAME = "ManageReservationsPrefs";
    private static final String KEY_CURRENT_PAGE = "current_page";

    private Date dateFrom = null;
    private Date dateTo = null;
    private String selectedStatus = "All";
    private String selectedDestinationId = "All";

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_reservations);

        recyclerView = findViewById(R.id.recyclerBookings);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageReservationsAdapter(currentPageList);
        recyclerView.setAdapter(adapter);

        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerDestination = findViewById(R.id.spinnerDestination);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        db = FirebaseFirestore.getInstance();

        // Load last saved page number
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentPage = prefs.getInt(KEY_CURRENT_PAGE, 1);

        setupFilters();
        loadAllBookings();

        swipeRefresh.setOnRefreshListener(() -> loadAllBookings(false));

        etDateFrom.setOnClickListener(v -> showDatePickerDialog(true));
        etDateTo.setOnClickListener(v -> showDatePickerDialog(false));

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                saveCurrentPage();
                updatePagination();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                saveCurrentPage();
                updatePagination();
            }
        });
    }

    // Save current page to SharedPreferences
    private void saveCurrentPage() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_CURRENT_PAGE, currentPage);
        editor.apply();
    }

    private void setupFilters() {
        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("All", "Pending", "Confirmed"));
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setSelection(statusAdapter.getPosition(selectedStatus));
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedStatus = parent.getItemAtPosition(pos).toString();
                applyFilters(false);
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

            int selectedIndex = destinationIds.indexOf(selectedDestinationId);
            if (selectedIndex >= 0) spinnerDestination.setSelection(selectedIndex);

            spinnerDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    selectedDestinationId = destinationIds.get(pos);
                    applyFilters(false);
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
                    applyFilters(false);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    // loadAllBookings - keep filters & page
    private void loadAllBookings() {
        loadAllBookings(true);
    }

    private void loadAllBookings(boolean resetPage) {
        swipeRefresh.setRefreshing(true);
        db.collection("bookings")
                .whereIn("status", Arrays.asList("Pending", "Confirmed"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(value -> {
                    swipeRefresh.setRefreshing(false);
                    allBookings.clear();

                    if (value != null && !value.isEmpty()) {
                        List<Booking> tempList = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                tempList.add(booking);
                            }
                        }

                        // Fetch destinations for each booking
                        if (tempList.isEmpty()) return;
                        for (Booking booking : tempList) {
                            db.collection("trips").document(booking.getTripId()).get()
                                    .addOnSuccessListener(tripDoc -> {
                                        if (tripDoc.exists()) {
                                            booking.setDestinationId(tripDoc.getString("destinationId"));
                                        }
                                        allBookings.add(booking);
                                        // Apply filters only once after all bookings loaded
                                        if (allBookings.size() == tempList.size()) {
                                            if (resetPage) currentPage = 1; // reset only if requested
                                            applyFilters(false);
                                        }
                                    });
                        }
                    } else {
                        applyFilters(false);
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters(boolean resetPage) {
        filteredBookings.clear();

        for (Booking b : allBookings) {
            boolean match = true;

            if (!selectedStatus.equals("All") && !b.getStatus().equalsIgnoreCase(selectedStatus))
                match = false;

            if (!selectedDestinationId.equals("All")) {
                if (b.getDestinationId() == null || !b.getDestinationId().equals(selectedDestinationId))
                    match = false;
            }

            if (dateFrom != null && b.getDeparture() != null && b.getDeparture().toDate().before(dateFrom))
                match = false;
            if (dateTo != null && b.getDeparture() != null && b.getDeparture().toDate().after(dateTo))
                match = false;

            if (match) filteredBookings.add(b);
        }

        filteredBookings.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        totalPages = Math.max(1, (int) Math.ceil((double) filteredBookings.size() / ITEMS_PER_PAGE));

        // Ensure currentPage is valid
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        saveCurrentPage(); // persist after filtering
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

        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    // ======================================================
    // Adapter
    // ======================================================
    private class ManageReservationsAdapter extends RecyclerView.Adapter<ManageReservationsAdapter.BookingViewHolder> {

        private final List<Booking> bookings;
        private final FirebaseFirestore db;
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

            db.collection("accounts").document(booking.getUserId()).get()
                    .addOnSuccessListener(doc -> holder.txtUser.setText(doc.exists() ? doc.getString("name") : "Unknown User"));

            db.collection("trips").document(booking.getTripId()).get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            String destinationId = tripDoc.getString("destinationId");
                            String vanId = tripDoc.getString("vanId");
                            holder.tvVan.setText(vanId != null ? vanId : "Unknown");
                            if (destinationId != null) {
                                db.collection("destinations").document(destinationId).get()
                                        .addOnSuccessListener(destDoc ->
                                                holder.tvRoute.setText(destDoc.exists() ? destDoc.getString("name") : "Unknown"));
                            }
                        }
                    });

            holder.tvDeparture.setText(booking.getDeparture() != null ? sdf.format(booking.getDeparture().toDate()) : "N/A");
            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));
            holder.tvPassengers.setText(String.valueOf(booking.getSeats()));
            holder.tvStatus.setText(booking.getStatus());

            switch (booking.getStatus()) {
                case "Confirmed":
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    break;
                case "Pending":
                default:
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
            }

            holder.btnConfirm.setOnClickListener(v -> updateBookingStatus(booking.getBookingId(), "Confirmed", booking.getSeats(), booking.getTripId(), null));
            holder.btnCancel.setOnClickListener(v -> showCancelReasonDialog(booking.getBookingId(), booking.getTripId(), booking.getSeats()));
        }

        @Override
        public int getItemCount() { return bookings.size(); }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            TextView txtUser, tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare, tvStatus;
            Button btnConfirm, btnCancel;
            BookingViewHolder(@NonNull View itemView) {
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

    private void updateBookingStatus(String bookingId, String status, int seats, String tripId, String reason) {
        if ("Cancelled".equals(status)) {
            db.collection("trips").document(tripId)
                    .update("availableSeats", FieldValue.increment(seats))
                    .addOnSuccessListener(unused -> db.collection("bookings")
                            .document(bookingId)
                            .update("status", status, "reason", reason)
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Booking Cancelled & seats restored", Toast.LENGTH_SHORT).show();
                                loadAllBookings(false); // preserve page & filters
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error restoring seats: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("bookings").document(bookingId)
                    .update("status", status)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Booking " + status, Toast.LENGTH_SHORT).show();
                        loadAllBookings(false); // preserve page & filters
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showCancelReasonDialog(String bookingId, String tripId, int seats) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_reason, null);
        builder.setView(dialogView);

        TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
        Button btnBack = dialogView.findViewById(R.id.btnBack);
        Button btnProceed = dialogView.findViewById(R.id.btnProceed);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnProceed.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please provide a reason.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateBookingStatus(bookingId, "Cancelled", seats, tripId, reason);
            dialog.dismiss();
        });
        dialog.show();
    }

    // Booking Model (unchanged)
    public static class Booking {
        private String bookingId;
        private Timestamp createdAt;
        private Timestamp departure;
        private String destinationId;
        private int regularCount;
        private int seniorCount;
        private int studentCount;
        private int seats;
        private String status;
        private double totalFare;
        private String tripId;
        private String userId;

        public Booking() {}

        public String getBookingId() { return bookingId; }
        public Timestamp getCreatedAt() { return createdAt; }
        public Timestamp getDeparture() { return departure; }
        public String getDestinationId() { return destinationId; }
        public int getRegularCount() { return regularCount; }
        public int getSeniorCount() { return seniorCount; }
        public int getStudentCount() { return studentCount; }
        public int getSeats() { return seats; }
        public String getStatus() { return status; }
        public double getTotalFare() { return totalFare; }
        public String getTripId() { return tripId; }
        public String getUserId() { return userId; }

        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public void setDeparture(Timestamp departure) { this.departure = departure; }
        public void setDestinationId(String destinationId) { this.destinationId = destinationId; }
        public void setRegularCount(int regularCount) { this.regularCount = regularCount; }
        public void setSeniorCount(int seniorCount) { this.seniorCount = seniorCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public void setSeats(int seats) { this.seats = seats; }
        public void setStatus(String status) { this.status = status; }
        public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
        public void setTripId(String tripId) { this.tripId = tripId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}