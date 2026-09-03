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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

        // Email / Phone OTP Sign-In
        binding.btnLoginOtp.setOnClickListener(v -> {
            String target = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            if (target.isEmpty()) {
                binding.etEmailLayout.setError("Enter your email or phone to receive an OTP");
                binding.etEmail.requestFocus();
                return;
            }

            binding.etEmailLayout.setError(null);
            binding.etEmailLayout.setErrorEnabled(false);

            final String loginTarget = target;
            if (otpVerificationDialog != null) {
                otpVerificationDialog.show(loginTarget, "login", (verifiedTarget, otpCode) -> {
                    loadingDialog.show(null);
                    ApiClient.loginUser(verifiedTarget, null, otpCode, (success, role, message) -> {
                        loadingDialog.hide();
                        if (success) {
                            Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                            if (verifiedTarget.contains("@")) {
                                Prefs.putString(Constants.PREFS_USER_EMAIL, verifiedTarget);
                            } else {
                                Prefs.putString(Constants.PREFS_USER_PHONE, verifiedTarget);
                            }
                            Prefs.putString("USER_ROLE", role != null ? role : "user");

                            if (getContext() != null) {
                                Toast.makeText(getContext(), "🎉 Signed in with OTP successfully!", Toast.LENGTH_SHORT).show();
                            }

                            Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                        } else {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), message != null ? message : "Verification failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
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

        // 1-Click Guardian / Responder Account Mode
        binding.btnGuardianDemoAccount.setOnClickListener(v -> {
            Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
            Prefs.putString(Constants.PREFS_USER_NAME, "Rajesh Sharma (Guardian Unit)");
            Prefs.putString(Constants.PREFS_USER_EMAIL, "guardian@sheguard.app");
            Prefs.putString(Constants.PREFS_USER_PHONE, "+919988776655");
            Prefs.putString("USER_ROLE", "guardian");

            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.guardian_logged_in_toast, Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 1-Click Chief SuperAdmin Account Mode
        binding.btnSuperadminDemoAccount.setOnClickListener(v -> {
            Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
            Prefs.putString(Constants.PREFS_USER_NAME, "Chief SuperAdmin");
            Prefs.putString(Constants.PREFS_USER_EMAIL, "admin@sheguard.app");
            Prefs.putString(Constants.PREFS_USER_PHONE, "+919876500000");
            Prefs.putString("USER_ROLE", "superadmin");

            if (getContext() != null) {
                Toast.makeText(getContext(), "👑 Logged in as Chief SuperAdmin", Toast.LENGTH_SHORT).show();
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

            binding.etEmailLayout.setError(null);
            binding.etEmailLayout.setErrorEnabled(false);
            binding.etPasswordLayout.setError(null);
            binding.etPasswordLayout.setErrorEnabled(false);

            ApiClient.loginUser(email, password, null, (success, role, message) -> {
                loadingDialog.hide();
                if (!success) {
                    boolean isUserNotFound = message != null && (
                            message.toLowerCase().contains("not found") ||
                            message.toLowerCase().contains("register") ||
                            message.toLowerCase().contains("no account")
                    );

                    boolean isPasswordError = message != null && message.toLowerCase().contains("password");

                    if (isUserNotFound && getContext() != null) {
                        binding.etEmailLayout.setError("Account not found");
                        new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialComponents_MaterialAlertDialog)
                                .setTitle("Account Not Found")
                                .setMessage("No GuardianAI account exists for \"" + email + "\".\n\nWould you like to register a new account now?")
                                .setPositiveButton("Register Now", (dialog, which) -> {
                                    Bundle bundle = new Bundle();
                                    if (email.contains("@")) {
                                        bundle.putString("PREFILL_EMAIL", email);
                                    } else {
                                        bundle.putString("PREFILL_PHONE", email);
                                    }
                                    NavHostFragment.findNavController(LoginFragment.this)
                                             .navigate(R.id.action_loginFragment_to_registerFragment, bundle);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else if (isPasswordError) {
                        binding.etPasswordLayout.setError(message);
                        binding.etPassword.requestFocus();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), message != null ? message : "❌ Invalid email or password", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                Prefs.putString(Constants.PREFS_USER_EMAIL, email);
                if ("superadmin".equalsIgnoreCase(role) || "admin@sheguard.app".equalsIgnoreCase(email)) {
                    Prefs.putString(Constants.PREFS_USER_PHONE, "+919876500000");
                    Prefs.putString(Constants.PREFS_USER_NAME, "Chief SuperAdmin");
                    Prefs.putString("USER_ROLE", "superadmin");
                } else {
                    if (Prefs.getString(Constants.PREFS_USER_PHONE, null) == null) {
                        Prefs.putString(Constants.PREFS_USER_PHONE, email.contains("@") ? "+919876543210" : email);
                    }
                    Prefs.putString("USER_ROLE", role != null ? role : "user");
                }

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