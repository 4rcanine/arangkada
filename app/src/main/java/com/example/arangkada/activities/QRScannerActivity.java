package com.example.arangkada.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.arangkada.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class QRScannerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private PreviewView previewView;
    private boolean isProcessing = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        db = FirebaseFirestore.getInstance();

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    if (isProcessing) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees());

                    scanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    if (barcode.getRawValue() != null) {
                                        String bookingId = barcode.getRawValue();
                                        isProcessing = true;
                                        fetchBookingDetails(bookingId);
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {})
                            .addOnCompleteListener(task -> imageProxy.close());
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this,
                        CameraSelector.DEFAULT_BACK_CAMERA, analysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void fetchBookingDetails(String bookingId) {
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        showBookingDialog(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Booking not found.", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching booking details.", Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                });
    }

    private void showBookingDialog(DocumentSnapshot doc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_details, null);
        builder.setView(dialogView);

        TextView tvBookingId = dialogView.findViewById(R.id.tvBookingId);
        TextView tvDestination = dialogView.findViewById(R.id.tvDestination);
        TextView tvDeparture = dialogView.findViewById(R.id.tvDeparture);
        TextView tvPassengers = dialogView.findViewById(R.id.tvPassengers);
        TextView tvFare = dialogView.findViewById(R.id.tvFare);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnComplete = dialogView.findViewById(R.id.btnComplete);

        tvBookingId.setText("Booking ID: " + doc.getString("bookingId"));
        tvDestination.setText("Destination: " + doc.getString("destinationId"));
        tvDeparture.setText("Departure: " + doc.getString("departure"));
        tvPassengers.setText("Passengers: Regular " + doc.getLong("regularCount") +
                ", Student " + doc.getLong("studentCount") +
                ", Senior " + doc.getLong("seniorCount"));
        tvFare.setText("Total Fare: ₱" + doc.getLong("totalFare"));

        AlertDialog dialog = builder.create();

        btnComplete.setOnClickListener(v -> {
            db.collection("bookings").document(doc.getId())
                    .update("status", "Completed")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Booking marked as Completed.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        isProcessing = false;
                    });
        });

        btnCancel.setOnClickListener(v -> showCancelReasonDialog(doc, dialog));

        dialog.show();
    }

    private void showCancelReasonDialog(DocumentSnapshot doc, AlertDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View reasonView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_reason, null);
        builder.setView(reasonView);

        EditText etReason = reasonView.findViewById(R.id.etReason);
        Button btnBack = reasonView.findViewById(R.id.btnBack);
        Button btnProceed = reasonView.findViewById(R.id.btnProceed);

        AlertDialog dialog = builder.create();

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
                        Toast.makeText(this, "Failed to cancel booking.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}
