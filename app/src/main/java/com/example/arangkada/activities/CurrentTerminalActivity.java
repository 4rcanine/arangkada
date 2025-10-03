package com.example.arangkada.activities;

import android.content.DialogInterface;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arangkada.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrentTerminalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TerminalAdapter adapter;
    private List<Terminal> terminalList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_terminal);

        recyclerView = findViewById(R.id.recyclerTerminals);
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

        });

        recyclerView.setAdapter(adapter);

        loadTerminals();
    }

    private void loadTerminals() {
        db.collection("destinations").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    terminalList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

                        String id = doc.getId();
                        String name = doc.getString("name");
                        String location = doc.getString("location");

                        // Safely handle null values
                        Long regularFareLong = doc.getLong("regularFare");
                        int regularFare = (regularFareLong != null) ? regularFareLong.intValue() : 0;

                        Long studentFareLong = doc.getLong("studentFare");
                        int studentFare = (studentFareLong != null) ? studentFareLong.intValue() : 0;

                        Long seniorFareLong = doc.getLong("seniorFare");
                        int seniorFare = (seniorFareLong != null) ? seniorFareLong.intValue() : 0;

                        Long travelTimeLong = doc.getLong("travelTime");
                        int travelTime = (travelTimeLong != null) ? travelTimeLong.intValue() : 0;

                        Terminal t = new Terminal(
                                id,
                                name,
                                location,
                                regularFare,
                                studentFare,
                                seniorFare,
                                travelTime
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

        // Prefill values
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
                        Toast.makeText(CurrentTerminalActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                        loadTerminals();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ---------------- MODEL CLASS ----------------
    public static class Terminal {
        private String id;
        private String name;
        private String location;
        private int regularFare;
        private int studentFare;
        private int seniorFare;
        private int travelTime;

        public Terminal() {} // Required for Firestore

        public Terminal(String id, String name, String location, int regularFare, int studentFare, int seniorFare, int travelTime) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.regularFare = regularFare;
            this.studentFare = studentFare;
            this.seniorFare = seniorFare;
            this.travelTime = travelTime;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getLocation() { return location; }
        public int getRegularFare() { return regularFare; }
        public int getStudentFare() { return studentFare; }
        public int getSeniorFare() { return seniorFare; }
        public int getTravelTime() { return travelTime; }
    }

    // ---------------- ADAPTER CLASS ----------------
    public static class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder> {

        private List<Terminal> terminalList;
        private OnItemActionListener listener;

        public interface OnItemActionListener {
            void onEdit(Terminal terminal);
            void onDelete(Terminal terminal);
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

            holder.btnEdit.setOnClickListener(v -> listener.onEdit(terminal));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(terminal));
        }

        @Override
        public int getItemCount() {
            return terminalList.size();
        }

        static class TerminalViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLocation, tvRegularFare, tvStudentFare, tvSeniorFare, tvTravelTime;
            Button btnEdit, btnDelete;

            public TerminalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvLocation = itemView.findViewById(R.id.tv_location);
                tvRegularFare = itemView.findViewById(R.id.tv_regularFare);
                tvStudentFare = itemView.findViewById(R.id.tv_studentFare);
                tvSeniorFare = itemView.findViewById(R.id.tv_seniorFare);
                tvTravelTime = itemView.findViewById(R.id.tv_travelTime);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
