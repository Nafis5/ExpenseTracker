package com.soltralabs.expensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryBudget> categoryBudgets = new ArrayList<>();
    private final Context context;
    private OnCategoryClickListener onCategoryClickListener;


    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryBudget categoryBudget);
    }

    public CategoryAdapter(Context context, List<CategoryBudget> categoryBudgets, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryBudgets = categoryBudgets;
        this.onCategoryClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryBudget categoryBudget = categoryBudgets.get(position);
        holder.bind(categoryBudget, onCategoryClickListener);
    }

    @Override
    public int getItemCount() {
        return categoryBudgets.size();
    }
    
    public void setData(List<CategoryBudget> newCategoryBudgets) {
        this.categoryBudgets.clear();
        this.categoryBudgets.addAll(newCategoryBudgets);
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon, editBudgetIcon;
        TextView categoryName, categorySpending;
        ProgressBar categoryProgressBar;

        CategoryViewHolder(View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            editBudgetIcon = itemView.findViewById(R.id.edit_budget_icon);
            categoryName = itemView.findViewById(R.id.category_name);
            categorySpending = itemView.findViewById(R.id.category_spending);
            categoryProgressBar = itemView.findViewById(R.id.category_progress_bar);
        }

        void bind(final CategoryBudget categoryBudget, final OnCategoryClickListener listener) {
            categoryName.setText(categoryBudget.getCategory());
            String spendingText = String.format(Locale.getDefault(), "$%.2f / $%.2f",
                    categoryBudget.getSpentAmount(), categoryBudget.getBudgetLimit());
            categorySpending.setText(spendingText);

            int progress = 0;
            if (categoryBudget.getBudgetLimit() > 0) {
                progress = (int) ((categoryBudget.getSpentAmount() / categoryBudget.getBudgetLimit()) * 100);
            }

            // For the visual bar, cap at 100.
            categoryProgressBar.setProgress(Math.min(progress, 100));
            
            // Set progress bar color based on the actual (un-capped) percentage
            android.graphics.drawable.Drawable progressDrawable = categoryProgressBar.getProgressDrawable();
            if (progressDrawable != null) {
                if (progress > 90) {
                    progressDrawable.setColorFilter(context.getResources().getColor(R.color.progress_red), android.graphics.PorterDuff.Mode.SRC_IN);
                } else if (progress > 70) {
                    progressDrawable.setColorFilter(context.getResources().getColor(R.color.progress_yellow), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    progressDrawable.setColorFilter(context.getResources().getColor(R.color.progress_green), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            // Set category icon
            switch (categoryBudget.getCategory().toLowerCase()) {
                case "food":
                    categoryIcon.setImageResource(R.drawable.ic_food);
                    break;
                case "transport":
                    categoryIcon.setImageResource(R.drawable.ic_transport);
                    break;
                case "bills":
                    categoryIcon.setImageResource(R.drawable.ic_bills);
                    break;
                case "other":
                    categoryIcon.setImageResource(R.drawable.ic_other);
                    break;
            }

            editBudgetIcon.setOnClickListener(v -> listener.onCategoryClick(categoryBudget));
        }
    }

    public static class CategoryBudget {
        private String category;
        private double spentAmount;
        private double budgetLimit;

        public CategoryBudget(String category, double spentAmount, double budgetLimit) {
            this.category = category;
            this.spentAmount = spentAmount;
            this.budgetLimit = budgetLimit;
        }

        public String getCategory() {
            return category;
        }

        public double getSpentAmount() {
            return spentAmount;
        }

        public double getBudgetLimit() {
            return budgetLimit;
        }
    }
} 