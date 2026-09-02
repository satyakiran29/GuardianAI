package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentAiAssistantBinding;
import com.google.android.material.card.MaterialCardView;

public class AiAssistantFragment extends Fragment {

    private FragmentAiAssistantBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiAssistantBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.chipQ1.setOnClickListener(v -> handleQuestion("🚨 I feel I'm being followed"));
        binding.chipQ2.setOnClickListener(v -> handleQuestion("🚕 Safety tips for night cab"));
        binding.chipQ3.setOnClickListener(v -> handleQuestion("🥋 Key self-defense moves"));
        binding.chipQ4.setOnClickListener(v -> handleQuestion("⚖️ My legal rights in public"));

        binding.fabSendAi.setOnClickListener(v -> {
            String query = binding.etAiPrompt.getText() != null ? binding.etAiPrompt.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                binding.etAiPrompt.setText("");
                handleQuestion(query);
            }
        });

        return view;
    }

    private void handleQuestion(String query) {
        addUserBubble(query);

        // Add temporary loading indicator bubble
        MaterialCardView loadingCard = addAiBubble("⚡ GuardianAI (Groq LLaMA 3.3) is analyzing your safety situation...");

        String lang = com.android.sheguard.util.LocaleUtil.getLanguage();
        String languageName = com.android.sheguard.util.LocaleUtil.getLanguageName(requireContext());

        com.android.sheguard.util.GroqAiUtil.askGroq(query, languageName + " (" + lang + ")", new com.android.sheguard.util.GroqAiUtil.GroqCallback() {
            @Override
            public void onSuccess(String response) {
                if (binding == null) return;
                updateAiBubbleText(loadingCard, response);
                binding.scrollAiChat.post(() -> binding.scrollAiChat.fullScroll(View.FOCUS_DOWN));
            }

            @Override
            public void onError(String errorMessage) {
                if (binding == null) return;
                // Fallback to local offline safety database if network or Groq API is unavailable
                String fallback = generateSafetyResponse(query);
                updateAiBubbleText(loadingCard, fallback);
                binding.scrollAiChat.post(() -> binding.scrollAiChat.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void addUserBubble(String text) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(64, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(0xFF6366F1);
        card.setRadius(32);

        TextView tv = new TextView(requireContext());
        tv.setPadding(28, 20, 28, 20);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(15);
        card.addView(tv);

        binding.layoutMessagesContainer.addView(card);
        binding.scrollAiChat.post(() -> binding.scrollAiChat.fullScroll(View.FOCUS_DOWN));
    }

    private MaterialCardView addAiBubble(String text) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 64, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(getResources().getColor(R.color.surface_card, requireContext().getTheme()));
        card.setStrokeColor(0xFF6366F1);
        card.setStrokeWidth(2);
        card.setRadius(32);

        TextView tv = new TextView(requireContext());
        tv.setId(android.R.id.text1);
        tv.setPadding(28, 20, 28, 20);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.textColorPrimary, requireContext().getTheme()));
        tv.setTextSize(15);
        card.addView(tv);

        binding.layoutMessagesContainer.addView(card);
        binding.scrollAiChat.post(() -> binding.scrollAiChat.fullScroll(View.FOCUS_DOWN));
        return card;
    }

    private void updateAiBubbleText(MaterialCardView card, String text) {
        if (card != null) {
            TextView tv = card.findViewById(android.R.id.text1);
            if (tv != null) {
                tv.setText(text);
            }
        }
    }

    private String generateSafetyResponse(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("follow") || lower.contains("chase") || lower.contains("behind")) {
            return "🚨 **IMMEDIATE ACTION PLAN: BEING FOLLOWED**\n\n" +
                    "1. **Do NOT head home** or into isolated lanes.\n" +
                    "2. **Cross the street** to see if they follow your path. Make 4 right turns.\n" +
                    "3. **Enter a crowded, well-lit store, restaurant, or bank ATM** immediately.\n" +
                    "4. **Activate GuardianAI Safe Mode** or tap 1-Tap SOS to send your live GPS to your guardians.\n" +
                    "5. **Call emergency helpline 109 / 112 / 999** while remaining in a public area.";
        } else if (lower.contains("cab") || lower.contains("taxi") || lower.contains("uber") || lower.contains("rapido")) {
            return "🚕 **CAB & RIDE SAFETY CHECKLIST**\n\n" +
                    "1. **Match number plates & driver photo** before getting in.\n" +
                    "2. **Check the child-lock**: Ensure doors open from inside.\n" +
                    "3. **Use GuardianAI Trip Monitoring**: Enter vehicle number and share live GPS telemetry with family.\n" +
                    "4. **Pretend you are on a phone call** stating: *\"I'm in the cab with plate number [XYZ], reaching in 15 mins.\"*\n" +
                    "5. Sit directly behind the driver's seat so they cannot reach back.";
        } else if (lower.contains("defense") || lower.contains("fight") || lower.contains("move") || lower.contains("protect")) {
            return "🥋 **HIGH-IMPACT VULNERABLE TARGETS FOR DEFENSE**\n\n" +
                    "• **Eyes**: Quick gouge / swipe.\n" +
                    "• **Nose**: Upward open-palm strike to the base of the nose.\n" +
                    "• **Throat / Windpipe**: Target with fingers or elbow.\n" +
                    "• **Groin**: Strong knee strike upwards.\n" +
                    "• **Shins / Foot Stomp**: Heavy heel stamp on opponent's toes.\n\n" +
                    "💡 *Remember: Self-defense is strictly to create a 5-second window to sprint to safety!*";
        } else if (lower.contains("right") || lower.contains("law") || lower.contains("police") || lower.contains("legal")) {
            return "⚖️ **KEY SAFETY & LEGAL RIGHTS (PROTECTION ACTS)**\n\n" +
                    "1. **Zero FIR**: You can register an FIR at ANY police station regardless of where the incident occurred.\n" +
                    "2. **Right to Privacy**: Medical examination of survivors can only be conducted with consent.\n" +
                    "3. **Emergency Police Response**: Dial **112 / 999 / 109** for instant dispatch.\n" +
                    "4. **Free Legal Aid**: Victims are entitled to free state legal counsel.";
        } else {
            return "🛡️ **GuardianAI Advice**:\n\n" +
                    "• Stay in well-lit areas and trust your instincts.\n" +
                    "• Keep your phone charged and enable **1-Tap SOS** and **Safe Mode**.\n" +
                    "• If you feel an immediate threat, trigger the **Emergency Siren 🔊** to attract crowd attention immediately.";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
