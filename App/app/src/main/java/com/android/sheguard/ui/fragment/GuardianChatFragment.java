package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.android.sheguard.databinding.FragmentGuardianChatBinding;
import com.android.sheguard.util.SosUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GuardianChatFragment extends Fragment {

    private FragmentGuardianChatBinding binding;
    private String partnerPhone = "";
    private String partnerName = "Safety Partner";
    private int partnerBattery = 85;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardianChatBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        if (getArguments() != null) {
            partnerPhone = getArguments().getString("PARTNER_PHONE", "");
            partnerName = getArguments().getString("PARTNER_NAME", "Safety Partner");
            partnerBattery = getArguments().getInt("PARTNER_BATTERY", 85);
        }

        binding.tvChatPartnerName.setText(partnerName);
        binding.tvChatPartnerStatus.setText("🔋 Battery: " + partnerBattery + "% • Online & Synced");

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.btnCallPartner.setOnClickListener(v -> {
            if (!partnerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + partnerPhone));
                startActivity(intent);
            }
        });

        // Quick Preset Safety Chips
        binding.chipImSafe.setOnClickListener(v -> sendMessage("✅ I have reached safely and everything is okay!", false));
        binding.chipCallMe.setOnClickListener(v -> sendMessage("📞 Please call me now; urgent update!", true));
        binding.chipSendLocation.setOnClickListener(v -> {
            double lat = Prefs.getFloat(Constants.PREFS_LAST_LATITUDE, 0f);
            double lng = Prefs.getFloat(Constants.PREFS_LAST_LONGITUDE, 0f);
            String locationUrl;
            if (lat != 0 && lng != 0) {
                locationUrl = "https://maps.google.com/?q=" + lat + "," + lng;
            } else {
                String fromSos = SosUtil.getLiveLocationUrl();
                locationUrl = fromSos.isEmpty() ? "https://maps.google.com" : fromSos;
            }
            sendMessage("📍 Live GPS Location:\n" + locationUrl, false);
        });
        binding.chipLowBattery.setOnClickListener(v -> {
            int myBattery = Prefs.getInt(Constants.PREFS_BATTERY_LEVEL, 15);
            sendMessage("🪫 Alert: My phone battery is at " + myBattery + "%! Tracking may disconnect soon.", true);
        });

        binding.btnSendChat.setOnClickListener(v -> {
            String msg = binding.etChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg, false);
                binding.etChatInput.setText("");
            }
        });

        loadChatMessages();

        // Auto poll chat messages every 4 seconds
        pollRunnable = () -> {
            loadChatMessages();
            pollHandler.postDelayed(pollRunnable, 4000);
        };
        pollHandler.postDelayed(pollRunnable, 4000);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void sendMessage(String text, boolean isSos) {
        String myPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (myPhone.isEmpty()) {
            myPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "+919999999999");
        }
        int battery = Prefs.getInt(Constants.PREFS_BATTERY_LEVEL, 85);
        double lat = Prefs.getFloat(Constants.PREFS_LAST_LATITUDE, 17.3850f);
        double lng = Prefs.getFloat(Constants.PREFS_LAST_LONGITUDE, 78.4867f);

        addMessageBubble(text, true, "Just now", isSos, battery);

        ApiClient.sendChatMessage(myPhone, partnerPhone, text, isSos, battery, lat, lng, (success, data, message) -> {
            if (success) {
                loadChatMessages();
            }
        });
    }

    private void loadChatMessages() {
        String phoneVal = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (phoneVal.isEmpty()) {
            phoneVal = Prefs.getString(Constants.PREFS_USER_EMAIL, "+919999999999");
        }
        final String myPhone = phoneVal;

        ApiClient.getChatMessages(myPhone, partnerPhone, (success, data, message) -> {
            if (binding == null || getContext() == null) return;

            if (success && data != null && data.has("messages")) {
                JsonArray messages = data.getAsJsonArray("messages");
                renderMessages(messages, myPhone);
            }
        });
    }

    private void renderMessages(JsonArray messages, String myPhone) {
        if (messages == null) return;

        binding.layoutChatMessages.removeAllViews();

        for (JsonElement el : messages) {
            JsonObject m = el.getAsJsonObject();
            String senderPhone = m.has("sender_phone") ? m.get("sender_phone").getAsString() : "";
            String text = m.has("message") ? m.get("message").getAsString() : "";
            String time = m.has("formatted_time") ? m.get("formatted_time").getAsString() : "";
            boolean isSos = m.has("is_sos") && m.get("is_sos").getAsBoolean();
            int battery = m.has("battery_level") && !m.get("battery_level").isJsonNull() ? m.get("battery_level").getAsInt() : 0;

            boolean isMe = senderPhone.equals(myPhone);
            addMessageBubble(text, isMe, time, isSos, battery);
        }

        binding.scrollChatMessages.post(() -> binding.scrollChatMessages.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessageBubble(String text, boolean isMe, String time, boolean isSos, int battery) {
        if (getContext() == null || binding == null) return;

        LinearLayout bubbleWrapper = new LinearLayout(getContext());
        bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
        bubbleWrapper.setGravity(isMe ? Gravity.END : Gravity.START);
        bubbleWrapper.setPadding(0, 6, 0, 6);

        LinearLayout bubble = new LinearLayout(getContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(28, 18, 28, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(28);

        if (isMe) {
            bg.setColor(Color.parseColor("#7C3AED")); // Purple Sent Bubble
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.END;
            lp.setMargins(100, 0, 0, 0);
            bubble.setLayoutParams(lp);
        } else {
            bg.setColor(Color.parseColor("#1E293B")); // Dark Midnight Received Bubble
            bg.setStroke(2, Color.parseColor("#334155"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.START;
            lp.setMargins(0, 0, 100, 0);
            bubble.setLayoutParams(lp);
        }
        bubble.setBackground(bg);

        if (isSos) {
            TextView tvSos = new TextView(getContext());
            tvSos.setText("🚨 [URGENT EMERGENCY ALERT]");
            tvSos.setTextColor(Color.parseColor("#EF4444"));
            tvSos.setTextSize(11f);
            tvSos.setTypeface(null, android.graphics.Typeface.BOLD);
            bubble.addView(tvSos);
        }

        TextView tvMsg = new TextView(getContext());
        tvMsg.setText(text);
        tvMsg.setTextColor(Color.WHITE);
        tvMsg.setTextSize(14f);
        bubble.addView(tvMsg);

        LinearLayout metaRow = new LinearLayout(getContext());
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.END);
        metaRow.setPadding(0, 6, 0, 0);

        if (battery > 0) {
            TextView tvBat = new TextView(getContext());
            tvBat.setText("🔋 " + battery + "%  ");
            tvBat.setTextColor(Color.parseColor("#94A3B8"));
            tvBat.setTextSize(10f);
            metaRow.addView(tvBat);
        }

        TextView tvTime = new TextView(getContext());
        tvTime.setText(time);
        tvTime.setTextColor(Color.parseColor("#CBD5E1"));
        tvTime.setTextSize(10f);
        metaRow.addView(tvTime);

        bubble.addView(metaRow);
        bubbleWrapper.addView(bubble);

        binding.layoutChatMessages.addView(bubbleWrapper);
    }
}
