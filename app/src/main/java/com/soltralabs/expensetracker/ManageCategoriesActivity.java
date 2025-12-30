package com.soltralabs.expensetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;


public class ManageCategoriesActivity extends AppCompatActivity {

    private RecyclerView categoriesRecyclerView;
    private CategoryManagementAdapter adapter;
    private List<Category> categoryList;
    private DatabaseHelper db;
    private EditText newCategoryEditText;
    private Button addCategoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        db = new DatabaseHelper(this);
        
        initViews();
        setupRecyclerView();
        loadCategories();
        
        addCategoryButton.setOnClickListener(v -> addCategory());
    }

    private void initViews() {
        categoriesRecyclerView = findViewById(R.id.categories_recyclerview);
        newCategoryEditText = findViewById(R.id.new_category_edittext);
        addCategoryButton = findViewById(R.id.add_category_button);
    }

    private void setupRecyclerView() {
        categoryList = new ArrayList<>();
        adapter = new CategoryManagementAdapter(categoryList, category -> {
            db.deleteCategory(category.getName());
            loadCategories();
            Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
        });
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(adapter);
    }

    private void loadCategories() {
        categoryList.clear();
        categoryList.addAll(db.getAllCategories());
        adapter.notifyDataSetChanged();
    }

    private void addCategory() {
        String categoryName = newCategoryEditText.getText().toString().trim();
        if (TextUtils.isEmpty(categoryName)) {
            Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        db.addCategory(categoryName);
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("NEW_CATEGORY_NAME", categoryName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
} 