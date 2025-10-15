package com.example.arangkada.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class MyTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser user;

    private TextView tvEmpty;
    private Button btnSelect;
    private Button btnPrevPage, btnNextPage;
    private TextView tvPageIndicator;

    // Bottom action bar (slides up from bottom)
    private View bottomActionBar;
    private Button btnArchiveAction, btnDeleteAction, btnCancelAction;

    // Pagination helpers
    private static final int PAGE_SIZE = 10;
    private List<DocumentSnapshot> allVisibleDocs = new ArrayList<>();
    private int currentPageIndex = 0;

    // Selection mode
    private boolean selectionMode = false;
    private final Set<String> selectedBookingIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TripAdapter(bookingList);
        recyclerView.setAdapter(adapter);

        tvEmpty = findViewById(R.id.tvEmpty);
        btnSelect = findViewById(R.id.btnSelect);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        bottomActionBar = findViewById(R.id.bottomActionBar);
        btnArchiveAction = findViewById(R.id.btnArchiveAction);
        btnDeleteAction = findViewById(R.id.btnDeleteAction);
        btnCancelAction = findViewById(R.id.btnCancelAction);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Button handlers
        btnSelect.setOnClickListener(v -> toggleSelectionMode());
        btnPrevPage.setOnClickListener(v -> gotoPreviousPage());
        btnNextPage.setOnClickListener(v -> gotoNextPage());

        btnArchiveAction.setOnClickListener(v -> handleArchiveAction());
        btnDeleteAction.setOnClickListener(v -> handleDeleteAction());
        btnCancelAction.setOnClickListener(v -> exitSelectionMode());

        if (user != null) {
            loadAllBookings();
        }
    }

    /* --------------------
       Pagination utilities
       -------------------- */

    private void loadAllBookings() {
        db.collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Filter: exclude hidden and auto-archive old bookings
                    allVisibleDocs.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Check if hidden
                        Boolean hidden = doc.getBoolean("hidden");
                        if (hidden != null && hidden) {
                            continue; // Skip hidden bookings
                        }

                        Booking b = doc.toObject(Booking.class);
                        if (b == null || b.getCreatedAt() == null) {
                            allVisibleDocs.add(doc);
                            continue;
                        }

                        Date created = b.getCreatedAt().toDate();
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, -30);
                        Date cutoff = cal.getTime();

                        if (created.before(cutoff)) {
                            archiveBookingDocument(doc);
                        } else {
                            allVisibleDocs.add(doc);
                        }
                    }

                    // Fetch favorites and display first page
                    fetchFavoritesAndDisplayPage(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MyTripsActivity.this, "Error fetching history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchFavoritesAndDisplayPage(int pageIndex) {
        db.collection("favorites")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(favSnapshot -> {
                    Set<String> favIds = new HashSet<>();
                    for (DocumentSnapshot f : favSnapshot.getDocuments()) {
                        String bId = f.getString("bookingId");
                        if (bId != null) favIds.add(bId);
                    }

                    // Convert all docs to Booking objects
                    List<Booking> allBookings = new ArrayList<>();
                    for (DocumentSnapshot doc : allVisibleDocs) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            booking.setBookingId(doc.getId());
                            booking.setFavorite(favIds.contains(doc.getId()));
                            allBookings.add(booking);
                        }
                    }

                    // Sort: favorites first, then by date
                    Collections.sort(allBookings, new Comparator<Booking>() {
                        @Override
                        public int compare(Booking a, Booking b) {
                            if (a.isFavorite() && !b.isFavorite()) return -1;
                            if (!a.isFavorite() && b.isFavorite()) return 1;
                            Date da = (a.getCreatedAt() != null) ? a.getCreatedAt().toDate() : new Date(0);
                            Date db = (b.getCreatedAt() != null) ? b.getCreatedAt().toDate() : new Date(0);
                            return db.compareTo(da);
                        }
                    });

                    // Display the requested page
                    displayPage(allBookings, pageIndex);
                })
                .addOnFailureListener(e -> {
                    // If favorites fetch fails, display without favorites
                    List<Booking> allBookings = new ArrayList<>();
                    for (DocumentSnapshot doc : allVisibleDocs) {
                        Booking booking = doc.toObject(Booking.class);
                        if (booking != null) {
                            booking.setBookingId(doc.getId());
                            booking.setFavorite(false);
                            allBookings.add(booking);
                        }
                    }
                    displayPage(allBookings, pageIndex);
                });
    }

    private void displayPage(List<Booking> allBookings, int pageIndex) {
        int totalItems = allBookings.size();
        int totalPages = Math.max(1, (totalItems + PAGE_SIZE - 1) / PAGE_SIZE);

        // Validate page index
        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;

        currentPageIndex = pageIndex;

        // Calculate start and end indices
        int startIndex = pageIndex * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalItems);

        // Extract page items
        bookingList.clear();
        if (startIndex < totalItems) {
            bookingList.addAll(allBookings.subList(startIndex, endIndex));
        }

        adapter.notifyDataSetChanged();
        updateUiAfterLoad(totalPages);
    }

    private void gotoNextPage() {
        int totalPages = Math.max(1, (allVisibleDocs.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (currentPageIndex < totalPages - 1) {
            fetchFavoritesAndDisplayPage(currentPageIndex + 1);
        }
    }

    private void gotoPreviousPage() {
        if (currentPageIndex > 0) {
            fetchFavoritesAndDisplayPage(currentPageIndex - 1);
        }
    }

    private void updateUiAfterLoad(int totalPages) {
        if (bookingList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            btnSelect.setVisibility(View.GONE);
            btnPrevPage.setVisibility(View.GONE);
            btnNextPage.setVisibility(View.GONE);
            tvPageIndicator.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            btnSelect.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);

            boolean hasPrev = currentPageIndex > 0;
            boolean hasNext = currentPageIndex < totalPages - 1;

            btnPrevPage.setVisibility(hasPrev ? View.VISIBLE : View.INVISIBLE);
            btnNextPage.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);

            tvPageIndicator.setVisibility(View.VISIBLE);
            tvPageIndicator.setText((currentPageIndex + 1) + " / " + totalPages);
        }
    }

    /* --------------------
       Selection Mode (Gmail-style)
       -------------------- */

    private void toggleSelectionMode() {
        if (selectionMode) {
            exitSelectionMode();
        } else {
            enterSelectionMode();
        }
    }

    private void enterSelectionMode() {
        selectionMode = true;
        selectedBookingIds.clear();
        btnSelect.setText("Cancel");
        slideUpActionBar();
        updateActionBarButtons();
        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectedBookingIds.clear();
        btnSelect.setText("Select");
        slideDownActionBar();
        adapter.notifyDataSetChanged();
    }

    private void slideUpActionBar() {
        if (bottomActionBar.getVisibility() == View.VISIBLE) return;

        bottomActionBar.setVisibility(View.VISIBLE);
        bottomActionBar.clearAnimation();
        TranslateAnimation animate = new TranslateAnimation(
                0, 0,
                bottomActionBar.getHeight(), 0);
        animate.setDuration(300);
        animate.setFillAfter(false);
        bottomActionBar.startAnimation(animate);
    }

    private void slideDownActionBar() {
        if (bottomActionBar.getVisibility() != View.VISIBLE) return;

        bottomActionBar.clearAnimation();
        TranslateAnimation animate = new TranslateAnimation(
                0, 0,
                0, bottomActionBar.getHeight());
        animate.setDuration(300);
        animate.setFillAfter(false);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                bottomActionBar.setVisibility(View.GONE);
                bottomActionBar.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        bottomActionBar.startAnimation(animate);
    }

    private void updateActionBarButtons() {
        if (selectedBookingIds.isEmpty()) {
            btnArchiveAction.setText("Archive All");
            btnDeleteAction.setText("Delete All");
        } else {
            btnArchiveAction.setText("Archive");
            btnDeleteAction.setText("Delete");
        }
    }

    private void handleArchiveAction() {
        if (selectedBookingIds.isEmpty()) {
            archiveAll();
        } else {
            archiveSelected();
        }
    }

    private void handleDeleteAction() {
        if (selectedBookingIds.isEmpty()) {
            deleteAll();
        } else {
            deleteSelected();
        }
    }

    /* --------------------
       Archive / Delete helpers
       -------------------- */

    private void archiveAll() {
        new AlertDialog.Builder(this)
                .setTitle("Archive All")
                .setMessage("Archive all booking history?")
                .setPositiveButton("Archive", (dialog, which) -> {
                    db.collection("bookings")
                            .whereEqualTo("userId", user.getUid())
                            .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                            .get()
                            .addOnSuccessListener(query -> {
                                for (DocumentSnapshot doc : query.getDocuments()) {
                                    Boolean hidden = doc.getBoolean("hidden");
                                    if (hidden == null || !hidden) {
                                        archiveBookingDocument(doc);
                                    }
                                }
                                Toast.makeText(MyTripsActivity.this, "All history archived.", Toast.LENGTH_SHORT).show();
                                exitSelectionMode();
                                loadAllBookings();
                            })
                            .addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Error archiving history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All")
                .setMessage("Hide all booking history from your view? (Admins can still see them)")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("bookings")
                            .whereEqualTo("userId", user.getUid())
                            .whereIn("status", Arrays.asList("Cancelled", "Completed"))
                            .get()
                            .addOnSuccessListener(query -> {
                                for (DocumentSnapshot doc : query.getDocuments()) {
                                    Boolean hidden = doc.getBoolean("hidden");
                                    if (hidden == null || !hidden) {
                                        doc.getReference().update("hidden", true);
                                    }
                                }
                                Toast.makeText(MyTripsActivity.this, "All history hidden from view.", Toast.LENGTH_SHORT).show();
                                exitSelectionMode();
                                loadAllBookings();
                            })
                            .addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Error hiding history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void archiveSelected() {
        new AlertDialog.Builder(this)
                .setTitle("Archive Selected")
                .setMessage("Archive " + selectedBookingIds.size() + " booking(s)?")
                .setPositiveButton("Archive", (dialog, which) -> {
                    for (String id : new ArrayList<>(selectedBookingIds)) {
                        db.collection("bookings").document(id).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) archiveBookingDocument(doc);
                                });
                    }
                    Toast.makeText(this, "Selected archived.", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                    loadAllBookings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelected() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected")
                .setMessage("Hide " + selectedBookingIds.size() + " booking(s) from your view?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (String id : new ArrayList<>(selectedBookingIds)) {
                        db.collection("bookings").document(id).update("hidden", true);
                    }
                    Toast.makeText(this, "Selected hidden from view.", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                    loadAllBookings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void archiveBookingDocument(DocumentSnapshot bookingDoc) {
        if (bookingDoc == null || !bookingDoc.exists()) return;
        String id = bookingDoc.getId();
        DocumentReference archivedRef = db.collection("archived").document(id);
        archivedRef.set(bookingDoc.getData() == null ? new java.util.HashMap<>() : bookingDoc.getData())
                .addOnSuccessListener(aVoid -> {
                    archivedRef.update("archivedAt", FieldValue.serverTimestamp());
                    bookingDoc.getReference().update("hidden", true);
                })
                .addOnFailureListener(e -> {
                    // Ignore failure
                });
    }

    /* --------------------
       Favorite helpers
       -------------------- */

    private void toggleFavorite(String bookingId) {
        if (bookingId == null || user == null) return;
        String favDocId = user.getUid() + "_" + bookingId;
        DocumentReference favRef = db.collection("favorites").document(favDocId);

        favRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                favRef.delete().addOnSuccessListener(aVoid -> {
                    Toast.makeText(MyTripsActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    loadAllBookings(); // Refresh to go back to page 1
                });
            } else {
                db.collection("favorites")
                        .whereEqualTo("userId", user.getUid())
                        .get()
                        .addOnSuccessListener(q -> {
                            int count = q.size();
                            if (count >= 10) {
                                Toast.makeText(MyTripsActivity.this, "Maximum of 10 favorites allowed.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            favRef.set(new java.util.HashMap<String, Object>() {{
                                put("userId", user.getUid());
                                put("bookingId", bookingId);
                                put("favoritedAt", FieldValue.serverTimestamp());
                            }}).addOnSuccessListener(aVoid -> {
                                Toast.makeText(MyTripsActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                                loadAllBookings(); // Refresh to go back to page 1
                            }).addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Failed to add favorite.", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Failed to check favorites.", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(MyTripsActivity.this, "Error toggling favorite.", Toast.LENGTH_SHORT).show());
    }

    /* --------------------
       Models & Adapter
       -------------------- */

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
        private boolean hidden = false;

        private boolean favorite = false;

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
        public boolean isHidden() { return hidden; }

        public boolean isFavorite() { return favorite; }
        public void setFavorite(boolean favorite) { this.favorite = favorite; }
    }

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

            // Checkbox visibility
            holder.checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            holder.checkbox.setChecked(selectedBookingIds.contains(booking.getBookingId()));
            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBookingIds.add(booking.getBookingId());
                } else {
                    selectedBookingIds.remove(booking.getBookingId());
                }
                updateActionBarButtons();
            });

            // Star icon for favorites
            holder.starIcon.setImageResource(booking.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            holder.starIcon.setOnClickListener(v -> toggleFavorite(booking.getBookingId()));

            // Card background
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);

            // Status
            String status = booking.getStatus();
            holder.tvStatus.setText(status);
            if ("Completed".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
            } else {
                holder.tvStatus.setBackgroundResource(0);
            }

            // Username
            db.collection("accounts").document(booking.getUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = (userDoc.exists() ? userDoc.getString("name") : "Unknown User");
                        holder.tvUsername.setText(name);
                    }).addOnFailureListener(e -> holder.tvUsername.setText("Unknown User"));

            // Trip details
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
                                        }).addOnFailureListener(e -> holder.tvRoute.setText("Unknown"));
                            }
                        }
                    }).addOnFailureListener(e -> {
                        holder.tvRoute.setText("Unknown");
                        holder.tvVan.setText("Unknown");
                    });

            // Departure
            String departureStr = booking.getDeparture() != null
                    ? DateFormat.format("MMM dd, yyyy hh:mm a", booking.getDeparture().toDate()).toString()
                    : "N/A";
            holder.tvDeparture.setText(departureStr);

            // Created At
            if (booking.getCreatedAt() != null) {
                String createdStr = DateFormat.format("MMM dd, yyyy hh:mm a", booking.getCreatedAt().toDate()).toString();
                holder.tvCreatedAt.setText(createdStr);
            } else {
                holder.tvCreatedAt.setText("N/A");
            }

            // Passengers
            holder.tvPassengers.setText(String.valueOf(booking.getSeats()));

            if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
                String reason = booking.getReason();
                if (reason != null && !reason.trim().isEmpty()) {
                    holder.tvReason.setVisibility(View.VISIBLE);
                    holder.tvReason.setText("Reason: " + reason);
                } else {
                    holder.tvReason.setVisibility(View.GONE);
                }
            } else {
                holder.tvReason.setVisibility(View.GONE);
            }

            // Fare
            holder.tvTotalFare.setText("₱" + String.format("%.2f", booking.getTotalFare()));

            // Click behavior
            holder.itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    holder.checkbox.setChecked(!holder.checkbox.isChecked());
                }
            });
        }

        @Override
        public int getItemCount() {
            return bookings.size();
        }

        class TripViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkbox;
            ImageView starIcon;
            TextView tvRoute, tvVan, tvDeparture, tvPassengers, tvTotalFare,
                    tvStatus, tvCreatedAt, tvUsername, tvReason;

            public TripViewHolder(@NonNull View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.checkbox);
                starIcon = itemView.findViewById(R.id.starIcon);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}