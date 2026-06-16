package com.moneybag.nativeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.moneybag.nativeapp.data.Account;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.TransactionAdapter;
import com.moneybag.nativeapp.databinding.FragmentAccountDetailBinding;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.moneybag.nativeapp.databinding.DialogEditAccountBinding;

public class AccountDetailFragment extends Fragment {

    private static final String ARG_ACCOUNT_ID = "account_id";
    private FragmentAccountDetailBinding binding;
    private MoneyBagRepository repository;
    private TransactionAdapter adapter;
    private int accountId;
    private Account currentAccount;
    private String selectedIconUri = null;
    public DialogEditAccountBinding currentDialogBinding;

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

    public static AccountDetailFragment newInstance(int accountId) {
        AccountDetailFragment fragment = new AccountDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_ID, accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountId = getArguments().getInt(ARG_ACCOUNT_ID);
        }
        repository = new MoneyBagRepository(requireActivity().getApplication());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TransactionAdapter();
        binding.transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionRecyclerView.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                if (currentAccount != null) {
                    showEditAccountDialog();
                }
                return true;
            }
            return false;
        });

        binding.fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddTransactionActivity.class);
            intent.putExtra("default_account_id", accountId);
            startActivity(intent);
        });

        loadAccountData();
    }

    private void loadAccountData() {
        repository.getAllAccounts(accounts -> {
            for (Account account : accounts) {
                if (account.id == accountId) {
                    currentAccount = account;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            binding.textAccountName.setText(account.name);
                            binding.textAccountBalance.setText(String.format(Locale.getDefault(), "৳%.2f", account.balance));
                            binding.textAccountType.setText(account.type.name());
                        });
                    }
                    break;
                }
            }
        });

        repository.getAllTransactions(transactions -> {
            if (getActivity() != null) {
                java.util.List<com.moneybag.nativeapp.data.TransactionWithAccount> filtered = new java.util.ArrayList<>();
                for (com.moneybag.nativeapp.data.TransactionWithAccount t : transactions) {
                    if (t.transaction.accountId == accountId || (t.transaction.toAccountId != null && t.transaction.toAccountId == accountId)) {
                        filtered.add(t);
                    }
                }
                getActivity().runOnUiThread(() -> adapter.setTransactions(filtered));
            }
        });
    }

    private void showEditAccountDialog() {
        AccountsFragment.showAccountDialog(currentAccount, this, repository, this::loadAccountData, iconPickerLauncher, new AccountsFragment.DialogState() {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
