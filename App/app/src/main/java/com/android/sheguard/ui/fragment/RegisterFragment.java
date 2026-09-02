package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentRegisterBinding;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.ui.view.LoadingDialog;
import com.android.sheguard.ui.view.OtpVerificationDialog;

import java.util.Objects;
import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private LoadingDialog loadingDialog;
    private OtpVerificationDialog otpVerificationDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        loadingDialog = new LoadingDialog(getContext());
        if (getContext() != null) {
            otpVerificationDialog = new OtpVerificationDialog(getContext());
        }

        if (getArguments() != null) {
            String prefillEmail = getArguments().getString("PREFILL_EMAIL");
            String prefillPhone = getArguments().getString("PREFILL_PHONE");
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                binding.etEmail.setText(prefillEmail);
            }
            if (prefillPhone != null && !prefillPhone.isEmpty()) {
                binding.etPhone.setText(prefillPhone);
            }
        }

        binding.btnRegister.setOnClickListener(v -> {
            if (!isInformationValid()) {
                return;
            }

            final String fullName = Objects.requireNonNull(binding.etFullName.getText()).toString().trim();
            final String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            final String phone = Objects.requireNonNull(binding.etPhone.getText()).toString().trim();
            final String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();
            final String role = binding.rbRoleGuardian.isChecked() ? "guardian" : "user";

            // Launch 6-digit OTP verification dialog before finalizing registration
            if (otpVerificationDialog != null) {
                otpVerificationDialog.show(phone, "registration", (target, otpCode) -> {
                    loadingDialog.show(null);

                    // 1. Sync with Django & Supabase Backend
                    ApiClient.registerUser(fullName, email, phone, password, role, (success, assignedRole, msg) -> {
                        loadingDialog.hide();
                        if (!success) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), msg != null ? msg : "❌ Registration failed", Toast.LENGTH_LONG).show();
                            }
                            return;
                        }

                        // 2. Save session locally in Prefs
                        Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                        Prefs.putString(Constants.PREFS_USER_NAME, fullName);
                        Prefs.putString(Constants.PREFS_USER_EMAIL, email);
                        Prefs.putString(Constants.PREFS_USER_PHONE, phone);
                        Prefs.putString("USER_ROLE", role);

                        if (getContext() != null) {
                            Toast.makeText(getContext(), "🎉 Account created and verified with Supabase Cloud!", Toast.LENGTH_SHORT).show();
                        }

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                });
            }
        });

        binding.btnLogin.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private boolean isInformationValid() {
        boolean isValid = true;

        if (Objects.requireNonNull(binding.etFullName.getText()).toString().trim().isEmpty()) {
            binding.etFullNameLayout.setError(getString(R.string.please_enter_your_full_name));
            isValid = false;
        } else {
            binding.etFullNameLayout.setErrorEnabled(false);
            binding.etFullNameLayout.setError(null);
        }

        if (Objects.requireNonNull(binding.etEmail.getText()).toString().trim().isEmpty()) {
            binding.etEmailLayout.setError(getString(R.string.please_enter_your_email));
            isValid = false;
        } else if (!isValidEmail(binding.etEmail.getText().toString().trim())) {
            binding.etEmailLayout.setError(getString(R.string.please_enter_a_valid_email));
            isValid = false;
        } else {
            binding.etEmailLayout.setErrorEnabled(false);
            binding.etEmailLayout.setError(null);
        }

        if (Objects.requireNonNull(binding.etPhone.getText()).toString().trim().isEmpty()) {
            binding.etPhoneLayout.setError(getString(R.string.please_enter_your_phone_number));
            isValid = false;
        } else if (binding.etPhone.getText().toString().trim().length() < 10) {
            binding.etPhoneLayout.setError(getString(R.string.please_enter_a_valid_phone_number));
            isValid = false;
        } else {
            binding.etPhoneLayout.setErrorEnabled(false);
            binding.etPhoneLayout.setError(null);
        }

        if (Objects.requireNonNull(binding.etPassword.getText()).toString().trim().isEmpty()) {
            binding.etPasswordLayout.setError(getString(R.string.please_enter_your_password));
            isValid = false;
        } else {
            binding.etPasswordLayout.setErrorEnabled(false);
            binding.etPasswordLayout.setError(null);
        }

        if (Objects.requireNonNull(binding.etConfirmPassword.getText()).toString().trim().isEmpty()) {
            binding.etConfirmPasswordLayout.setError(getString(R.string.please_enter_your_password_again));
            isValid = false;
        } else {
            binding.etConfirmPasswordLayout.setErrorEnabled(false);
            binding.etConfirmPasswordLayout.setError(null);
        }

        if (!binding.etPassword.getText().toString().trim().isEmpty() && !binding.etConfirmPassword.getText().toString().trim().isEmpty()) {
            if (!binding.etPassword.getText().toString().trim().equals(binding.etConfirmPassword.getText().toString().trim())) {
                binding.etConfirmPasswordLayout.setError(getString(R.string.password_does_not_match));
                isValid = false;
            } else {
                if (!isValidPassword(binding.etPassword.getText().toString().trim())) {
                    binding.etConfirmPasswordLayout.setError(getString(R.string.password_constraints));
                    isValid = false;
                } else {
                    binding.etConfirmPasswordLayout.setErrorEnabled(false);
                    binding.etConfirmPasswordLayout.setError(null);
                }
            }
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        final String PASSWORD_PATTERN = "^" +   // Start of string
                "(?=.*[0-9])" +                 // at least 1 digit
                "(?=.*[a-z])" +                 // at least 1 lower case letter
                "(?=.*[A-Z])" +                 // at least 1 upper case letter
                "(?=.*[a-zA-Z])" +              // any letter
                "(?=\\S+$)" +                   // no white spaces
                ".{8,}" +                       // at least 8 characters
                "$";                            // end of string

        return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    @Override
    public void onDestroyView() {
        if (loadingDialog != null) loadingDialog.hide();
        if (otpVerificationDialog != null) otpVerificationDialog.dismiss();
        super.onDestroyView();
    }
}