package com.example.arangkada.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.models.Trip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.TripViewHolder> {

    private List<Trip> tripList;

    public TripsAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        // Show van plate number instead of bookingId
        holder.plateNumber.setText("Plate: " + trip.getPlateNumber());
        holder.status.setText(trip.getStatus());
        holder.passengers.setText(trip.getPassengerType() + ": " + trip.getSeats());
        holder.destination.setText(trip.getDestinationName());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        holder.departure.setText(sdf.format(trip.getDeparture()));
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView plateNumber, status, passengers, departure, destination;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            plateNumber = itemView.findViewById(R.id.tv_booking_id); // 👈 reuse tv_booking_id for plate
            status = itemView.findViewById(R.id.tv_status);
            passengers = itemView.findViewById(R.id.tv_passengers);
            departure = itemView.findViewById(R.id.tv_departure);
            destination = itemView.findViewById(R.id.tv_destination);
        }
    }
}
