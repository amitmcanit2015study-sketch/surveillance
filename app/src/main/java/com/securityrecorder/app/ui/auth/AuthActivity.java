package com.securityrecorder.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.databinding.ActivityAuthBinding;
import com.securityrecorder.app.utils.HapticUtils;

/**
 * Authentication Activity handling Biometric and PIN authentication to unlock the app.
 */
public class AuthActivity extends AppCompatActivity {

    public static final String EXTRA_MODE_SETUP = "extra_mode_setup";
    private ActivityAuthBinding binding;
    private AppPreferences preferences;
    private StringBuilder enteredPin = new StringBuilder();
    private boolean isSetupMode = false;
    private String firstEnteredPin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = new AppPreferences(this);
        isSetupMode = getIntent().getBooleanExtra(EXTRA_MODE_SETUP, false);

        initViews();
        setupNumberPad();

        if (!isSetupMode && BiometricHelper.isBiometricAvailable(this)) {
            triggerBiometricAuth();
        }
    }

    private void initViews() {
        if (isSetupMode) {
            binding.tvAuthTitle.setText(R.string.auth_setup_pin);
            binding.btnBiometricAuth.setVisibility(View.INVISIBLE);
        } else {
            binding.tvAuthTitle.setText(R.string.auth_enter_pin);
            binding.btnBiometricAuth.setVisibility(
                    BiometricHelper.isBiometricAvailable(this) ? View.VISIBLE : View.INVISIBLE
            );
        }
    }

    private void setupNumberPad() {
        int[] numBtnIds = {
                R.id.btnNum0, R.id.btnNum1, R.id.btnNum2, R.id.btnNum3,
                R.id.btnNum4, R.id.btnNum5, R.id.btnNum6, R.id.btnNum7,
                R.id.btnNum8, R.id.btnNum9
        };

        for (int id : numBtnIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> {
                HapticUtils.performClickFeedback(this);
                onDigitPressed(btn.getText().toString());
            });
        }

        binding.btnNumBackspace.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            onBackspacePressed();
        });

        binding.btnBiometricAuth.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            triggerBiometricAuth();
        });
    }

    private void onDigitPressed(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updatePinDots();
            if (enteredPin.length() == 4) {
                handlePinComplete();
            }
        }
    }

    private void onBackspacePressed() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updatePinDots();
            binding.tvAuthError.setVisibility(View.INVISIBLE);
        }
    }

    private void updatePinDots() {
        int len = enteredPin.length();
        int activeColor = getColor(R.color.md_theme_light_primary);
        int inactiveColor = getColor(R.color.md_theme_light_outlineVariant);

        binding.dot1.setBackgroundColor(len >= 1 ? activeColor : inactiveColor);
        binding.dot2.setBackgroundColor(len >= 2 ? activeColor : inactiveColor);
        binding.dot3.setBackgroundColor(len >= 3 ? activeColor : inactiveColor);
        binding.dot4.setBackgroundColor(len >= 4 ? activeColor : inactiveColor);
    }

    private void handlePinComplete() {
        String pin = enteredPin.toString();
        if (isSetupMode) {
            if (firstEnteredPin == null) {
                firstEnteredPin = pin;
                enteredPin.setLength(0);
                updatePinDots();
                binding.tvAuthTitle.setText(R.string.auth_confirm_pin);
            } else {
                if (firstEnteredPin.equals(pin)) {
                    preferences.setSecurityPin(pin);
                    preferences.setAppLockEnabled(true);
                    setResult(RESULT_OK);
                    finish();
                } else {
                    showError(getString(R.string.auth_pin_mismatch));
                    firstEnteredPin = null;
                    enteredPin.setLength(0);
                    updatePinDots();
                    binding.tvAuthTitle.setText(R.string.auth_setup_pin);
                }
            }
        } else {
            String savedPin = preferences.getSecurityPin();
            if (pin.equals(savedPin)) {
                setResult(RESULT_OK);
                finish();
            } else {
                showError(getString(R.string.auth_wrong_pin));
                enteredPin.setLength(0);
                updatePinDots();
            }
        }
    }

    private void triggerBiometricAuth() {
        BiometricHelper.showBiometricPrompt(this, new BiometricHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                // Fallback to PIN
            }

            @Override
            public void onFailed() {
                showError(getString(R.string.auth_wrong_pin));
            }
        });
    }

    private void showError(String message) {
        binding.tvAuthError.setText(message);
        binding.tvAuthError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (isSetupMode) {
            super.onBackPressed();
        } else {
            finishAffinity();
        }
    }
}
