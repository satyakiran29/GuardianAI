package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentEditProfileBinding;
import com.android.sheguard.ui.view.LoadingDialog;

import java.util.Objects;
import java.util.regex.Pattern;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_edit_profile_title));
            binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_edit_profile_desc));
        }

        loadingDialog = new LoadingDialog(getContext());

        populateExistingDetails();

        binding.btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveProfileChanges(view);
            }
        });

        return view;
    }

    private void populateExistingDetails() {
        String currentName = Prefs.getString(Constants.PREFS_USER_NAME, "");
        String currentPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        String currentEmail = Prefs.getString(Constants.PREFS_USER_EMAIL, "");

        if (!currentName.isEmpty()) {
            binding.etNewName.setText(currentName);
        }
        if (!currentPhone.isEmpty()) {
            binding.etNewPhone.setText(currentPhone);
        }
        if (!currentEmail.isEmpty()) {
            binding.etNewEmail.setText(currentEmail);
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String name = Objects.requireNonNull(binding.etNewName.getText()).toString().trim();
        String phone = Objects.requireNonNull(binding.etNewPhone.getText()).toString().trim();
        String email = Objects.requireNonNull(binding.etNewEmail.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(binding.etNewPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(binding.etConfirmNewPassword.getText()).toString().trim();

        // 1. Name validation
        if (name.isEmpty()) {
            binding.etNewNameLayout.setError(getString(R.string.please_enter_your_full_name));
            isValid = false;
        } else {
            binding.etNewNameLayout.setErrorEnabled(false);
            binding.etNewNameLayout.setError(null);
        }

        // 2. Phone validation
        if (phone.isEmpty()) {
            binding.etNewPhoneLayout.setError(getString(R.string.please_enter_your_phone_number));
            isValid = false;
        } else if (phone.length() < 10) {
            binding.etNewPhoneLayout.setError(getString(R.string.please_enter_a_valid_phone_number));
            isValid = false;
        } else {
            binding.etNewPhoneLayout.setErrorEnabled(false);
            binding.etNewPhoneLayout.setError(null);
        }

        // 3. Email validation
        if (email.isEmpty()) {
            binding.etNewEmailLayout.setError(getString(R.string.please_enter_your_email));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etNewEmailLayout.setError(getString(R.string.please_enter_a_valid_email));
            isValid = false;
        } else {
            binding.etNewEmailLayout.setErrorEnabled(false);
            binding.etNewEmailLayout.setError(null);
        }

        // 4. Password validation (Optional if user doesn't wish to change it)
        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (newPassword.isEmpty()) {
                binding.etNewPasswordLayout.setError(getString(R.string.please_enter_your_password));
                isValid = false;
            } else if (newPassword.length() < 6) {
                binding.etNewPasswordLayout.setError(getString(R.string.password_constraints));
                isValid = false;
            } else {
                binding.etNewPasswordLayout.setErrorEnabled(false);
                binding.etNewPasswordLayout.setError(null);
            }

            if (confirmPassword.isEmpty()) {
                binding.etConfirmNewPasswordLayout.setError(getString(R.string.please_enter_your_password_again));
                isValid = false;
            } else if (!newPassword.equals(confirmPassword)) {
                binding.etConfirmNewPasswordLayout.setError(getString(R.string.password_does_not_match));
                isValid = false;
            } else {
                binding.etConfirmNewPasswordLayout.setErrorEnabled(false);
                binding.etConfirmNewPasswordLayout.setError(null);
            }
        } else {
            binding.etNewPasswordLayout.setErrorEnabled(false);
            binding.etNewPasswordLayout.setError(null);
            binding.etConfirmNewPasswordLayout.setErrorEnabled(false);
            binding.etConfirmNewPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void saveProfileChanges(View view) {
        final String name = Objects.requireNonNull(binding.etNewName.getText()).toString().trim();
        final String phone = Objects.requireNonNull(binding.etNewPhone.getText()).toString().trim();
        final String email = Objects.requireNonNull(binding.etNewEmail.getText()).toString().trim();
        final String password = Objects.requireNonNull(binding.etNewPassword.getText()).toString().trim();

        final String currentPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        final String currentEmail = Prefs.getString(Constants.PREFS_USER_EMAIL, "");
        final String role = Prefs.getString("USER_ROLE", "user");

        loadingDialog.show(null);

        // Sync with Django & Supabase Backend
        ApiClient.updateProfile(
                currentPhone,
                currentEmail,
                name,
                email,
                phone,
                password,
                role,
                (success, updatedRole, msg) -> {
                    loadingDialog.hide();

                    // Update local Prefs cache
                    Prefs.putString(Constants.PREFS_USER_NAME, name);
                    Prefs.putString(Constants.PREFS_USER_PHONE, phone);
                    Prefs.putString(Constants.PREFS_USER_EMAIL, email);
                    if (updatedRole != null && !updatedRole.isEmpty()) {
                        Prefs.putString("USER_ROLE", updatedRole);
                    }

                    if (getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show();
                    }

                    // Return to previous profile view
                    try {
                        Navigation.findNavController(view).popBackStack();
                    } catch (Exception e) {
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        if (loadingDialog != null) loadingDialog.hide();
        super.onDestroyView();
    }
}