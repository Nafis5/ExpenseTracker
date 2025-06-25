# Expense Tracker Android App

This is a complete Android budgeting app developed in Android Studio using Java and XML. It allows users to track their income and expenses, set monthly budgets for different categories, and view their spending habits over time.

## Features

- **Dashboard:** A main screen that provides a summary of the current month's income vs. expenses, category-wise budget progress, and a list of recent transactions.
- **Add Transactions:** Users can easily add new income or expense transactions with details like amount, category, description, and date.
- **Transaction List:** A comprehensive list of all transactions for a selected month, with the ability to edit or delete individual entries.
- **Budgeting:** Users can set monthly spending limits for predefined categories (Food, Transport, Bills, Other).
- **Premium Tier:** The app includes a simulated premium tier with the following differences:
    - **Free Users:** Can add up to 50 transactions per month, can only view the current month's data, and see banner ads.
    - **Premium Users:** Have no transaction limits, can view historical data from previous months, and have an ad-free experience.
- **User-Friendly UI:** The app is built with Material Design components for a clean and intuitive user experience.

## Technical Specifications

- **Language:** Java
- **UI:** XML Layouts
- **Database:** SQLite for local data storage.
- **Architecture:** The app follows a basic MVP/MVVM pattern.
- **Ads:** Google AdMob is integrated for banner ads.
- **Target SDK:** 33 (Android 13)
- **Minimum SDK:** 28 (Android 9.0)

## How to Build and Run

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies.
4.  Run the app on an Android emulator or a physical device.

## Code Structure

-   `com.soltralabs.expensetracker`: Main package
    -   `activities`: Contains `MainActivity`, `AddTransactionActivity`, `TransactionListActivity`.
    -   `adapters`: Contains `CategoryAdapter` and `TransactionAdapter` for the RecyclerViews.
    -   `database`: `DatabaseHelper.java` for all SQLite operations.
    -   `dialogs`: `BudgetSettingDialog.java` for setting category budgets.
    -   `managers`: `PremiumManager.java` and `AdManager.java` for handling premium features and ads.
    -   `models`: `Transaction.java`, `Budget.java`, and `UserPreferences.java` data models.
    -   `res/layout`: All XML layout files for activities and list items.
    -   `res/drawable`: Vector drawables for icons.
    -   `res/values`: String, color, and theme resources.

This project serves as a comprehensive example of building a full-featured Android application with common functionalities like database management, RecyclerView implementation, and third-party library integration. 