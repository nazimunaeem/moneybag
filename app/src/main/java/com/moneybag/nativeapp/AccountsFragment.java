package com.moneybag.nativeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.moneybag.nativeapp.data.Account;
import com.moneybag.nativeapp.data.AccountAdapter;
import com.moneybag.nativeapp.data.AccountType;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.databinding.DialogEditAccountBinding;
import com.moneybag.nativeapp.databinding.FragmentAccountsBinding;
import com.moneybag.nativeapp.data.Transaction;
import com.moneybag.nativeapp.data.TransactionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class AccountsFragment extends Fragment {
    private FragmentAccountsBinding binding;
    private AccountAdapter adapter;
    private MoneyBagRepository repository;
    private String selectedIconUri = null;
    private DialogEditAccountBinding currentDialogBinding;

    private final ActivityResultLauncher<String> iconPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedIconUri = uri.toString();
                    if (currentDialogBinding != null) {
                        com.bumptech.glide.Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(currentDialogBinding.imageAccountIcon);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());
        
        adapter = new AccountAdapter();
        adapter.setOnAccountClickListener(account -> {
            showAccountDialog(account);
        });
        binding.accountRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.accountRecyclerView.setAdapter(adapter);

        binding.fabAddAccount.setOnClickListener(v -> showAccountDialog(null));

        loadAccounts();
    }

    private void loadAccounts() {
        repository.getAllAccounts(accounts -> {
            if (getActivity() != null) {
                double assets = 0;
                double liabilities = 0;
                for (Account account : accounts) {
                    if (account.type == AccountType.LOAN || account.type == AccountType.CREDIT_CARD) {
                        liabilities += account.balance;
                    } else {
                        assets += account.balance;
                    }
                }
                final double finalAssets = assets;
                final double finalLiabilities = liabilities;

                getActivity().runOnUiThread(() -> {
                    adapter.setAccounts(accounts);
                    binding.headerAssets.setText(String.format(java.util.Locale.getDefault(), "৳%.2f", finalAssets));
                    binding.headerLiabilities.setText(String.format(java.util.Locale.getDefault(), "৳%.2f", finalLiabilities));
                    binding.headerTotalAccount.setText(String.format(java.util.Locale.getDefault(), "৳%.2f", finalAssets - finalLiabilities));
                });
            }
        });
    }

    public void showAccountDialog(@Nullable Account account) {
        showAccountDialog(account, this, repository, this::loadAccounts, iconPickerLauncher, new DialogState() {
            @Override
            public void onIconPicked(String uri) {
                selectedIconUri = uri;
            }

            @Override
            public String getSelectedIconUri() {
                return selectedIconUri;
            }
        });
    }

    public static void showAccountDialog(
            @Nullable Account account,
            Fragment fragment,
            MoneyBagRepository repository,
            Runnable onUpdate,
            ActivityResultLauncher<String> iconPickerLauncher,
            DialogState state
    ) {
        DialogEditAccountBinding dialogBinding = DialogEditAccountBinding.inflate(fragment.getLayoutInflater());
        String initialIconUri = (account != null) ? account.iconUri : null;
        state.onIconPicked(initialIconUri);

        if (initialIconUri != null) {
            com.bumptech.glide.Glide.with(fragment)
                    .load(android.net.Uri.parse(initialIconUri))
                    .circleCrop()
                    .into(dialogBinding.imageAccountIcon);
        }

        dialogBinding.btnSelectIcon.setOnClickListener(v -> {
            // We need to tell the fragment WHICH dialog binding to update when the result comes back.
            // This is tricky with static methods. 
            // For now, let's assume the fragment handles the UI update in its launcher callback.
            if (fragment instanceof AccountsFragment) {
                ((AccountsFragment) fragment).currentDialogBinding = dialogBinding;
            } else if (fragment instanceof AccountDetailFragment) {
                ((AccountDetailFragment) fragment).currentDialogBinding = dialogBinding;
            }
            iconPickerLauncher.launch("image/*");
        });

        // Setup Spinner
        java.util.List<String> types = new java.util.ArrayList<>();
        for (AccountType type : AccountType.values()) {
            types.add(type.name());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(fragment.requireContext(), android.R.layout.simple_spinner_item, types);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerAccountType.setAdapter(spinnerAdapter);

        // Setup Currency Spinner
        java.util.List<String> currencies = Arrays.asList("BDT", "USD");
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(fragment.requireContext(), android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerCurrency.setAdapter(currencyAdapter);

        if (account != null) {
            dialogBinding.dialogTitle.setText(fragment.getString(R.string.title_account_details));
            dialogBinding.editAccountName.setText(account.name);
            dialogBinding.editAccountBalance.setText(String.valueOf(account.balance));
            dialogBinding.spinnerAccountType.setSelection(types.indexOf(account.type.name()));
            dialogBinding.spinnerCurrency.setSelection(currencies.indexOf(account.currency));
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
                .setView(dialogBinding.getRoot())
                .setPositiveButton(account == null ? fragment.getString(R.string.add) : fragment.getString(R.string.update), (dialog, which) -> {
                    String name = dialogBinding.editAccountName.getText().toString();
                    String balanceStr = dialogBinding.editAccountBalance.getText().toString();
                    AccountType type = AccountType.valueOf(dialogBinding.spinnerAccountType.getSelectedItem().toString());
                    String currency = dialogBinding.spinnerCurrency.getSelectedItem().toString();

                    if (!name.isEmpty() && !balanceStr.isEmpty()) {
                        double newBalance = Double.parseDouble(balanceStr);
                        if (account == null) {
                            Account newAcc = new Account(name, newBalance, type, currency);
                            newAcc.iconUri = state.getSelectedIconUri();
                            repository.insertAccount(newAcc, onUpdate);
                        } else {
                            if (Math.abs(newBalance - account.balance) > 0.001) {
                                promptAdjustment(fragment.requireContext(), account, newBalance, name, type, currency, state.getSelectedIconUri(), onUpdate, repository);
                            } else {
                                account.name = name;
                                account.type = type;
                                account.currency = currency;
                                account.iconUri = state.getSelectedIconUri();
                                repository.updateAccount(account, onUpdate);
                            }
                        }
                    } else {
                        Toast.makeText(fragment.requireContext(), fragment.getString(R.string.error_fill_fields), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(fragment.getString(R.string.cancel), null);

        if (account != null) {
            builder.setNeutralButton(fragment.getString(R.string.delete), (dialog, which) -> {
                new androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
                        .setTitle(fragment.getString(R.string.delete))
                        .setMessage(fragment.getString(R.string.confirm_delete_account))
                        .setPositiveButton(fragment.getString(R.string.delete), (d, w) -> {
                            repository.deleteAccount(account, onUpdate);
                        })
                        .setNegativeButton(fragment.getString(R.string.cancel), null)
                        .show();
            });
        }
        builder.show();
    }

    public interface DialogState {
        void onIconPicked(String uri);
        String getSelectedIconUri();
    }

    private static void promptAdjustment(android.content.Context context, Account account, double newBalance, String name, AccountType type, String currency, String iconUri, Runnable onUpdate, MoneyBagRepository repository) {
        double diff = newBalance - account.balance;
        String action = diff > 0 ? context.getString(R.string.label_income) : context.getString(R.string.label_expense);
        
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.modify_balance_title))
                .setMessage(context.getString(R.string.msg_add_adjustment_transaction, action))
                .setPositiveButton(context.getString(R.string.label_yes), (dialog, which) -> {
                    Transaction adjustment = new Transaction(
                            "Balance Adjustment",
                            Math.abs(diff),
                            "Adjustment",
                            System.currentTimeMillis(),
                            diff > 0 ? TransactionType.INCOME : TransactionType.EXPENSE,
                            account.id,
                            currency
                    );
                    adjustment.note = "Manual balance adjustment";
                    
                    account.name = name;
                    account.type = type;
                    account.currency = currency;
                    account.iconUri = iconUri;
                    repository.updateAccount(account, null);
                    
                    repository.insertTransaction(adjustment, onUpdate);
                })
                .setNegativeButton(context.getString(R.string.label_no_update_only), (dialog, which) -> {
                    account.name = name;
                    account.balance = newBalance;
                    account.type = type;
                    account.currency = currency;
                    account.iconUri = iconUri;
                    repository.updateAccount(account, onUpdate);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
