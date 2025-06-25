package com.soltralabs.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        premiumManager = PremiumManager.getInstance(this);



        // Set current month
        Calendar cal = Calendar.getInstance();
        currentMonthYear = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        initViews();
        setupListeners();
        loadDashboardData();
        AdManager.loadBannerAd(this, findViewById(R.id.ad_container));
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
        previousMonthButton.setOnClickListener(v -> showMonthChangeUpgradeDialog());
        nextMonthButton.setOnClickListener(v -> showMonthChangeUpgradeDialog());

        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);
        fabAddTransaction.setOnClickListener(view -> {
            UserPreferences prefs = db.getUserPreferences();
            if(!premiumManager.canAddTransaction(prefs.getTransactionCount())){
                showUpgradeDialog(getString(R.string.upgrade_prompt_message_limit));
                return;
            }
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST);
        });

        Button viewAllButton = findViewById(R.id.view_all_transactions_button);
        viewAllButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });
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
        String[] categories = getResources().getStringArray(R.array.transaction_categories);
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
                    // Non-functional for this project
                    Toast.makeText(this, "Upgrade feature not implemented.", Toast.LENGTH_SHORT).show();
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
        if (requestCode == ADD_TRANSACTION_REQUEST && resultCode == RESULT_OK && data != null) {
            String categoryToPrompt = data.getStringExtra("PROMPT_BUDGET_FOR_CATEGORY");
            if (categoryToPrompt != null) {
                // Since this is the first time, budget limit is 0
                showBudgetSettingDialog(categoryToPrompt, 0);
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
}