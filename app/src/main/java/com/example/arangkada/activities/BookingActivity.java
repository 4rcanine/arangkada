package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

    // Updated passenger inputs
    private EditText regularCountEditText;
    private EditText studentCountEditText;
    private EditText seniorCountEditText;

    // Updated payment method
    private RadioGroup paymentMethodGroup;
    private RadioButton rbCash, rbGCash;

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

        // Passenger input fields
        regularCountEditText = findViewById(R.id.et_regular_count);
        studentCountEditText = findViewById(R.id.et_student_count);
        seniorCountEditText = findViewById(R.id.et_senior_count);

        // Payment method radio buttons
        paymentMethodGroup = findViewById(R.id.rg_payment_method);
        rbCash = findViewById(R.id.rb_cash);
        rbGCash = findViewById(R.id.rb_gcash);
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

        if (fromLocation == null) fromLocation = "Cervantes";
        if (toLocation == null) toLocation = "Baguio";

        fromLocationTextView.setText(fromLocation);
        toLocationTextView.setText(toLocation);
    }

    private void setupClickListeners() {
        datePickerCard.setOnClickListener(v -> showDatePicker());
        timePickerCard.setOnClickListener(v -> showTimePicker());
        confirmBookingButton.setOnClickListener(v -> confirmBooking());
        cancelButton.setOnClickListener(v -> cancelBooking());
    }

    private void setDefaultDateTime() {
        updateDateTimeDisplay();
    }

    private void showDatePicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year1);
                    selectedCalendar.set(Calendar.MONTH, month1);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeDisplay();
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
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
                (view, hourOfDay, minute1) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute1);
                    updateDateTimeDisplay();
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
        String regularCount = regularCountEditText.getText().toString();
        String studentCount = studentCountEditText.getText().toString();
        String seniorCount = seniorCountEditText.getText().toString();

        if (regularCount.isEmpty()) regularCount = "0";
        if (studentCount.isEmpty()) studentCount = "0";
        if (seniorCount.isEmpty()) seniorCount = "0";

        String paymentMethod = rbCash.isChecked() ? "Cash" : "GCash";

        String passengers = "Regular: " + regularCount +
                "\nStudent: " + studentCount +
                "\nSenior/PWD: " + seniorCount;

        String dateTime = dateFormat.format(selectedCalendar.getTime()) + " " +
                timeFormat.format(selectedCalendar.getTime());
        String bookingId = "AR" + (System.currentTimeMillis() % 10000);
        String fare = "₱45.00";

        Intent intent = new Intent(BookingActivity.this, TicketConfirmationActivity.class);
        intent.putExtra("booking_id", bookingId);
        intent.putExtra("from_location", fromLocation);
        intent.putExtra("to_location", toLocation);
        intent.putExtra("date_time", dateTime);
        intent.putExtra("passengers", passengers);
        intent.putExtra("payment_method", paymentMethod);
        intent.putExtra("fare", fare);
        startActivity(intent);
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
