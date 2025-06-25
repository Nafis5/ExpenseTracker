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
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private final Context context;
    private OnTransactionClickListener onTransactionClickListener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, OnTransactionClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.onTransactionClickListener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction, onTransactionClickListener);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }
    
    public void updateData(List<Transaction> newTransactionList) {
        this.transactionList.clear();
        this.transactionList.addAll(newTransactionList);
        notifyDataSetChanged();
    }
    
    public Transaction getTransactionAt(int position) {
        return transactionList.get(position);
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView transactionCategory, transactionDescription, transactionAmount, transactionDate;

        TransactionViewHolder(View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            transactionCategory = itemView.findViewById(R.id.transaction_category);
            transactionDescription = itemView.findViewById(R.id.transaction_description);
            transactionAmount = itemView.findViewById(R.id.transaction_amount);
            transactionDate = itemView.findViewById(R.id.transaction_date);
        }

        void bind(final Transaction transaction, final OnTransactionClickListener listener) {
            transactionDate.setText(transaction.getDate());
            transactionAmount.setText(String.format(Locale.getDefault(), "$%.2f", transaction.getAmount()));

            if ("income".equals(transaction.getType())) {
                categoryIcon.setImageResource(R.drawable.ic_income);
                transactionDescription.setText(transaction.getDescription()); // Source
                transactionCategory.setVisibility(View.GONE);
                transactionAmount.setTextColor(context.getResources().getColor(R.color.incomeColor));
            } else { // Expense
                transactionCategory.setVisibility(View.VISIBLE);
                transactionDescription.setText(transaction.getDescription());
                transactionCategory.setText(String.format("Category: %s", transaction.getCategory()));
                transactionAmount.setTextColor(context.getResources().getColor(R.color.expenseColor));
                
                // Set category icon
                switch (transaction.getCategory().toLowerCase()) {
                    case "food":
                        categoryIcon.setImageResource(R.drawable.ic_food);
                        break;
                    case "transport":
                        categoryIcon.setImageResource(R.drawable.ic_transport);
                        break;
                    case "bills":
                        categoryIcon.setImageResource(R.drawable.ic_bills);
                        break;
                    default:
                        categoryIcon.setImageResource(R.drawable.ic_other);
                        break;
                }
            }
            
            itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
        }
    }
} 