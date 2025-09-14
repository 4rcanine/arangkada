package com.example.arangkada.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TicketConfirmationActivity extends AppCompatActivity {

    private TextView tvBookingId;
    private TextView tvRoute;
    private TextView tvDateTime;
    private TextView tvPassengers;
    private TextView tvPaymentMethod;
    private TextView tvFare;
    private TextView tvStatus;

    private CardView qrCodeCard;
    private ImageView ivQrCode;
    private LinearLayout cashPaymentInstructions;
    private LinearLayout gcashPaymentInstructions;

    private Button btnConfirmTicket;
    private Button btnBackToBooking;

    private String bookingId;
    private String fromLocation;
    private String toLocation;
    private String dateTime;
    private String passengers;
    private String paymentMethod;
    private String fare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_confirmation);

        initializeViews();
        getBookingData();
        displayBookingDetails();
        setupPaymentDisplay();
        setupClickListeners();
    }

    private void initializeViews() {
        tvBookingId = findViewById(R.id.tv_booking_id);
        tvRoute = findViewById(R.id.tv_route);
        tvDateTime = findViewById(R.id.tv_date_time);
        tvPassengers = findViewById(R.id.tv_passengers);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvFare = findViewById(R.id.tv_fare);
        tvStatus = findViewById(R.id.tv_status);

        qrCodeCard = findViewById(R.id.card_qr_code);
        ivQrCode = findViewById(R.id.iv_qr_code);
        cashPaymentInstructions = findViewById(R.id.layout_cash_instructions);
        gcashPaymentInstructions = findViewById(R.id.layout_gcash_instructions);

        btnConfirmTicket = findViewById(R.id.btn_confirm_ticket);
        btnBackToBooking = findViewById(R.id.btn_back_to_booking);
    }

    private void getBookingData() {
        Intent intent = getIntent();
        bookingId = intent.getStringExtra("booking_id");
        fromLocation = intent.getStringExtra("from_location");
        toLocation = intent.getStringExtra("to_location");
        dateTime = intent.getStringExtra("date_time");
        passengers = intent.getStringExtra("passengers");
        paymentMethod = intent.getStringExtra("payment_method");
        fare = intent.getStringExtra("fare");

        // Generate booking ID if not provided
        if (bookingId == null) {
            bookingId = "AR" + (System.currentTimeMillis() % 10000);
        }

        // Set default fare if not provided
        if (fare == null) {
            fare = "₱45.00";
        }
    }

    private void displayBookingDetails() {
        tvBookingId.setText("Booking ID: " + bookingId);
        tvRoute.setText(fromLocation + " → " + toLocation);
        tvDateTime.setText(dateTime);
        tvPassengers.setText(passengers);
        tvPaymentMethod.setText(paymentMethod);
        tvFare.setText(fare);
        tvStatus.setText("PENDING CONFIRMATION");
    }

    private void setupPaymentDisplay() {
        if ("Cash".equalsIgnoreCase(paymentMethod)) {
            setupCashPayment();
        } else if ("GCash".equalsIgnoreCase(paymentMethod)) {
            setupGCashPayment();
        }
    }

    private void setupCashPayment() {
        qrCodeCard.setVisibility(View.VISIBLE);
        cashPaymentInstructions.setVisibility(View.VISIBLE);
        gcashPaymentInstructions.setVisibility(View.GONE);

        generateQRCode();
    }

    private void setupGCashPayment() {
        qrCodeCard.setVisibility(View.GONE);
        cashPaymentInstructions.setVisibility(View.GONE);
        gcashPaymentInstructions.setVisibility(View.VISIBLE);

        // Update status for GCash
        tvStatus.setText("READY FOR GCASH PAYMENT");
    }

    private void generateQRCode() {
        String qrData = createQRData();

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivQrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private String createQRData() {
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = timestampFormat.format(new Date());

        return String.format(
                "ARANGKADA_BOOKING\n" +
                        "ID:%s\n" +
                        "ROUTE:%s->%s\n" +
                        "DATETIME:%s\n" +
                        "PASSENGERS:%s\n" +
                        "FARE:%s\n" +
                        "PAYMENT:CASH\n" +
                        "GENERATED:%s\n" +
                        "STATUS:PENDING",
                bookingId,
                fromLocation,
                toLocation,
                dateTime,
                passengers.replace("\n", " "),
                fare,
                currentTime
        );
    }

    private void setupClickListeners() {
        btnConfirmTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmTicket();
            }
        });

        btnBackToBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToBooking();
            }
        });
    }

    private void confirmTicket() {
        String confirmationMessage;

        if ("Cash".equals(paymentMethod)) {
            confirmationMessage = "Ticket confirmed! Please save the QR code and present it to the driver upon boarding.\n\nBooking ID: " + bookingId;
        } else {
            confirmationMessage = "Ticket confirmed! GCash payment will be processed separately.\n\nBooking ID: " + bookingId;
        }

        Toast.makeText(this, confirmationMessage, Toast.LENGTH_LONG).show();

        // Navigate back to dashboard
        Intent intent = new Intent(TicketConfirmationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void backToBooking() {
        Toast.makeText(this, "Returning to booking...", Toast.LENGTH_SHORT).show();
        finish(); // Go back to BookingActivity
    }

    @Override
    public void onBackPressed() {
        backToBooking();
    }
}