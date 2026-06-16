package com.moneybag.nativeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.moneybag.nativeapp.databinding.ActivityPasscodeBinding;
import com.moneybag.nativeapp.databinding.LayoutPasscodeKeyboardBinding;

public class PasscodeActivity extends AppCompatActivity {

    private ActivityPasscodeBinding binding;
    private String passcode = "";
    private String setupFirstPasscode = "";
    private String mode = "login"; // setup, login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasscodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            if (isPasscodeSet()) {
                mode = "login";
            } else {
                // If no passcode, go to Main
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
        }

        if (mode.equals("setup")) {
            binding.textPasscodeTitle.setText(getString(R.string.title_setup_passcode));
            binding.textPasscodeStatus.setText(getString(R.string.msg_create_passcode));
        }

        setupKeyboard();
        updateDots();
    }

    private void setupKeyboard() {
        LayoutPasscodeKeyboardBinding kbBinding = LayoutPasscodeKeyboardBinding.inflate(getLayoutInflater());
        binding.keyboardContainer.addView(kbBinding.getRoot());

        View.OnClickListener listener = v -> {
            if (passcode.length() < 4) {
                passcode += ((Button) v).getText().toString();
                updateDots();
                if (passcode.length() == 4) {
                    handlePasscodeEntry();
                }
            }
        };

        kbBinding.btn1.setOnClickListener(listener);
        kbBinding.btn2.setOnClickListener(listener);
        kbBinding.btn3.setOnClickListener(listener);
        kbBinding.btn4.setOnClickListener(listener);
        kbBinding.btn5.setOnClickListener(listener);
        kbBinding.btn6.setOnClickListener(listener);
        kbBinding.btn7.setOnClickListener(listener);
        kbBinding.btn8.setOnClickListener(listener);
        kbBinding.btn9.setOnClickListener(listener);
        kbBinding.btn0.setOnClickListener(listener);

        kbBinding.btnDelete.setOnClickListener(v -> {
            if (!passcode.isEmpty()) {
                passcode = passcode.substring(0, passcode.length() - 1);
                updateDots();
            }
        });
    }

    private void updateDots() {
        binding.dot1.setSelected(passcode.length() >= 1);
        binding.dot2.setSelected(passcode.length() >= 2);
        binding.dot3.setSelected(passcode.length() >= 3);
        binding.dot4.setSelected(passcode.length() >= 4);
    }

    private void handlePasscodeEntry() {
        if (mode.equals("setup")) {
            if (setupFirstPasscode.isEmpty()) {
                setupFirstPasscode = passcode;
                passcode = "";
                binding.textPasscodeTitle.setText(getString(R.string.title_confirm_passcode));
                binding.textPasscodeStatus.setText(getString(R.string.msg_reenter_passcode));
                updateDots();
            } else {
                if (passcode.equals(setupFirstPasscode)) {
                    savePasscode(passcode);
                    Toast.makeText(this, getString(R.string.toast_passcode_set), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.toast_passcode_mismatch), Toast.LENGTH_SHORT).show();
                    passcode = "";
                    updateDots();
                }
            }
        } else {
            if (verifyPasscode(passcode)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, getString(R.string.toast_incorrect_passcode), Toast.LENGTH_SHORT).show();
                passcode = "";
                updateDots();
            }
        }
    }

    private boolean isPasscodeSet() {
        SharedPreferences pref = getSharedPreferences("security", MODE_PRIVATE);
        return pref.contains("passcode");
    }

    private void savePasscode(String code) {
        SharedPreferences pref = getSharedPreferences("security", MODE_PRIVATE);
        pref.edit().putString("passcode", code).apply();
    }

    private boolean verifyPasscode(String code) {
        SharedPreferences pref = getSharedPreferences("security", MODE_PRIVATE);
        String saved = pref.getString("passcode", "");
        return code.equals(saved);
    }
}
