package com.example.arangkada.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MyTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private ArrayList<Trip> tripList;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        recyclerView = findViewById(R.id.recyclerTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tripList = new ArrayList<>();
        adapter = new TripAdapter(tripList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            loadUserBookings();
        }
    }

    private void loadUserBookings() {
        db.collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot bookingDoc : querySnapshot) {
                        String bookingId = bookingDoc.getId();
                        String status = bookingDoc.getString("status");
                        String type = bookingDoc.getString("type");
                        long seats = bookingDoc.getLong("seats") != null ? bookingDoc.getLong("seats") : 0;
                        String tripId = bookingDoc.getString("tripId");

                        if (tripId != null) {
                            db.collection("trips").document(tripId).get()
                                    .addOnSuccessListener(tripDoc -> {
                                        if (tripDoc.exists()) {
                                            Date departure = tripDoc.getTimestamp("departure").toDate();
                                            String destinationId = tripDoc.getString("destinationId");

                                            if (destinationId != null) {
                                                db.collection("destinations").document(destinationId).get()
                                                        .addOnSuccessListener(destDoc -> {
                                                            String destinationName = destDoc.exists() ? destDoc.getString("name") : "Unknown";

                                                            Trip trip = new Trip(
                                                                    bookingId,
                                                                    status,
                                                                    type,
                                                                    seats,
                                                                    departure,
                                                                    destinationName
                                                            );
                                                            tripList.add(trip);
                                                            adapter.notifyDataSetChanged();
                                                        })
                                                        .addOnFailureListener(e -> Log.e("MyTripsActivity", "Failed to get destination", e));
                                            } else {
                                                Trip trip = new Trip(
                                                        bookingId,
                                                        status,
                                                        type,
                                                        seats,
                                                        departure,
                                                        "Unknown"
                                                );
                                                tripList.add(trip);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("MyTripsActivity", "Failed to get trip", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("MyTripsActivity", "Failed to get bookings", e));
    }

    // RecyclerView Adapter
    private static class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

        private final ArrayList<Trip> trips;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

        public TripAdapter(ArrayList<Trip> trips) {
            this.trips = trips;
        }

        @NonNull
        @Override
        public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
            return new TripViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
            Trip trip = trips.get(position);

            holder.tvBookingId.setText("Booking ID: " + trip.bookingId);
            holder.tvStatus.setText(trip.status);
            holder.tvPassengers.setText(trip.type + ": " + trip.seats);
            holder.tvDeparture.setText(sdf.format(trip.departure));
            holder.tvDestination.setText(trip.destination);
        }

        @Override
        public int getItemCount() {
            return trips.size();
        }

        static class TripViewHolder extends RecyclerView.ViewHolder {

            TextView tvBookingId, tvStatus, tvPassengers, tvDeparture, tvDestination;

            public TripViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDestination = itemView.findViewById(R.id.tv_booking_id);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvPassengers = itemView.findViewById(R.id.tv_passengers);
                tvDeparture = itemView.findViewById(R.id.tv_departure);
                tvBookingId = itemView.findViewById(R.id.tv_destination);

            }
        }
    }

    // Model class
    private static class Trip {
        String bookingId;
        String status;
        String type;
        long seats;
        Date departure;
        String destination;

        public Trip(String bookingId, String status, String type, long seats, Date departure, String destination) {
            this.bookingId = bookingId;
            this.status = status;
            this.type = type;
            this.seats = seats;
            this.departure = departure;
            this.destination = destination;
        }
    }
}
