package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentEditProfileBinding;
import com.android.sheguard.ui.view.LoadingDialog;
import com.google.android.material.snackbar.Snackbar;

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

        binding.btnSave.setOnClickListener(v -> {
            loadingDialog.show(null);
            saveDetailsInDatabase();
        });

        return view;
    }

    private void saveDetailsInDatabase() {
        String name = binding.etNewName.getText() != null ? binding.etNewName.getText().toString().trim() : "";
        String phone = binding.etNewPhone.getText() != null ? binding.etNewPhone.getText().toString().trim() : "";

        if (!name.isEmpty()) {
            Prefs.putString(Constants.PREFS_USER_NAME, name);
        }
        if (!phone.isEmpty()) {
            Prefs.putString(Constants.PREFS_USER_PHONE, phone);
        }

        String email = Prefs.getString(Constants.PREFS_USER_EMAIL, "user@guardian.ai");
        String role = Prefs.getString("USER_ROLE", "user");

        // Sync updated profile to Django + Supabase Backend
        ApiClient.registerUser(
                Prefs.getString(Constants.PREFS_USER_NAME, "Guardian User"),
                email,
                Prefs.getString(Constants.PREFS_USER_PHONE, "+919876501234"),
                role,
                (success, r, msg) -> {
                    loadingDialog.hide();
                    binding.etNewName.setText("");
                    binding.etNewPhone.setText("");
                    Snackbar.make(binding.getRoot(), getString(R.string.details_saved_successfully), Snackbar.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onDestroyView() {
        if (loadingDialog != null) loadingDialog.hide();
        super.onDestroyView();
    }
}