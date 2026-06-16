package com.moneybag.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moneybag.nativeapp.databinding.FragmentCurrencySettingBinding;
import com.moneybag.nativeapp.databinding.ItemSecondaryCurrencyBinding;
import java.util.ArrayList;
import java.util.List;

public class CurrencySettingFragment extends Fragment {

    private FragmentCurrencySettingBinding binding;
    private SecondaryCurrencyAdapter adapter;
    private List<SecondaryCurrency> secondaryCurrencies;
    private final String[] allCurrencies = {"BDT", "USD", "EUR", "GBP", "INR", "SAR", "AED", "MYR", "SGD"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCurrencySettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadSettings();

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, allCurrencies);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPrimaryCurrency.setAdapter(spinnerAdapter);

        SharedPreferences prefs = requireContext().getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
        String primary = prefs.getString("primary_currency", "BDT");
        for (int i = 0; i < allCurrencies.length; i++) {
            if (allCurrencies[i].equals(primary)) {
                binding.spinnerPrimaryCurrency.setSelection(i);
                break;
            }
        }

        binding.recyclerSecondaryCurrencies.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SecondaryCurrencyAdapter();
        binding.recyclerSecondaryCurrencies.setAdapter(adapter);

        binding.btnAddSecondary.setOnClickListener(v -> showAddCurrencyDialog());

        binding.btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("secondary_currencies", "[]");
        secondaryCurrencies = new Gson().fromJson(json, new TypeToken<List<SecondaryCurrency>>() {}.getType());
        if (secondaryCurrencies == null) secondaryCurrencies = new ArrayList<>();
    }

    private void showAddCurrencyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Currency")
                .setItems(allCurrencies, (dialog, which) -> {
                    String code = allCurrencies[which];
                    for (SecondaryCurrency sc : secondaryCurrencies) {
                        if (sc.code.equals(code)) {
                            Toast.makeText(getContext(), "Already added", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    secondaryCurrencies.add(new SecondaryCurrency(code, 1.0));
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private void saveSettings() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
        String primary = binding.spinnerPrimaryCurrency.getSelectedItem().toString();
        
        prefs.edit()
                .putString("primary_currency", primary)
                .putString("secondary_currencies", new Gson().toJson(secondaryCurrencies))
                .apply();

        Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    public static class SecondaryCurrency {
        public String code;
        public double rate;

        public SecondaryCurrency(String code, double rate) {
            this.code = code;
            this.rate = rate;
        }
    }

    private class SecondaryCurrencyAdapter extends RecyclerView.Adapter<SecondaryCurrencyAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSecondaryCurrencyBinding itemBinding = ItemSecondaryCurrencyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SecondaryCurrency sc = secondaryCurrencies.get(position);
            holder.binding.textCurrencyCode.setText(sc.code);
            holder.binding.editExchangeRate.setText(String.valueOf(sc.rate));

            holder.binding.editExchangeRate.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        sc.rate = Double.parseDouble(holder.binding.editExchangeRate.getText().toString());
                    } catch (Exception ignored) {}
                }
            });

            holder.binding.btnDelete.setOnClickListener(v -> {
                secondaryCurrencies.remove(position);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return secondaryCurrencies.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemSecondaryCurrencyBinding binding;
            ViewHolder(ItemSecondaryCurrencyBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
