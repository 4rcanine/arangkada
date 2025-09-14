package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private TextView fromLocationTextView;
    private TextView toLocationTextView;
    private TextView selectedDateTextView;
    private TextView selectedTimeTextView;
    private CardView datePickerCard;
    private CardView timePickerCard;
    private Button confirmBookingButton;
    private Button cancelButton;

    private Calendar selectedCalendar;
    private String fromLocation;
    private String toLocation;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initializeViews();
        setupDateTimeFormatters();
        getIntentData();
        setupClickListeners();
        setDefaultDateTime();
    }

    private void initializeViews() {
        fromLocationTextView = findViewById(R.id.tv_from_location);
        toLocationTextView = findViewById(R.id.tv_to_location);
        selectedDateTextView = findViewById(R.id.tv_selected_date);
        selectedTimeTextView = findViewById(R.id.tv_selected_time);
        datePickerCard = findViewById(R.id.card_date_picker);
        timePickerCard = findViewById(R.id.card_time_picker);
        confirmBookingButton = findViewById(R.id.btn_confirm_booking);
        cancelButton = findViewById(R.id.btn_cancel);
    }

    private void setupDateTimeFormatters() {
        dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        selectedCalendar = Calendar.getInstance();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        fromLocation = intent.getStringExtra("from_location");
        toLocation = intent.getStringExtra("to_location");

        // Set default values if null
        if (fromLocation == null) fromLocation = "Cervantes";
        if (toLocation == null) toLocation = "Baguio";

        fromLocationTextView.setText(fromLocation);
        toLocationTextView.setText(toLocation);
    }

    private void setupClickListeners() {
        datePickerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        timePickerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        confirmBookingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmBooking();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelBooking();
            }
        });
    }

    private void setDefaultDateTime() {
        // Set default to current date and time
        updateDateTimeDisplay();
    }

    private void showDatePicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateTimeDisplay();
                    }
                },
                year, month, day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // Set maximum date to 30 days from now
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_MONTH, 30);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute);
                        updateDateTimeDisplay();
                    }
                },
                hour, minute, false
        );

        timePickerDialog.show();
    }

    private void updateDateTimeDisplay() {
        selectedDateTextView.setText(dateFormat.format(selectedCalendar.getTime()));
        selectedTimeTextView.setText(timeFormat.format(selectedCalendar.getTime()));
    }

    private void confirmBooking() {
        String bookingDetails = String.format(
                "Booking Confirmed!\n\nRoute: %s → %s\nDate: %s\nTime: %s\n\nBooking ID: AR%d\nFare: ₱45.00",
                fromLocation,
                toLocation,
                dateFormat.format(selectedCalendar.getTime()),
                timeFormat.format(selectedCalendar.getTime()),
                System.currentTimeMillis() % 10000
        );

        Toast.makeText(this, bookingDetails, Toast.LENGTH_LONG).show();

        // Navigate back to dashboard
        Intent intent = new Intent(BookingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void cancelBooking() {
        Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        cancelBooking();
    }
}