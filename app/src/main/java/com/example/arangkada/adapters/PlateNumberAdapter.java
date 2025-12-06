package com.example.arangkada.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;

import java.util.List;

public class PlateNumberAdapter extends RecyclerView.Adapter<PlateNumberAdapter.ViewHolder> {

    private List<String> plateNumbers;
    private OnPlateRemoveListener removeListener;

    public interface OnPlateRemoveListener {
        void onPlateRemove(int position, String plateNumber);
    }

    public PlateNumberAdapter(List<String> plateNumbers, OnPlateRemoveListener listener) {
        this.plateNumbers = plateNumbers;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plate_number, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String plateNumber = plateNumbers.get(position);
        holder.tvPlateNumber.setText(plateNumber);

        holder.btnRemovePlate.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onPlateRemove(holder.getAdapterPosition(), plateNumber);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plateNumbers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlateNumber;
        ImageButton btnRemovePlate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlateNumber = itemView.findViewById(R.id.tvPlateNumber);
            btnRemovePlate = itemView.findViewById(R.id.btnRemovePlate);
        }
    }
}