package com.example.arangkada.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookRideActivity extends AppCompatActivity {

    private Spinner spinnerDestinations, spinnerTrips;
    private EditText etRegularCount, etStudentCount, etSeniorCount;
    private TextView tvTotalFare, tvTripDeparture, tvTripVan, tvTripSeats;
    private View layoutTripDetails;
    private Button btnBookNow;

    private FirebaseFirestore db;

    private List<DocumentSnapshot> tripList = new ArrayList<>();
    private int selectedTripIndex = -1;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

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

        // Use anonymous OnClickListener to avoid lambda/resolution issues
        btnBookNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBooking();
            }
        });
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
        db.collection("trips")
                .whereEqualTo("destinationId", destinationId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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

                        tripNames.add(formattedDate);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            tripNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTrips.setAdapter(adapter);

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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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

        // Transaction ensures correct deduction
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                DocumentReference tripRef = db.collection("trips").document(tripId);
                DocumentSnapshot tripSnapshot = transaction.get(tripRef);

                Long availableSeatsObj = tripSnapshot.getLong("availableSeats");
                long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

                if (passengerCount > availableSeats) {
                    // If thrown, the transaction fails and onFailureListener will run
                    throw new FirebaseFirestoreException(
                            "Not enough available seats",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                long newSeats = availableSeats - passengerCount;
                transaction.update(tripRef, "availableSeats", newSeats);

                // Create booking document with generated ID and store that ID in booking map
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
            }
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(BookRideActivity.this, "Booking confirmed!", Toast.LENGTH_SHORT).show();

            // Reload the trip doc to refresh UI seats (and update the local tripList snapshot for consistency)
            db.collection("trips").document(tripId)
                    .get()
                    .addOnSuccessListener(updatedTrip -> {
                        // Update local tripList entry (if selectedTripIndex still valid)
                        if (selectedTripIndex >= 0 && selectedTripIndex < tripList.size()) {
                            tripList.set(selectedTripIndex, updatedTrip);
                        }
                        showTripDetails(updatedTrip);
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(BookRideActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
