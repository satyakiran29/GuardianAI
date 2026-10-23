package com.android.sheguard.ui.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

        binding.btnBack.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigateUp();
            } catch (Exception e) {
                requireActivity().onBackPressed();
            }
        });

        binding.btnClearChat.setOnClickListener(v -> resetChat());

        binding.chipQ1.setOnClickListener(v -> handleQuestion("I feel I am being followed"));
        binding.chipQ2.setOnClickListener(v -> handleQuestion("Safety tips for night cab"));
        binding.chipQ3.setOnClickListener(v -> handleQuestion("Key self-defense moves"));
        binding.chipQ4.setOnClickListener(v -> handleQuestion("My legal rights in public"));

        binding.fabSendAi.setOnClickListener(v -> {
            String query = binding.etAiPrompt.getText() != null ? binding.etAiPrompt.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                binding.etAiPrompt.setText("");
                handleQuestion(query);
            }
        });

        return view;
    }

    private void resetChat() {
        if (binding == null) return;
        int childCount = binding.layoutMessagesContainer.getChildCount();
        if (childCount > 1) {
            binding.layoutMessagesContainer.removeViews(1, childCount - 1);
            Toast.makeText(requireContext(), "Conversation cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleQuestion(String query) {
        addUserBubble(query);

        // Add temporary loading indicator bubble
        MaterialCardView loadingCard = addAiBubble("GuardianAI is analyzing your safety situation...");

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
        if (getContext() == null || binding == null) return;

        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(52), 0, 0, dpToPx(12));
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#4F46E5")); // Deep vibrant indigo/purple
        card.setStrokeColor(Color.parseColor("#6366F1"));
        card.setStrokeWidth(dpToPx(1));
        card.setRadius(dpToPx(16));
        card.setCardElevation(0);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        TextView tag = new TextView(requireContext());
        tag.setText("You");
        tag.setTextColor(Color.parseColor("#E0E7FF"));
        tag.setTextSize(11f);
        tag.setGravity(Gravity.END);
        inner.addView(tag);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14.5f);
        tv.setLineSpacing(dpToPx(2), 1.0f);
        inner.addView(tv);

        card.addView(inner);

        binding.layoutMessagesContainer.addView(card);
        binding.scrollAiChat.post(() -> binding.scrollAiChat.fullScroll(View.FOCUS_DOWN));
    }

    private MaterialCardView addAiBubble(String text) {
        if (getContext() == null || binding == null) return null;

        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dpToPx(36), dpToPx(12));
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#16171C"));
        card.setStrokeColor(Color.parseColor("#262833"));
        card.setStrokeWidth(dpToPx(1));
        card.setRadius(dpToPx(16));
        card.setCardElevation(0);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dpToPx(6));

        TextView title = new TextView(requireContext());
        title.setText("GuardianAI Intelligence");
        title.setTextColor(Color.parseColor("#A78BFA"));
        title.setTextSize(11.5f);
        headerRow.addView(title);

        TextView verified = new TextView(requireContext());
        verified.setText("Verified");
        verified.setBackgroundResource(R.drawable.bg_monogram_badge);
        verified.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
        verified.setTextColor(Color.parseColor("#34D399"));
        verified.setTextSize(10f);
        LinearLayout.LayoutParams vParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vParams.setMarginStart(dpToPx(8));
        verified.setLayoutParams(vParams);
        headerRow.addView(verified);

        inner.addView(headerRow);

        TextView tv = new TextView(requireContext());
        tv.setId(android.R.id.text1);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#F8FAFC"));
        tv.setTextSize(14.5f);
        tv.setLineSpacing(dpToPx(3), 1.0f);
        inner.addView(tv);

        card.addView(inner);

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

    private int dpToPx(int dp) {
        if (getContext() == null) return dp * 2;
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private String generateSafetyResponse(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("follow") || lower.contains("chase") || lower.contains("behind")) {
            return "[IMMEDIATE ACTION PLAN: BEING FOLLOWED]\n\n" +
                    "1. Do NOT head home or into deserted lanes.\n" +
                    "2. Cross the street to confirm if you are followed. Turn 4 right corners.\n" +
                    "3. Enter a crowded, well-lit pharmacy, restaurant, or 24/7 bank ATM immediately.\n" +
                    "4. Activate GuardianAI Safe Mode or tap 1-Tap SOS to alert your trusted circle.\n" +
                    "5. Call National Emergency Helpline 112 or Women Helpline 1091 while remaining in a public area.";
        } else if (lower.contains("cab") || lower.contains("taxi") || lower.contains("uber") || lower.contains("rapido")) {
            return "[CAB & RIDE SAFETY CHECKLIST]\n\n" +
                    "1. Verify vehicle number plate and driver photo before boarding.\n" +
                    "2. Test inside door locks to ensure child-lock is disengaged.\n" +
                    "3. Use GuardianAI Trip Monitoring to log vehicle details and share live telemetry.\n" +
                    "4. Make an audible call stating: 'I am in vehicle [Number], arriving in 15 minutes.'\n" +
                    "5. Sit directly behind the driver's seat to maintain tactical line-of-sight.";
        } else if (lower.contains("defense") || lower.contains("fight") || lower.contains("move") || lower.contains("protect")) {
            return "[HIGH-IMPACT DEFENSE TARGETS]\n\n" +
                    "• Eyes: Rapid gouge or palm swipe.\n" +
                    "• Nose: Upward open-palm strike to the base of the nasal bone.\n" +
                    "• Throat & Windpipe: Direct strike with knuckles or elbow.\n" +
                    "• Groin: Explosive upward knee strike.\n" +
                    "• Feet: Forceful heel stamp directly onto opponent's toes.\n\n" +
                    "Key Principle: Self-defense creates an immediate 5-second escape window to sprint to safety.";
        } else if (lower.contains("right") || lower.contains("law") || lower.contains("police") || lower.contains("legal")) {
            return "[KEY LEGAL RIGHTS & SAFETY ACTS]\n\n" +
                    "1. Zero FIR: You can file an FIR at ANY police station across India, regardless of jurisdiction.\n" +
                    "2. Right to Privacy: Medical examination requires explicit informed consent.\n" +
                    "3. Emergency Police Response: Dial 112, 1091 (Women Helpline), or 181 (Domestic Abuse).\n" +
                    "4. Free Legal Aid: Entitled to state legal defense representation under Legal Services Authorities Act.";
        } else {
            return "[GUARDIAN AI SAFETY ADVICE]\n\n" +
                    "• Stay in illuminated areas and trust your personal instincts.\n" +
                    "• Keep phone battery charged and enable 1-Tap SOS and Safe Mode.\n" +
                    "• If you sense imminent danger, activate Emergency Siren to draw instant public attention.";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
