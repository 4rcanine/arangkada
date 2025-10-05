package com.example.arangkada.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class CurrentVanScheduleActivity extends AppCompatActivity {

    private RecyclerView rvTrips;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnCreateVanSchedule;
    private FirebaseFirestore db;

    // Mixed list: header strings + HashMap trip objects
    private final List<Object> items = new ArrayList<>();
    private final TripAdapter adapter = new TripAdapter();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TRIP = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_van_schedule);

        rvTrips = findViewById(R.id.recyclerViewTrips);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnCreateVanSchedule = findViewById(R.id.btnCreateVanSchedule);

        db = FirebaseFirestore.getInstance();

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        rvTrips.setAdapter(adapter);

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadTrips);

        // Create button -> ManageVansActivity
        btnCreateVanSchedule.setOnClickListener(v -> {
            startActivity(new Intent(CurrentVanScheduleActivity.this, ManageVansActivity.class));
        });

        loadTrips();
    }

    /**
     * Load trips for the current day, split into upcoming and completed,
     * and populate "items" with headers + trip HashMaps.
     */
    private void loadTrips() {
        showLoading(true);

        db.collection("trips")
                .orderBy("departure", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    items.clear();

                    List<HashMap<String, Object>> tripsToday = new ArrayList<>();
                    List<HashMap<String, Object>> upcomingTrips = new ArrayList<>();
                    List<HashMap<String, Object>> completedToday = new ArrayList<>();

                    Timestamp now = Timestamp.now();
                    Calendar cal = Calendar.getInstance();

                    // Start of today
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Timestamp startOfToday = new Timestamp(cal.getTime());

                    // End of today
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    Timestamp endOfToday = new Timestamp(cal.getTime());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (!doc.exists()) continue;

                        HashMap<String, Object> map = new HashMap<>();
                        map.putAll(doc.getData() != null ? doc.getData() : Collections.emptyMap());
                        map.put("id", doc.getId());

                        Object depObj = map.get("departure");
                        Timestamp depTs = (depObj instanceof Timestamp) ? (Timestamp) depObj : null;
                        if (depTs == null) continue;

                        // Categorize trip
                        if (depTs.compareTo(startOfToday) >= 0 && depTs.compareTo(endOfToday) <= 0) {
                            // It's a trip for today
                            if (depTs.compareTo(now) < 0) {
                                completedToday.add(map); // already departed
                            } else {
                                tripsToday.add(map); // will depart later today
                            }
                        } else if (depTs.compareTo(endOfToday) > 0) {
                            upcomingTrips.add(map); // future trips (tomorrow+)
                        }
                    }

                    // Sort each list
                    tripsToday.sort((a, b) -> ((Timestamp) a.get("departure")).compareTo((Timestamp) b.get("departure")));
                    upcomingTrips.sort((a, b) -> ((Timestamp) a.get("departure")).compareTo((Timestamp) b.get("departure")));
                    completedToday.sort((a, b) -> ((Timestamp) b.get("departure")).compareTo((Timestamp) a.get("departure")));

                    // Add headers & trips
                    if (!tripsToday.isEmpty()) {
                        items.add("Trips Today");
                        items.addAll(tripsToday);
                    }
                    if (!upcomingTrips.isEmpty()) {
                        items.add("Upcoming Trips");
                        items.addAll(upcomingTrips);
                    }
                    if (!completedToday.isEmpty()) {
                        items.add("Completed Trips Today");
                        items.addAll(completedToday);
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(CurrentVanScheduleActivity.this, "Error loading trips: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        // stop swipe refresh if active
        if (!show && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        rvTrips.setAlpha(show ? 0.5f : 1f);
        rvTrips.setEnabled(!show);
    }

    // -------------------------
    // RecyclerView Adapter
    // -------------------------
    private class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return (items.get(position) instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_TRIP;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_header, parent, false);
                return new HeaderVH(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_van_schedule, parent, false);
                return new TripVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).tvHeader.setText((String) items.get(position));
            } else {
                @SuppressWarnings("unchecked")
                HashMap<String, Object> trip = (HashMap<String, Object>) items.get(position);
                bindTrip((TripVH) holder, trip);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // Header ViewHolder
        class HeaderVH extends RecyclerView.ViewHolder {
            TextView tvHeader;
            HeaderVH(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tvHeader);
            }
        }

        // Trip ViewHolder
        class TripVH extends RecyclerView.ViewHolder {
            TextView tvVanId, tvDestination, tvDeparture, tvSeats, tvStatus;
            Button btnEdit, btnDelete, btnAddWalkIn;

            TripVH(@NonNull View itemView) {
                super(itemView);
                tvVanId = itemView.findViewById(R.id.tv_vanId);
                tvDestination = itemView.findViewById(R.id.tv_destination);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvSeats = itemView.findViewById(R.id.tv_seats);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnAddWalkIn = itemView.findViewById(R.id.btnAddWalkIn); // ✅ new
            }
        }
    }

    // -------------------------
    // Bind trip data and actions
    // -------------------------
    private void bindTrip(TripAdapter.TripVH holder, HashMap<String, Object> trip) {
        final String tripId = (String) trip.get("id");
        final String vanId = trip.get("vanId") instanceof String ? (String) trip.get("vanId") : null;
        final String destinationId = trip.get("destinationId") instanceof String ? (String) trip.get("destinationId") : null;

        Object seatsObj = trip.get("availableSeats");
        final long availableSeats = (seatsObj instanceof Number) ? ((Number) seatsObj).longValue() : 0L;

        Object depObj = trip.get("departure");
        final Timestamp departure = (depObj instanceof Timestamp) ? (Timestamp) depObj : null;

        holder.tvVanId.setText("Van Plate: " + (vanId != null ? vanId : "N/A"));
        holder.tvSeats.setText(String.valueOf(availableSeats));

        boolean isUpcoming = false;
        if (departure != null) {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTime(departure.toDate());
            String formatted = DateFormat.format("MMM dd, yyyy hh:mm a", cal).toString();
            holder.tvDeparture.setText(formatted);

            Timestamp now = Timestamp.now();
            if (departure.compareTo(now) >= 0) {
                isUpcoming = true;
                holder.tvStatus.setText("Upcoming");
                holder.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                isUpcoming = false;
                holder.tvStatus.setText("Completed");
                holder.tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        } else {
            holder.tvDeparture.setText("N/A");
            holder.tvStatus.setText("N/A");
        }

        // Load destination name
        if (destinationId != null) {
            db.collection("destinations").document(destinationId).get()
                    .addOnSuccessListener(destDoc -> {
                        if (destDoc.exists()) {
                            String name = destDoc.getString("name");
                            holder.tvDestination.setText(name != null ? name : "Unknown");
                        } else {
                            holder.tvDestination.setText("Unknown");
                        }
                    })
                    .addOnFailureListener(e -> holder.tvDestination.setText("Error"));
        } else {
            holder.tvDestination.setText("Unknown");
        }

        // Delete action
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(CurrentVanScheduleActivity.this)
                    .setTitle("Delete Trip")
                    .setMessage("Are you sure you want to delete this trip?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        showLoading(true);
                        db.collection("trips").document(tripId).delete()
                                .addOnSuccessListener(unused -> {
                                    int idx = items.indexOf(trip);
                                    if (idx != -1) {
                                        items.remove(idx);
                                        if (idx - 1 >= 0 && items.get(idx - 1) instanceof String) {
                                            boolean removeHeader = (idx >= items.size()) || (items.get(idx) instanceof String);
                                            if (removeHeader) items.remove(idx - 1);
                                        }
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        loadTrips();
                                    }
                                    showLoading(false);
                                    Toast.makeText(CurrentVanScheduleActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(CurrentVanScheduleActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Show/Hide buttons based on trip status
        if (isUpcoming) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnAddWalkIn.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnAddWalkIn.setVisibility(View.GONE);
        }
        holder.btnAddWalkIn.setVisibility(View.VISIBLE);

        // Edit Trip dialog (same as before)
// Edit Trip dialog
        // --- PUT THIS IN PLACE OF YOUR CURRENT holder.btnEdit.setOnClickListener(...) ---
        holder.btnEdit.setOnClickListener(v -> {
            // inflate dialog view
            View dialogView = LayoutInflater.from(CurrentVanScheduleActivity.this)
                    .inflate(R.layout.dialog_edit_trip, null);

            EditText etVanId = dialogView.findViewById(R.id.etVanId);
            Spinner spDestination = dialogView.findViewById(R.id.spDestination);
            TextView tvDeparture = dialogView.findViewById(R.id.etDeparture); // clickable textview in your XML
            EditText etSeats = dialogView.findViewById(R.id.etSeats);

            // Prefill fields
            etVanId.setText(vanId != null ? vanId : "");
            etSeats.setText(String.valueOf(availableSeats));

            // Calendar to hold chosen departure
            final Calendar calDep = Calendar.getInstance();
            if (departure != null) {
                calDep.setTime(departure.toDate());
                tvDeparture.setText(DateFormat.format("yyyy-MM-dd HH:mm", calDep));
            } else {
                tvDeparture.setText("");
            }

            // clicking departure opens DatePicker -> TimePicker
            tvDeparture.setOnClickListener(v2 -> {
                Calendar pickCal = (Calendar) calDep.clone();
                new DatePickerDialog(CurrentVanScheduleActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            pickCal.set(Calendar.YEAR, year);
                            pickCal.set(Calendar.MONTH, month);
                            pickCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            new TimePickerDialog(CurrentVanScheduleActivity.this,
                                    (timeView, hourOfDay, minute) -> {
                                        pickCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        pickCal.set(Calendar.MINUTE, minute);
                                        // write chosen time back to calDep and textview
                                        calDep.setTime(pickCal.getTime());
                                        tvDeparture.setText(DateFormat.format("yyyy-MM-dd HH:mm", calDep));
                                    },
                                    pickCal.get(Calendar.HOUR_OF_DAY),
                                    pickCal.get(Calendar.MINUTE),
                                    false // 12-hour format? change to true if you want 24h
                            ).show();
                        },
                        pickCal.get(Calendar.YEAR),
                        pickCal.get(Calendar.MONTH),
                        pickCal.get(Calendar.DAY_OF_MONTH)
                ).show();
            });

            // Load destinations into spinner and keep mapping of ids
            final List<String> destNames = new ArrayList<>();
            final List<String> destIds = new ArrayList<>();
            ArrayAdapter<String> destAdapter = new ArrayAdapter<>(CurrentVanScheduleActivity.this,
                    android.R.layout.simple_spinner_item, destNames);
            destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spDestination.setAdapter(destAdapter);

            db.collection("destinations")
                    .get()
                    .addOnSuccessListener(snap -> {
                        destNames.clear();
                        destIds.clear();
                        int selIndex = 0;
                        int idx = 0;
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String name = d.getString("name");
                            destNames.add(name != null ? name : "Unknown");
                            destIds.add(d.getId());
                            if (d.getId().equals(destinationId)) {
                                selIndex = idx;
                            }
                            idx++;
                        }
                        destAdapter.notifyDataSetChanged();
                        if (!destNames.isEmpty()) spDestination.setSelection(Math.max(0, selIndex));
                    })
                    .addOnFailureListener(e -> {
                        // ignore, spinner will be empty
                    });

            // Build dialog and override positive button later so we can validate
            AlertDialog dialog = new AlertDialog.Builder(CurrentVanScheduleActivity.this)
                    .setTitle("Edit Trip")
                    .setView(dialogView)
                    .setPositiveButton("Save", null) // we'll override to prevent auto-dismiss
                    .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                    .create();

            dialog.setOnShowListener(d -> {
                Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                saveBtn.setOnClickListener(btn -> {
                    String newVanId = etVanId.getText().toString().trim();
                    String newSeatsStr = etSeats.getText().toString().trim();
                    int destPos = spDestination.getSelectedItemPosition();
                    String departureText = tvDeparture.getText().toString().trim();

                    if (newVanId.isEmpty() || newSeatsStr.isEmpty() || departureText.isEmpty() || destPos < 0 || destPos >= destIds.size()) {
                        Toast.makeText(CurrentVanScheduleActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long newSeats;
                    try {
                        newSeats = Long.parseLong(newSeatsStr);
                        if (newSeats < 0) throw new NumberFormatException("negative");
                    } catch (NumberFormatException ex) {
                        Toast.makeText(CurrentVanScheduleActivity.this, "Invalid seats value", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // create firetore Timestamp from calDep
                    Timestamp newDepartureTs = new Timestamp(calDep.getTime()); // com.google.firebase.Timestamp
                    String newDestinationId = destIds.get(destPos);

                    // --- THIS IS THE FIX: declare the map BEFORE calling put() ---
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("vanId", newVanId);
                    updateData.put("destinationId", newDestinationId);
                    updateData.put("departure", newDepartureTs);
                    updateData.put("availableSeats", newSeats);

                    // disable button & show loading state if you want (optional)
                    saveBtn.setEnabled(false);
                    showLoading(true);

                    db.collection("trips").document(tripId)
                            .update(updateData)
                            .addOnSuccessListener(unused -> {
                                // update local trip map to reflect changes without reloading everything
                                trip.put("vanId", newVanId);
                                trip.put("destinationId", newDestinationId);
                                trip.put("departure", newDepartureTs);
                                trip.put("availableSeats", newSeats);

                                // find position and notify adapter
                                int pos = items.indexOf(trip);
                                if (pos >= 0) adapter.notifyItemChanged(pos);
                                else adapter.notifyDataSetChanged(); // fallback

                                showLoading(false);
                                Toast.makeText(CurrentVanScheduleActivity.this, "Trip updated", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                saveBtn.setEnabled(true);
                                Toast.makeText(CurrentVanScheduleActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
            });

            dialog.show();
        });



        // Add Walk-In button
        holder.btnAddWalkIn.setOnClickListener(v -> {
            Object seatsObjInner = trip.get("availableSeats");
            long currentSeats = (seatsObjInner instanceof Number) ? ((Number) seatsObjInner).longValue() : 0L;

            if (currentSeats > 0) {
                long updatedSeats = currentSeats - 1;

                db.collection("trips").document(tripId)
                        .update("availableSeats", updatedSeats)
                        .addOnSuccessListener(unused -> {
                            trip.put("availableSeats", updatedSeats);
                            holder.tvSeats.setText(String.valueOf(updatedSeats));

                            Toast.makeText(CurrentVanScheduleActivity.this,
                                    "Walk-in added. Seats left: " + updatedSeats,
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CurrentVanScheduleActivity.this,
                                    "Failed to update seats: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(CurrentVanScheduleActivity.this,
                        "No seats available!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
