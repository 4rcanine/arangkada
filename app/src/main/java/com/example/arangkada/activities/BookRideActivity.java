package com.example.arangkada.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BookRideActivity extends AppCompatActivity {

    private Spinner spinnerDestinations, spinnerTrips;
    private EditText etRegularCount, etStudentCount, etSeniorCount;
    private TextView tvTotalFare, tvTripDeparture, tvTripVan, tvTripSeats;
    private View layoutTripDetails;
    private Button btnBookNow;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    private List<DocumentSnapshot> tripList = new ArrayList<>();
    private int selectedTripIndex = -1;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    private ListenerRegistration tripsListener; // 🔹 to stop listening when needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_ride);

        db = FirebaseFirestore.getInstance();

        spinnerDestinations = findViewById(R.id.spinnerDestinations);
        spinnerTrips = findViewById(R.id.spinnerTrips);
        etRegularCount = findViewById(R.id.et_regular_count);
        etStudentCount = findViewById(R.id.et_student_count);
        etSeniorCount = findViewById(R.id.et_senior_count);
        tvTotalFare = findViewById(R.id.tvTotalFare);

        // Trip details preview
        tvTripDeparture = findViewById(R.id.tvTripDeparture);
        tvTripVan = findViewById(R.id.tvTripVan);
        tvTripSeats = findViewById(R.id.tvTripSeats);
        layoutTripDetails = findViewById(R.id.layoutTripDetails);

        btnBookNow = findViewById(R.id.btnBookNow);
        progressBar = findViewById(R.id.progressBar);

        // Load destinations from Firestore
        loadDestinations();

        // Real-time fare calculation
        TextWatcher fareWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFarePreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etRegularCount.addTextChangedListener(fareWatcher);
        etStudentCount.addTextChangedListener(fareWatcher);
        etSeniorCount.addTextChangedListener(fareWatcher);

        btnBookNow.setOnClickListener(v -> saveBooking());
    }

    private void loadDestinations() {
        db.collection("destinations").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> destinationNames = new ArrayList<>();
            final List<String> destinationIds = new ArrayList<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                destinationNames.add(name != null ? name : "Unknown");
                destinationIds.add(doc.getId());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    destinationNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDestinations.setAdapter(adapter);

            spinnerDestinations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedDestinationId = destinationIds.get(position);
                    loadTrips(selectedDestinationId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        });
    }

    private void loadTrips(String destinationId) {
        // 🔹 Remove old listener if switching destinations
        if (tripsListener != null) {
            tripsListener.remove();
        }

        tripsListener = db.collection("trips")
                .whereEqualTo("destinationId", destinationId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to listen to trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots == null) return;

                    // 🔹 Save previous selection
                    int previousSelection = spinnerTrips.getSelectedItemPosition();

                    tripList.clear();
                    List<String> tripNames = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        tripList.add(doc);

                        Timestamp departure = doc.getTimestamp("departure");
                        String formattedDate = "-";

                        if (departure != null) {
                            Date date = departure.toDate();
                            formattedDate = dateFormat.format(date);
                        }

                        Long availableSeatsObj = doc.getLong("availableSeats");
                        long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

                        if (availableSeats == 0) {
                            formattedDate += "  -- Sold Out --";
                        }

                        tripNames.add(formattedDate);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            tripNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTrips.setAdapter(adapter);

                    // 🔹 Restore previous selection if valid
                    if (previousSelection >= 0 && previousSelection < tripList.size()) {
                        spinnerTrips.setSelection(previousSelection);
                    }

                    spinnerTrips.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (tripList != null && !tripList.isEmpty() && position < tripList.size()) {
                                selectedTripIndex = position;
                                showTripDetails(tripList.get(position));
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            layoutTripDetails.setVisibility(View.GONE);
                        }
                    });
                });
    }

    private void showTripDetails(DocumentSnapshot tripDoc) {
        String departure = formatDeparture(tripDoc.get("departure"));
        String vanPlate = tripDoc.getString("vanId");
        Long availableSeatsObj = tripDoc.getLong("availableSeats");
        long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

        tvTripDeparture.setText("Departure: " + departure);
        tvTripVan.setText("Van: " + (vanPlate != null ? vanPlate : "-"));
        tvTripSeats.setText("Available Seats: " + availableSeats);

        layoutTripDetails.setVisibility(View.VISIBLE);

        // 🔹 Disable passenger input + booking if sold out
        boolean isSoldOut = (availableSeats == 0);
        etRegularCount.setEnabled(!isSoldOut);
        etStudentCount.setEnabled(!isSoldOut);
        etSeniorCount.setEnabled(!isSoldOut);
        btnBookNow.setEnabled(!isSoldOut);

        if (isSoldOut) {
            etRegularCount.setText("0");
            etStudentCount.setText("0");
            etSeniorCount.setText("0");
            tvTotalFare.setText("Total Fare: ₱0");
        }

        // 🔹 Real-time listener for just this trip
        db.collection("trips").document(tripDoc.getId())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Long seats = snapshot.getLong("availableSeats");
                        long liveSeats = (seats != null) ? seats : 0L;

                        tvTripSeats.setText("Available Seats: " + liveSeats);

                        boolean nowSoldOut = (liveSeats == 0);
                        etRegularCount.setEnabled(!nowSoldOut);
                        etStudentCount.setEnabled(!nowSoldOut);
                        etSeniorCount.setEnabled(!nowSoldOut);
                        btnBookNow.setEnabled(!nowSoldOut);

                        if (nowSoldOut) {
                            etRegularCount.setText("0");
                            etStudentCount.setText("0");
                            etSeniorCount.setText("0");
                            tvTotalFare.setText("Total Fare: ₱0");
                        }
                    }
                });
    }

    private String formatDeparture(Object departureObj) {
        if (departureObj instanceof Timestamp) {
            return dateFormat.format(((Timestamp) departureObj).toDate());
        } else if (departureObj != null) {
            return departureObj.toString();
        } else {
            return "-";
        }
    }

    // Real-time fare calculation
    private void updateFarePreview() {
        int regular = parseIntSafe(etRegularCount.getText().toString());
        int student = parseIntSafe(etStudentCount.getText().toString());
        int senior = parseIntSafe(etSeniorCount.getText().toString());

        int totalFare = (regular * 350) + (student * 300) + (senior * 300);
        tvTotalFare.setText("Total Fare: ₱" + totalFare);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveBooking() {
        if (selectedTripIndex == -1) {
            Toast.makeText(this, "Please select a trip first", Toast.LENGTH_SHORT).show();
            return;
        }

        final int regular = parseIntSafe(etRegularCount.getText().toString());
        final int student = parseIntSafe(etStudentCount.getText().toString());
        final int senior = parseIntSafe(etSeniorCount.getText().toString());
        final int passengerCount = regular + student + senior;

        if (passengerCount <= 0) {
            Toast.makeText(this, "Enter at least one passenger", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentSnapshot selectedTrip = tripList.get(selectedTripIndex);
        final String tripId = selectedTrip.getId();
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final Timestamp departure = selectedTrip.getTimestamp("departure");
        final String destinationId = selectedTrip.getString("destinationId");
        final int totalFare = (regular * 350) + (student * 300) + (senior * 300);

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        btnBookNow.setEnabled(false);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference tripRef = db.collection("trips").document(tripId);
            DocumentSnapshot tripSnapshot = transaction.get(tripRef);

            Long availableSeatsObj = tripSnapshot.getLong("availableSeats");
            long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

            if (passengerCount > availableSeats) {
                throw new FirebaseFirestoreException(
                        "Not enough available seats",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            long newSeats = availableSeats - passengerCount;
            transaction.update(tripRef, "availableSeats", newSeats);

            DocumentReference bookingRef = db.collection("bookings").document(); // auto ID
            String bookingId = bookingRef.getId();

            Map<String, Object> booking = new HashMap<>();
            booking.put("bookingId", bookingId);
            booking.put("status", "Pending");
            booking.put("regularCount", regular);
            booking.put("studentCount", student);
            booking.put("seniorCount", senior);
            booking.put("seats", passengerCount);
            booking.put("departure", departure);
            booking.put("destinationId", destinationId);
            booking.put("tripId", tripId);
            booking.put("userId", userId);
            booking.put("totalFare", totalFare);

            transaction.set(bookingRef, booking);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(BookRideActivity.this, "Booking confirmed!", Toast.LENGTH_SHORT).show();

            // 🔹 Reset passenger inputs & fare
            etRegularCount.setText("0");
            etStudentCount.setText("0");
            etSeniorCount.setText("0");
            updateFarePreview();

        }).addOnFailureListener(e ->
                Toast.makeText(BookRideActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tripsListener != null) {
            tripsListener.remove();
        }
    }
}
