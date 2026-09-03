package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentMyGuardiansBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MyGuardiansFragment extends Fragment {

    private FragmentMyGuardiansBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyGuardiansBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnAddGuardian.setOnClickListener(v -> showAddGuardianDialog());

        loadMyGuardians();

        return view;
    }

    private void loadMyGuardians() {
        String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (myPhone.isEmpty()) {
            myPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "");
        }

        binding.pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getMyGuardians(myPhone, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("links")) {
                JsonArray links = data.getAsJsonArray("links");
                renderGuardiansList(links);
            } else {
                renderDemoGuardiansIfEmpty();
            }
        });
    }

    private void renderGuardiansList(JsonArray links) {
        binding.layoutGuardiansList.removeAllViews();

        if (links == null || links.size() == 0) {
            binding.layoutEmptyGuardians.setVisibility(View.VISIBLE);
            return;
        }

        binding.layoutEmptyGuardians.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (JsonElement el : links) {
            JsonObject item = el.getAsJsonObject();
            String gName = item.has("guardian_name") ? item.get("guardian_name").getAsString() : "Guardian";
            String gPhone = item.has("guardian_phone") ? item.get("guardian_phone").getAsString() : "";
            String relationship = item.has("relationship") ? item.get("relationship").getAsString() : "Family";

            View card = inflater.inflate(R.layout.item_guardian_link_card, binding.layoutGuardiansList, false);

            TextView tvName = card.findViewById(R.id.tv_guardian_name);
            TextView tvRel = card.findViewById(R.id.tv_guardian_relationship);
            TextView tvPhone = card.findViewById(R.id.tv_guardian_phone);
            MaterialButton btnChat = card.findViewById(R.id.btn_chat_guardian);
            MaterialButton btnCall = card.findViewById(R.id.btn_call_guardian);

            tvName.setText("🛡️ " + gName);
            tvRel.setText(relationship);
            tvPhone.setText("📞 " + gPhone);

            btnChat.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("PARTNER_PHONE", gPhone);
                args.putString("PARTNER_NAME", gName);
                args.putInt("PARTNER_BATTERY", 90);
                Navigation.findNavController(v).navigate(R.id.action_myGuardiansFragment_to_guardianChatFragment, args);
            });

            btnCall.setOnClickListener(v -> {
                if (!gPhone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + gPhone));
                    startActivity(intent);
                }
            });

            binding.layoutGuardiansList.addView(card);
        }
    }

    private void renderDemoGuardiansIfEmpty() {
        if (binding.layoutGuardiansList.getChildCount() > 0) return;

        JsonArray demoArray = new JsonArray();

        JsonObject g1 = new JsonObject();
        g1.addProperty("guardian_name", "Rajesh Sharma");
        g1.addProperty("guardian_phone", "+919876500001");
        g1.addProperty("relationship", "Father / Primary");
        demoArray.add(g1);

        JsonObject g2 = new JsonObject();
        g2.addProperty("guardian_name", "SHE Team Responder (Madhapur Unit)");
        g2.addProperty("guardian_phone", "1091");
        g2.addProperty("relationship", "Police Escort");
        demoArray.add(g2);

        renderGuardiansList(demoArray);
    }

    private void showAddGuardianDialog() {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        EditText etPhone = new EditText(getContext());
        etPhone.setHint("Guardian Phone Number (+91...)");
        layout.addView(etPhone);

        EditText etName = new EditText(getContext());
        etName.setHint("Guardian Name (e.g. Dad, Mom, Brother)");
        layout.addView(etName);

        EditText etRelationship = new EditText(getContext());
        etRelationship.setHint("Relationship (e.g. Family, Friend, Escort)");
        layout.addView(etRelationship);

        new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialComponents_MaterialAlertDialog)
                .setTitle("🛡️ Add Trusted Guardian")
                .setView(layout)
                .setPositiveButton("Add Guardian", (dialog, which) -> {
                    String gPhone = etPhone.getText().toString().trim();
                    String gName = etName.getText().toString().trim();
                    String rel = etRelationship.getText().toString().trim();
                    String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "+919999999999");

                    if (gPhone.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiClient.linkGuardian(myPhone, gPhone, gName, rel.isEmpty() ? "Family" : rel, (success, data, message) -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "🎉 Guardian linked successfully!", Toast.LENGTH_SHORT).show();
                            loadMyGuardians();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
