package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ManageVansActivity extends AppCompatActivity {

    private Spinner spinnerDestination;
    private TextView tvDeparture;
    private Button btnPickDeparture;
    private EditText etVanPlate, etSeatCapacity;
    private Button btnSaveSchedule;
    private ProgressBar progressBar;
    private FrameLayout rootLayout; // NEW - parent container for dimming/disabling

    private FirebaseFirestore db;

    private List<String> destinationNames = new ArrayList<>();
    private List<String> destinationIds = new ArrayList<>();

    private Calendar departureCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_vans);

        // Bind UI elements
        spinnerDestination = findViewById(R.id.spinnerDestination);
        tvDeparture = findViewById(R.id.tvDeparture);
        btnPickDeparture = findViewById(R.id.btnPickDeparture);
        etVanPlate = findViewById(R.id.etVanPlate);
        etSeatCapacity = findViewById(R.id.etSeatCapacity);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);
        progressBar = findViewById(R.id.progressBar);
        rootLayout = findViewById(R.id.rootLayout); // FrameLayout in XML

        db = FirebaseFirestore.getInstance();

        loadDestinations();

        btnPickDeparture.setOnClickListener(v -> showDateTimePicker());
        tvDeparture.setOnClickListener(v -> showDateTimePicker());
        btnSaveSchedule.setOnClickListener(v -> saveTripSchedule());
    }

    private void loadDestinations() {
        showLoading(true);
        db.collection("destinations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    destinationNames.clear();
                    destinationIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null) {
                                destinationNames.add(name);
                                destinationIds.add(doc.getId());
                            }
                        }
                    }

                    if (destinationNames.isEmpty()) {
                        destinationNames.add("No destinations available");
                        destinationIds.add("");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, destinationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDestination.setAdapter(adapter);

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading destinations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void showDateTimePicker() {
        int year = departureCalendar.get(Calendar.YEAR);
        int month = departureCalendar.get(Calendar.MONTH);
        int day = departureCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            departureCalendar.set(Calendar.YEAR, selectedYear);
            departureCalendar.set(Calendar.MONTH, selectedMonth);
            departureCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);

            int hour = departureCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = departureCalendar.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, selectedHour, selectedMinute) -> {
                departureCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                departureCalendar.set(Calendar.MINUTE, selectedMinute);
                departureCalendar.set(Calendar.SECOND, 0);
                departureCalendar.set(Calendar.MILLISECOND, 0);

                CharSequence formatted = DateFormat.format("MMM dd, yyyy hh:mm a", departureCalendar);
                tvDeparture.setText(formatted);
            }, hour, minute, false);

            timePicker.show();
        }, year, month, day);

        datePicker.show();
    }

    private void saveTripSchedule() {
        int selectedIndex = spinnerDestination.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= destinationIds.size() || destinationIds.get(selectedIndex).isEmpty()) {
            Toast.makeText(this, "Please select a valid destination", Toast.LENGTH_SHORT).show();
            return;
        }
        String destinationId = destinationIds.get(selectedIndex);

        String departureText = tvDeparture.getText().toString().trim();
        if (departureText.isEmpty() || "Select date & time".equalsIgnoreCase(departureText)) {
            Toast.makeText(this, "Please select departure date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        String vanPlate = etVanPlate.getText().toString().trim();
        if (vanPlate.isEmpty()) {
            Toast.makeText(this, "Please enter Van Plate Number", Toast.LENGTH_SHORT).show();
            return;
        }

        String seatStr = etSeatCapacity.getText().toString().trim();
        if (seatStr.isEmpty()) {
            Toast.makeText(this, "Please enter Seat Capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        int seatCapacity;
        try {
            seatCapacity = Integer.parseInt(seatStr);
            if (seatCapacity <= 0) {
                Toast.makeText(this, "Seat Capacity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid seat capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> tripData = new HashMap<>();
        tripData.put("destinationId", destinationId);
        tripData.put("departure", new Timestamp(departureCalendar.getTime()));
        tripData.put("vanId", vanPlate);
        tripData.put("availableSeats", seatCapacity);

        showLoading(true);
        db.collection("trips")
                .add(tripData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Trip schedule saved successfully!", Toast.LENGTH_SHORT).show();

                    if (!destinationNames.isEmpty()) spinnerDestination.setSelection(0);
                    tvDeparture.setText("Select date & time");
                    etVanPlate.setText("");
                    etSeatCapacity.setText("");
                    departureCalendar = Calendar.getInstance();

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    // Show/hide loading overlay with UI dimming & blocking
    private void showLoading(boolean show) {
        if (progressBar != null && rootLayout != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            rootLayout.setAlpha(show ? 0.5f : 1f); // dim background
            rootLayout.setEnabled(!show);          // disable interactions
        }
    }
}
