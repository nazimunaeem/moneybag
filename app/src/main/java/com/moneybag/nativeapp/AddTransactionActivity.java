package com.moneybag.nativeapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.moneybag.nativeapp.data.Account;
import com.moneybag.nativeapp.data.Category;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.SimpleGridAdapter;
import com.moneybag.nativeapp.data.Transaction;
import com.moneybag.nativeapp.data.TransactionType;
import com.moneybag.nativeapp.databinding.ActivityAddTransactionBinding;
import com.moneybag.nativeapp.databinding.LayoutGridPickerBinding;
import com.moneybag.nativeapp.databinding.LayoutT9KeyboardBinding;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    public static final String ARG_TRANSACTION_ID = "transaction_id";
    private ActivityAddTransactionBinding binding;
    private Calendar calendar = Calendar.getInstance();
    // FIX #1: 12-hour clock format (hh = 12h, HH = 24h)
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
    private MoneyBagRepository repository;
    private TransactionType currentType = TransactionType.EXPENSE;

    private int selectedAccountId = -1;
    private int selectedToAccountId = -1;
    private String selectedCategory = "General";

    private String expression = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new MoneyBagRepository(getApplication());

        setupUI();
        // FIX #2: Ensure T9 keyboard is visible by posting to UI thread after layout
        binding.getRoot().post(() -> showT9Keyboard());
    }

    private void setupUI() {
        binding.typeToggleGroup.check(R.id.btnExpense);
        binding.typeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedCategory = "";
                binding.categoryInput.setText("");
                if (checkedId == R.id.btnIncome) {
                    currentType = TransactionType.INCOME;
                    binding.toAccountInputLayout.setVisibility(View.GONE);
                    binding.categoryInputLayout.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnExpense) {
                    currentType = TransactionType.EXPENSE;
                    binding.toAccountInputLayout.setVisibility(View.GONE);
                    binding.categoryInputLayout.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnTransfer) {
                    currentType = TransactionType.TRANSFER;
                    binding.toAccountInputLayout.setVisibility(View.VISIBLE);
                    binding.categoryInputLayout.setVisibility(View.GONE);
                }
                View currentView = binding.bottomPanel.getChildAt(0);
                if (currentView != null && "category_picker".equals(currentView.getTag())) {
                    showCategoryPicker();
                }
            }
        });

        binding.dateInput.setOnClickListener(v -> showDateTimePicker());
        updateDateLabel();

        binding.amountInput.setOnClickListener(v -> showT9Keyboard());
        binding.accountInput.setOnClickListener(v -> showAccountPicker(false));
        binding.toAccountInput.setOnClickListener(v -> showAccountPicker(true));
        binding.categoryInput.setOnClickListener(v -> showCategoryPicker());

        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> saveTransaction(true));
        binding.btnContinue.setOnClickListener(v -> saveTransaction(false));
    }

    private void showT9Keyboard() {
        LayoutT9KeyboardBinding kb = LayoutT9KeyboardBinding.inflate(getLayoutInflater(), binding.bottomPanel, false);
        binding.bottomPanel.removeAllViews();
        binding.bottomPanel.addView(kb.getRoot());
        kb.getRoot().setTag("t9_keyboard");

        View.OnClickListener numListener = v -> {
            String text = ((android.widget.Button) v).getText().toString();
            if (expression.equals("0")) expression = text;
            else expression += text;
            binding.amountInput.setText(expression);
        };

        kb.btn1.setOnClickListener(numListener); kb.btn2.setOnClickListener(numListener); kb.btn3.setOnClickListener(numListener);
        kb.btn4.setOnClickListener(numListener); kb.btn5.setOnClickListener(numListener); kb.btn6.setOnClickListener(numListener);
        kb.btn7.setOnClickListener(numListener); kb.btn8.setOnClickListener(numListener); kb.btn9.setOnClickListener(numListener);
        kb.btn0.setOnClickListener(numListener); kb.btnDot.setOnClickListener(numListener);
        kb.btnMinus.setOnClickListener(numListener); kb.btnEqual.setOnClickListener(numListener);

        kb.btnDelete.setOnClickListener(v -> {
            if (expression.length() > 1) {
                expression = expression.substring(0, expression.length() - 1);
            } else {
                expression = "0";
            }
            binding.amountInput.setText(expression);
        });

        kb.btnDone.setOnClickListener(v -> binding.accountInput.performClick());
    }

    private void showAccountPicker(boolean isToAccount) {
        LayoutGridPickerBinding picker = LayoutGridPickerBinding.inflate(getLayoutInflater(), binding.bottomPanel, false);
        picker.pickerTitle.setText(isToAccount ? "Select Destination Account" : "Select Source Account");
        binding.bottomPanel.removeAllViews();
        binding.bottomPanel.addView(picker.getRoot());
        picker.getRoot().setTag("account_picker");

        SimpleGridAdapter<Account> adapter = new SimpleGridAdapter<>(
                account -> account.name,
                account -> {
                    if (isToAccount) {
                        selectedToAccountId = account.id;
                        binding.toAccountInput.setText(account.name);
                        showT9Keyboard();
                    } else {
                        selectedAccountId = account.id;
                        binding.accountInput.setText(account.name);
                        binding.amountLabel.setText("Amount (" + account.currency + ")");
                        if (currentType == TransactionType.TRANSFER) showAccountPicker(true);
                        else showCategoryPicker();
                    }
                }
        );
        picker.pickerRecyclerView.setAdapter(adapter);
        repository.getAllAccounts(adapter::setItems);
    }

    private void showCategoryPicker() {
        LayoutGridPickerBinding picker = LayoutGridPickerBinding.inflate(getLayoutInflater(), binding.bottomPanel, false);
        picker.pickerTitle.setText("Select Category");
        binding.bottomPanel.removeAllViews();
        binding.bottomPanel.addView(picker.getRoot());
        picker.getRoot().setTag("category_picker");

        SimpleGridAdapter<Category> adapter = new SimpleGridAdapter<>(
                category -> category.name,
                category -> {
                    selectedCategory = category.name;
                    binding.categoryInput.setText(category.name);
                    showT9Keyboard();
                }
        );
        picker.pickerRecyclerView.setAdapter(adapter);
        repository.getCategories(currentType, categories -> {
            if (categories.isEmpty()) {
                if (currentType == TransactionType.INCOME) {
                    repository.insertCategory(new Category("Salary 💰", TransactionType.INCOME), null);
                    repository.insertCategory(new Category("Bonus 🧧", TransactionType.INCOME), null);
                } else {
                    repository.insertCategory(new Category("Food 🍔", TransactionType.EXPENSE), null);
                    repository.insertCategory(new Category("Transport 🚖", TransactionType.EXPENSE), null);
                }
                repository.getCategories(currentType, adapter::setItems);
            } else {
                adapter.setItems(categories);
            }
        });
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            // FIX #1 continued: TimePickerDialog false = 12h mode
            new TimePickerDialog(this, (v, h, m) -> {
                calendar.set(Calendar.HOUR_OF_DAY, h);
                calendar.set(Calendar.MINUTE, m);
                updateDateLabel();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        binding.dateInput.setText(dateTimeFormat.format(calendar.getTime()));
    }

    private void saveTransaction(boolean finish) {
        if (expression.isEmpty() || expression.equals("0") || selectedAccountId == -1) {
            Toast.makeText(this, "Enter amount and select account", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(expression);
            Transaction transaction = new Transaction(selectedCategory, amount, selectedCategory,
                    calendar.getTimeInMillis(), currentType, selectedAccountId, "BDT");
            transaction.note = binding.noteInput.getText().toString();
            if (currentType == TransactionType.TRANSFER) transaction.toAccountId = selectedToAccountId;
            repository.insertTransaction(transaction, () -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    if (finish) finish();
                    else clearForm();
                });
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        expression = "0";
        binding.amountInput.setText("0");
        binding.noteInput.setText("");
        calendar = Calendar.getInstance();
        updateDateLabel();
        showT9Keyboard();
    }
}
