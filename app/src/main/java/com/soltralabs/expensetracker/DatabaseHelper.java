package com.soltralabs.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expensetracker.db";
    private static final int DATABASE_VERSION = 4;

    // Table Names
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_BUDGETS = "budgets";
    public static final String TABLE_USER_PREFERENCES = "user_preferences";
    public static final String TABLE_CATEGORIES = "categories";

    // Common column names
    public static final String KEY_ID = "id";

    // TRANSACTIONS Table - column names
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATE = "date";
    public static final String KEY_CREATED_AT = "created_at";

    // CATEGORIES Table - column names
    public static final String KEY_CATEGORY_NAME = "category_name";
    public static final String KEY_IS_DEFAULT = "is_default"; // 1 for default, 0 for custom

    // BUDGETS Table - column names
    public static final String KEY_MONTHLY_LIMIT = "monthly_limit";
    public static final String KEY_MONTH_YEAR = "month_year";

    // USER_PREFERENCES Table - column names
    public static final String KEY_IS_PREMIUM = "is_premium";
    public static final String KEY_MONTHLY_INCOME = "monthly_income";
    public static final String KEY_TRANSACTION_COUNT = "transaction_count";


    // Table Create Statements
    private static final String CREATE_TABLE_TRANSACTIONS = "CREATE TABLE "
            + TABLE_TRANSACTIONS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_AMOUNT + " REAL NOT NULL," + KEY_DESCRIPTION + " TEXT,"
            + KEY_CATEGORY + " TEXT NOT NULL," + KEY_TYPE + " TEXT NOT NULL,"
            + KEY_DATE + " TEXT NOT NULL,"
            + KEY_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" + ")";

    private static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE "
            + TABLE_CATEGORIES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_CATEGORY_NAME + " TEXT UNIQUE NOT NULL,"
            + KEY_IS_DEFAULT + " INTEGER DEFAULT 0" + ")";

    private static final String CREATE_TABLE_BUDGETS = "CREATE TABLE "
            + TABLE_BUDGETS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_CATEGORY + " TEXT NOT NULL," + KEY_MONTHLY_LIMIT + " REAL NOT NULL,"
            + KEY_MONTH_YEAR + " TEXT NOT NULL,"
            + "UNIQUE (" + KEY_CATEGORY + ", " + KEY_MONTH_YEAR + "))";

    private static final String CREATE_TABLE_USER_PREFERENCES = "CREATE TABLE "
            + TABLE_USER_PREFERENCES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_IS_PREMIUM + " INTEGER DEFAULT 0,"
            + KEY_MONTHLY_INCOME + " REAL DEFAULT 0,"
            + KEY_TRANSACTION_COUNT + " INTEGER DEFAULT 0" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_BUDGETS);
        db.execSQL(CREATE_TABLE_USER_PREFERENCES);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        addInitialCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PREFERENCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    private void addInitialCategories(SQLiteDatabase db) {
        String[] defaultCategories = {"Food", "Transport", "Bills", "Housing", "Savings", "Debt Payments", "Health", "Other"};
        for (String category : defaultCategories) {
            ContentValues values = new ContentValues();
            values.put(KEY_CATEGORY_NAME, category);
            values.put(KEY_IS_DEFAULT, 1);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    // Adding new transaction
    public long addTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_AMOUNT, transaction.getAmount());
        values.put(KEY_DESCRIPTION, transaction.getDescription());
        values.put(KEY_CATEGORY, transaction.getCategory());
        values.put(KEY_TYPE, transaction.getType());
        values.put(KEY_DATE, transaction.getDate());
        // insert row
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    // Getting single transaction
    public Transaction getTransaction(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, new String[]{KEY_ID,
                        KEY_AMOUNT, KEY_DESCRIPTION, KEY_CATEGORY, KEY_TYPE, KEY_DATE, KEY_CREATED_AT}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Transaction transaction = null;
        if (cursor != null && cursor.moveToFirst()) {
            transaction = new Transaction(
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE))
            );
            cursor.close();
        }
        
        db.close();
        return transaction;
    }

    // Getting all transactions for a specific month
    public java.util.List<Transaction> getTransactionsForMonth(String monthYear) { // "YYYY-MM"
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_DATE + " LIKE '" + monthYear + "%'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
                transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
                transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
                transaction.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));
                transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
                transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    // Getting all transactions for a specific date
    public java.util.List<Transaction> getTransactionsForDate(String date) { // "YYYY-MM-DD"
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_DATE + " = '" + date + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
                transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
                transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
                transaction.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));
                transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
                transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

     // Getting recent transactions
    public java.util.List<Transaction> getRecentTransactions(int limit) {
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + KEY_DATE + " DESC LIMIT " + limit;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction();
                transaction.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
                transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_AMOUNT)));
                transaction.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
                transaction.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)));
                transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
                transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
                transactions.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }


    // Updating single transaction
    public int updateTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_AMOUNT, transaction.getAmount());
        values.put(KEY_DESCRIPTION, transaction.getDescription());
        values.put(KEY_CATEGORY, transaction.getCategory());
        values.put(KEY_TYPE, transaction.getType());
        values.put(KEY_DATE, transaction.getDate());

        return db.update(TABLE_TRANSACTIONS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(transaction.getId())});
    }

    // Deleting single transaction
    public void deleteTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Getting transactions count
    public int getTransactionsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_TRANSACTIONS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // Category methods
    public long addCategory(String categoryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY_NAME, categoryName);
        values.put(KEY_IS_DEFAULT, 0); // Custom category
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public void deleteCategory(String categoryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, KEY_CATEGORY_NAME + " = ? AND " + KEY_IS_DEFAULT + " = 0", new String[]{categoryName});
        db.close();
    }

    public List<String> getAllCategoryNames() {
        List<String> categories = new ArrayList<>();
        String selectQuery = "SELECT " + KEY_CATEGORY_NAME + " FROM " + TABLE_CATEGORIES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY_NAME)));
                category.setDefault(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_DEFAULT)) == 1);
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Budget methods
    public long addBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY, budget.getCategory());
        values.put(KEY_MONTHLY_LIMIT, budget.getMonthlyLimit());
        values.put(KEY_MONTH_YEAR, budget.getMonthYear());

        long id = db.insert(TABLE_BUDGETS, null, values);
        db.close();
        return id;
    }

    public Budget getBudget(String category, String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGETS, new String[]{KEY_ID, KEY_CATEGORY, KEY_MONTHLY_LIMIT, KEY_MONTH_YEAR},
                KEY_CATEGORY + "=? AND " + KEY_MONTH_YEAR + "=?",
                new String[]{category, monthYear}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Budget budget = new Budget(
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_MONTHLY_LIMIT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_MONTH_YEAR))
            );
            cursor.close();
            db.close();
            return budget;
        }
        if(cursor != null) {
            cursor.close();
        }
        db.close();
        return null;
    }

    public int updateBudget(Budget budget) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MONTHLY_LIMIT, budget.getMonthlyLimit());

        return db.update(TABLE_BUDGETS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(budget.getId())});
    }

    // User Preferences methods
    public void saveUserPreferences(UserPreferences prefs) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IS_PREMIUM, prefs.isPremium() ? 1 : 0);
        values.put(KEY_MONTHLY_INCOME, prefs.getMonthlyIncome());
        values.put(KEY_TRANSACTION_COUNT, prefs.getTransactionCount());

        // Check if preferences exist
        Cursor cursor = db.query(TABLE_USER_PREFERENCES, new String[]{KEY_ID}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            // Update existing preferences
            db.update(TABLE_USER_PREFERENCES, values, KEY_ID + " = ?", new String[]{String.valueOf(cursor.getInt(0))});
        } else {
            // Insert new preferences
            db.insert(TABLE_USER_PREFERENCES, null, values);
        }
        if(cursor != null){
            cursor.close();
        }
        db.close();
    }

    public UserPreferences getUserPreferences() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER_PREFERENCES, new String[]{KEY_ID, KEY_IS_PREMIUM, KEY_MONTHLY_INCOME, KEY_TRANSACTION_COUNT},
                null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            UserPreferences prefs = new UserPreferences(
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_PREMIUM)) == 1,
                    cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_MONTHLY_INCOME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRANSACTION_COUNT))
            );
            cursor.close();
            db.close();
            return prefs;
        }
         if(cursor != null) {
            cursor.close();
        }
        db.close();
        // Return default preferences if none found
        return new UserPreferences(false, 0, 0);
    }
} 