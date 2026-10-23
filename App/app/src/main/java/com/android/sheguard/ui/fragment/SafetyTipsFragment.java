package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentSafetyTipsBinding;
import com.android.sheguard.ui.adapter.SafetyTipsAdapter;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class SafetyTipsFragment extends Fragment {

    private final ArrayList<String> safetyTips = new ArrayList<>();
    private FragmentSafetyTipsBinding binding;
    private SafetyTipsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSafetyTipsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.btnBack.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigateUp();
            } catch (Exception e) {
                requireActivity().onBackPressed();
            }
        });

        safetyTips.clear();
        safetyTips.add(getString(R.string.safety_tips_tip_1));
        safetyTips.add(getString(R.string.safety_tips_tip_2));
        safetyTips.add(getString(R.string.safety_tips_tip_3));
        safetyTips.add(getString(R.string.safety_tips_tip_4));
        safetyTips.add(getString(R.string.safety_tips_tip_5));
        safetyTips.add(getString(R.string.safety_tips_tip_6));
        safetyTips.add(getString(R.string.safety_tips_tip_7));
        safetyTips.add(getString(R.string.safety_tips_tip_8));
        safetyTips.add(getString(R.string.safety_tips_tip_9));
        safetyTips.add(getString(R.string.safety_tips_tip_10));
        safetyTips.add(getString(R.string.safety_tips_tip_11));
        safetyTips.add(getString(R.string.safety_tips_tip_12));
        safetyTips.add(getString(R.string.safety_tips_tip_13));
        safetyTips.add(getString(R.string.safety_tips_tip_14));
        safetyTips.add(getString(R.string.safety_tips_tip_15));
        safetyTips.add(getString(R.string.safety_tips_tip_16));
        safetyTips.add(getString(R.string.safety_tips_tip_17));
        safetyTips.add(getString(R.string.safety_tips_tip_18));
        safetyTips.add(getString(R.string.safety_tips_tip_19));
        safetyTips.add(getString(R.string.safety_tips_tip_20));
        safetyTips.add(getString(R.string.safety_tips_tip_21));

        binding.tvTipsCount.setText(String.valueOf(safetyTips.size()));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SafetyTipsAdapter(requireContext(), safetyTips);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}