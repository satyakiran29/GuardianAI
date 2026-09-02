package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentAboutBinding;
import com.android.sheguard.databinding.ItemDeveloperCardBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;
    private final List<Developer> developerList = new ArrayList<>();

    private static class Developer {
        final int nameRes;
        final int roleRes;
        final String url;
        final boolean isHighlight;

        Developer(int nameRes, int roleRes, String url, boolean isHighlight) {
            this.nameRes = nameRes;
            this.roleRes = roleRes;
            this.url = url;
            this.isHighlight = isHighlight;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_about_title));
            binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_about_desc));
        }

        initDeveloperList();
        renderDevelopers(inflater);

        binding.btnShuffleTeam.setOnClickListener(v -> {
            renderDevelopers(inflater);
            Snackbar.make(binding.getRoot(), "🔀 Team order randomized!", Snackbar.LENGTH_SHORT).show();
        });

        return view;
    }

    private void initDeveloperList() {
        developerList.clear();
        developerList.add(new Developer(R.string.team_member_1_name, R.string.team_member_1_role, "http://psatyakiran.in/", true));
        developerList.add(new Developer(R.string.team_member_2_name, R.string.team_member_2_role, null, false));
        developerList.add(new Developer(R.string.team_member_3_name, R.string.team_member_3_role, null, false));
        developerList.add(new Developer(R.string.team_member_4_name, R.string.team_member_4_role, null, false));
        developerList.add(new Developer(R.string.team_member_5_name, R.string.team_member_5_role, null, false));
    }

    private void renderDevelopers(LayoutInflater inflater) {
        Collections.shuffle(developerList);
        binding.layoutTeamContainer.removeAllViews();

        for (Developer dev : developerList) {
            ItemDeveloperCardBinding cardBinding = ItemDeveloperCardBinding.inflate(inflater, binding.layoutTeamContainer, false);
            cardBinding.tvDevName.setText(getString(dev.nameRes));
            cardBinding.tvDevRole.setText(getString(dev.roleRes));

            if (dev.url != null && !dev.url.isEmpty()) {
                cardBinding.tvDevUrl.setVisibility(View.VISIBLE);
                cardBinding.tvDevUrl.setText("🌐 " + dev.url);
                cardBinding.ivDevArrow.setVisibility(View.VISIBLE);
                cardBinding.cardDeveloper.setStrokeWidth(3);
                cardBinding.cardDeveloper.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dev.url)));
                    } catch (Exception ignored) {
                    }
                });
            } else {
                cardBinding.tvDevUrl.setVisibility(View.GONE);
                cardBinding.ivDevArrow.setVisibility(View.GONE);
                cardBinding.cardDeveloper.setStrokeWidth(0);
                cardBinding.cardDeveloper.setOnClickListener(v -> {
                    renderDevelopers(inflater);
                    Snackbar.make(binding.getRoot(), "🔀 Randomized: " + getString(dev.nameRes), Snackbar.LENGTH_SHORT).show();
                });
            }

            binding.layoutTeamContainer.addView(cardBinding.getRoot());
        }
    }
}