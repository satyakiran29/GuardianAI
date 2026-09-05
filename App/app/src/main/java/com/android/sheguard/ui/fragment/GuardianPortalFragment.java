package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.android.sheguard.databinding.FragmentGuardianPortalBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GuardianPortalFragment extends Fragment {

    private FragmentGuardianPortalBinding binding;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardianPortalBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnRefresh.setOnClickListener(v -> loadTrackedWards());

        binding.btnLinkNewWard.setOnClickListener(v -> showLinkWardDialog());

        String role = Prefs.getString("USER_ROLE", "user");
        if ("guardian".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role)) {
            loadTrackedWards();

            // Auto poll ward telemetry every 15 seconds
            pollRunnable = () -> {
                loadTrackedWards();
                pollHandler.postDelayed(pollRunnable, 15000);
            };
            pollHandler.postDelayed(pollRunnable, 15000);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String role = Prefs.getString("USER_ROLE", "user");
        if (!"guardian".equalsIgnoreCase(role) && !"superadmin".equalsIgnoreCase(role)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Guardian Portal is reserved for verified Guardians.", Toast.LENGTH_SHORT).show();
            }
            Navigation.findNavController(view).popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void loadTrackedWards() {
        String role = Prefs.getString("USER_ROLE", "user");
        if (!"guardian".equalsIgnoreCase(role) && !"superadmin".equalsIgnoreCase(role)) {
            return;
        }

        String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (myPhone.isEmpty()) {
            myPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "");
        }

        binding.pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getTrackedWards(myPhone, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("wards")) {
                JsonArray wards = data.getAsJsonArray("wards");
                renderWardsList(wards);
            } else if (!success && Prefs.getBoolean(Constants.IS_DEMO_MODE, false)) {
                renderMockDemoWardsIfEmpty();
            } else {
                renderWardsList(new JsonArray());
            }
        });
    }

    private void renderWardsList(JsonArray wards) {
        binding.layoutWardsList.removeAllViews();

        if (wards == null || wards.size() == 0) {
            binding.layoutEmptyWards.setVisibility(View.VISIBLE);
            binding.tvWardsCountSummary.setText("0 Protected Wards Active");
            return;
        }

        binding.layoutEmptyWards.setVisibility(View.GONE);
        binding.tvWardsCountSummary.setText("Tracking " + wards.size() + " Protected Ward(s)");

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (JsonElement el : wards) {
            JsonObject ward = el.getAsJsonObject();
            String name = ward.has("name") ? ward.get("name").getAsString() : "Protected User";
            String phone = ward.has("phone") ? ward.get("phone").getAsString() : "";
            String relationship = ward.has("relationship") ? ward.get("relationship").getAsString() : "Family";
            int battery = ward.has("battery_level") ? ward.get("battery_level").getAsInt() : 85;
            String address = ward.has("address") ? ward.get("address").getAsString() : "Location Pending";
            double lat = ward.has("latitude") && !ward.get("latitude").isJsonNull() ? ward.get("latitude").getAsDouble() : 17.3850;
            double lng = ward.has("longitude") && !ward.get("longitude").isJsonNull() ? ward.get("longitude").getAsDouble() : 78.4867;
            boolean hasSos = ward.has("has_active_sos") && ward.get("has_active_sos").getAsBoolean();

            View card = inflater.inflate(R.layout.item_guardian_ward_card, binding.layoutWardsList, false);

            TextView tvName = card.findViewById(R.id.tv_ward_name);
            TextView tvRel = card.findViewById(R.id.tv_ward_relationship);
            TextView tvPhone = card.findViewById(R.id.tv_ward_phone);
            TextView tvBattery = card.findViewById(R.id.tv_ward_battery);
            TextView tvBatteryIcon = card.findViewById(R.id.tv_battery_icon);
            TextView tvAddress = card.findViewById(R.id.tv_ward_address);
            TextView tvCoords = card.findViewById(R.id.tv_ward_coords);
            View sosBanner = card.findViewById(R.id.layout_ward_sos_banner);

            tvName.setText("🌸 " + name);
            tvRel.setText(relationship);
            tvPhone.setText("📞 " + phone);
            tvBattery.setText(battery + "%");
            tvAddress.setText(address);
            tvCoords.setText(String.format("%.4f, %.4f • Live Sync", lat, lng));

            if (battery <= 15) {
                tvBatteryIcon.setText("🪫");
                tvBattery.setTextColor(0xFFEF4444);
            } else if (battery <= 30) {
                tvBatteryIcon.setText("🔋");
                tvBattery.setTextColor(0xFFFBBF24);
            } else {
                tvBatteryIcon.setText("🔋");
                tvBattery.setTextColor(0xFF34D399);
            }

            if (hasSos) {
                sosBanner.setVisibility(View.VISIBLE);
            } else {
                sosBanner.setVisibility(View.GONE);
            }

            // Buttons
            MaterialButton btnReplay = card.findViewById(R.id.btn_ward_replay);
            MaterialButton btnChat = card.findViewById(R.id.btn_ward_chat);
            MaterialButton btnMap = card.findViewById(R.id.btn_ward_map);
            MaterialButton btnCall = card.findViewById(R.id.btn_ward_call);

            if (btnReplay != null) {
                btnReplay.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("WARD_PHONE", phone);
                    args.putString("WARD_NAME", name);
                    args.putDouble("WARD_LAT", lat);
                    args.putDouble("WARD_LNG", lng);
                    Navigation.findNavController(v).navigate(R.id.action_guardianPortalFragment_to_locationReplayFragment, args);
                });
            }

            btnChat.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("PARTNER_PHONE", phone);
                args.putString("PARTNER_NAME", name);
                args.putInt("PARTNER_BATTERY", battery);
                Navigation.findNavController(v).navigate(R.id.action_guardianPortalFragment_to_guardianChatFragment, args);
            });

            btnMap.setOnClickListener(v -> {
                String geoUri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(name + " Location") + ")";
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
                startActivity(mapIntent);
            });

            btnCall.setOnClickListener(v -> {
                if (!phone.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(callIntent);
                }
            });

            binding.layoutWardsList.addView(card);
        }
    }

    private void renderMockDemoWardsIfEmpty() {
        if (binding.layoutWardsList.getChildCount() > 0) return;

        JsonArray demoArray = new JsonArray();

        JsonObject w1 = new JsonObject();
        w1.addProperty("name", "Priya Sharma");
        w1.addProperty("phone", "+919876543210");
        w1.addProperty("relationship", "Daughter");
        w1.addProperty("battery_level", 82);
        w1.addProperty("address", "Madhapur, Hitech City, Hyderabad");
        w1.addProperty("latitude", 17.4482);
        w1.addProperty("longitude", 78.3914);
        w1.addProperty("has_active_sos", false);
        demoArray.add(w1);

        JsonObject w2 = new JsonObject();
        w2.addProperty("name", "Ananya Verma");
        w2.addProperty("phone", "+919876543211");
        w2.addProperty("relationship", "Sister");
        w2.addProperty("battery_level", 14);
        w2.addProperty("address", "Gachibowli Stadium Road, Hyderabad");
        w2.addProperty("latitude", 17.4435);
        w2.addProperty("longitude", 78.3512);
        w2.addProperty("has_active_sos", false);
        demoArray.add(w2);

        renderWardsList(demoArray);
    }

    private void showLinkWardDialog() {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        EditText etPhone = new EditText(getContext());
        etPhone.setHint("Protected User Phone (+91...)");
        layout.addView(etPhone);

        EditText etName = new EditText(getContext());
        etName.setHint("User Name (Optional)");
        layout.addView(etName);

        EditText etRelationship = new EditText(getContext());
        etRelationship.setHint("Relationship (e.g. Daughter, Sister, Friend)");
        layout.addView(etRelationship);

        new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialComponents_MaterialAlertDialog)
                .setTitle("🛡️ Link Protected Ward")
                .setView(layout)
                .setPositiveButton("Link Ward", (dialog, which) -> {
                    String wardPhone = etPhone.getText().toString().trim();
                    String wardName = etName.getText().toString().trim();
                    String rel = etRelationship.getText().toString().trim();
                    String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "+919999999999");
                    String myName = Prefs.getString(Constants.PREFS_USER_NAME, "Guardian Unit");

                    if (wardPhone.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiClient.linkGuardian(wardPhone, myPhone, myName, rel.isEmpty() ? "Family" : rel, (success, data, message) -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "🎉 Ward linked successfully!", Toast.LENGTH_SHORT).show();
                            loadTrackedWards();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
