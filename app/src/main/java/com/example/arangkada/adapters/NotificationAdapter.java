package com.example.arangkada.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.activities.NotificationItem;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> notifications;
    private final Context context;

    public NotificationAdapter(Context context, List<NotificationItem> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        holder.timestamp.setText(item.getTimestamp());

        // 🧭 Show trip details if available
        if (!item.getTripName().isEmpty()) {
            holder.tripDetails.setVisibility(View.VISIBLE);
            holder.tripDetails.setText(
                    "Trip: " + item.getTripName() + "\n" +
                            "Date: " + item.getTripDate() + "\n" +
                            "Fare: ₱" + item.getTripFare()
            );
        } else {
            holder.tripDetails.setVisibility(View.GONE);
        }

        // 🟢 Icons and colors
        switch (item.getType()) {
            case "confirmed":
                holder.icon.setImageResource(R.drawable.ic_check_circle);
                holder.icon.setColorFilter(Color.parseColor("#2E7D32"));
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                break;
            case "rejected":
                holder.icon.setImageResource(R.drawable.ic_cancel);
                holder.icon.setColorFilter(Color.parseColor("#C62828"));
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                break;
            default:
                holder.icon.setImageResource(R.drawable.ic_notifications);
                holder.icon.setColorFilter(Color.parseColor("#1565C0"));
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
                break;
        }
    }


    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp, tripDetails;
        ImageView icon;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            message = itemView.findViewById(R.id.tv_message);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            tripDetails = itemView.findViewById(R.id.tv_trip_details); // 🔹 add this to layout
            icon = itemView.findViewById(R.id.iv_icon);
            cardView = itemView.findViewById(R.id.notification_card);
        }
    }

}
