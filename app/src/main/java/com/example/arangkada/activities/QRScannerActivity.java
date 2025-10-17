package com.example.arangkada.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.arangkada.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";
    private FirebaseFirestore db;
    private PreviewView previewView;
    private boolean isProcessing = false;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted");
                    startCamera();
                } else {
                    Log.e(TAG, "Camera permission denied");
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        db = FirebaseFirestore.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission already granted");
            startCamera();
        } else {
            Log.d(TAG, "Requesting camera permission");
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        Log.d(TAG, "Starting camera...");

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "Camera provider obtained");
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Error getting camera provider", e);
                Toast.makeText(this, "Failed to start camera: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider is null");
            return;
        }


        cameraProvider.unbindAll();


        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;


        Preview preview = new Preview.Builder()
                .build();


        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Log.d(TAG, "Preview surface provider set");


        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            processImageProxy(scanner, imageProxy);
        });

        try {

            Camera camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );

            Log.d(TAG, "Camera bound successfully");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(this, "Camera binding failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void processImageProxy(BarcodeScanner scanner, ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        try {
            @SuppressWarnings("UnsafeOptInUsageError")
            android.media.Image mediaImage = imageProxy.getImage();

            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            if (barcode.getRawValue() != null && !barcode.getRawValue().isEmpty()) {
                                String bookingId = barcode.getRawValue();
                                Log.d(TAG, "QR Code detected: " + bookingId);
                                isProcessing = true;
                                runOnUiThread(() -> fetchBookingDetails(bookingId));
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Barcode scanning failed", e);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            imageProxy.close();
        }
    }

    private void fetchBookingDetails(String bookingId) {
        Log.d(TAG, "Fetching booking details for: " + bookingId);

        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Booking found, showing dialog");
                            showBookingDialog(documentSnapshot);
                        } else {
                            Log.w(TAG, "Booking not found");
                            Toast.makeText(this, "Booking not found.", Toast.LENGTH_SHORT).show();
                            isProcessing = false;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing booking data", e);
                        Toast.makeText(this, "Error loading booking: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching booking", e);
                    Toast.makeText(this, "Error fetching booking details: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                });
    }

    private void showBookingDialog(DocumentSnapshot doc) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_details, null);
            builder.setView(dialogView);

            TextView txtUser = dialogView.findViewById(R.id.txtUser);
            TextView tvStatus = dialogView.findViewById(R.id.tv_status);
            TextView tvRoute = dialogView.findViewById(R.id.tv_route);
            TextView tvDeparture = dialogView.findViewById(R.id.tv_departure);
            TextView tvPassengers = dialogView.findViewById(R.id.tv_passengers);
            TextView tvTotalFare = dialogView.findViewById(R.id.tv_total_fare);
            TextView tvPaymentMethod = dialogView.findViewById(R.id.tv_payment_method);
            TextView tvPaymentWarning = dialogView.findViewById(R.id.tv_payment_warning);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnComplete = dialogView.findViewById(R.id.btnComplete);

            String userId = doc.getString("userId");
            String destinationId = doc.getString("destinationId");
            String status = doc.getString("status");
            String paymentMethod = doc.getString("paymentMethod");

            tvStatus.setText(status != null ? status : "Pending");

            // Display payment method
            if (paymentMethod == null) paymentMethod = "Cash";
            tvPaymentMethod.setText("Payment: " + paymentMethod);

            // Show warning if payment method is Cash
            if (paymentMethod.equals("Cash")) {
                tvPaymentWarning.setVisibility(View.VISIBLE);
            } else {
                tvPaymentWarning.setVisibility(View.GONE);
            }

            Object departureObj = doc.get("departure");
            String formattedDeparture = "N/A";
            if (departureObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) departureObj;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy h:mm a", java.util.Locale.getDefault());
                formattedDeparture = sdf.format(ts.toDate());
            }

            Long regularCount = doc.getLong("regularCount");
            Long studentCount = doc.getLong("studentCount");
            Long seniorCount = doc.getLong("seniorCount");
            Long totalFare = doc.getLong("totalFare");

            tvPassengers.setText("Regular: " + (regularCount != null ? regularCount : 0) +
                    ", Student: " + (studentCount != null ? studentCount : 0) +
                    ", Senior: " + (seniorCount != null ? seniorCount : 0));
            tvTotalFare.setText("₱" + (totalFare != null ? totalFare : 0));
            tvDeparture.setText(formattedDeparture);

            AlertDialog dialog = builder.create();


            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }


            if (userId != null) {
                db.collection("accounts").document(userId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String userName = userDoc.getString("name");
                            txtUser.setText(userName != null ? userName : "Unknown User");
                        });
            } else {
                txtUser.setText("Unknown User");
            }


            if (destinationId != null) {
                db.collection("destinations").document(destinationId)
                        .get()
                        .addOnSuccessListener(destDoc -> {
                            String destName = destDoc.getString("name");
                            tvRoute.setText(destName != null ? destName : "Unknown Destination");
                        });
            } else {
                tvRoute.setText("Unknown Destination");
            }

            btnComplete.setOnClickListener(v -> {
                db.collection("bookings").document(doc.getId())
                        .update("status", "Completed")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Booking marked as Completed.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            isProcessing = false;
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to complete booking.", Toast.LENGTH_SHORT).show();
                            isProcessing = false;
                        });
            });

            btnCancel.setOnClickListener(v -> showCancelReasonDialog(doc, dialog));

            dialog.setOnDismissListener(d -> isProcessing = false);
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing booking dialog", e);
            Toast.makeText(this, "Error displaying booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isProcessing = false;
        }
    }


    private void showCancelReasonDialog(DocumentSnapshot doc, AlertDialog parentDialog) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View reasonView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_reason, null);
            builder.setView(reasonView);

            EditText etReason = reasonView.findViewById(R.id.etReason);
            Button btnBack = reasonView.findViewById(R.id.btnBack);
            Button btnProceed = reasonView.findViewById(R.id.btnProceed);

            AlertDialog dialog = builder.create();


            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            btnBack.setOnClickListener(v -> dialog.dismiss());

            btnProceed.setOnClickListener(v -> {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please provide a reason.", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("bookings").document(doc.getId())
                        .update("status", "Cancelled", "reason", reason)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            parentDialog.dismiss();
                            isProcessing = false;
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to cancel booking", e);
                            Toast.makeText(this, "Failed to cancel booking.", Toast.LENGTH_SHORT).show();
                        });
            });

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing cancel dialog", e);
            Toast.makeText(this, "Error displaying cancel dialog: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}