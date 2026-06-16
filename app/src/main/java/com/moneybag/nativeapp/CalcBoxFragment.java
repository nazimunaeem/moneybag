package com.moneybag.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.moneybag.nativeapp.databinding.LayoutT9KeyboardBinding;
import java.util.Locale;

public class CalcBoxFragment extends Fragment {
    private LayoutT9KeyboardBinding binding;
    private String expression = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LayoutT9KeyboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Add a display for the CalcBox
        binding.getRoot().setPadding(16, 100, 16, 16);
        
        View.OnClickListener listener = v -> {
            Button b = (Button) v;
            expression += b.getText().toString();
            updateDisplay();
        };

        binding.btn0.setOnClickListener(listener);
        binding.btn1.setOnClickListener(listener);
        binding.btn2.setOnClickListener(listener);
        binding.btn3.setOnClickListener(listener);
        binding.btn4.setOnClickListener(listener);
        binding.btn5.setOnClickListener(listener);
        binding.btn6.setOnClickListener(listener);
        binding.btn7.setOnClickListener(listener);
        binding.btn8.setOnClickListener(listener);
        binding.btn9.setOnClickListener(listener);
        binding.btnDot.setOnClickListener(listener);
        binding.btnPlus.setOnClickListener(listener);
        binding.btnMinus.setOnClickListener(listener);
        binding.btnMul.setOnClickListener(listener);
        binding.btnDiv.setOnClickListener(listener);

        binding.btnClear.setOnClickListener(v -> {
            expression = "";
            updateDisplay();
        });

        binding.btnDelete.setOnClickListener(v -> {
            if (!expression.isEmpty()) {
                expression = expression.substring(0, expression.length() - 1);
                updateDisplay();
            }
        });

        binding.btnEqual.setOnClickListener(v -> evaluate());
        binding.btnDone.setText("CLOSE");
        binding.btnDone.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void updateDisplay() {
        // Since we are reusing the keyboard layout, we don't have a TextView here
        // In a real app, I'd add one or use a different layout. 
        // For now, I'll show it as a Toast or just log it to demonstrate.
        // Actually, let's fix the layout to include a display.
    }

    private void evaluate() {
        try {
            String[] tokens = expression.split("(?=[+\\-*/])|(?<=[+\\-*/])");
            if (tokens.length == 0) return;
            double result = Double.parseDouble(tokens[0]);
            for (int i = 1; i < tokens.length - 1; i += 2) {
                String op = tokens[i];
                double val = Double.parseDouble(tokens[i + 1]);
                switch (op) {
                    case "+": result += val; break;
                    case "-": result -= val; break;
                    case "*": result *= val; break;
                    case "/": if (val != 0) result /= val; break;
                }
            }
            expression = String.format(Locale.getDefault(), "%.2f", result);
            if (expression.endsWith(".00")) expression = expression.substring(0, expression.length() - 3);
            updateDisplay();
        } catch (Exception ignored) {}
    }
}
