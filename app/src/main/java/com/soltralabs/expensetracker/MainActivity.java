package com.soltralabs.expensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener, BudgetSettingDialog.BudgetDialogListener {

    private static final int ADD_TRANSACTION_REQUEST = 1;
    private static final int MANAGE_CATEGORIES_REQUEST = 2;

    private DatabaseHelper db;
    private PremiumManager premiumManager;
    private String currentMonthYear;

    private TextView totalIncomeTextView, totalExpenseTextView, totalRemainingTextView, monthDisplayTextView;
    private ImageView previousMonthButton, nextMonthButton;
    private RecyclerView categoriesRecyclerView, recentTransactionsRecyclerView;
    private CategoryAdapter categoryAdapter;
    private TransactionAdapter transactionAdapter;
    private List<CategoryAdapter.CategoryBudget> categoryBudgets;
    private List<Transaction> recentTransactions;
    private FirebaseAnalytics mFirebaseAnalytics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = new DatabaseHelper(this);
        premiumManager = PremiumManager.getInstance(this);
        premiumManager.setIsPremium(true, this);
        // Set current month
        Calendar cal = Calendar.getInstance();
        currentMonthYear = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        initViews();
        setupListeners();
        loadDashboardData();
        if (premiumManager.shouldShowAds()) {
            AdManager.loadBannerAd(this, findViewById(R.id.ad_container));
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        
        checkAndTriggerRestore();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_transaction) {
            UserPreferences prefs = db.getUserPreferences();
            if(!premiumManager.canAddTransaction(prefs.getTransactionCount())){
                showUpgradeDialog(getString(R.string.upgrade_prompt_message_limit));
                return true;
            }
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST);
            return true;
        } else if (item.getItemId() == R.id.action_manage_categories) {
            Intent intent = new Intent(this, ManageCategoriesActivity.class);
            startActivityForResult(intent, MANAGE_CATEGORIES_REQUEST);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        totalIncomeTextView = findViewById(R.id.total_income_amount);
        totalExpenseTextView = findViewById(R.id.total_expense_amount);
        totalRemainingTextView = findViewById(R.id.total_remaining_amount);
        monthDisplayTextView = findViewById(R.id.month_display_textview);
        previousMonthButton = findViewById(R.id.previous_month_button);
        nextMonthButton = findViewById(R.id.next_month_button);
        
        // Setup Categories RecyclerView
        categoriesRecyclerView = findViewById(R.id.categories_recyclerview);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryBudgets = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, categoryBudgets, this);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Setup Recent Transactions RecyclerView
        recentTransactionsRecyclerView = findViewById(R.id.recent_transactions_recyclerview);
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTransactions = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, recentTransactions, transaction -> {
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            intent.putExtra("TRANSACTION_ID", transaction.getId());
            startActivity(intent);
        });
        recentTransactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void setupListeners() {
        setupMonthNavigation();

        Button viewAllButton = findViewById(R.id.view_all_transactions_button);
        viewAllButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });
    }

    private void setupMonthNavigation() {
        previousMonthButton.setOnClickListener(v -> {
            if (premiumManager.canViewHistoricalData()) {
                changeMonth(-1);
            } else {
                showMonthChangeUpgradeDialog();
            }
        });

        nextMonthButton.setOnClickListener(v -> {
            if (premiumManager.canViewHistoricalData()) {
                changeMonth(1);
            } else {
                showMonthChangeUpgradeDialog();
            }
        });
    }

    private void changeMonth(int amount) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = sdf.parse(currentMonthYear);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MONTH, amount);
            currentMonthYear = sdf.format(cal.getTime());
            loadDashboardData();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void loadDashboardData() {
        // Load summary
        List<Transaction> monthTransactions = db.getTransactionsForMonth(currentMonthYear);
        double totalIncome = 0;
        double totalExpense = 0;
        for (Transaction t : monthTransactions) {
            if (t.getType().equals("income")) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        updateMonthDisplay();
        totalIncomeTextView.setText(String.format(Locale.getDefault(), "$%.2f", totalIncome));
        totalExpenseTextView.setText(String.format(Locale.getDefault(), "$%.2f", totalExpense));
        
        double remaining = totalIncome - totalExpense;
        totalRemainingTextView.setText(String.format(Locale.getDefault(), "$%.2f", remaining));
        if (remaining < 0) {
            totalRemainingTextView.setTextColor(ContextCompat.getColor(this, R.color.expenseColor));
        } else {
            totalRemainingTextView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        
        // Load categories
        loadCategoryBudgetData();
        
        // Load recent transactions
        recentTransactions.clear();
        recentTransactions.addAll(db.getRecentTransactions(5));
        transactionAdapter.notifyDataSetChanged();
    }
    
    private void updateMonthDisplay() {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = parser.parse(currentMonthYear);
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            monthDisplayTextView.setText(formatter.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
            monthDisplayTextView.setText(currentMonthYear); // Fallback
        }
    }
    
    private void loadCategoryBudgetData() {
        List<CategoryAdapter.CategoryBudget> newBudgets = new ArrayList<>();
        List<String> categories = db.getAllCategoryNames();
        List<Transaction> monthTransactions = db.getTransactionsForMonth(currentMonthYear);
        
        for(String category : categories) {
            Budget budget = db.getBudget(category, currentMonthYear);
            double spentAmount = 0;
            for(Transaction t : monthTransactions) {
                if(t.getCategory() != null && t.getCategory().equals(category) && t.getType().equals("expense")) {
                    spentAmount += t.getAmount();
                }
            }
            double budgetLimit = budget != null ? budget.getMonthlyLimit() : 0;
            newBudgets.add(new CategoryAdapter.CategoryBudget(category, spentAmount, budgetLimit));
        
            if (budgetLimit > 0 && (spentAmount / budgetLimit) > 0.9) {
                checkAndShowBudgetAlert(category);
            }
        }
        categoryAdapter.setData(newBudgets);
    }

    private void checkAndShowBudgetAlert(String category) {
        SharedPreferences prefs = getSharedPreferences("BudgetAlerts", MODE_PRIVATE);
        String key = "alert_shown_" + category + "_" + currentMonthYear;
        boolean alertShown = prefs.getBoolean(key, false);

        if (!alertShown) {
            showBudgetAlert(category, key, prefs);
        }
    }

    @Override
    public void onCategoryClick(CategoryAdapter.CategoryBudget categoryBudget) {
        showBudgetSettingDialog(categoryBudget.getCategory(), categoryBudget.getBudgetLimit());
    }

    @Override
    public void onBudgetSet(String category, double newLimit) {
        Budget existingBudget = db.getBudget(category, currentMonthYear);
        if (existingBudget != null) {
            existingBudget.setMonthlyLimit(newLimit);
            db.updateBudget(existingBudget);
        } else {
            Budget newBudget = new Budget(category, newLimit, currentMonthYear);
            db.addBudget(newBudget);
        }
        loadCategoryBudgetData();
        Toast.makeText(this, "Budget for " + category + " updated.", Toast.LENGTH_SHORT).show();
    }
    
    private void showUpgradeDialog(String message){
         new AlertDialog.Builder(this)
                .setTitle(R.string.upgrade_prompt_title)
                .setMessage(message)
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(this, SubscriptionActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showBudgetAlert(String category, String key, SharedPreferences prefs) {
        String message = getString(R.string.budget_alert_message) + " " + category + ".";
        new AlertDialog.Builder(this)
                .setTitle(R.string.budget_alert_title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(key, true);
                    editor.apply();
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
            
            // Show Interstitial Ad if user is not premium
            UserPreferences prefs = db.getUserPreferences();
            if (!prefs.isPremium()) {
                InterstitialAdHelper.showAd(this);
            }
            
            if (data != null) {
                String categoryToPrompt = data.getStringExtra("PROMPT_BUDGET_FOR_CATEGORY");
                if (categoryToPrompt != null) {
                    // Since this is the first time, budget limit is 0
                    showBudgetSettingDialog(categoryToPrompt, 0);
                }
            }
        } else if (requestCode == MANAGE_CATEGORIES_REQUEST && resultCode == RESULT_OK && data != null) {
            String newCategoryName = data.getStringExtra("NEW_CATEGORY_NAME");
            if (newCategoryName != null) {
                // Prompt to set budget for the new category - REMOVED per user request
                // showBudgetSettingDialog(newCategoryName, 0);
                Toast.makeText(this, "Category " + newCategoryName + " created", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMonthChangeUpgradeDialog() {
        showUpgradeDialog(getString(R.string.upgrade_prompt_message_history));
    }

    private void showBudgetSettingDialog(String category, double limit) {
        BudgetSettingDialog dialog = BudgetSettingDialog.newInstance(
                category,
                limit
        );
        dialog.show(getSupportFragmentManager(), "BudgetSettingDialog");
    }


    //Code for data restoring for premium vesion
    private void checkAndTriggerRestore() {
        UserPreferences prefs = db.getUserPreferences();
        if (prefs.isPremium()) {
            DatabaseBackupManager backupManager = new DatabaseBackupManager(this);

            // Use the asynchronous method with a callback
            backupManager.hasUserData(hasData -> {
                if (!hasData) {
                    // If the user has no data, show the restore dialog on the main thread
                    runOnUiThread(() -> showEmailRestoreDialog(MainActivity.this));
                }
            });
        }
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void showEmailRestoreDialog(Context context) {
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
        titleText.setText("Backup/Restore Your Data");
        messageText.setText("Enter your email address to backup/restore your data. if you have notes saved on the free version then please enter the same email");

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
                //  saveEmailToPreferences(email,context);
                try {
                    processDataRestore(email, context);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                dialog.dismiss();

            }
        });

        // Skip button click listener
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

            }
        });

        dialog.show();
    }
    private void processDataRestore(String email, Context context) throws IOException {
        // Show loading or progress indicator here if needed
        DatabaseBackupManager.userEmail = email;
        DatabaseBackupManager backupManager = new DatabaseBackupManager(this);

        backupManager.restoreDatabase(new DatabaseBackupManager.OnRestoreListener() {
            @Override
            public void onRestoreSuccess() {
                // Handle success - e.g., show a toast and maybe restart the app or refresh data
                Toast.makeText(MainActivity.this, "Restore successful! Please re-open the app", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRestoreFailure(Exception e) {
                // Handle failure
                Log.e("Restore", "Restore failed", e);
            }
        });

    }
}