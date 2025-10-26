package com.example.arangkada.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.arangkada.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.*;

public class ManageVansActivity extends BaseActivity {
    private static final int PICK_QR_IMAGE = 101;
    private static final String TAG = "ManageVansActivity";

    private Spinner spinnerDestination, spinnerPaymentMethod;
    private TextView tvDeparture;
    private Button btnPickDeparture, btnSaveSchedule, btnUploadQR;
    private EditText etVanPlate, etSeatCapacity, etPaymentNumber, etDriverName, etDriverNumber;
    private ImageView imgQRPreview;
    private LinearLayout layoutQRUpload;
    private ProgressBar progressBar;
    private FrameLayout rootLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<String> destinationNames = new ArrayList<>();
    private List<String> destinationIds = new ArrayList<>();
    private Calendar departureCalendar = Calendar.getInstance();

    private Uri selectedQRUri = null;
    private String uploadedQRUrl = null;
    private String selectedPaymentMethod = "Cash";

    // ImageKit Configuration
    private static final String IMAGEKIT_URL_ENDPOINT = "https://ik.imagekit.io/xqqzgzvy9";
    private static final String IMAGEKIT_PUBLIC_KEY = "public_aM1dq8aVaA7PBiP8Pdfo6mYpUsM=";

    private static final String IMAGEKIT_PRIVATE_KEY = "private_xix6Ergz3zAHuAwotsM7a+4WsdU=";
    private static final String IMAGEKIT_UPLOAD_URL = "https://upload.imagekit.io/api/v1/files/upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);


        View contentView = getLayoutInflater().inflate(
                R.layout.activity_manage_vans,
                findViewById(R.id.content_frame),
                true
        );

        setupNavigation();

        spinnerDestination = contentView.findViewById(R.id.spinnerDestination);
        spinnerPaymentMethod = contentView.findViewById(R.id.spinnerPaymentMethod);
        tvDeparture = contentView.findViewById(R.id.tvDeparture);
        btnPickDeparture = contentView.findViewById(R.id.btnPickDeparture);
        etVanPlate = contentView.findViewById(R.id.etVanPlate);
        etSeatCapacity = contentView.findViewById(R.id.etSeatCapacity);
        etDriverName = contentView.findViewById(R.id.etDriverName);
        etDriverNumber = contentView.findViewById(R.id.etDriverNumber);
        etPaymentNumber = contentView.findViewById(R.id.etPaymentNumber);
        btnSaveSchedule = contentView.findViewById(R.id.btnSaveSchedule);
        progressBar = contentView.findViewById(R.id.progressBar);
        rootLayout = contentView.findViewById(R.id.rootLayout);
        layoutQRUpload = contentView.findViewById(R.id.layoutQRUpload);
        btnUploadQR = contentView.findViewById(R.id.btnUploadQR);
        imgQRPreview = contentView.findViewById(R.id.imgQRPreview);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadDestinations();
        setupPaymentMethodSpinner();

        btnPickDeparture.setOnClickListener(v -> showDateTimePicker());
        tvDeparture.setOnClickListener(v -> showDateTimePicker());
        btnSaveSchedule.setOnClickListener(v -> saveTripSchedule());
        btnUploadQR.setOnClickListener(v -> pickQRImage());
    }

    @Override
    protected void onNavigationSetup() {
        // Optional: leave empty or use if you need custom navigation logic
    }
    private void setupPaymentMethodSpinner() {
        String[] methods = {"Cash", "Gcash", "Cash & Gcash"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, methods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(adapter);

        spinnerPaymentMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = methods[position];
                if (selectedPaymentMethod.equals("Gcash") || selectedPaymentMethod.equals("Cash & Gcash")) {
                    layoutQRUpload.setVisibility(View.VISIBLE);
                } else {
                    layoutQRUpload.setVisibility(View.GONE);
                    selectedQRUri = null;
                    uploadedQRUrl = null;
                    imgQRPreview.setVisibility(View.GONE);
                    etPaymentNumber.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void pickQRImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_QR_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_QR_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedQRUri = data.getData();
            imgQRPreview.setImageURI(selectedQRUri);
            imgQRPreview.setVisibility(View.VISIBLE);
            uploadQRToImageKit(selectedQRUri);
        }
    }

    private String generateToken() {
        // Generate a random UUID token
        return UUID.randomUUID().toString();
    }

    private long getExpireTimestamp() {
        // Set expiration to 1 hour from now
        return (System.currentTimeMillis() / 1000) + 3600;
    }

    private String generateSignature(String token, long expire) {
        try {
            // Create the string to sign: token + expire
            String stringToSign = token + expire;

            // Create HMAC-SHA1 signature
            Mac sha256_HMAC = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret_key = new SecretKeySpec(IMAGEKIT_PRIVATE_KEY.getBytes("UTF-8"), "HmacSHA1");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(stringToSign.getBytes("UTF-8"));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error generating signature", e);
            return null;
        }
    }

    private void uploadQRToImageKit(Uri imageUri) {
        showLoading(true);

        new Thread(() -> {
            try {
                // Get input stream from URI
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                    return;
                }

                // Read bytes from input stream
                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                inputStream.close();

                // Generate authentication parameters
                String token = generateToken();
                long expire = getExpireTimestamp();
                String signature = generateSignature(token, expire);

                if (signature == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to generate signature", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                    return;
                }

                // Create OkHttp client
                OkHttpClient client = new OkHttpClient();

                // Generate unique filename
                String fileName = "qr_code_" + System.currentTimeMillis() + ".jpg";

                // Build multipart request body with authentication
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName,
                                RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .addFormDataPart("fileName", fileName)
                        .addFormDataPart("publicKey", IMAGEKIT_PUBLIC_KEY)
                        .addFormDataPart("signature", signature)
                        .addFormDataPart("expire", String.valueOf(expire))
                        .addFormDataPart("token", token)
                        .addFormDataPart("folder", "qr_codes")
                        .addFormDataPart("useUniqueFileName", "true")
                        .build();

                // Build request
                Request request = new Request.Builder()
                        .url(IMAGEKIT_UPLOAD_URL)
                        .post(requestBody)
                        .build();

                Log.d(TAG, "Uploading with token: " + token);
                Log.d(TAG, "Expire: " + expire);
                Log.d(TAG, "Signature: " + signature);

                // Execute request
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Upload response: " + responseBody);

                    // Parse JSON response to get URL
                    uploadedQRUrl = parseUrlFromResponse(responseBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "QR uploaded successfully!", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + errorBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload failed: " + errorBody, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Upload error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
            }
        }).start();
    }

    private String parseUrlFromResponse(String jsonResponse) {

        try {
            int urlStart = jsonResponse.indexOf("\"url\":\"") + 7;
            int urlEnd = jsonResponse.indexOf("\"", urlStart);
            return jsonResponse.substring(urlStart, urlEnd);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL from response", e);
            return null;
        }
    }

    private void loadDestinations() {
        showLoading(true);
        db.collection("destinations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    destinationNames.clear();
                    destinationIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null) {
                                destinationNames.add(name);
                                destinationIds.add(doc.getId());
                            }
                        }
                    }

                    if (destinationNames.isEmpty()) {
                        destinationNames.add("No destinations available");
                        destinationIds.add("");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, destinationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDestination.setAdapter(adapter);

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading destinations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void showDateTimePicker() {
        int year = departureCalendar.get(Calendar.YEAR);
        int month = departureCalendar.get(Calendar.MONTH);
        int day = departureCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            departureCalendar.set(Calendar.YEAR, selectedYear);
            departureCalendar.set(Calendar.MONTH, selectedMonth);
            departureCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);

            int hour = departureCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = departureCalendar.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, selectedHour, selectedMinute) -> {
                departureCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                departureCalendar.set(Calendar.MINUTE, selectedMinute);
                departureCalendar.set(Calendar.SECOND, 0);
                departureCalendar.set(Calendar.MILLISECOND, 0);

                CharSequence formatted = DateFormat.format("MMM dd, yyyy hh:mm a", departureCalendar);
                tvDeparture.setText(formatted);
            }, hour, minute, false);

            timePicker.show();
        }, year, month, day);

        datePicker.show();
    }

    private void saveTripSchedule() {
        // Get current user ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String adminId = currentUser.getUid();

        int selectedIndex = spinnerDestination.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= destinationIds.size() || destinationIds.get(selectedIndex).isEmpty()) {
            Toast.makeText(this, "Please select a valid destination", Toast.LENGTH_SHORT).show();
            return;
        }
        String destinationId = destinationIds.get(selectedIndex);

        String departureText = tvDeparture.getText().toString().trim();
        if (departureText.isEmpty() || "Select date & time".equalsIgnoreCase(departureText)) {
            Toast.makeText(this, "Please select departure date & time", Toast.LENGTH_SHORT).show();
            return;
        }

        String vanPlate = etVanPlate.getText().toString().trim();
        if (vanPlate.isEmpty()) {
            Toast.makeText(this, "Please enter Van Plate Number", Toast.LENGTH_SHORT).show();
            return;
        }

        String seatStr = etSeatCapacity.getText().toString().trim();
        if (seatStr.isEmpty()) {
            Toast.makeText(this, "Please enter Seat Capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        int seatCapacity;
        try {
            seatCapacity = Integer.parseInt(seatStr);
            if (seatCapacity <= 0) {
                Toast.makeText(this, "Seat Capacity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid seat capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        String driverName = etDriverName.getText().toString().trim();
        if (driverName.isEmpty()) {
            Toast.makeText(this, "Please enter Driver Name", Toast.LENGTH_SHORT).show();
            return;
        }

        String driverNumber = etDriverNumber.getText().toString().trim();
        if (driverNumber.isEmpty()) {
            Toast.makeText(this, "Please enter Driver's Mobile Number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate driver's phone number format
        if (!isValidPhoneNumber(driverNumber)) {
            Toast.makeText(this, "Please enter a valid driver mobile number (e.g., 09123456789)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Gcash payment requirements: must have QR or number or both
        String paymentNumber = etPaymentNumber.getText().toString().trim();
        if ((selectedPaymentMethod.equals("Gcash") || selectedPaymentMethod.equals("Cash & Gcash"))) {
            if (uploadedQRUrl == null && paymentNumber.isEmpty()) {
                Toast.makeText(this, "Please provide either a GCash QR code, mobile number, or both", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate phone number format if provided
            if (!paymentNumber.isEmpty() && !isValidPhoneNumber(paymentNumber)) {
                Toast.makeText(this, "Please enter a valid mobile number (e.g., 09123456789)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        HashMap<String, Object> tripData = new HashMap<>();
        tripData.put("adminID", adminId);
        tripData.put("destinationId", destinationId);
        tripData.put("departure", new Timestamp(departureCalendar.getTime()));
        tripData.put("vanId", vanPlate);
        tripData.put("availableSeats", seatCapacity);
        tripData.put("driverName", driverName);
        tripData.put("driverNumber", driverNumber);
        tripData.put("paymentMethod", selectedPaymentMethod);

        // Add payment details based on what was provided
        if (uploadedQRUrl != null) {
            tripData.put("qrCodeUrl", uploadedQRUrl);
        }
        if (!paymentNumber.isEmpty()) {
            tripData.put("paymentNumber", paymentNumber);
        }

        showLoading(true);
        db.collection("trips")
                .add(tripData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Trip schedule saved successfully!", Toast.LENGTH_SHORT).show();
                    resetForm();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    private boolean isValidPhoneNumber(String number) {

        // Accepts formats: 09123456789 or 9123456789 (11 or 10 digits)
        return number.matches("^(09|9)\\d{9}$");
    }

    private void resetForm() {
        if (!destinationNames.isEmpty()) spinnerDestination.setSelection(0);
        tvDeparture.setText("Select date & time");
        etVanPlate.setText("");
        etSeatCapacity.setText("");
        etDriverName.setText("");
        etDriverNumber.setText("");
        etPaymentNumber.setText("");
        spinnerPaymentMethod.setSelection(0);
        layoutQRUpload.setVisibility(View.GONE);
        imgQRPreview.setVisibility(View.GONE);
        uploadedQRUrl = null;
        selectedQRUri = null;
        departureCalendar = Calendar.getInstance();
    }

    private void showLoading(boolean show) {
        if (progressBar != null && rootLayout != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            rootLayout.setAlpha(show ? 0.5f : 1f);
            rootLayout.setEnabled(!show);
        }
    }
}