package com.soltralabs.expensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryManagementAdapter extends RecyclerView.Adapter<CategoryManagementAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Category category);
    }

    public CategoryManagementAdapter(List<Category> categoryList, OnDeleteClickListener listener) {
        this.categoryList = categoryList;
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category, onDeleteClickListener);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void setData(List<Category> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;
        ImageView deleteCategoryIcon;

        CategoryViewHolder(View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.category_name_textview);
            deleteCategoryIcon = itemView.findViewById(R.id.delete_category_icon);
        }

        void bind(final Category category, final OnDeleteClickListener listener) {
            categoryNameTextView.setText(category.getName());
            if (category.isDefault()) {
                deleteCategoryIcon.setVisibility(View.GONE);
            } else {
                deleteCategoryIcon.setVisibility(View.VISIBLE);
                deleteCategoryIcon.setOnClickListener(v -> listener.onDeleteClick(category));
            }
        }
    }
} 