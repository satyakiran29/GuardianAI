package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private final List<TeamMember> teamMembers = new ArrayList<>();

    public static class TeamMember {
        final String name;
        final String role;
        final String tagline;
        final int avatarRes;
        final boolean hasExternalLink;
        final String webUrl;
        final String githubUrl;
        final String linkedinUrl;

        public TeamMember(String name, String role, String tagline, int avatarRes,
                          boolean hasExternalLink, String webUrl, String githubUrl, String linkedinUrl) {
            this.name = name;
            this.role = role;
            this.tagline = tagline;
            this.avatarRes = avatarRes;
            this.hasExternalLink = hasExternalLink;
            this.webUrl = webUrl;
            this.githubUrl = githubUrl;
            this.linkedinUrl = linkedinUrl;
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

        binding.tvAppVersion.setText("GuardianAI v" + BuildConfig.VERSION_NAME);

        initTeamList();
        renderTeamMembers(inflater);

        return view;
    }

    private void initTeamList() {
        teamMembers.clear();

        // 1. Pampana Satya Kiran
        teamMembers.add(new TeamMember(
                "Pampana Satya Kiran",
                "Lead Architect & Android Systems",
                "Engineered the core role router, SuperAdmin suite & release pipeline.",
                R.drawable.ic_avatar_male,
                true,
                "http://psatyakiran.in/",
                "https://github.com/satyakiran29",
                "https://www.linkedin.com/in/satyakiran29"
        ));

        // 2. Amarthaluri Harshavardhan
        teamMembers.add(new TeamMember(
                "Amarthaluri Harshavardhan",
                "Android Security & Sensor Engineer",
                "Built background panic triggers, power button listeners & SOS daemon.",
                R.drawable.ic_avatar_male,
                false,
                null,
                "https://github.com",
                "https://www.linkedin.com"
        ));

        // 3. Madeli Narasimha
        teamMembers.add(new TeamMember(
                "Madeli Narasimha",
                "Cloud Backend & Database Architect",
                "Built Django REST APIs, Supabase real-time sync & emergency chat.",
                R.drawable.ic_avatar_male,
                false,
                null,
                "https://github.com",
                "https://www.linkedin.com/in/narasimha-madeli-ba7b73338"
        ));

        // 4. Mammula Sneha
        teamMembers.add(new TeamMember(
                "Mammula Sneha",
                "UI/UX & Safety Systems Designer",
                "Designed empathetic mobile interfaces, safety suites & radar controls.",
                R.drawable.ic_avatar_female,
                true,
                null,
                null,
                "https://www.linkedin.com/in/sneha-mammula-b0651832a/"
        ));

        // 5. Kadagala Meghana
        teamMembers.add(new TeamMember(
                "Kadagala Meghana",
                "Location Telemetry & QA Lead",
                "Ensured high-accuracy GPS telemetry and battle-tested emergency dispatch.",
                R.drawable.ic_avatar_female,
                false,
                null,
                "https://github.com",
                "https://www.linkedin.com"
        ));
    }

    private void renderTeamMembers(LayoutInflater inflater) {
        binding.layoutTeamContainer.removeAllViews();

        for (TeamMember member : teamMembers) {
            ItemDeveloperCardBinding card = ItemDeveloperCardBinding.inflate(inflater, binding.layoutTeamContainer, false);

            card.imgMemberAvatar.setImageResource(member.avatarRes);
            card.tvDevName.setText(member.name);
            card.tvDevRole.setText(member.role);
            card.tvDevTagline.setText(member.tagline);

            if (member.hasExternalLink) {
                card.ivDevLinkIcon.setVisibility(View.VISIBLE);
            } else {
                card.ivDevLinkIcon.setVisibility(View.GONE);
            }

            // Web
            if (member.webUrl != null && !member.webUrl.isEmpty()) {
                card.btnDevWeb.setVisibility(View.VISIBLE);
                card.btnDevWeb.setOnClickListener(v -> openUrl(member.webUrl, member.name));
            } else {
                card.btnDevWeb.setVisibility(View.GONE);
            }

            // GitHub
            if (member.githubUrl != null && !member.githubUrl.isEmpty()) {
                card.btnDevGithub.setVisibility(View.VISIBLE);
                card.btnDevGithub.setOnClickListener(v -> openUrl(member.githubUrl, member.name));
            } else {
                card.btnDevGithub.setVisibility(View.GONE);
            }

            // LinkedIn
            if (member.linkedinUrl != null && !member.linkedinUrl.isEmpty()) {
                card.btnDevLinkedin.setVisibility(View.VISIBLE);
                card.btnDevLinkedin.setOnClickListener(v -> openUrl(member.linkedinUrl, member.name));
            } else {
                card.btnDevLinkedin.setVisibility(View.GONE);
            }

            // Whole card click
            card.cardDeveloper.setOnClickListener(v -> {
                String primaryUrl = member.webUrl != null ? member.webUrl : (member.linkedinUrl != null ? member.linkedinUrl : member.githubUrl);
                if (primaryUrl != null) {
                    openUrl(primaryUrl, member.name);
                } else {
                    Snackbar.make(binding.getRoot(), member.name + " profile coming soon!", Snackbar.LENGTH_SHORT).show();
                }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}