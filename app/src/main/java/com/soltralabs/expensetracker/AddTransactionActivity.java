package com.soltralabs.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText amountEditText, descriptionEditText;
    private Spinner categorySpinner;
    private Button datePickerButton, saveTransactionButton, deleteTransactionButton;
    private RadioGroup transactionTypeRadioGroup;
    private RadioButton expenseRadioButton, incomeRadioButton;
    private TextInputLayout descriptionInputLayout;

    private DatabaseHelper db;
    private Calendar selectedDate;
    private long transactionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        db = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();

        initViews();
        setupSpinner();
        setupListeners();

        // Check if we are editing an existing transaction or creating a new one
        if (getIntent().hasExtra("TRANSACTION_ID")) {
            setTitle("Edit Transaction");
            transactionId = getIntent().getLongExtra("TRANSACTION_ID", -1);
            loadTransactionData();
        } else {
            setTitle("Add Transaction");
            String type = getIntent().getStringExtra("TRANSACTION_TYPE");
            if ("income".equals(type)) {
                incomeRadioButton.setChecked(true);
            }
        }
        updateDateButtonText();
    }

    private void initViews() {
        amountEditText = findViewById(R.id.amount_edittext);
        descriptionEditText = findViewById(R.id.description_edittext);
        categorySpinner = findViewById(R.id.category_spinner);
        datePickerButton = findViewById(R.id.date_picker_button);
        saveTransactionButton = findViewById(R.id.save_transaction_button);
        deleteTransactionButton = findViewById(R.id.delete_transaction_button);
        transactionTypeRadioGroup = findViewById(R.id.transaction_type_radiogroup);
        expenseRadioButton = findViewById(R.id.expense_radiobutton);
        incomeRadioButton = findViewById(R.id.income_radiobutton);
        descriptionInputLayout = findViewById(R.id.description_text_input_layout);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        saveTransactionButton.setOnClickListener(v -> saveTransaction());
        deleteTransactionButton.setOnClickListener(v -> deleteTransaction());
        transactionTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.income_radiobutton) {
                categorySpinner.setVisibility(View.GONE);
                descriptionInputLayout.setHint("Source (Optional)");
            } else {
                categorySpinner.setVisibility(View.VISIBLE);
                descriptionInputLayout.setHint("Description (Optional)");
            }
        });
    }

    private void loadTransactionData() {
        Transaction transaction = db.getTransaction(transactionId);
        if (transaction == null) {
            Toast.makeText(this, "Transaction not found. It may have been deleted.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        amountEditText.setText(String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
        descriptionEditText.setText(transaction.getDescription());

        deleteTransactionButton.setVisibility(View.VISIBLE);
        saveTransactionButton.setText("Update");

        if (transaction.getType().equals("income")) {
            incomeRadioButton.setChecked(true);
            categorySpinner.setVisibility(View.GONE);
            descriptionInputLayout.setHint("Source (Optional)");
        } else {
            expenseRadioButton.setChecked(true);
            categorySpinner.setVisibility(View.VISIBLE);
            descriptionInputLayout.setHint("Description (Optional)");
            // Set spinner selection
            String[] categories = getResources().getStringArray(R.array.transaction_categories);
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(transaction.getCategory())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }

        // Set date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate.setTime(sdf.parse(transaction.getDate()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateDateButtonText();
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButtonText();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        datePickerButton.setText(sdf.format(selectedDate.getTime()));
    }

    private void deleteTransaction() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteTransaction(transactionId);
                    Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String description = descriptionEditText.getText().toString().trim();
        String type = expenseRadioButton.isChecked() ? "expense" : "income";
        String category;

        if (type.equals("income")) {
            category = "Income";
        } else {
            category = categorySpinner.getSelectedItem().toString();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = sdf.format(selectedDate.getTime());

        if (transactionId == -1) {
            // Add new transaction
            Transaction newTransaction = new Transaction(amount, description, category, type, date);
            db.addTransaction(newTransaction);
            // Update transaction count
            UserPreferences prefs = db.getUserPreferences();
            prefs.setTransactionCount(prefs.getTransactionCount() + 1);
            db.saveUserPreferences(prefs);

            Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show();

            // Check if it's a new expense for a category without a budget
            if (type.equals("expense")) {
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                String monthYear = monthYearFormat.format(selectedDate.getTime());
                Budget existingBudget = db.getBudget(category, monthYear);
                if (existingBudget == null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("PROMPT_BUDGET_FOR_CATEGORY", category);
                    setResult(RESULT_OK, resultIntent);
                }
            }
        } else {
            // Update existing transaction
            Transaction updatedTransaction = new Transaction(transactionId, amount, description, category, type, date);
            db.updateTransaction(updatedTransaction);
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show();
        }

        finish(); // Go back to the previous activity
    }
} 