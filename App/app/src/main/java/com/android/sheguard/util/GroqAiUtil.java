package com.android.sheguard.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.sheguard.common.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroqAiUtil {

    private static final String TAG = "GroqAiUtil";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GroqCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public static void askGroq(String userPrompt, String languageCode, GroqCallback callback) {
        if (Constants.GROQ_API_KEY == null || Constants.GROQ_API_KEY.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(getOfflineSafetyAdvice(userPrompt, languageCode)));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(Constants.GROQ_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + Constants.GROQ_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);

                String systemPrompt = "You are GuardianAI, an advanced, empathetic, and ultra-responsive personal safety & emergency defense AI. "
                        + "Your mission is to protect the user from danger, harassment, stalking, unsafe cab rides, and medical/physical threats. "
                        + "Always provide clear, actionable, high-priority step-by-step guidance. Use bullet points and appropriate emojis for readability. "
                        + "Always reply in the user's requested language (" + (languageCode != null ? languageCode : "English") + ").";

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", userPrompt);

                JSONArray messages = new JSONArray();
                messages.put(systemMessage);
                messages.put(userMessage);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", Constants.GROQ_MODEL);
                jsonBody.put("messages", messages);
                jsonBody.put("temperature", 0.6);
                jsonBody.put("max_tokens", 850);

                byte[] postData = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject responseJson = new JSONObject(responseBuilder.toString());
                    JSONArray choices = responseJson.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject messageObj = firstChoice.getJSONObject("message");
                        String aiContent = messageObj.getString("content").trim();

                        mainHandler.post(() -> callback.onSuccess(aiContent));
                        return;
                    }
                }

                String errMessage = "Groq HTTP " + responseCode + ": " + responseBuilder;
                Log.e(TAG, errMessage);
                mainHandler.post(() -> callback.onError(errMessage));

            } catch (Exception e) {
                Log.e(TAG, "Groq API error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private static String getOfflineSafetyAdvice(String prompt, String lang) {
        String lower = prompt != null ? prompt.toLowerCase() : "";
        if (lower.contains("stalk") || lower.contains("follow") || lower.contains("behind")) {
            return "🛡️ **GuardianAI Emergency Guidance: Being Followed / Stalked**\n\n"
                    + "1. **Do NOT head directly home**: Head into a well-lit public space (store, restaurant, or metro station).\n"
                    + "2. **Change your pace & cross the street**: This confirms if someone is intentionally following you.\n"
                    + "3. **Trigger Safe Mode or Fake Call**: Use the Fake Call button on GuardianAI to speak loudly.\n"
                    + "4. **Broadcast your location**: Use 1-Tap SOS to send your live GPS link to your trusted contacts.\n"
                    + "5. **Make eye contact with nearby people**: Alert store clerks or security personnel immediately.";
        } else if (lower.contains("cab") || lower.contains("taxi") || lower.contains("driver") || lower.contains("ride")) {
            return "🚖 **GuardianAI Trip Safety & Cab Security Protocol**\n\n"
                    + "1. **Verify vehicle match**: Confirm license plate number, car model, and driver identity before entering.\n"
                    + "2. **Activate Safe Route / Safe Arrival**: Share your live tracking link via WhatsApp with family.\n"
                    + "3. **Check child locks**: Ensure rear door handles can open freely from the inside.\n"
                    + "4. **Sit directly behind driver**: This offers the best physical safety and vantage point.\n"
                    + "5. **If route diverts**: Ask loudly about the detour and prepare to press the 1-Tap SOS button.";
        } else if (lower.contains("defend") || lower.contains("attack") || lower.contains("fight") || lower.contains("physical")) {
            return "🥋 **Self-Defense Emergency Tactics**\n\n"
                    + "1. **Target vulnerable areas**: Eyes, nose, throat, groin, and shins offer maximum escape leverage.\n"
                    + "2. **Use voice as a weapon**: Yell 'FIRE!' or 'HELP!' loudly to draw maximum bystander attention.\n"
                    + "3. **Improvised tools**: Use keys between fingers, heavy bags, pens, or pepper spray.\n"
                    + "4. **Run immediately**: Create distance as soon as an opening presents itself and seek public shelter.";
        } else {
            return "🤖 **GuardianAI Safety Advisor Active**\n\n"
                    + "• **Instant SOS**: Press and hold the big SOS button for emergency alert dispatch.\n"
                    + "• **Safe Mode**: Activates power-saving and broadcasts live GPS location pins.\n"
                    + "• **Fake Call**: Simulates urgent incoming calls to escape uncomfortable social situations.\n"
                    + "• **Safety Timer**: Set check-in countdowns when traveling alone or at night.\n\n"
                    + "Stay calm, stay alert, and trust your instincts. GuardianAI is watching your back 24/7.";
        }
    }
}
