package com.soltralabs.expensetracker;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DatabaseBackupManager {

    private static final String TAG = "DatabaseBackupManager";
    private static final String BACKUP_FOLDER = "expense_tracker_backup";

    // This will be populated with the user's email address upon login
    public static String userEmail = "";

    private final DatabaseHelper db;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final FirebaseStorage storage;

    public interface OnBackupListener {
        void onBackupSuccess();
        void onBackupFailure(Exception e);
    }

    public interface OnRestoreListener {
        void onRestoreSuccess();
        void onRestoreFailure(Exception e);
    }

    public interface OnDataCheckListener {
        void onDataCheckComplete(boolean hasData);
    }

    public DatabaseBackupManager(Context context) {
        this.db = new DatabaseHelper(context);
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Backs up the entire database to a JSON file in Firebase Storage.
     * The file is named after the user's email.
     */
    public void backupDatabase(OnBackupListener listener) {
        if (userEmail == null || userEmail.isEmpty()) {
            listener.onBackupFailure(new Exception("User email is not set."));
            return;
        }

        executor.execute(() -> {
            try {
                // 1. Fetch all data from all tables
                List<Transaction> transactions = db.getAllTransactions();
                List<Category> categories = db.getAllCategories();
                List<Budget> budgets = db.getAllBudgets();
                UserPreferences userPreferences = db.getUserPreferences();

                FullBackup fullBackup = new FullBackup(transactions, categories, budgets, userPreferences);

                // 2. Convert to JSON
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonBackup = gson.toJson(fullBackup);

                // 3. Upload to Firebase Storage
                uploadToFirebase(jsonBackup.getBytes(StandardCharsets.UTF_8), listener);

            } catch (Exception e) {
                Log.e(TAG, "Error during database backup preparation", e);
                listener.onBackupFailure(e);
            }
        });
    }

    /**
     * Restores the database from a JSON file in Firebase Storage.
     */
    public void restoreDatabase(OnRestoreListener listener) {
        if (userEmail == null || userEmail.isEmpty()) {
            listener.onRestoreFailure(new Exception("User email is not set."));
            return;
        }

        StorageReference storageRef = storage.getReference().child(BACKUP_FOLDER).child(userEmail + ".json");

        final long ONE_MEGABYTE = 1024 * 1024;
        storageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            String jsonBackup = new String(bytes, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            FullBackup fullBackup = gson.fromJson(jsonBackup, FullBackup.class);

            executor.execute(() -> {
                try {
                    // Clear all tables before restoring
                    // Note: clearAllTables re-adds default categories
                    db.clearAllTables();

                    // Insert restored data
                    
                    // 1. User Preferences
                    if (fullBackup.userPreferences != null) {
                        db.saveUserPreferences(fullBackup.userPreferences);
                    }

                    // 2. Categories
                    // Only add non-default (custom) categories, as default ones are re-created by clearAllTables
                    // Or check existence to avoid duplicates/errors
                    List<String> existingCategoryNames = db.getAllCategoryNames();
                    if (fullBackup.categories != null) {
                        for (Category cat : fullBackup.categories) {
                            if (!existingCategoryNames.contains(cat.getName())) {
                                db.addCategory(cat.getName());
                            }
                        }
                    }

                    // 3. Budgets
                    if (fullBackup.budgets != null) {
                        for (Budget budget : fullBackup.budgets) {
                            db.addBudget(budget);
                        }
                    }

                    // 4. Transactions
                    if (fullBackup.transactions != null) {
                        for (Transaction t : fullBackup.transactions) {
                            db.addTransaction(t);
                        }
                    }

                    listener.onRestoreSuccess();
                } catch (Exception e) {
                    Log.e(TAG, "Error restoring database from JSON", e);
                    listener.onRestoreFailure(e);
                }
            });
        }).addOnFailureListener(listener::onRestoreFailure);
    }

    private void uploadToFirebase(byte[] data, OnBackupListener listener) {
        StorageReference storageRef = storage.getReference().child(BACKUP_FOLDER).child(userEmail + ".json");

        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> listener.onBackupSuccess())
                .addOnFailureListener(listener::onBackupFailure);
    }

    /**
     * Asynchronously checks if the database contains any user-generated transaction data.
     * This method runs the query on a background thread and returns the result via a callback.
     */
    public void hasUserData(OnDataCheckListener listener) {
        executor.execute(() -> {
            int transactionCount = db.getTransactionsCount();
            listener.onDataCheckComplete(transactionCount > 0);
        });
    }

    /**
     * A wrapper class to hold all database tables for JSON serialization.
     */
    private static class FullBackup {
        List<Transaction> transactions;
        List<Category> categories;
        List<Budget> budgets;
        UserPreferences userPreferences;

        public FullBackup(List<Transaction> transactions, List<Category> categories, List<Budget> budgets, UserPreferences userPreferences) {
            this.transactions = transactions;
            this.categories = categories;
            this.budgets = budgets;
            this.userPreferences = userPreferences;
        }
    }
}
