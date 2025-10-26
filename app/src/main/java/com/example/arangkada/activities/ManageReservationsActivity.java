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

import com.bumptech.glide.Glide;
import com.example.arangkada.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ManageReservationsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ManageReservationsAdapter adapter;

    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private List<Booking> currentPageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentAdminId;

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
        setContentView(R.layout.activity_base);

        View contentView = getLayoutInflater().inflate(
                R.layout.activity_manage_reservations,
                findViewById(R.id.content_frame),
                true
        );

        setupNavigation();

        recyclerView = contentView.findViewById(R.id.recyclerBookings);
        swipeRefresh = contentView.findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageReservationsAdapter(currentPageList);
        recyclerView.setAdapter(adapter);

        spinnerStatus = contentView.findViewById(R.id.spinnerStatus);
        spinnerDestination = contentView.findViewById(R.id.spinnerDestination);
        etDateFrom = contentView.findViewById(R.id.etDateFrom);
        etDateTo = contentView.findViewById(R.id.etDateTo);
        btnPrev = contentView.findViewById(R.id.btnPrev);
        btnNext = contentView.findViewById(R.id.btnNext);
        tvPageIndicator = contentView.findViewById(R.id.tvPageIndicator);

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


    @Override
    protected void onNavigationSetup() {
        showBackButton(); // or showMenuButton()
        setToolbarTitle("Your Title");
    }
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

                        // Fetch trip details and filter by adminID
                        if (tempList.isEmpty()) {
                            applyFilters(false);
                            return;
                        }

                        final int totalBookings = tempList.size();
                        final int[] processedCount = {0};

                        for (Booking booking : tempList) {
                            db.collection("trips").document(booking.getTripId()).get()
                                    .addOnSuccessListener(tripDoc -> {
                                        processedCount[0]++;

                                        if (tripDoc.exists()) {
                                            String tripAdminId = tripDoc.getString("adminID");
                                            String destinationId = tripDoc.getString("destinationId");

                                            // Only add booking if trip belongs to current admin
                                            if (tripAdminId != null && tripAdminId.equals(currentAdminId)) {
                                                booking.setDestinationId(destinationId);
                                                allBookings.add(booking);
                                            }
                                        }

                                        // When all bookings are processed, apply filters
                                        if (processedCount[0] == totalBookings) {
                                            if (resetPage) currentPage = 1;
                                            applyFilters(false);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processedCount[0]++;
                                        // Continue even if one fails
                                        if (processedCount[0] == totalBookings) {
                                            if (resetPage) currentPage = 1;
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


        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        saveCurrentPage();
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

    private void showFullScreenImage(String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);

        ImageView imgFullScreen = dialogView.findViewById(R.id.imgFullScreen);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        // Load image
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgFullScreen);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Make dialog fullscreen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Hide status bar and navigation bar for true fullscreen
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        imgFullScreen.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }




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
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            // Set user name
                            String userName = doc.getString("name");
                            holder.txtUser.setText(userName != null ? userName : "Unknown User");

                            // Display mobile number
                            String userNumber = doc.getString("number");
                            holder.tvUserNumber.setText(userNumber != null && !userNumber.isEmpty()
                                    ? "Mobile: " + userNumber
                                    : "Mobile: N/A");

                            // Load profile picture or show placeholder
                            String profilePicture = doc.getString("profilePicture");
                            if (profilePicture != null && !profilePicture.isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(profilePicture)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .circleCrop()
                                        .into(holder.ivUserProfilePicture);
                            } else {
                                // Explicitly set placeholder when no profile picture exists
                                holder.ivUserProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        } else {
                            holder.txtUser.setText("Unknown User");
                            holder.tvUserNumber.setText("Mobile: N/A");
                            holder.ivUserProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.txtUser.setText("Unknown User");
                        holder.tvUserNumber.setText("Mobile: N/A");
                        holder.ivUserProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                    });

            db.collection("trips").document(booking.getTripId()).get()
                    .addOnSuccessListener(tripDoc -> {
                        if (tripDoc.exists()) {
                            String destinationId = tripDoc.getString("destinationId");
                            String vanId = tripDoc.getString("vanId");
                            String driverName = tripDoc.getString("driverName");
                            String driverNumber = tripDoc.getString("driverNumber");

                            holder.tvVan.setText(vanId != null ? vanId : "Unknown");

                            // Display driver name
                            holder.tvDriverName.setText(driverName != null ? driverName : "N/A");

                            // Display driver number
                            holder.tvDriverNumber.setText(driverNumber != null ? driverNumber : "N/A");

                            if (destinationId != null) {
                                db.collection("destinations").document(destinationId).get()
                                        .addOnSuccessListener(destDoc ->
                                                holder.tvRoute.setText(destDoc.exists() ? destDoc.getString("name") : "Unknown"));
                            }
                        }
                    });

            holder.tvDeparture.setText(booking.getDeparture() != null ? sdf.format(booking.getDeparture().toDate()) : "N/A");
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
            holder.tvStatus.setText(booking.getStatus());

            // Display payment method
            String paymentMethod = booking.getPaymentMethod() != null ? booking.getPaymentMethod() : "Cash";
            holder.tvPaymentMethod.setText("Payment: " + paymentMethod);

            // Handle payment proof button visibility
            if ("Gcash".equals(paymentMethod) && booking.getPaymentProofUrl() != null) {
                holder.btnViewProof.setVisibility(View.VISIBLE);
                holder.btnViewProof.setOnClickListener(v ->
                        showFullScreenImage(booking.getPaymentProofUrl())
                );
            } else {
                holder.btnViewProof.setVisibility(View.GONE);
            }

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
            ImageView ivUserProfilePicture;
            TextView txtUser, tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare, tvStatus, tvPaymentMethod, tvUserNumber;
            TextView tvDriverName, tvDriverNumber;
            Button btnConfirm, btnCancel, btnViewProof;

            BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserProfilePicture = itemView.findViewById(R.id.ivUserProfilePicture);
                txtUser = itemView.findViewById(R.id.txtUser);
                tvRoute = itemView.findViewById(R.id.tv_route);
                tvVan = itemView.findViewById(R.id.tv_van);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvTotalFare = itemView.findViewById(R.id.tv_total_fare);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
                btnConfirm = itemView.findViewById(R.id.btnConfirm);
                btnCancel = itemView.findViewById(R.id.btnCancel);
                btnViewProof = itemView.findViewById(R.id.btnViewProof);
                tvUserNumber = itemView.findViewById(R.id.tvUserNumber);
                tvDriverName = itemView.findViewById(R.id.tv_driver_name);
                tvDriverNumber = itemView.findViewById(R.id.tv_driver_number);
            }
        }
    }

    private void updateBookingStatus(String bookingId, String status, int seats, String tripId, String reason) {
        if ("Cancelled".equals(status)) {
            // Only restore seats if the booking was previously "Confirmed"
            db.collection("bookings").document(bookingId).get()
                    .addOnSuccessListener(bookingDoc -> {
                        if (bookingDoc.exists()) {
                            String currentStatus = bookingDoc.getString("status");

                            if ("Confirmed".equals(currentStatus)) {
                                // Booking was confirmed, restore seats
                                db.collection("trips").document(tripId)
                                        .update("availableSeats", FieldValue.increment(seats))
                                        .addOnSuccessListener(unused -> db.collection("bookings")
                                                .document(bookingId)
                                                .update("status", status, "reason", reason)
                                                .addOnSuccessListener(unused2 -> {
                                                    Toast.makeText(this, "Booking Cancelled & seats restored", Toast.LENGTH_SHORT).show();
                                                    loadAllBookings(false);
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                                        .addOnFailureListener(e -> Toast.makeText(this, "Error restoring seats: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                // Booking was pending, just cancel without restoring seats
                                db.collection("bookings").document(bookingId)
                                        .update("status", status, "reason", reason)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show();
                                            loadAllBookings(false);
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error fetching booking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else if ("Confirmed".equals(status)) {
            // Deduct seats when confirming
            db.collection("trips").document(tripId)
                    .update("availableSeats", FieldValue.increment(-seats))
                    .addOnSuccessListener(unused -> db.collection("bookings")
                            .document(bookingId)
                            .update("status", status)
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Booking Confirmed & seats deducted", Toast.LENGTH_SHORT).show();
                                loadAllBookings(false);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error updating booking: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error deducting seats: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("bookings").document(bookingId)
                    .update("status", status)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Booking " + status, Toast.LENGTH_SHORT).show();
                        loadAllBookings(false);
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

    // Booking Model
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
        private String paymentMethod;
        private String paymentProofUrl;

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
        public String getPaymentMethod() { return paymentMethod; }
        public String getPaymentProofUrl() { return paymentProofUrl; }

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
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public void setPaymentProofUrl(String paymentProofUrl) { this.paymentProofUrl = paymentProofUrl; }
    }
}