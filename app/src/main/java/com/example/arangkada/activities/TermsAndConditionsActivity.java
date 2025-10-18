package com.example.arangkada.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import com.example.arangkada.R;

public class TermsAndConditionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        setupNavigation();
        showBackButton();
        setToolbarTitle("Terms & Conditions");

        CheckBox checkboxAgree = findViewById(R.id.checkbox_agree);
        Button btnAccept = findViewById(R.id.btn_accept_terms);

        // Disable button until checkbox is checked
        checkboxAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAccept.setEnabled(isChecked);
            btnAccept.setAlpha(isChecked ? 1f : 0.5f); // visual feedback
        });

        btnAccept.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for accepting the Terms & Conditions.", Toast.LENGTH_SHORT).show();
            finish(); // close activity
        });
    }

    @Override
    protected void onNavigationSetup() {
        // Nothing special here
    }
}
