package com.moneybag.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.moneybag.nativeapp.databinding.FragmentSyncFrequencyBinding;

public class SyncFrequencyFragment extends Fragment {

    private FragmentSyncFrequencyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSyncFrequencyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        SharedPreferences prefs = requireContext().getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
        int currentFreq = prefs.getInt("sync_frequency", 0); // 0: Manual, 1: Hourly, 2: Daily, 3: Weekly

        if (currentFreq == 0) binding.radioManual.setChecked(true);
        else if (currentFreq == 1) binding.radioHourly.setChecked(true);
        else if (currentFreq == 2) binding.radioDaily.setChecked(true);
        else if (currentFreq == 3) binding.radioWeekly.setChecked(true);

        binding.btnSaveSync.setOnClickListener(v -> {
            int selected = 0;
            if (binding.radioManual.isChecked()) selected = 0;
            else if (binding.radioHourly.isChecked()) selected = 1;
            else if (binding.radioDaily.isChecked()) selected = 2;
            else if (binding.radioWeekly.isChecked()) selected = 3;

            prefs.edit().putInt("sync_frequency", selected).apply();
            Toast.makeText(getContext(), "Sync frequency updated", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
