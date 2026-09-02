package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.android.sheguard.R;
import com.android.sheguard.SheGuard;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentLoginBinding;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.ui.view.LoadingDialog;
import com.android.sheguard.ui.view.OtpVerificationDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Objects;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoadingDialog loadingDialog;
    private OtpVerificationDialog otpVerificationDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        loadingDialog = new LoadingDialog(getContext());
        if (getContext() != null) {
            otpVerificationDialog = new OtpVerificationDialog(getContext());
        }

        binding.btnRegister.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment));

        // Phone OTP Sign-In
        binding.btnLoginOtp.setOnClickListener(v -> {
            String target = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            if (target.isEmpty()) {
                target = "+919876501234"; // Demo phone
            }

            final String loginTarget = target;
            if (otpVerificationDialog != null) {
                otpVerificationDialog.show(loginTarget, "login", (verifiedTarget, otpCode) -> {
                    Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                    Prefs.putString(Constants.PREFS_USER_NAME, "Guardian User");
                    Prefs.putString(Constants.PREFS_USER_PHONE, verifiedTarget);
                    Prefs.putString("USER_ROLE", "user");

                    if (getContext() != null) {
                        Toast.makeText(getContext(), "🎉 Signed in with OTP successfully!", Toast.LENGTH_SHORT).show();
                    }

                    Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                });
            }
        });

        // 1-Click Demo Account Mode
        binding.btnDemoAccount.setOnClickListener(v -> {
            Prefs.putBoolean(Constants.IS_DEMO_MODE, true);
            Prefs.putString(Constants.PREFS_USER_NAME, Constants.DEMO_USER_NAME);
            Prefs.putString(Constants.PREFS_USER_EMAIL, Constants.DEMO_USER_EMAIL);
            Prefs.putString(Constants.PREFS_USER_PHONE, Constants.DEMO_USER_PHONE);
            Prefs.putString("USER_ROLE", "user");

            String existingContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
            if (existingContacts == null || existingContacts.isEmpty() || "[]".equals(existingContacts)) {
                ArrayList<ContactModel> demoContacts = new ArrayList<>();
                demoContacts.add(new ContactModel("Mom", "+15552345678"));
                demoContacts.add(new ContactModel("Alex (Roommate)", "+15558765432"));
                demoContacts.add(new ContactModel("Campus Security", "+15559990000"));
                Prefs.putString(Constants.CONTACTS_LIST, SheGuard.GSON.toJson(demoContacts));
            }

            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.demo_logged_in_toast, Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Email & Password Sign-In via Django/Supabase REST Backend
        binding.btnLogin.setOnClickListener(v -> {
            if (!isInformationValid()) {
                return;
            }

            loadingDialog.show(null);
            String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();

            ApiClient.loginUser(email, password, null, (success, role, message) -> {
                loadingDialog.hide();
                if (!success) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message != null ? message : "❌ Invalid email or password", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                Prefs.putString(Constants.PREFS_USER_EMAIL, email);
                if (Prefs.getString(Constants.PREFS_USER_PHONE, null) == null) {
                    Prefs.putString(Constants.PREFS_USER_PHONE, "+919876501234");
                }
                Prefs.putString("USER_ROLE", role != null ? role : "user");

                if (getContext() != null) {
                    Toast.makeText(getContext(), "🎉 Welcome back!", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            String target = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            if (target.isEmpty()) {
                Snackbar.make(view, getString(R.string.enter_your_email_address), Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (otpVerificationDialog != null) {
                otpVerificationDialog.show(target, "reset", (verifiedTarget, otpCode) -> {
                    Snackbar.make(view, "Password reset passcode verified!", Snackbar.LENGTH_LONG).show();
                });
            }
        });

        return view;
    }

    private boolean isInformationValid() {
        boolean isValid = true;

        if (Objects.requireNonNull(binding.etEmail.getText()).toString().trim().isEmpty()) {
            binding.etEmailLayout.setError(getString(R.string.email_is_required));
            isValid = false;
        } else {
            binding.etEmailLayout.setErrorEnabled(false);
            binding.etEmailLayout.setError(null);
        }

        if (Objects.requireNonNull(binding.etPassword.getText()).toString().trim().isEmpty()) {
            binding.etPasswordLayout.setError(getString(R.string.password_is_required));
            isValid = false;
        } else {
            binding.etPasswordLayout.setErrorEnabled(false);
            binding.etPasswordLayout.setError(null);
        }

        return isValid;
    }

    @Override
    public void onDestroyView() {
        if (loadingDialog != null) loadingDialog.hide();
        if (otpVerificationDialog != null) otpVerificationDialog.dismiss();
        super.onDestroyView();
    }
}