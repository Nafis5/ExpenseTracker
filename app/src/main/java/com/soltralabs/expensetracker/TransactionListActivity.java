package com.soltralabs.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionListActivity extends AppCompatActivity {

    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private DatabaseHelper db;
    private PremiumManager premiumManager;
    private Spinner monthSpinner;
    private ImageView calendarIcon;
    private TextView filterInfoTextView;

    private String currentMonthYear;
    private String selectedDate;

    private enum FilterMode {
        MONTH,
        DATE
    }

    private FilterMode currentFilterMode = FilterMode.MONTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        db = new DatabaseHelper(this);
        premiumManager = PremiumManager.getInstance(this);

        initViews();
        setupMonthSpinner();
        setupRecyclerView();
        setupSwipeToDelete();
        setupCalendarIcon();

        // Initial load for the current month.
        if (currentMonthYear != null) {
            loadTransactionsForMonth(currentMonthYear);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the data based on the last selected filter
        if (currentFilterMode == FilterMode.DATE && selectedDate != null) {
            loadTransactionsForDate(selectedDate);
        } else if (currentMonthYear != null) {
            loadTransactionsForMonth(currentMonthYear);
        }
    }

    private void initViews() {
        transactionsRecyclerView = findViewById(R.id.transactions_recyclerview);
        monthSpinner = findViewById(R.id.month_filter_spinner);
        calendarIcon = findViewById(R.id.calendar_icon);
        filterInfoTextView = findViewById(R.id.filter_info_textview);
    }

    private void setupCalendarIcon() {
        calendarIcon.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(year1, monthOfYear, dayOfMonth);
            
            if (!premiumManager.canViewHistoricalData() && !isDateInCurrentMonth(selectedCal)) {
                showUpgradeDialog();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(selectedCal.getTime());
            currentFilterMode = FilterMode.DATE;
            loadTransactionsForDate(selectedDate);

        }, year, month, day);
        datePickerDialog.show();
    }

    private boolean isDateInCurrentMonth(Calendar date) {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) && now.get(Calendar.MONTH) == date.get(Calendar.MONTH);
    }

    private void setupMonthSpinner() {
        ArrayList<String> months = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        // Set current month first
        currentMonthYear = sdf.format(cal.getTime());
        months.add(currentMonthYear);

        if (premiumManager.canViewHistoricalData()) {
            for (int i = 0; i < 11; i++) {
                cal.add(Calendar.MONTH, -1);
                months.add(sdf.format(cal.getTime()));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(adapter);

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonth = (String) parent.getItemAtPosition(position);
                // The first item is always the current month and allowed.
                if (position > 0 && !premiumManager.canViewHistoricalData()) {
                    showUpgradeDialog();
                    monthSpinner.setSelection(0);
                } else {
                    currentMonthYear = selectedMonth;
                    currentFilterMode = FilterMode.MONTH;
                    loadTransactionsForMonth(currentMonthYear);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList, transaction -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("TRANSACTION_ID", transaction.getId());
            startActivity(intent);
        });
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void loadTransactionsForMonth(String monthYear) {
        transactionList.clear();
        transactionList.addAll(db.getTransactionsForMonth(monthYear));
        transactionAdapter.notifyDataSetChanged();
        filterInfoTextView.setText("Showing transactions for " + monthYear);
    }

    private void loadTransactionsForDate(String date) {
        transactionList.clear();
        transactionList.addAll(db.getTransactionsForDate(date));
        transactionAdapter.notifyDataSetChanged();
        filterInfoTextView.setText("Showing transactions for " + date);
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Transaction transactionToDelete = transactionAdapter.getTransactionAt(position);
                db.deleteTransaction(transactionToDelete.getId());
                transactionList.remove(position);
                transactionAdapter.notifyItemRemoved(position);
                Toast.makeText(TransactionListActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(transactionsRecyclerView);
    }

    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.upgrade_prompt_title)
                .setMessage(R.string.upgrade_prompt_message_history)
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    Toast.makeText(this, "Upgrade feature not implemented.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 