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
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.TransactionAdapter;
import com.moneybag.nativeapp.data.TransactionWithAccount;
import com.moneybag.nativeapp.databinding.FragmentDayTransactionsBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayTransactionsFragment extends Fragment {

    private static final String ARG_TIMESTAMP = "timestamp";
    private FragmentDayTransactionsBinding binding;
    private long timestamp;
    private TransactionAdapter adapter;
    private MoneyBagRepository repository;

    public static DayTransactionsFragment newInstance(long timestamp) {
        DayTransactionsFragment fragment = new DayTransactionsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TIMESTAMP, timestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            timestamp = getArguments().getLong(ARG_TIMESTAMP);
        }
        repository = new MoneyBagRepository(requireActivity().getApplication());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDayTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TransactionAdapter();
        binding.transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionRecyclerView.setAdapter(adapter);

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        binding.textDate.setText(sdf.format(new Date(timestamp)));

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddTransactionActivity.class);
            intent.putExtra("default_timestamp", timestamp);
            startActivity(intent);
        });

        loadTransactions();
    }

    private void loadTransactions() {
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(timestamp);
        int day = selectedCal.get(Calendar.DAY_OF_YEAR);
        int year = selectedCal.get(Calendar.YEAR);

        repository.getAllTransactions(transactions -> {
            List<TransactionWithAccount> filtered = new ArrayList<>();
            for (TransactionWithAccount t : transactions) {
                Calendar tCal = Calendar.getInstance();
                tCal.setTimeInMillis(t.transaction.timestamp);
                if (tCal.get(Calendar.DAY_OF_YEAR) == day && tCal.get(Calendar.YEAR) == year) {
                    filtered.add(t);
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setTransactions(filtered));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
