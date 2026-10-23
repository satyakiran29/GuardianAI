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
import com.android.sheguard.databinding.FragmentHelplineBinding;
import com.android.sheguard.model.HelplineModel;
import com.android.sheguard.ui.adapter.HelplineAdapter;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class HelplineFragment extends Fragment {

    private final ArrayList<HelplineModel> helplines = new ArrayList<>();
    private FragmentHelplineBinding binding;
    private HelplineAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHelplineBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.btnBack.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigateUp();
            } catch (Exception e) {
                requireActivity().onBackPressed();
            }
        });

        helplines.clear();
        helplines.add(new HelplineModel(getString(R.string.helpline_name1), getString(R.string.helpline_desc1), getString(R.string.helpline_num1)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name2), getString(R.string.helpline_desc2), getString(R.string.helpline_num2)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name3), getString(R.string.helpline_desc3), getString(R.string.helpline_num3)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name4), getString(R.string.helpline_desc4), getString(R.string.helpline_num4)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name5), getString(R.string.helpline_desc5), getString(R.string.helpline_num5)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name6), getString(R.string.helpline_desc6), getString(R.string.helpline_num6)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name7), getString(R.string.helpline_desc7), getString(R.string.helpline_num7)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name8), getString(R.string.helpline_desc8), getString(R.string.helpline_num8)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name9), getString(R.string.helpline_desc9), getString(R.string.helpline_num9)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name10), getString(R.string.helpline_desc10), getString(R.string.helpline_num10)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name11), getString(R.string.helpline_desc11), getString(R.string.helpline_num11)));
        helplines.add(new HelplineModel(getString(R.string.helpline_name12), getString(R.string.helpline_desc12), getString(R.string.helpline_num12)));

        binding.tvHelplineCount.setText(String.valueOf(helplines.size()));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HelplineAdapter(requireContext(), helplines);
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