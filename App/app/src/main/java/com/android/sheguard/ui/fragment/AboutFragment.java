package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.BuildConfig;
import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentAboutBinding;
import com.android.sheguard.databinding.ItemDeveloperCardBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;
    private final List<Contributor> contributorList = new ArrayList<>();

    private static class Contributor {
        final String monogram;
        final String name;
        final String role;
        final String tile1Label;
        final String tile1Url;
        final String tile2Label;
        final String tile2Url;
        final String tile3Label;
        final String tile3Url;

        Contributor(String monogram, String name, String role,
                String tile1Label, String tile1Url,
                String tile2Label, String tile2Url,
                String tile3Label, String tile3Url) {
            this.monogram = monogram;
            this.name = name;
            this.role = role;
            this.tile1Label = tile1Label;
            this.tile1Url = tile1Url;
            this.tile2Label = tile2Label;
            this.tile2Url = tile2Url;
            this.tile3Label = tile3Label;
            this.tile3Url = tile3Url;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        // Close / Back button action
        binding.btnCloseCredits.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigateUp();
            } catch (Exception ignored) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        binding.tvAppVersion.setText("guardianai v" + BuildConfig.VERSION_NAME);

        initContributorList();
        renderContributors(inflater);

        // Also randomize on clicking the CREDITS title
        binding.tvAppVersion.setOnClickListener(v -> {
            java.util.Collections.shuffle(contributorList);
            renderContributors(inflater);
        });

        return view;
    }

    private void initContributorList() {
        contributorList.clear();

        // Satya Kiran
        contributorList.add(new Contributor(
                "P",
                "Pampana Satya Kiran",
                "Lead Developer & System Architect",
                "Website", "http://psatyakiran.in/",
                "GitHub", "https://github.com/satyakiran29",
                "LinkedIn", "https://www.linkedin.com/in/satyakiran29"));

        // Narasimha
        contributorList.add(new Contributor(
                "M",
                "Madeli Narasimha",
                "Backend & Cloud Integration",
                "LinkedIn",
                "https://www.linkedin.com/in/narasimha-madeli-ba7b73338?utm_source=share_via&utm_content=profile&utm_medium=member_android",
                "GitHub", "https://github.com",
                "Website", "https://guardianai.app"));

        // Harshavardhan
        contributorList.add(new Contributor(
                "H",
                "Amarthaluri Harshavardhan",
                "Core Android & Security Engineer",
                "LinkedIn", "https://www.linkedin.com",
                "GitHub", "https://github.com",
                "Website", "https://guardianai.app"));

        // Sneha
        contributorList.add(new Contributor(
                "S",
                "Mammula Sneha",
                "UI/UX & Safety Systems",
                "LinkedIn", "https://guardianai.app",
                "Instagram", "https://www.instagram.com",
                "Website", "https://guardianai.app"));

        // Meghana
        contributorList.add(new Contributor(
                "M",
                "Kadagala Meghana",
                "QA & Location Telemetry",
                "LinkedIn", "https://www.linkedin.com",
                "GitHub", "https://github.com",
                "Website", "https://guardianai.app"));

        // Randomize the order each time
        java.util.Collections.shuffle(contributorList);
    }

    private void renderContributors(LayoutInflater inflater) {
        binding.layoutTeamContainer.removeAllViews();

        for (Contributor dev : contributorList) {
            ItemDeveloperCardBinding card = ItemDeveloperCardBinding.inflate(inflater, binding.layoutTeamContainer,
                    false);

            card.tvDevMonogram.setText(dev.monogram);
            card.tvDevName.setText(dev.name);
            card.tvDevRole.setText(dev.role);

            // Bento Tile 1
            card.tvBentoTile1.setText(dev.tile1Label);
            card.btnBentoTile1.setOnClickListener(v -> openUrl(dev.tile1Url, dev.name));

            // Bento Tile 2
            card.tvBentoTile2.setText(dev.tile2Label);
            card.btnBentoTile2.setOnClickListener(v -> openUrl(dev.tile2Url, dev.name));

            // Bento Tile 3
            card.tvBentoTile3.setText(dev.tile3Label);
            card.btnBentoTile3.setOnClickListener(v -> openUrl(dev.tile3Url, dev.name));

            // Tapping card header also re-shuffles
            card.cardDeveloper.setOnClickListener(v -> {
                java.util.Collections.shuffle(contributorList);
                renderContributors(inflater);
            });

            binding.layoutTeamContainer.addView(card.getRoot());
        }
    }

    private void openUrl(String url, String devName) {
        if (url == null || url.isEmpty()) {
            Snackbar.make(binding.getRoot(), devName + " profile coming soon!", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Could not open link", Snackbar.LENGTH_SHORT).show();
        }
    }
}