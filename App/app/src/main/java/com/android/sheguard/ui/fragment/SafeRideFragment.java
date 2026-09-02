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
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSafeRideBinding;
import com.google.android.material.snackbar.Snackbar;

public class SafeRideFragment extends Fragment {

    private FragmentSafeRideBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSafeRideBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_safe_ride_title));
            binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_safe_ride_desc));
        }

        boolean isNotified = Prefs.getBoolean("safe_ride_notified", false);
        if (isNotified) {
            binding.btnNotifyMe.setText("✅ You Will Be Notified!");
            binding.btnNotifyMe.setEnabled(false);
        }

        binding.btnNotifyMe.setOnClickListener(v -> {
            Prefs.putBoolean("safe_ride_notified", true);
            binding.btnNotifyMe.setText("✅ You Will Be Notified!");
            binding.btnNotifyMe.setEnabled(false);
            Snackbar.make(view, getString(R.string.safe_ride_notify_success), Snackbar.LENGTH_LONG).show();
        });

        return view;
    }
}
