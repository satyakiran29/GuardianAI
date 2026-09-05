package com.android.sheguard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BatteryManager;
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
import com.android.sheguard.databinding.FragmentGuardianHomeBinding;
import com.android.sheguard.util.LocationHelper;
import com.android.sheguard.ui.activity.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GuardianHomeFragment extends Fragment {

    private FragmentGuardianHomeBinding binding;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardianHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        String guardianName = Prefs.getString(Constants.PREFS_USER_NAME, "Guardian Unit");
        String guardianPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        binding.tvGuardianSubtitle.setText("Active: " + guardianName + (guardianPhone.isEmpty() ? "" : " • " + guardianPhone));

        binding.btnMenu.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleDrawer();
            }
        });

        binding.btnTopProfile.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_guardianHomeFragment_to_profileFragment);
            } catch (Exception ignored) {}
        });

        binding.btnRefresh.setOnClickListener(v -> loadTrackedWards());
        binding.btnLinkNewWard.setOnClickListener(v -> showLinkWardDialog());
        binding.btnShareGuardianLocation.setOnClickListener(v -> pingGuardianLocation());

        // Empty state buttons
        binding.btnEmptyLinkWard.setOnClickListener(v -> showLinkWardDialog());
        binding.btnEmptySampleWards.setOnClickListener(v -> renderMockDemoWards());

        loadTrackedWards();

        // Auto-refresh telemetry every 15 seconds
        pollRunnable = () -> {
            loadTrackedWards();
            pollHandler.postDelayed(pollRunnable, 15000);
        };
        pollHandler.postDelayed(pollRunnable, 15000);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrackedWards();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        binding = null;
    }

    private void loadTrackedWards() {
        if (binding == null || getContext() == null) return;

        String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        String myEmail = Prefs.getString(Constants.PREFS_USER_EMAIL, "");
        String identifier = !myPhone.isEmpty() ? myPhone : (!myEmail.isEmpty() ? myEmail : "+919988776655");

        binding.pbLoading.setVisibility(View.VISIBLE);
        ApiClient.getTrackedWards(identifier, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("wards")) {
                JsonArray wards = data.getAsJsonArray("wards");
                if (wards.size() > 0) {
                    renderWardsList(wards);
                } else if (Prefs.getBoolean(Constants.IS_DEMO_MODE, false) || "+919988776655".equals(identifier) || "guardian@sheguard.app".equalsIgnoreCase(identifier)) {
                    renderMockDemoWards();
                } else {
                    renderWardsList(wards);
                }
            } else if (Prefs.getBoolean(Constants.IS_DEMO_MODE, false) || "+919988776655".equals(identifier) || "guardian@sheguard.app".equalsIgnoreCase(identifier)) {
                renderMockDemoWards();
            } else {
                renderWardsList(new JsonArray());
            }
        });
    }

    private void renderWardsList(JsonArray wards) {
        if (binding == null || getContext() == null) return;
        binding.layoutWardsList.removeAllViews();

        if (wards == null || wards.size() == 0) {
            binding.layoutEmptyWards.setVisibility(View.VISIBLE);
            binding.tvGuardianWardsSummary.setText("0 Protected Wards Active");
            binding.layoutCriticalSosBanner.setVisibility(View.GONE);
            return;
        }

        binding.layoutEmptyWards.setVisibility(View.GONE);
        binding.tvGuardianWardsSummary.setText("Tracking " + wards.size() + " Assigned Ward(s)");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        boolean hasDistress = false;
        JsonObject distressWard = null;

        for (JsonElement el : wards) {
            JsonObject ward = el.getAsJsonObject();
            String name = ward.has("name") ? ward.get("name").getAsString() : "Protected User";
            String phone = ward.has("phone") ? ward.get("phone").getAsString() : "";
            String relationship = ward.has("relationship") ? ward.get("relationship").getAsString() : "Ward";
            int battery = ward.has("battery_level") ? ward.get("battery_level").getAsInt() : 80;
            String address = ward.has("address") ? ward.get("address").getAsString() : "Location synced";
            double lat = ward.has("latitude") && !ward.get("latitude").isJsonNull() ? ward.get("latitude").getAsDouble() : 17.4482;
            double lng = ward.has("longitude") && !ward.get("longitude").isJsonNull() ? ward.get("longitude").getAsDouble() : 78.3914;
            boolean isSos = ward.has("has_active_sos") && ward.get("has_active_sos").getAsBoolean();

            if (isSos && !hasDistress) {
                hasDistress = true;
                distressWard = ward;
            }

            View card = inflater.inflate(R.layout.item_guardian_ward_card, binding.layoutWardsList, false);

            TextView tvName = card.findViewById(R.id.tv_ward_name);
            TextView tvRel = card.findViewById(R.id.tv_ward_relationship);
            TextView tvPhone = card.findViewById(R.id.tv_ward_phone);
            TextView tvBattery = card.findViewById(R.id.tv_ward_battery);
            TextView tvBatteryIcon = card.findViewById(R.id.tv_battery_icon);
            TextView tvAddress = card.findViewById(R.id.tv_ward_address);
            TextView tvCoords = card.findViewById(R.id.tv_ward_coords);
            View sosBanner = card.findViewById(R.id.layout_ward_sos_banner);

            MaterialButton btnReplay = card.findViewById(R.id.btn_ward_replay);
            MaterialButton btnChat = card.findViewById(R.id.btn_ward_chat);
            MaterialButton btnCall = card.findViewById(R.id.btn_ward_call);
            MaterialButton btnLocate = card.findViewById(R.id.btn_ward_map);

            if (btnReplay != null) {
                btnReplay.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("WARD_PHONE", phone);
                    args.putString("WARD_NAME", name);
                    args.putDouble("WARD_LAT", lat);
                    args.putDouble("WARD_LNG", lng);
                    try {
                        Navigation.findNavController(v).navigate(R.id.action_guardianHomeFragment_to_locationReplayFragment, args);
                    } catch (Exception ignored) {}
                });
            }

            tvName.setText(name);
            tvRel.setText(relationship);
            tvPhone.setText(phone);
            tvBattery.setText(battery + "%");
            tvAddress.setText("📍 " + address);
            tvCoords.setText(String.format("GPS: %.4f, %.4f", lat, lng));

            if (battery <= 15) {
                tvBattery.setTextColor(0xFFEF4444);
                tvBatteryIcon.setTextColor(0xFFEF4444);
            } else if (battery <= 30) {
                tvBattery.setTextColor(0xFFF59E0B);
                tvBatteryIcon.setTextColor(0xFFF59E0B);
            } else {
                tvBattery.setTextColor(0xFF34D399);
                tvBatteryIcon.setTextColor(0xFF34D399);
            }

            if (isSos) {
                sosBanner.setVisibility(View.VISIBLE);
                card.setBackgroundResource(R.drawable.bg_card_safety_dark);
            } else {
                sosBanner.setVisibility(View.GONE);
            }

            btnChat.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("PARTNER_PHONE", phone);
                args.putString("PARTNER_NAME", name);
                args.putInt("PARTNER_BATTERY", battery);
                try {
                    Navigation.findNavController(v).navigate(R.id.action_guardianHomeFragment_to_guardianChatFragment, args);
                } catch (Exception ignored) {}
            });

            btnCall.setOnClickListener(v -> {
                if (!phone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            });

            btnLocate.setOnClickListener(v -> {
                String uriStr = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(name + " Location") + ")";
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
                mapIntent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Intent webMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + lat + "," + lng));
                    startActivity(webMap);
                }
            });

            binding.layoutWardsList.addView(card);
        }

        // Active Emergency SOS Alert Card at top
        if (hasDistress && distressWard != null) {
            binding.layoutCriticalSosBanner.setVisibility(View.VISIBLE);
            String distName = distressWard.has("name") ? distressWard.get("name").getAsString() : "Your Ward";
            String distPhone = distressWard.has("phone") ? distressWard.get("phone").getAsString() : "";
            double dLat = distressWard.has("latitude") && !distressWard.get("latitude").isJsonNull() ? distressWard.get("latitude").getAsDouble() : 17.4482;
            double dLng = distressWard.has("longitude") && !distressWard.get("longitude").isJsonNull() ? distressWard.get("longitude").getAsDouble() : 78.3914;

            binding.tvSosAlertDesc.setText("🚨 CRITICAL: " + distName + " has activated emergency SOS distress! Immediate escort required.");
            binding.btnEmergencyCall.setOnClickListener(v -> {
                if (!distPhone.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + distPhone)));
                }
            });
            binding.btnEmergencyTrack.setOnClickListener(v -> {
                Intent webMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + dLat + "," + dLng));
                startActivity(webMap);
            });
            binding.btnEmergencyReplay.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("WARD_PHONE", distPhone);
                args.putString("WARD_NAME", distName);
                args.putDouble("WARD_LAT", dLat);
                args.putDouble("WARD_LNG", dLng);
                try {
                    Navigation.findNavController(v).navigate(R.id.action_guardianHomeFragment_to_locationReplayFragment, args);
                } catch (Exception ignored) {}
            });
        } else {
            binding.layoutCriticalSosBanner.setVisibility(View.GONE);
        }
    }

    private void renderMockDemoWards() {
        JsonArray demo = new JsonArray();

        JsonObject w1 = new JsonObject();
        w1.addProperty("name", "Priya Sharma");
        w1.addProperty("phone", "+919876543210");
        w1.addProperty("relationship", "Daughter");
        w1.addProperty("battery_level", 82);
        w1.addProperty("address", "Madhapur, Hitech City, Hyderabad");
        w1.addProperty("latitude", 17.4482);
        w1.addProperty("longitude", 78.3914);
        w1.addProperty("has_active_sos", false);
        demo.add(w1);

        JsonObject w2 = new JsonObject();
        w2.addProperty("name", "SK User (Protected Citizen)");
        w2.addProperty("phone", "+919100000001");
        w2.addProperty("relationship", "Family Member");
        w2.addProperty("battery_level", 14);
        w2.addProperty("address", "Gachibowli Stadium Road, Hyderabad");
        w2.addProperty("latitude", 17.4430);
        w2.addProperty("longitude", 78.3570);
        w2.addProperty("has_active_sos", false);
        demo.add(w2);

        renderWardsList(demo);
    }

    private void pingGuardianLocation() {
        if (getContext() == null) return;
        if (!LocationHelper.hasLocationPermission(requireContext())) {
            Toast.makeText(getContext(), "Location permission required to share GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "📡 Syncing live GPS coordinates...", Toast.LENGTH_SHORT).show();
        LocationHelper.requestSingleLocationUpdate(requireContext(), new LocationHelper.LocationResultListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String addressName) {
                if (getContext() == null) return;
                String phone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
                int battery = 90;
                try {
                    BatteryManager bm = (BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
                    if (bm != null) {
                        battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    }
                } catch (Exception ignored) {}

                if (!phone.isEmpty()) {
                    ApiClient.pingLocation(phone, latitude, longitude, addressName, battery);
                    Toast.makeText(getContext(), "✅ Live GPS & " + battery + "% battery synced to cloud!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLocationError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "GPS update failed: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLinkWardDialog() {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etPhone = new EditText(getContext());
        etPhone.setHint("Protected User Phone (e.g. +919876543210)");
        etPhone.setTextColor(0xFFFFFFFF);
        etPhone.setHintTextColor(0xFF94A3B8);
        layout.addView(etPhone);

        final EditText etName = new EditText(getContext());
        etName.setHint("Ward Name (Optional)");
        etName.setTextColor(0xFFFFFFFF);
        etName.setHintTextColor(0xFF94A3B8);
        layout.addView(etName);

        final EditText etRel = new EditText(getContext());
        etRel.setHint("Relationship (e.g. Daughter, Sister)");
        etRel.setTextColor(0xFFFFFFFF);
        etRel.setHintTextColor(0xFF94A3B8);
        layout.addView(etRel);

        new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialComponents_MaterialAlertDialog)
                .setTitle("🛡️ Link Protected Ward")
                .setView(layout)
                .setPositiveButton("Link Ward", (dialog, which) -> {
                    String wardPhone = etPhone.getText().toString().trim();
                    String wardName = etName.getText().toString().trim();
                    String rel = etRel.getText().toString().trim();

                    if (wardPhone.isEmpty()) {
                        Toast.makeText(getContext(), "Ward phone number is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
                    if (myPhone.isEmpty()) {
                        myPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "+919988776655");
                    }
                    String myName = Prefs.getString(Constants.PREFS_USER_NAME, "Guardian");

                    ApiClient.linkGuardian(wardPhone, myPhone, myName, rel.isEmpty() ? "Protected User" : rel, (success, data, message) -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), success ? "🎉 Ward linked successfully!" : message, Toast.LENGTH_LONG).show();
                            if (success) {
                                loadTrackedWards();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
