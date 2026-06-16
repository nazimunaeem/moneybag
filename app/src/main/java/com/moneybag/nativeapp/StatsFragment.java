package com.moneybag.nativeapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.TransactionType;
import com.moneybag.nativeapp.data.TransactionWithAccount;
import com.moneybag.nativeapp.databinding.FragmentStatsBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;
    private MoneyBagRepository repository;
    private StatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());
        
        adapter = new StatsAdapter();
        binding.statsList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.statsList.setAdapter(adapter);

        loadStats();
    }

    private void loadStats() {
        repository.getAllTransactions(transactions -> {
            if (getActivity() == null) return;

            Map<String, Double> expenseCategoryTotals = new HashMap<>();
            Map<String, Double> incomeCategoryTotals = new HashMap<>();
            Map<Long, Double> dailyBalances = new TreeMap<>();
            
            double totalIncome = 0;
            double totalExpense = 0;

            // Sort transactions by timestamp for line chart
            Collections.sort(transactions, (o1, o2) -> Long.compare(o1.transaction.timestamp, o2.transaction.timestamp));

            double runningBalance = 0;
            for (TransactionWithAccount t : transactions) {
                if (t.transaction.type == TransactionType.INCOME) {
                    totalIncome += t.transaction.amount;
                    runningBalance += t.transaction.amount;
                    String cat = t.transaction.category != null ? t.transaction.category : "General";
                    incomeCategoryTotals.put(cat, incomeCategoryTotals.getOrDefault(cat, 0.0) + t.transaction.amount);
                } else if (t.transaction.type == TransactionType.EXPENSE) {
                    totalExpense += t.transaction.amount;
                    runningBalance -= t.transaction.amount;
                    String cat = t.transaction.category != null ? t.transaction.category : "General";
                    expenseCategoryTotals.put(cat, expenseCategoryTotals.getOrDefault(cat, 0.0) + t.transaction.amount);
                }
                dailyBalances.put(t.transaction.timestamp, runningBalance);
            }

            final double income = totalIncome;
            final double expense = totalExpense;
            
            List<PieEntry> expenseEntries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : expenseCategoryTotals.entrySet()) {
                expenseEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            List<PieEntry> incomeEntries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : incomeCategoryTotals.entrySet()) {
                incomeEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            List<Entry> balanceEntries = new ArrayList<>();
            int i = 0;
            for (Map.Entry<Long, Double> entry : dailyBalances.entrySet()) {
                balanceEntries.add(new Entry(i++, entry.getValue().floatValue()));
            }

            getActivity().runOnUiThread(() -> {
                binding.statsIncome.setText(String.format(Locale.getDefault(), "৳%.2f", income));
                binding.statsExpense.setText(String.format(Locale.getDefault(), "৳%.2f", expense));
                
                setupPieChart(binding.pieChartExpense, expenseEntries, "Expenses");
                setupPieChart(binding.pieChartIncome, incomeEntries, "Income");
                setupLineChart(balanceEntries);

                // Prepare list data: Income categories first, then Expense categories
                List<StatItem> statItems = new ArrayList<>();
                List<Map.Entry<String, Double>> incomeList = new ArrayList<>(incomeCategoryTotals.entrySet());
                Collections.sort(incomeList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
                for (Map.Entry<String, Double> entry : incomeList) {
                    statItems.add(new StatItem(entry.getKey(), entry.getValue(), income > 0 ? (entry.getValue() / income) * 100 : 0));
                }

                List<Map.Entry<String, Double>> expenseList = new ArrayList<>(expenseCategoryTotals.entrySet());
                Collections.sort(expenseList, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
                for (Map.Entry<String, Double> entry : expenseList) {
                    statItems.add(new StatItem(entry.getKey(), entry.getValue(), expense > 0 ? (entry.getValue() / expense) * 100 : 0));
                }
                adapter.setItems(statItems);
            });
        });
    }

    private void setupPieChart(PieChart chart, List<PieEntry> entries, String label) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setDescription(null);
        chart.setCenterText(label);
        chart.setCenterTextSize(16f);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(10f);
        chart.getLegend().setEnabled(false); // Hide legend to save space since labels are outside
        chart.setExtraOffsets(20, 0, 20, 0); // Add space for outside labels
        chart.animateY(1000);
        chart.invalidate();
    }

    private void setupLineChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Balance");
        dataSet.setColor(getContext().getColor(R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getContext().getColor(R.color.primary));
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getContext().getColor(R.color.primaryContainer));

        LineData lineData = new LineData(dataSet);
        binding.lineChart.setData(lineData);
        binding.lineChart.setDescription(null);
        binding.lineChart.getXAxis().setDrawGridLines(false);
        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.animateX(1000);
        binding.lineChart.invalidate();
    }

    private static class StatItem {
        String category;
        double amount;
        double percentage;

        StatItem(String category, double amount, double percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
        }
    }

    private static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        private List<StatItem> items = new ArrayList<>();

        void setItems(List<StatItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StatItem item = items.get(position);
            ((android.widget.TextView) holder.itemView.findViewById(android.R.id.text1))
                    .setText(String.format(Locale.getDefault(), "%s (%.1f%%)", item.category, item.percentage));
            ((android.widget.TextView) holder.itemView.findViewById(android.R.id.text2))
                    .setText(String.format(Locale.getDefault(), "৳%.2f", item.amount));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
