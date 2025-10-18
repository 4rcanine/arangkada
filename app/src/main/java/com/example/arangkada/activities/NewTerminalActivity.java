package com.example.arangkada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arangkada.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NewTerminalActivity extends BaseActivity {

    private EditText etTerminal, etDestination, etLocation, etRegularFare, etStudentFare, etSeniorFare, etTravelTime;
    private Button btnAddRoute, btnCurrentRoutes;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Inflate your content layout inside BaseActivity’s content frame
        View contentView = getLayoutInflater().inflate(
                R.layout.activity_new_terminal,
                findViewById(R.id.content_frame),
                true
        );;
        setupNavigation();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
        // Optional: Add menu logic here if needed
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


        db.collection("destinations")
                .add(route)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(NewTerminalActivity.this, "✅ Route added successfully", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(NewTerminalActivity.this, "❌ Failed to add route", Toast.LENGTH_SHORT).show()
                );
    }
}
