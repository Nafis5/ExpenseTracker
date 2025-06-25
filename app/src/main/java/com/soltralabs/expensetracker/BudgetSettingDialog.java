package com.soltralabs.expensetracker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class BudgetSettingDialog extends DialogFragment {

    private static final String ARG_CATEGORY = "ARG_CATEGORY";
    private static final String ARG_CURRENT_LIMIT = "ARG_CURRENT_LIMIT";

    private BudgetDialogListener listener;

    public interface BudgetDialogListener {
        void onBudgetSet(String category, double newLimit);
    }

    public static BudgetSettingDialog newInstance(String category, double currentLimit) {
        BudgetSettingDialog fragment = new BudgetSettingDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putDouble(ARG_CURRENT_LIMIT, currentLimit);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (BudgetDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement BudgetDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String category = "";
        double currentLimit = 0;

        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
            currentLimit = getArguments().getDouble(ARG_CURRENT_LIMIT);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_budget_setting, null);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        TextInputEditText budgetInput = view.findViewById(R.id.budget_input);

        dialogTitle.setText(getString(R.string.set_budget_for_category, category));
        if (currentLimit > 0) {
            budgetInput.setText(String.format(Locale.getDefault(), "%.2f", currentLimit));
        }

        String finalCategory = category;
        builder.setView(view)
                .setPositiveButton(R.string.save_button_text, (dialog, id) -> {
                    String budgetString = budgetInput.getText().toString();
                    double newLimit = 0;
                    if (!budgetString.isEmpty()) {
                        try {
                            newLimit = Double.parseDouble(budgetString);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getActivity(), "Invalid number format.", Toast.LENGTH_SHORT).show();
                            return; // Don't close dialog
                        }
                    }
                    listener.onBudgetSet(finalCategory, newLimit);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    if (getDialog() != null) {
                        getDialog().cancel();
                    }
                });
        return builder.create();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}

 