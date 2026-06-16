package com.moneybag.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.databinding.FragmentDeleteDataBinding;

public class DeleteDataFragment extends Fragment {

    private FragmentDeleteDataBinding binding;
    private MoneyBagRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeleteDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.checkConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.btnDeleteAll.setEnabled(isChecked);
        });

        binding.btnDeleteAll.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Final Confirmation")
                    .setMessage("Are you absolutely sure? This cannot be reversed.")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        repository.clearAllData(() -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "All data deleted", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                });
                            }
                        });
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
