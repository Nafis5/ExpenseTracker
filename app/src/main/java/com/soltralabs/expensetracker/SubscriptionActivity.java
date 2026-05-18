package com.soltralabs.expensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class SubscriptionActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    AlertDialog.Builder builder;
    DatabaseBackupManager backupManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize backupManager to avoid NullPointerException
        backupManager = new DatabaseBackupManager(this);

        Button continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> {
            Log.d("SubscriptionActivity", "Continue button clicked");
            Toast.makeText(this, "Upgraded", Toast.LENGTH_SHORT).show();
            
            boolean isConnected = isInternetConnected(SubscriptionActivity.this);
            Log.d("SubscriptionActivity", "Internet connected: " + isConnected);

            if (isConnected) {
                Log.d("SubscriptionActivity", "Checking for user data...");
                backupManager.hasUserData(hasData -> {
                    Log.d("SubscriptionActivity", "User data check complete. Has data: " + hasData);
                    // This code runs on the main thread after the check is complete
                    if (hasData) {
                        // User has data, so proceed with your logic (e.g., call backup)
                        runOnUiThread(() -> {
                            Log.d("SubscriptionActivity", "Showing email backup dialog");
                            // Any UI updates must be here
                            showEmailBackupDialog(this);
                            // Now call the backup method
                        });
                    } else {
                        // User has no data
                        runOnUiThread(() -> {
                            Log.d("SubscriptionActivity", "No user data, opening Galaxy App Store");
                            openAppInGalaxyAppStore("com.soltralabs.workoutlog.gymworkouttracker.pro");
                        });
                    }
                });
            } else {
                Log.d("SubscriptionActivity", "No internet connection");
                Toast.makeText(this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public static boolean isInternetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    void backUpData(String email) throws IOException {

        DatabaseBackupManager.userEmail = email;

        // 'this' is your Activity context

        backupManager.backupDatabase(new DatabaseBackupManager.OnBackupListener() {
            @Override
            public void onBackupSuccess() {
                // Handle success - e.g., show a toast message
                Toast.makeText(SubscriptionActivity.this, "Backup successful!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBackupFailure(Exception e) {
                // Handle failure - e.g., log the error or show an error message
                Log.e("Backup", "Backup failed", e);
            }
        });


    }





    private void openAppInGalaxyAppStore(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://galaxystore.samsung.com/detail/com.soltralabs.expensetracker.pro"));
        startActivity(intent);

    }

    void goToMain() {
        Intent i = new Intent(this, MainActivity.class);

        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goToMain();
    }

    public void showEmailBackupDialog(Context context) {
        // Create AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_email_backup, null);

        // Get references to views
        TextView titleText = dialogView.findViewById(R.id.tv_dialog_title);
        TextView messageText = dialogView.findViewById(R.id.tv_dialog_message);
        EditText emailInput = dialogView.findViewById(R.id.et_email_input);
        Button submitButton = dialogView.findViewById(R.id.btn_submit);
        Button skipButton = dialogView.findViewById(R.id.btn_skip);

        // Set dialog content
        titleText.setText("Backup Your Data");
        messageText.setText("Enter your email address to backup your data to the cloud. This will help you restore your data on other devices.");

        // Set the custom view to dialog
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissal by touching outside

        // Create and show dialog
        AlertDialog dialog = builder.create();

        // Submit button click listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    emailInput.setError("Please enter your email address");
                    emailInput.requestFocus();
                    return;
                }

                if (!isValidEmail(email)) {
                    emailInput.setError("Please enter a valid email address");
                    emailInput.requestFocus();
                    return;
                }

                // Process the email for backup
                try {
                    processEmailBackup(email, context);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                openAppInGalaxyAppStore("com.soltralabs.workoutlog.gymworkouttracker.pro");
                dialog.dismiss();
            }
        });

        // Skip button click listener
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle skip action
                openAppInGalaxyAppStore("com.soltralabs.expensetracker.pro");
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Helper method to validate email
    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Method to handle email backup process
    private void processEmailBackup(String email, Context context) throws IOException {
        // Show loading or progress indicator here if needed
        DatabaseBackupManager.userEmail = email;

        DatabaseBackupManager backupManager = new DatabaseBackupManager(this); // 'this' is your Activity context

        backupManager.backupDatabase(new DatabaseBackupManager.OnBackupListener() {
            @Override
            public void onBackupSuccess() {
                // Handle success - e.g., show a toast message
                Toast.makeText(SubscriptionActivity.this, "Backup successful!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBackupFailure(Exception e) {
                // Handle failure - e.g., log the error or show an error message
                Log.e("Backup", "Backup failed", e);
            }
        });
    }
}
