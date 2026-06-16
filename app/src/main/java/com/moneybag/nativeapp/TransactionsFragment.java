package com.moneybag.nativeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.TransactionAdapter;
import com.moneybag.nativeapp.data.TransactionType;
import com.moneybag.nativeapp.data.TransactionWithAccount;
import com.moneybag.nativeapp.data.Transaction;
import com.moneybag.nativeapp.databinding.FragmentTransactionsBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionAdapter adapter;
    private MoneyBagRepository repository;
    private final Calendar currentCalendar = Calendar.getInstance();
    private List<TransactionWithAccount> allTransactions = new ArrayList<>();
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());
        
        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(t -> {
            if (t.transaction != null && t.transaction.id != 0) {
                Intent intent = new Intent(getContext(), AddTransactionActivity.class);
                intent.putExtra(AddTransactionActivity.ARG_TRANSACTION_ID, t.transaction.id);
                startActivity(intent);
            }
        });
        binding.transactionList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionList.setAdapter(adapter);

        setupTabs();

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                SearchView searchView = (SearchView) item.getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            currentQuery = query; filterTransactions(); return true;
                        }
                        @Override
                        public boolean onQueryTextChange(String newText) {
                            currentQuery = newText; filterTransactions(); return true;
                        }
                    });
                }
                return true;
            }
            return false;
        });

        binding.fabAdd.setOnClickListener(v -> startActivity(new Intent(getContext(), AddTransactionActivity.class)));
        loadTransactions();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void setupTabs() {
        binding.transactionsTabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) { 
                if (tab.getPosition() == 1) {
                    binding.calendarView.setVisibility(View.VISIBLE);
                } else {
                    binding.calendarView.setVisibility(View.GONE);
                }
                filterTransactions(); 
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            currentCalendar.set(year, month, dayOfMonth);
            filterTransactions();
        });
    }

    private void loadTransactions() {
        repository.getAllTransactions(transactions -> {
            allTransactions = transactions;
            if (getActivity() != null) getActivity().runOnUiThread(this::filterTransactions);
        });
    }

    private void filterTransactions() {
        if (getActivity() == null) return;
        int selectedTab = binding.transactionsTabLayout.getSelectedTabPosition();
        List<TransactionWithAccount> filtered = new ArrayList<>();
        
        for (TransactionWithAccount t : allTransactions) {
            if (!currentQuery.isEmpty()) {
                String q = currentQuery.toLowerCase();
                boolean match = (t.transaction.category != null && t.transaction.category.toLowerCase().contains(q)) ||
                                (t.transaction.note != null && t.transaction.note.toLowerCase().contains(q)) ||
                                (t.account != null && t.account.name.toLowerCase().contains(q));
                if (!match) continue;
            }

            if (selectedTab == 0) filtered.add(t); // Daily shows all
            else if (selectedTab == 1) { // Calendar (selected day)
                Calendar tCal = Calendar.getInstance(); tCal.setTimeInMillis(t.transaction.timestamp);
                if (tCal.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR) && 
                    tCal.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) filtered.add(t);
            }
        }

        if (selectedTab == 2) showMonthlyTotals();
        else if (selectedTab == 3) showTotalBreakdown();
        else {
            adapter.setTransactions(filtered);
            updateHeaders(filtered);
        }
    }

    private void updateHeaders(List<TransactionWithAccount> list) {
        double inc = 0, exp = 0;
        for (TransactionWithAccount t : list) {
            if (t.transaction.type == TransactionType.INCOME) inc += t.transaction.amount;
            else if (t.transaction.type == TransactionType.EXPENSE) exp += t.transaction.amount;
        }
        binding.headerIncome.setText(String.format(Locale.getDefault(), "৳%.1f", inc));
        binding.headerExpense.setText(String.format(Locale.getDefault(), "৳%.1f", exp));
        binding.headerTotal.setText(String.format(Locale.getDefault(), "৳%.1f", inc - exp));
    }

    private void showMonthlyTotals() {
        Map<String, Double> monthlyTotals = new TreeMap<>(java.util.Collections.reverseOrder());
        java.text.SimpleDateFormat monthYearFormat = new java.text.SimpleDateFormat("MMM yyyy", Locale.getDefault());
        for (TransactionWithAccount t : allTransactions) {
            String key = monthYearFormat.format(new java.util.Date(t.transaction.timestamp));
            double amount = t.transaction.type == TransactionType.EXPENSE ? -t.transaction.amount : t.transaction.amount;
            monthlyTotals.put(key, monthlyTotals.getOrDefault(key, 0.0) + amount);
        }
        List<TransactionWithAccount> dummyList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
            Transaction dummy = new Transaction(entry.getKey(), Math.abs(entry.getValue()), "Monthly Total", System.currentTimeMillis(), entry.getValue() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME, -1, "BDT");
            TransactionWithAccount twa = new TransactionWithAccount(); twa.transaction = dummy;
            dummyList.add(twa);
        }
        adapter.setTransactions(dummyList);
    }

    private void showTotalBreakdown() {
        Map<String, Double> incomeCatTotals = new HashMap<>();
        Map<String, Double> expenseCatTotals = new HashMap<>();
        
        for (TransactionWithAccount t : allTransactions) {
            String cat = t.transaction.category != null ? t.transaction.category : "General";
            if (t.transaction.type == TransactionType.INCOME) {
                incomeCatTotals.put(cat, incomeCatTotals.getOrDefault(cat, 0.0) + t.transaction.amount);
            } else if (t.transaction.type == TransactionType.EXPENSE) {
                expenseCatTotals.put(cat, expenseCatTotals.getOrDefault(cat, 0.0) + t.transaction.amount);
            }
        }

        List<TransactionWithAccount> dummyList = new ArrayList<>();
        
        // Income first, sorted by amount
        List<Map.Entry<String, Double>> incomeList = new ArrayList<>(incomeCatTotals.entrySet());
        Collections.sort(incomeList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
        for (Map.Entry<String, Double> entry : incomeList) {
            Transaction dummy = new Transaction(entry.getKey(), entry.getValue(), "Income Breakdown", System.currentTimeMillis(), TransactionType.INCOME, -1, "BDT");
            TransactionWithAccount twa = new TransactionWithAccount(); twa.transaction = dummy;
            dummyList.add(twa);
        }

        // Expenses second, sorted by amount
        List<Map.Entry<String, Double>> expenseList = new ArrayList<>(expenseCatTotals.entrySet());
        Collections.sort(expenseList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
        for (Map.Entry<String, Double> entry : expenseList) {
            Transaction dummy = new Transaction(entry.getKey(), entry.getValue(), "Expense Breakdown", System.currentTimeMillis(), TransactionType.EXPENSE, -1, "BDT");
            TransactionWithAccount twa = new TransactionWithAccount(); twa.transaction = dummy;
            dummyList.add(twa);
        }

        adapter.setTransactions(dummyList);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
