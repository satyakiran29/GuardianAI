package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSuperAdminHomeBinding;
import com.android.sheguard.ui.activity.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SuperAdminHomeFragment extends Fragment {

    private FragmentSuperAdminHomeBinding binding;
    private final List<JsonObject> allItems = new ArrayList<>();
    private String currentFilter = "all"; // all, user, guardian
    private String currentSearch = "";
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperAdminHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        String adminName = Prefs.getString(Constants.PREFS_USER_NAME, "Chief SuperAdmin");
        binding.tvAdminSubtitle.setText("Console: " + adminName + " • Full Platform Telemetry");

        binding.btnMenu.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleDrawer();
            }
        });

        binding.btnTopProfile.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_superAdminHomeFragment_to_profileFragment);
            } catch (Exception ignored) {}
        });

        binding.btnRefresh.setOnClickListener(v -> loadPlatformData());

        // Setup filter buttons
        binding.btnFilterAll.setOnClickListener(v -> setFilter("all"));
        binding.btnFilterUsers.setOnClickListener(v -> setFilter("user"));
        binding.btnFilterGuardians.setOnClickListener(v -> setFilter("guardian"));

        // Setup search listener
        binding.etSearchDirectory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                renderFilteredList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadPlatformData();

        pollRunnable = () -> {
            loadPlatformData();
            pollHandler.postDelayed(pollRunnable, 20000);
        };
        pollHandler.postDelayed(pollRunnable, 20000);

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
        loadPlatformData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
        binding = null;
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        if (binding == null) return;

        int activeColor = 0xFF4F46E5;
        int inactiveColor = 0xFF1E293B;

        binding.btnFilterAll.setBackgroundColor("all".equals(filter) ? activeColor : inactiveColor);
        binding.btnFilterUsers.setBackgroundColor("user".equals(filter) ? activeColor : inactiveColor);
        binding.btnFilterGuardians.setBackgroundColor("guardian".equals(filter) ? activeColor : inactiveColor);

        renderFilteredList();
    }

    private void loadPlatformData() {
        if (binding == null || getContext() == null) return;

        String adminPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (adminPhone.isEmpty()) {
            adminPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "admin@sheguard.app");
        }

        binding.pbAdminLoading.setVisibility(View.VISIBLE);
        ApiClient.getTrackedWards(adminPhone, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbAdminLoading.setVisibility(View.GONE);

            allItems.clear();
            if (success && data != null && data.has("wards")) {
                JsonArray wards = data.getAsJsonArray("wards");
                for (JsonElement el : wards) {
                    allItems.add(el.getAsJsonObject());
                }
            } else if (!success && Prefs.getBoolean(Constants.IS_DEMO_MODE, false)) {
                loadDemoAdminItems();
            }

            calculateMetricsAndRender();
        });
    }

    private void calculateMetricsAndRender() {
        if (binding == null) return;

        int totalUsers = allItems.size();
        int activeSos = 0;
        int lowBattery = 0;
        int guardiansCount = 0;

        JsonObject activeDistress = null;

        for (JsonObject item : allItems) {
            int battery = item.has("battery_level") ? item.get("battery_level").getAsInt() : 80;
            if (battery <= 15) {
                lowBattery++;
            }

            boolean isSos = item.has("has_active_sos") && item.get("has_active_sos").getAsBoolean();
            if (isSos) {
                activeSos++;
                if (activeDistress == null) {
                    activeDistress = item;
                }
            }

            String rel = item.has("relationship") ? item.get("relationship").getAsString().toLowerCase() : "";
            if (rel.contains("guardian") || rel.contains("protector")) {
                guardiansCount++;
            }
        }

        // Update KPI metrics
        binding.tvStatTotalUsers.setText(String.valueOf(totalUsers));
        binding.tvStatGuardians.setText(String.valueOf(Math.max(guardiansCount, totalUsers > 0 ? 2 : 0)));
        binding.tvStatActiveSos.setText(String.valueOf(activeSos));
        binding.tvStatLowBattery.setText(String.valueOf(lowBattery));

        // Active Emergency Banner
        if (activeDistress != null) {
            binding.layoutAdminDistressCard.setVisibility(View.VISIBLE);
            String distName = activeDistress.has("name") ? activeDistress.get("name").getAsString() : "User in Distress";
            String distAddr = activeDistress.has("address") ? activeDistress.get("address").getAsString() : "Live coordinates dispatched";
            binding.tvAdminSosDesc.setText("🚨 CRITICAL ALERT: " + distName + " has triggered an emergency SOS!\n📍 Last Known Location: " + distAddr);
        } else {
            binding.layoutAdminDistressCard.setVisibility(View.GONE);
        }

        renderFilteredList();
    }

    private void renderFilteredList() {
        if (binding == null || getContext() == null) return;
        binding.layoutAdminDirectoryList.removeAllViews();

        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject item : allItems) {
            String name = item.has("name") ? item.get("name").getAsString().toLowerCase() : "";
            String phone = item.has("phone") ? item.get("phone").getAsString().toLowerCase() : "";
            String email = item.has("email") ? item.get("email").getAsString().toLowerCase() : "";
            String rel = item.has("relationship") ? item.get("relationship").getAsString().toLowerCase() : "";

            boolean matchesSearch = currentSearch.isEmpty() ||
                    name.contains(currentSearch) ||
                    phone.contains(currentSearch) ||
                    email.contains(currentSearch);

            if (!matchesSearch) continue;

            if ("guardian".equals(currentFilter)) {
                if (!rel.contains("guardian") && !rel.contains("protector") && !rel.contains("responder")) {
                    continue;
                }
            } else if ("user".equals(currentFilter)) {
                if (rel.contains("guardian") || rel.contains("protector")) {
                    continue;
                }
            }

            filtered.add(item);
        }

        binding.tvAdminUserCount.setText(filtered.size() + " of " + allItems.size() + " accounts");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (JsonObject item : filtered) {
            View card = inflater.inflate(R.layout.item_admin_user_card, binding.layoutAdminDirectoryList, false);

            TextView tvAvatar = card.findViewById(R.id.tv_user_avatar);
            TextView tvName = card.findViewById(R.id.tv_admin_user_name);
            TextView tvRole = card.findViewById(R.id.tv_admin_user_role_badge);
            TextView tvPhone = card.findViewById(R.id.tv_admin_user_phone);
            TextView tvBattery = card.findViewById(R.id.tv_admin_user_battery);
            TextView tvLocation = card.findViewById(R.id.tv_admin_user_location);

            MaterialButton btnChat = card.findViewById(R.id.btn_admin_chat);
            MaterialButton btnCall = card.findViewById(R.id.btn_admin_call);

            String name = item.has("name") ? item.get("name").getAsString() : "User";
            String phone = item.has("phone") ? item.get("phone").getAsString() : "";
            int battery = item.has("battery_level") ? item.get("battery_level").getAsInt() : 80;
            String address = item.has("address") ? item.get("address").getAsString() : "Location synced";
            String rel = item.has("relationship") ? item.get("relationship").getAsString() : "Citizen";
            boolean isSos = item.has("has_active_sos") && item.get("has_active_sos").getAsBoolean();

            tvName.setText(name);
            tvPhone.setText(phone);
            tvBattery.setText(battery + "%");
            tvLocation.setText("📍 " + address);

            if (rel.toLowerCase().contains("guardian") || rel.toLowerCase().contains("protector")) {
                tvAvatar.setText("🛡️");
                tvRole.setText("Guardian");
                tvRole.setTextColor(0xFF34D399);
            } else {
                tvAvatar.setText(isSos ? "🚨" : "🌸");
                tvRole.setText(isSos ? "SOS Active" : "Protected User");
                tvRole.setTextColor(isSos ? 0xFFEF4444 : 0xFFC084FC);
            }

            if (battery <= 15) {
                tvBattery.setTextColor(0xFFEF4444);
            } else if (battery <= 30) {
                tvBattery.setTextColor(0xFFF59E0B);
            } else {
                tvBattery.setTextColor(0xFF34D399);
            }

            btnChat.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("PARTNER_PHONE", phone);
                args.putString("PARTNER_NAME", name);
                args.putInt("PARTNER_BATTERY", battery);
                try {
                    Navigation.findNavController(v).navigate(R.id.action_superAdminHomeFragment_to_guardianChatFragment, args);
                } catch (Exception ignored) {}
            });

            btnCall.setOnClickListener(v -> {
                if (!phone.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                }
            });

            binding.layoutAdminDirectoryList.addView(card);
        }
    }

    private void loadDemoAdminItems() {
        JsonObject u1 = new JsonObject();
        u1.addProperty("name", "Priya Sharma");
        u1.addProperty("phone", "+919876543210");
        u1.addProperty("email", "priya@example.com");
        u1.addProperty("relationship", "Protected Ward");
        u1.addProperty("battery_level", 78);
        u1.addProperty("address", "Madhapur, Hyderabad");
        u1.addProperty("has_active_sos", false);
        allItems.add(u1);

        JsonObject u2 = new JsonObject();
        u2.addProperty("name", "Rajesh Sharma");
        u2.addProperty("phone", "+919988776655");
        u2.addProperty("email", "rajesh@example.com");
        u2.addProperty("relationship", "Guardian Unit");
        u2.addProperty("battery_level", 94);
        u2.addProperty("address", "Jubilee Hills, Hyderabad");
        u2.addProperty("has_active_sos", false);
        allItems.add(u2);
    }
}
