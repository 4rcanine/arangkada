package com.example.arangkada.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.example.arangkada.R;

public class NotificationsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        setupNavigation();
        showBackButton();
        setToolbarTitle("Notifications");
    }

    @Override
    protected void onNavigationSetup() {

    }
}
