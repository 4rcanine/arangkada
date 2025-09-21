package com.example.arangkada.activities;

import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookRideActivity extends AppCompatActivity {

    private Spinner spinnerDestinations, spinnerTrips, spinnerPassengerType;
    private EditText etPassengerCount;
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
        spinnerPassengerType = findViewById(R.id.spinnerPassengerType);
        etPassengerCount = findViewById(R.id.etPassengerCount);
        tvTotalFare = findViewById(R.id.tvTotalFare);

        // Trip details preview
        tvTripDeparture = findViewById(R.id.tvTripDeparture);
        tvTripVan = findViewById(R.id.tvTripVan);
        tvTripSeats = findViewById(R.id.tvTripSeats);
        layoutTripDetails = findViewById(R.id.layoutTripDetails);

        btnBookNow = findViewById(R.id.btnBookNow);

        // Load destinations from Firestore
        loadDestinations();

        // Passenger type spinner
        ArrayAdapter<String> passengerTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Regular", "Student", "Senior Citizen/PWD"}
        );
        passengerTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPassengerType.setAdapter(passengerTypeAdapter);

        // When a trip is selected → show trip details
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

        // Book button
        btnBookNow.setOnClickListener(v -> saveBooking());
    }

    private void loadDestinations() {
        db.collection("destinations").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> destinationNames = new ArrayList<>();
            List<String> destinationIds = new ArrayList<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                destinationNames.add(name != null ? name : "Unknown");
                destinationIds.add(doc.getId()); // use documentId (destination1, destination2)
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

                        // Get departure timestamp
                        com.google.firebase.Timestamp departure = doc.getTimestamp("departure");
                        String formattedDate = "-";

                        if (departure != null) {
                            Date date = departure.toDate();
                            formattedDate = dateFormat.format(date);
                        }

                        // ✅ Only add date/time to spinner
                        tripNames.add(formattedDate);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            tripNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTrips.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showTripDetails(DocumentSnapshot tripDoc) {
        String departure = formatDeparture(tripDoc.get("departure"));
        String vanPlate = tripDoc.getString("vanId");
        Long availableSeats = tripDoc.getLong("availableSeats");

        tvTripDeparture.setText("Departure: " + departure);
        tvTripVan.setText("Van: " + (vanPlate != null ? vanPlate : "-"));
        tvTripSeats.setText("Available Seats: " + (availableSeats != null ? availableSeats : 0));

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

    private void saveBooking() {
        if (selectedTripIndex == -1) {
            Toast.makeText(this, "Please select a trip first", Toast.LENGTH_SHORT).show();
            return;
        }

        String passengerType = spinnerPassengerType.getSelectedItem().toString();
        String passengerCountStr = etPassengerCount.getText().toString();

        if (passengerCountStr.isEmpty()) {
            Toast.makeText(this, "Enter number of passengers", Toast.LENGTH_SHORT).show();
            return;
        }

        int passengerCount = Integer.parseInt(passengerCountStr);

        DocumentSnapshot selectedTrip = tripList.get(selectedTripIndex);

        Timestamp departure = selectedTrip.getTimestamp("departure");
        String destinationId = selectedTrip.getString("destinationId");
        String tripId = selectedTrip.getId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fare calculation
        int farePerPerson = passengerType.equals("Regular") ? 350 : 300;
        int totalFare = farePerPerson * passengerCount;
        tvTotalFare.setText("Total Fare: ₱" + totalFare);

        // 🔥 Generate sequential bookingId
        db.collection("bookings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int nextBookingNumber = querySnapshot.size() + 1;
                    String bookingId = "booking" + nextBookingNumber;

                    // ✅ Save booking with custom field names
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("bookingId", bookingId);
                    booking.put("status", "Pending");
                    booking.put("type", passengerType);       // changed from passengerType
                    booking.put("seats", passengerCount);     // changed from passengerCount
                    booking.put("departure", departure);
                    booking.put("destinationId", destinationId);
                    booking.put("tripId", tripId);
                    booking.put("userId", userId);
                    booking.put("totalFare", totalFare);

                    // Save with Firestore auto-ID
                    db.collection("bookings")
                            .add(booking)
                            .addOnSuccessListener(documentReference ->
                                    Toast.makeText(this, "Booking saved as " + bookingId, Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });
    }
}
