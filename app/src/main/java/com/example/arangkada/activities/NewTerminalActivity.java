package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.adapters.PlateNumberAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewTerminalActivity extends BaseActivity {

    private EditText etTerminal, etDestination, etLocation, etRegularFare, etStudentFare, etSeniorFare, etTravelTime;
    private EditText etPlateNumberInput;
    private Button btnAddRoute, btnAddPlateNumber;
    private MaterialButton btnCurrentRoutes, btnStandardFare;
    private CardView cvPlateNumbersList;
    private RecyclerView rvPlateNumbers;
    private TextView tvPlateCount;

    private FirebaseFirestore db;

    // List to store plate numbers
    private List<String> plateNumbersList;
    private PlateNumberAdapter plateNumberAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Inflate your content layout inside BaseActivity's content frame
        View contentView = getLayoutInflater().inflate(
                R.layout.activity_new_terminal,
                findViewById(R.id.content_frame),
                true
        );
        setupNavigation();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize plate numbers list
        plateNumbersList = new ArrayList<>();

        // Bind views
        etTerminal = contentView.findViewById(R.id.etTerminal);
        etDestination = contentView.findViewById(R.id.etDestination);
        etLocation = contentView.findViewById(R.id.etLocation);
        etRegularFare = contentView.findViewById(R.id.etRegularFare);
        etStudentFare = contentView.findViewById(R.id.etStudentFare);
        etSeniorFare = contentView.findViewById(R.id.etSeniorFare);
        etTravelTime = contentView.findViewById(R.id.etTravelTime);
        btnAddRoute = contentView.findViewById(R.id.btnAddRoute);
        btnCurrentRoutes = contentView.findViewById(R.id.btnCurrentRoutes);
        btnStandardFare = contentView.findViewById(R.id.btnStandardFare);

        // Plate number views
        etPlateNumberInput = contentView.findViewById(R.id.etPlateNumberInput);
        btnAddPlateNumber = contentView.findViewById(R.id.btnAddPlateNumber);
        cvPlateNumbersList = contentView.findViewById(R.id.cvPlateNumbersList);
        rvPlateNumbers = contentView.findViewById(R.id.rvPlateNumbers);
        tvPlateCount = contentView.findViewById(R.id.tvPlateCount);

        // Setup RecyclerView
        setupPlateNumbersRecyclerView();

        // Standard Fare Button - Auto-fill fares
        btnStandardFare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyStandardFares();
            }
        });

        // Add Plate Number Button
        btnAddPlateNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPlateNumber();
            }
        });

        // Add Route Button
        btnAddRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRoute();
            }
        });

        // Current Routes Button
        btnCurrentRoutes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewTerminalActivity.this, CurrentTerminalActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onNavigationSetup() {

    }

    private void setupPlateNumbersRecyclerView() {
        plateNumberAdapter = new PlateNumberAdapter(plateNumbersList, new PlateNumberAdapter.OnPlateRemoveListener() {
            @Override
            public void onPlateRemove(int position, String plateNumber) {
                removePlateNumber(position);
            }
        });

        rvPlateNumbers.setLayoutManager(new LinearLayoutManager(this));
        rvPlateNumbers.setAdapter(plateNumberAdapter);
    }

    private void addPlateNumber() {
        String plateNumber = etPlateNumberInput.getText().toString().trim().toUpperCase();

        // Validate input
        if (TextUtils.isEmpty(plateNumber)) {
            Toast.makeText(this, "⚠ Please enter a plate number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for duplicates
        if (plateNumbersList.contains(plateNumber)) {
            Toast.makeText(this, "⚠ This plate number is already added", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to list
        plateNumbersList.add(plateNumber);
        plateNumberAdapter.notifyItemInserted(plateNumbersList.size() - 1);

        // Update count and show list
        updatePlateCount();
        cvPlateNumbersList.setVisibility(View.VISIBLE);

        // Clear input
        etPlateNumberInput.setText("");

        Toast.makeText(this, "✓ Plate number added", Toast.LENGTH_SHORT).show();
    }

    private void removePlateNumber(int position) {
        if (position >= 0 && position < plateNumbersList.size()) {
            plateNumbersList.remove(position);
            plateNumberAdapter.notifyItemRemoved(position);
            updatePlateCount();

            // Hide list if empty
            if (plateNumbersList.isEmpty()) {
                cvPlateNumbersList.setVisibility(View.GONE);
            }

            Toast.makeText(this, "✓ Plate number removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePlateCount() {
        tvPlateCount.setText(String.valueOf(plateNumbersList.size()));
    }

    private void applyStandardFares() {
        // Auto-fill the standard fare values
        etRegularFare.setText("350");
        etStudentFare.setText("300");
        etSeniorFare.setText("300");

        // Show a toast message for user feedback
        Toast.makeText(this, "✓ Standard fares applied", Toast.LENGTH_SHORT).show();
    }

    private void saveRoute() {
        String terminal = etTerminal.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        String regularFareStr = etRegularFare.getText().toString().trim();
        String studentFareStr = etStudentFare.getText().toString().trim();
        String seniorFareStr = etSeniorFare.getText().toString().trim();
        String travelTimeStr = etTravelTime.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(terminal) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(location)
                || TextUtils.isEmpty(regularFareStr) || TextUtils.isEmpty(studentFareStr)
                || TextUtils.isEmpty(seniorFareStr) || TextUtils.isEmpty(travelTimeStr)) {
            Toast.makeText(this, "⚠ Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int regularFare, studentFare, seniorFare, travelTime;
        try {
            regularFare = Integer.parseInt(regularFareStr);
            studentFare = Integer.parseInt(studentFareStr);
            seniorFare = Integer.parseInt(seniorFareStr);
            travelTime = Integer.parseInt(travelTimeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "⚠ Please enter valid numbers for fares and travel time", Toast.LENGTH_SHORT).show();
            return;
        }

        String routeName = terminal + " - " + destination;

        // Prepare data
        Map<String, Object> route = new HashMap<>();
        route.put("name", routeName);
        route.put("location", location);
        route.put("regularFare", regularFare);
        route.put("studentFare", studentFare);
        route.put("seniorFare", seniorFare);
        route.put("travelTime", travelTime);

        // Add plate numbers if any
        if (!plateNumbersList.isEmpty()) {
            route.put("plateNumbers", new ArrayList<>(plateNumbersList));
        }

        db.collection("destinations")
                .add(route)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NewTerminalActivity.this, "✅ Route added successfully", Toast.LENGTH_SHORT).show();
                    // Clear all fields after successful save
                    clearFields();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(NewTerminalActivity.this, "❌ Failed to add route", Toast.LENGTH_SHORT).show()
                );
    }

    private void clearFields() {
        etTerminal.setText("");
        etDestination.setText("");
        etLocation.setText("");
        etRegularFare.setText("");
        etStudentFare.setText("");
        etSeniorFare.setText("");
        etTravelTime.setText("");
        etPlateNumberInput.setText("");

        // Clear plate numbers list
        plateNumbersList.clear();
        plateNumberAdapter.notifyDataSetChanged();
        cvPlateNumbersList.setVisibility(View.GONE);
        updatePlateCount();
    }
}