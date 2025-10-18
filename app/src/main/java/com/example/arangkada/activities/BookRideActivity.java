package com.example.arangkada.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.arangkada.MainActivity;
import com.example.arangkada.R;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.*;

public class BookRideActivity extends BaseActivity {

    private static final int PICK_PAYMENT_PROOF = 201;
    private static final String TAG = "BookRideActivity";

    private Spinner spinnerDestinations, spinnerTrips;
    private EditText etRegularCount, etStudentCount, etSeniorCount;
    private TextView tvTotalFare, tvTripDeparture, tvTripVan, tvTripSeats, tvTripTravelTime;
    private LinearLayout layoutTripDetails, layoutPaymentProof, layoutQRCode, layoutMobileNumber, layoutPaymentChoice;
    private Button btnBookNow, btnCancel, btnUploadProof;
    private ProgressBar progressBar;
    private ImageView imgQRCode, imgPaymentProof;
    private TextView tvPaymentMethod, tvMobileNumber;
    private RadioGroup radioGroupPayment;
    private RadioButton radioCash, radioGcash;

    private FirebaseFirestore db;

    private List<DocumentSnapshot> tripList = new ArrayList<>();
    private int selectedTripIndex = -1;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    private ListenerRegistration tripsListener;

    // Fare + TravelTime
    private int regularFare = 0;
    private int studentFare = 0;
    private int seniorFare = 0;
    private int travelTimeMinutes = 0;

    // Payment handling
    private String currentPaymentMethod = "Cash";
    private String currentQRCodeUrl = null;
    private String currentPaymentNumber = null;
    private Uri selectedProofUri = null;
    private String uploadedProofUrl = null;
    private String userSelectedPayment = "Cash"; // User's choice for Cash & Gcash

    // ImageKit Configuration
    private static final String IMAGEKIT_PUBLIC_KEY = "public_aM1dq8aVaA7PBiP8Pdfo6mYpUsM=";
    private static final String IMAGEKIT_PRIVATE_KEY = "private_xix6Ergz3zAHuAwotsM7a+4WsdU=";
    private static final String IMAGEKIT_UPLOAD_URL = "https://upload.imagekit.io/api/v1/files/upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // Inflate content into base layout
        getLayoutInflater().inflate(R.layout.activity_book_ride,
                findViewById(R.id.content_frame), true);

        setupNavigation();

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();

        // Load destinations
        loadDestinations();
    }

    @Override
    protected void onNavigationSetup() {
        showBackButton();
        setToolbarTitle("Book a Ride");
    }

    private void initializeViews() {
        spinnerDestinations = findViewById(R.id.spinnerDestinations);
        spinnerTrips = findViewById(R.id.spinnerTrips);
        etRegularCount = findViewById(R.id.et_regular_count);
        etStudentCount = findViewById(R.id.et_student_count);
        etSeniorCount = findViewById(R.id.et_senior_count);
        tvTotalFare = findViewById(R.id.tvTotalFare);

        // Trip details
        tvTripDeparture = findViewById(R.id.tvTripDeparture);
        tvTripVan = findViewById(R.id.tvTripVan);
        tvTripSeats = findViewById(R.id.tvTripSeats);
        tvTripTravelTime = findViewById(R.id.tvTripTravelTime);
        layoutTripDetails = findViewById(R.id.layoutTripDetails);

        // Payment views
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        layoutPaymentProof = findViewById(R.id.layoutPaymentProof);
        layoutQRCode = findViewById(R.id.layoutQRCode);
        layoutMobileNumber = findViewById(R.id.layoutMobileNumber);
        layoutPaymentChoice = findViewById(R.id.layoutPaymentChoice);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        radioCash = findViewById(R.id.radioCash);
        radioGcash = findViewById(R.id.radioGcash);
        imgQRCode = findViewById(R.id.imgQRCode);
        tvMobileNumber = findViewById(R.id.tvMobileNumber);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        imgPaymentProof = findViewById(R.id.imgPaymentProof);

        btnBookNow = findViewById(R.id.btnBookNow);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Real-time fare preview
        TextWatcher fareWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFarePreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etRegularCount.addTextChangedListener(fareWatcher);
        etStudentCount.addTextChangedListener(fareWatcher);
        etSeniorCount.addTextChangedListener(fareWatcher);

        // Radio button listener for Cash & Gcash option
        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCash) {
                userSelectedPayment = "Cash";
                layoutPaymentProof.setVisibility(View.GONE);
                uploadedProofUrl = null;
                selectedProofUri = null;
                imgPaymentProof.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioGcash) {
                userSelectedPayment = "Gcash";
                layoutPaymentProof.setVisibility(View.VISIBLE);
                updatePaymentProofUI();
            }
        });

        btnBookNow.setOnClickListener(v -> handleBooking());
        btnUploadProof.setOnClickListener(v -> pickPaymentProof());

        btnCancel.setOnClickListener(v -> {
            Intent intent = new Intent(BookRideActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void pickPaymentProof() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PAYMENT_PROOF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PAYMENT_PROOF && resultCode == RESULT_OK && data != null) {
            selectedProofUri = data.getData();
            imgPaymentProof.setImageURI(selectedProofUri);
            imgPaymentProof.setVisibility(View.VISIBLE);
            uploadProofToImageKit(selectedProofUri);
        }
    }

    private void loadDestinations() {
        db.collection("destinations").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> destinationNames = new ArrayList<>();
            final List<String> destinationIds = new ArrayList<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                destinationNames.add(name != null ? name : "Unknown");
                destinationIds.add(doc.getId());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    destinationNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDestinations.setAdapter(adapter);

            spinnerDestinations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedDestinationId = destinationIds.get(position);
                    loadDestinationDetails(selectedDestinationId);
                    loadTrips(selectedDestinationId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        });
    }

    private void loadDestinationDetails(String destinationId) {
        db.collection("destinations").document(destinationId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long reg = doc.getLong("regularFare");
                        Long stu = doc.getLong("studentFare");
                        Long sen = doc.getLong("seniorFare");
                        Long tTime = doc.getLong("travelTime");

                        regularFare = (reg != null) ? reg.intValue() : 0;
                        studentFare = (stu != null) ? stu.intValue() : 0;
                        seniorFare = (sen != null) ? sen.intValue() : 0;
                        travelTimeMinutes = (tTime != null) ? tTime.intValue() : 0;

                        updateFarePreview();
                        tvTripTravelTime.setText("Travel Time: " + formatTravelTime(travelTimeMinutes));
                        layoutTripDetails.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadTrips(String destinationId) {
        if (tripsListener != null) {
            tripsListener.remove();
        }

        tripsListener = db.collection("trips")
                .whereEqualTo("destinationId", destinationId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to listen to trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots == null) return;

                    int previousSelection = spinnerTrips.getSelectedItemPosition();

                    tripList.clear();
                    List<String> tripNames = new ArrayList<>();

                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                    Date now = calendar.getTime();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp departure = doc.getTimestamp("departure");
                        if (departure == null) continue;

                        Date depDate = departure.toDate();
                        if (depDate.before(now)) continue;

                        tripList.add(doc);

                        String formattedDate = dateFormat.format(depDate);
                        Long availableSeatsObj = doc.getLong("availableSeats");
                        long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

                        if (availableSeats == 0) {
                            formattedDate += "  -- Sold Out --";
                        }

                        tripNames.add(formattedDate);
                    }

                    if (tripNames.isEmpty()) {
                        tripNames.add("No available schedule");
                        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                tripNames
                        );
                        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerTrips.setAdapter(emptyAdapter);
                        spinnerTrips.setEnabled(false);
                        layoutTripDetails.setVisibility(View.GONE);

                        etRegularCount.setEnabled(false);
                        etStudentCount.setEnabled(false);
                        etSeniorCount.setEnabled(false);
                        btnBookNow.setEnabled(false);
                        etRegularCount.setText("0");
                        etStudentCount.setText("0");
                        etSeniorCount.setText("0");
                        tvTotalFare.setText("Total Fare: ₱0");
                        return;
                    } else {
                        spinnerTrips.setEnabled(true);
                        etRegularCount.setEnabled(true);
                        etStudentCount.setEnabled(true);
                        etSeniorCount.setEnabled(true);
                        btnBookNow.setEnabled(true);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            tripNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTrips.setAdapter(adapter);

                    if (previousSelection >= 0 && previousSelection < tripList.size()) {
                        spinnerTrips.setSelection(previousSelection);
                    }

                    spinnerTrips.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (tripList != null && !tripList.isEmpty() && position < tripList.size()) {
                                selectedTripIndex = position;
                                showTripDetails(tripList.get(position));
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            layoutTripDetails.setVisibility(View.GONE);
                        }
                    });
                });
    }

    private void showTripDetails(DocumentSnapshot tripDoc) {
        String departure = formatDeparture(tripDoc.get("departure"));
        String vanPlate = tripDoc.getString("vanId");
        Long availableSeatsObj = tripDoc.getLong("availableSeats");
        long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

        currentPaymentMethod = tripDoc.getString("paymentMethod");
        currentQRCodeUrl = tripDoc.getString("qrCodeUrl");
        currentPaymentNumber = tripDoc.getString("paymentNumber");

        if (currentPaymentMethod == null) currentPaymentMethod = "Cash";

        tvTripDeparture.setText("Departure: " + departure);
        tvTripVan.setText("Van: " + (vanPlate != null ? vanPlate : "-"));
        tvTripSeats.setText("Available Seats: " + availableSeats);
        tvTripTravelTime.setText("Travel Time: " + formatTravelTime(travelTimeMinutes));
        tvPaymentMethod.setText("Payment Method: " + currentPaymentMethod);

        updatePaymentUI();
        layoutTripDetails.setVisibility(View.VISIBLE);

        boolean isSoldOut = (availableSeats == 0);
        etRegularCount.setEnabled(!isSoldOut);
        etStudentCount.setEnabled(!isSoldOut);
        etSeniorCount.setEnabled(!isSoldOut);
        btnBookNow.setEnabled(!isSoldOut);

        if (isSoldOut) {
            etRegularCount.setText("0");
            etStudentCount.setText("0");
            etSeniorCount.setText("0");
            tvTotalFare.setText("Total Fare: ₱0");
        }

        db.collection("trips").document(tripDoc.getId())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Long seats = snapshot.getLong("availableSeats");
                        long liveSeats = (seats != null) ? seats : 0L;

                        tvTripSeats.setText("Available Seats: " + liveSeats);

                        boolean nowSoldOut = (liveSeats == 0);
                        etRegularCount.setEnabled(!nowSoldOut);
                        etStudentCount.setEnabled(!nowSoldOut);
                        etSeniorCount.setEnabled(!nowSoldOut);
                        btnBookNow.setEnabled(!nowSoldOut);

                        if (nowSoldOut) {
                            etRegularCount.setText("0");
                            etStudentCount.setText("0");
                            etSeniorCount.setText("0");
                            tvTotalFare.setText("Total Fare: ₱0");
                        }
                    }
                });
    }

    private void updatePaymentUI() {
        uploadedProofUrl = null;
        selectedProofUri = null;
        imgPaymentProof.setVisibility(View.GONE);

        if (currentPaymentMethod.equals("Cash & Gcash")) {
            layoutPaymentChoice.setVisibility(View.VISIBLE);
            radioCash.setChecked(true);
            userSelectedPayment = "Cash";
            layoutPaymentProof.setVisibility(View.GONE);
        } else if (currentPaymentMethod.equals("Gcash")) {
            layoutPaymentChoice.setVisibility(View.GONE);
            layoutPaymentProof.setVisibility(View.VISIBLE);
            updatePaymentProofUI();
        } else {
            layoutPaymentChoice.setVisibility(View.GONE);
            layoutPaymentProof.setVisibility(View.GONE);
        }
    }

    private void updatePaymentProofUI() {
        if (currentQRCodeUrl != null && !currentQRCodeUrl.isEmpty()) {
            layoutQRCode.setVisibility(View.VISIBLE);
            imgQRCode.setVisibility(View.VISIBLE);
            loadImageWithGlide(currentQRCodeUrl, imgQRCode);
            imgQRCode.setOnClickListener(v -> showFullScreenImage(currentQRCodeUrl));
        } else {
            layoutQRCode.setVisibility(View.GONE);
            imgQRCode.setVisibility(View.GONE);
        }

        if (currentPaymentNumber != null && !currentPaymentNumber.isEmpty()) {
            layoutMobileNumber.setVisibility(View.VISIBLE);
            tvMobileNumber.setText(currentPaymentNumber);
        } else {
            layoutMobileNumber.setVisibility(View.GONE);
        }

        if ((currentQRCodeUrl == null || currentQRCodeUrl.isEmpty()) &&
                (currentPaymentNumber == null || currentPaymentNumber.isEmpty())) {
            layoutPaymentProof.setVisibility(View.GONE);
        }
    }

    private void handleBooking() {
        if (currentPaymentMethod.equals("Cash & Gcash")) {
            if (userSelectedPayment.equals("Gcash")) {
                if (uploadedProofUrl == null) {
                    Toast.makeText(this, "Please upload payment proof before booking", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            saveBooking(userSelectedPayment);
        } else if (currentPaymentMethod.equals("Gcash")) {
            if (uploadedProofUrl == null) {
                Toast.makeText(this, "Please upload payment proof before booking", Toast.LENGTH_SHORT).show();
                return;
            }
            saveBooking("Gcash");
        } else {
            saveBooking("Cash");
        }
    }

    private String formatDeparture(Object departureObj) {
        if (departureObj instanceof Timestamp) {
            return dateFormat.format(((Timestamp) departureObj).toDate());
        } else if (departureObj != null) {
            return departureObj.toString();
        } else {
            return "-";
        }
    }

    private String formatTravelTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + " hours " + mins + " minutes";
        } else {
            return mins + " minutes";
        }
    }

    private void updateFarePreview() {
        int regular = parseIntSafe(etRegularCount.getText().toString());
        int student = parseIntSafe(etStudentCount.getText().toString());
        int senior = parseIntSafe(etSeniorCount.getText().toString());

        int totalFare = (regular * regularFare) + (student * studentFare) + (senior * seniorFare);
        tvTotalFare.setText("Total Fare: ₱" + totalFare);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveBooking(String finalPaymentMethod) {
        if (selectedTripIndex == -1) {
            Toast.makeText(this, "Please select a trip first", Toast.LENGTH_SHORT).show();
            return;
        }

        final int regular = parseIntSafe(etRegularCount.getText().toString());
        final int student = parseIntSafe(etStudentCount.getText().toString());
        final int senior = parseIntSafe(etSeniorCount.getText().toString());
        final int passengerCount = regular + student + senior;

        if (passengerCount <= 0) {
            Toast.makeText(this, "Enter at least one passenger", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentSnapshot selectedTrip = tripList.get(selectedTripIndex);
        final String tripId = selectedTrip.getId();
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final Timestamp departure = selectedTrip.getTimestamp("departure");
        final String destinationId = selectedTrip.getString("destinationId");
        final int totalFare = (regular * regularFare) + (student * studentFare) + (senior * seniorFare);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
        Date now = calendar.getTime();
        final Timestamp createdAt = new Timestamp(now);

        progressBar.setVisibility(View.VISIBLE);
        btnBookNow.setEnabled(false);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentReference tripRef = db.collection("trips").document(tripId);
            DocumentSnapshot tripSnapshot = transaction.get(tripRef);

            Long availableSeatsObj = tripSnapshot.getLong("availableSeats");
            long availableSeats = (availableSeatsObj != null) ? availableSeatsObj : 0L;

            if (passengerCount > availableSeats) {
                throw new FirebaseFirestoreException(
                        "Not enough available seats",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            long newSeats = availableSeats - passengerCount;
            transaction.update(tripRef, "availableSeats", newSeats);

            DocumentReference bookingRef = db.collection("bookings").document();
            String bookingId = bookingRef.getId();

            Map<String, Object> booking = new HashMap<>();
            booking.put("bookingId", bookingId);
            booking.put("status", "Pending");
            booking.put("regularCount", regular);
            booking.put("studentCount", student);
            booking.put("seniorCount", senior);
            booking.put("seats", passengerCount);
            booking.put("departure", departure);
            booking.put("destinationId", destinationId);
            booking.put("tripId", tripId);
            booking.put("userId", userId);
            booking.put("totalFare", totalFare);
            booking.put("createdAt", createdAt);
            booking.put("paymentMethod", finalPaymentMethod);

            if (uploadedProofUrl != null) {
                booking.put("paymentProofUrl", uploadedProofUrl);
            }

            transaction.set(bookingRef, booking);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(BookRideActivity.this, "Booking confirmed!", Toast.LENGTH_SHORT).show();

            etRegularCount.setText("0");
            etStudentCount.setText("0");
            etSeniorCount.setText("0");
            updateFarePreview();

            uploadedProofUrl = null;
            selectedProofUri = null;
            imgPaymentProof.setVisibility(View.GONE);

        }).addOnFailureListener(e ->
                Toast.makeText(BookRideActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            btnBookNow.setEnabled(true);
        });
    }

    // ImageKit Upload Methods
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private long getExpireTimestamp() {
        return (System.currentTimeMillis() / 1000) + 3600;
    }

    private String generateSignature(String token, long expire) {
        try {
            String stringToSign = token + expire;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret_key = new SecretKeySpec(IMAGEKIT_PRIVATE_KEY.getBytes("UTF-8"), "HmacSHA1");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(stringToSign.getBytes("UTF-8"));

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

    private void uploadProofToImageKit(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                inputStream.close();

                String token = generateToken();
                long expire = getExpireTimestamp();
                String signature = generateSignature(token, expire);

                if (signature == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to generate signature", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                String fileName = "payment_proof_" + System.currentTimeMillis() + ".jpg";

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName,
                                RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .addFormDataPart("fileName", fileName)
                        .addFormDataPart("publicKey", IMAGEKIT_PUBLIC_KEY)
                        .addFormDataPart("signature", signature)
                        .addFormDataPart("expire", String.valueOf(expire))
                        .addFormDataPart("token", token)
                        .addFormDataPart("folder", "payment_proofs")
                        .addFormDataPart("useUniqueFileName", "true")
                        .build();

                Request request = new Request.Builder()
                        .url(IMAGEKIT_UPLOAD_URL)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    uploadedProofUrl = parseUrlFromResponse(responseBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Payment proof uploaded!", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + errorBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Upload error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
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

    private void loadImageWithGlide(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView);
        }
    }

    private void showFullScreenImage(String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);

        ImageView imgFullScreen = dialogView.findViewById(R.id.imgFullScreen);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgFullScreen);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        imgFullScreen.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tripsListener != null) {
            tripsListener.remove();
        }
    }
}