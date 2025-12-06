package com.example.arangkada.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.example.arangkada.adapters.PlateNumberAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrentTerminalActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TerminalAdapter adapter;
    private List<Terminal> terminalList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Inflate your content layout inside BaseActivity's content frame
        View contentView = getLayoutInflater().inflate(
                R.layout.activity_current_terminal,
                findViewById(R.id.content_frame),
                true
        );
        setupNavigation();

        recyclerView = contentView.findViewById(R.id.recyclerTerminals);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        adapter = new TerminalAdapter(terminalList, new TerminalAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Terminal terminal) {
                showEditDialog(terminal);
            }

            @Override
            public void onDelete(Terminal terminal) {
                new AlertDialog.Builder(CurrentTerminalActivity.this)
                        .setTitle("Delete Terminal")
                        .setMessage("Are you sure you want to delete " + terminal.getName() + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("destinations").document(terminal.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(CurrentTerminalActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                        loadTerminals();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(CurrentTerminalActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onManagePlates(Terminal terminal) {
                showManagePlatesDialog(terminal);
            }
        });

        recyclerView.setAdapter(adapter);
        loadTerminals();
    }

    @Override
    protected void onNavigationSetup() {
        // Optional: Add menu logic here if needed
    }

    private void loadTerminals() {
        db.collection("destinations").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    terminalList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String location = doc.getString("location");

                        Long regularFareLong = doc.getLong("regularFare");
                        int regularFare = (regularFareLong != null) ? regularFareLong.intValue() : 0;

                        Long studentFareLong = doc.getLong("studentFare");
                        int studentFare = (studentFareLong != null) ? studentFareLong.intValue() : 0;

                        Long seniorFareLong = doc.getLong("seniorFare");
                        int seniorFare = (seniorFareLong != null) ? seniorFareLong.intValue() : 0;

                        Long travelTimeLong = doc.getLong("travelTime");
                        int travelTime = (travelTimeLong != null) ? travelTimeLong.intValue() : 0;

                        // Get plate numbers array
                        List<String> plateNumbers = (List<String>) doc.get("plateNumbers");
                        if (plateNumbers == null) {
                            plateNumbers = new ArrayList<>();
                        }

                        Terminal t = new Terminal(
                                id,
                                name,
                                location,
                                regularFare,
                                studentFare,
                                seniorFare,
                                travelTime,
                                plateNumbers
                        );
                        terminalList.add(t);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showEditDialog(Terminal terminal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Terminal");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_terminal, null);

        EditText etName = view.findViewById(R.id.etName);
        EditText etLocation = view.findViewById(R.id.etLocation);
        EditText etRegularFare = view.findViewById(R.id.etRegularFare);
        EditText etStudentFare = view.findViewById(R.id.etStudentFare);
        EditText etSeniorFare = view.findViewById(R.id.etSeniorFare);
        EditText etTravelTime = view.findViewById(R.id.etTravelTime);

        etName.setText(terminal.getName());
        etLocation.setText(terminal.getLocation());
        etRegularFare.setText(String.valueOf(terminal.getRegularFare()));
        etStudentFare.setText(String.valueOf(terminal.getStudentFare()));
        etSeniorFare.setText(String.valueOf(terminal.getSeniorFare()));
        etTravelTime.setText(String.valueOf(terminal.getTravelTime()));

        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", etName.getText().toString().trim());
            updates.put("location", etLocation.getText().toString().trim());
            updates.put("regularFare", Integer.parseInt(etRegularFare.getText().toString().trim()));
            updates.put("studentFare", Integer.parseInt(etStudentFare.getText().toString().trim()));
            updates.put("seniorFare", Integer.parseInt(etSeniorFare.getText().toString().trim()));
            updates.put("travelTime", Integer.parseInt(etTravelTime.getText().toString().trim()));

            db.collection("destinations").document(terminal.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CurrentTerminalActivity.this, "✅ Updated", Toast.LENGTH_SHORT).show();
                        loadTerminals();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showManagePlatesDialog(Terminal terminal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Plate Numbers - " + terminal.getName());

        View view = getLayoutInflater().inflate(R.layout.dialog_manage_plates, null);

        EditText etPlateInput = view.findViewById(R.id.etPlateInput);
        Button btnAddPlate = view.findViewById(R.id.btnAddPlate);
        RecyclerView rvPlates = view.findViewById(R.id.rvPlates);
        TextView tvPlatesCount = view.findViewById(R.id.tvPlatesCount);
        TextView tvEmptyState = view.findViewById(R.id.tvEmptyState);

        // Create a mutable copy of the plate numbers
        List<String> plateNumbers = new ArrayList<>(terminal.getPlateNumbers());

        // Setup RecyclerView - declare adapter as final array to use in lambda
        final PlateNumberAdapter[] plateAdapterArray = new PlateNumberAdapter[1];

        PlateNumberAdapter plateAdapter = new PlateNumberAdapter(plateNumbers, new PlateNumberAdapter.OnPlateRemoveListener() {
            @Override
            public void onPlateRemove(int position, String plateNumber) {
                plateNumbers.remove(position);
                if (plateAdapterArray[0] != null) {
                    plateAdapterArray[0].notifyItemRemoved(position);
                }
                updatePlatesUI(plateNumbers, tvPlatesCount, tvEmptyState, rvPlates);
            }
        });

        plateAdapterArray[0] = plateAdapter;

        rvPlates.setLayoutManager(new LinearLayoutManager(this));
        rvPlates.setAdapter(plateAdapter);

        // Initial UI update
        updatePlatesUI(plateNumbers, tvPlatesCount, tvEmptyState, rvPlates);

        // Add plate number button
        btnAddPlate.setOnClickListener(v -> {
            String plateNumber = etPlateInput.getText().toString().trim().toUpperCase();

            if (TextUtils.isEmpty(plateNumber)) {
                Toast.makeText(this, "⚠ Please enter a plate number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (plateNumbers.contains(plateNumber)) {
                Toast.makeText(this, "⚠ This plate number is already added", Toast.LENGTH_SHORT).show();
                return;
            }

            plateNumbers.add(plateNumber);
            plateAdapter.notifyItemInserted(plateNumbers.size() - 1);
            etPlateInput.setText("");
            updatePlatesUI(plateNumbers, tvPlatesCount, tvEmptyState, rvPlates);
            Toast.makeText(this, "✓ Plate number added", Toast.LENGTH_SHORT).show();
        });

        builder.setView(view);

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            // Update Firestore with new plate numbers
            db.collection("destinations").document(terminal.getId())
                    .update("plateNumbers", plateNumbers)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CurrentTerminalActivity.this, "✅ Plate numbers updated", Toast.LENGTH_SHORT).show();
                        loadTerminals();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CurrentTerminalActivity.this, "❌ Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updatePlatesUI(List<String> plateNumbers, TextView tvCount, TextView tvEmpty, RecyclerView rv) {
        tvCount.setText(String.valueOf(plateNumbers.size()));
        if (plateNumbers.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }

    public static class Terminal {
        private String id;
        private String name;
        private String location;
        private int regularFare;
        private int studentFare;
        private int seniorFare;
        private int travelTime;
        private List<String> plateNumbers;

        public Terminal() {}

        public Terminal(String id, String name, String location, int regularFare, int studentFare, int seniorFare, int travelTime, List<String> plateNumbers) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.regularFare = regularFare;
            this.studentFare = studentFare;
            this.seniorFare = seniorFare;
            this.travelTime = travelTime;
            this.plateNumbers = plateNumbers != null ? plateNumbers : new ArrayList<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getLocation() { return location; }
        public int getRegularFare() { return regularFare; }
        public int getStudentFare() { return studentFare; }
        public int getSeniorFare() { return seniorFare; }
        public int getTravelTime() { return travelTime; }
        public List<String> getPlateNumbers() { return plateNumbers; }
    }

    public static class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder> {

        private List<Terminal> terminalList;
        private OnItemActionListener listener;

        public interface OnItemActionListener {
            void onEdit(Terminal terminal);
            void onDelete(Terminal terminal);
            void onManagePlates(Terminal terminal);
        }

        public TerminalAdapter(List<Terminal> terminalList, OnItemActionListener listener) {
            this.terminalList = terminalList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TerminalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_terminal, parent, false);
            return new TerminalViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TerminalViewHolder holder, int position) {
            Terminal terminal = terminalList.get(position);

            holder.tvName.setText(terminal.getName());
            holder.tvLocation.setText(terminal.getLocation());
            holder.tvRegularFare.setText("₱" + terminal.getRegularFare());
            holder.tvStudentFare.setText("₱" + terminal.getStudentFare());
            holder.tvSeniorFare.setText("₱" + terminal.getSeniorFare());
            holder.tvTravelTime.setText(terminal.getTravelTime() + " mins");

            // Display plate numbers count
            int plateCount = terminal.getPlateNumbers().size();
            holder.tvPlateCount.setText(plateCount + " van" + (plateCount != 1 ? "s" : ""));

            // Show/hide plate numbers section based on availability
            if (plateCount > 0) {
                holder.tvPlateCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvPlateCount.setVisibility(View.GONE);
            }

            holder.btnEdit.setOnClickListener(v -> listener.onEdit(terminal));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(terminal));
            holder.btnManagePlates.setOnClickListener(v -> listener.onManagePlates(terminal));
        }

        @Override
        public int getItemCount() {
            return terminalList.size();
        }

        static class TerminalViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLocation, tvRegularFare, tvStudentFare, tvSeniorFare, tvTravelTime, tvPlateCount;
            Button btnEdit, btnDelete, btnManagePlates;

            public TerminalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvLocation = itemView.findViewById(R.id.tv_location);
                tvRegularFare = itemView.findViewById(R.id.tv_regularFare);
                tvStudentFare = itemView.findViewById(R.id.tv_studentFare);
                tvSeniorFare = itemView.findViewById(R.id.tv_seniorFare);
                tvTravelTime = itemView.findViewById(R.id.tv_travelTime);
                tvPlateCount = itemView.findViewById(R.id.tv_plateCount);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnManagePlates = itemView.findViewById(R.id.btnManagePlates);
            }
        }
    }
}