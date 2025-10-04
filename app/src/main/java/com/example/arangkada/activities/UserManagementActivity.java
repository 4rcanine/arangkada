package com.example.arangkada.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private UserAdapter adapter;
    private final List<UserModel> userList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "UserManagement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        recyclerUsers = findViewById(R.id.recyclerUsers);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList);
        recyclerUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        db.collection("accounts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String userId = doc.getString("userId");
                        if (userId == null) userId = doc.getId();

                        String name = doc.getString("name");
                        String email = doc.getString("email");

                        if (name == null) name = "Unknown User";
                        if (email == null) email = "No email";

                        UserModel user = new UserModel(userId, name, email, 0, "N/A");
                        userList.add(user);

                        // fetch bookings for each user
                        fetchBookingStats(user);
                    }

                    // show initial list (names & emails)
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading users", e));
    }

    private void fetchBookingStats(UserModel user) {
        db.collection("bookings")
                .whereEqualTo("userId", user.getUserId())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalBookings = querySnapshot.size();
                    String lastBookingDate = "N/A";

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot first = querySnapshot.getDocuments().get(0);

                        Object createdAtObj = first.get("createdAt");
                        if (createdAtObj != null) {
                            Date createdAt = null;
                            if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                createdAt = ((com.google.firebase.Timestamp) createdAtObj).toDate();
                            } else if (createdAtObj instanceof Date) {
                                createdAt = (Date) createdAtObj;
                            }

                            if (createdAt != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                                lastBookingDate = sdf.format(createdAt);
                            }
                        }
                    }

                    user.setTotalBookings(totalBookings);
                    user.setLastBookingDate(lastBookingDate);

                    // Update only the changed item
                    int index = userList.indexOf(user);
                    if (index != -1) {
                        adapter.notifyItemChanged(index);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching booking stats for " + user.getUserId(), e)
                );
    }


    // ------------------------------
    // Model class
    // ------------------------------
    public static class UserModel {
        private String userId;
        private String name;
        private String email;
        private int totalBookings;
        private String lastBookingDate;

        public UserModel() {}

        public UserModel(String userId, String name, String email, int totalBookings, String lastBookingDate) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.totalBookings = totalBookings;
            this.lastBookingDate = lastBookingDate;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public int getTotalBookings() { return totalBookings; }
        public String getLastBookingDate() { return lastBookingDate; }

        public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }
        public void setLastBookingDate(String lastBookingDate) { this.lastBookingDate = lastBookingDate; }
    }

    // ------------------------------
    // Adapter class
    // ------------------------------
    public static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private final List<UserModel> userList;

        public UserAdapter(List<UserModel> userList) {
            this.userList = userList;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_card, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel user = userList.get(position);
            holder.tvUserName.setText(user.getName());
            holder.tvUserEmail.setText(user.getEmail());
            holder.tvTotalBookings.setText("Total Bookings: " + user.getTotalBookings());
            holder.tvLastBooking.setText("Last Booking: " + user.getLastBookingDate());

            holder.btnViewHistory.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                android.content.Intent intent = new android.content.Intent(context, UserHistoryActivity.class);
                intent.putExtra("userId", user.getUserId()); // pass userId
                context.startActivity(intent);
            });

        }


        @Override
        public int getItemCount() {
            return userList.size();
        }

        public static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserEmail, tvTotalBookings, tvLastBooking;
            Button btnViewHistory;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
                tvTotalBookings = itemView.findViewById(R.id.tvTotalBookings);
                tvLastBooking = itemView.findViewById(R.id.tvLastBooking);
                btnViewHistory = itemView.findViewById(R.id.btnViewHistory);
            }
        }
    }
}
