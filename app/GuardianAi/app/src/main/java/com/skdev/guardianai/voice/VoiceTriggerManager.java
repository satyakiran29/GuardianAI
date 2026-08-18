package com.skdev.guardianai.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Continuous / On-demand Voice Keyword Trigger Detector for Emergency SOS.
 * Listens for critical words: "help", "emergency", "danger", "bachao", "save me", "police".
 */
public class VoiceTriggerManager {

    private static final String TAG = "VoiceTriggerManager";

    public interface VoiceTriggerListener {
        void onKeywordDetected(String matchedKeyword);
        void onListeningStateChanged(boolean isListening);
        void onError(String message);
    }

    private static final List<String> TRIGGER_KEYWORDS = Arrays.asList(
            // English
            "help", "emergency", "danger", "save me", "police", "threat", "attack", "sos",
            // Hindi
            "बचाओ", "मदद", "खतरा", "आपातकाल", "bachao", "madad", "khatra",
            // Telugu
            "కాపాడండి", "సహాయం", "ప్రమాదం", "పోలీస్", "kapadandi", "sahayam", "pramadam"
    );

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private VoiceTriggerListener listener;
    private boolean isListening = false;
    private boolean isManualStop = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VoiceTriggerManager(Context context) {
        this.context = context.getApplicationContext();
        initRecognizer();
    }

    public void setListener(VoiceTriggerListener listener) {
        this.listener = listener;
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition is not available on this device");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Log.w(TAG, "Speech error code: " + error);
                // Restart listening automatically if still active and not manually stopped
                if (isListening && !isManualStop) {
                    mainHandler.postDelayed(() -> {
                        if (isListening && !isManualStop) {
                            startListening();
                        }
                    }, 1000);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String text : matches) {
                        Log.d(TAG, "Recognized text: " + text);
                        String matchedWord = checkTriggerWords(text);
                        if (matchedWord != null) {
                            Log.i(TAG, "EMERGENCY KEYWORD TRIGGERED: " + matchedWord);
                            if (listener != null) {
                                listener.onKeywordDetected(matchedWord);
                            }
                            break;
                        }
                    }
                }

                // Continue continuous listening if active
                if (isListening && !isManualStop) {
                    mainHandler.postDelayed(() -> {
                        if (isListening && !isManualStop) {
                            startListening();
                        }
                    }, 500);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String text : matches) {
                        String matchedWord = checkTriggerWords(text);
                        if (matchedWord != null) {
                            Log.i(TAG, "EMERGENCY KEYWORD PARTIAL TRIGGER: " + matchedWord);
                            if (listener != null) {
                                listener.onKeywordDetected(matchedWord);
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    private String checkTriggerWords(String spokenText) {
        if (spokenText == null) return null;
        String clean = spokenText.toLowerCase(Locale.ROOT);
        for (String trigger : TRIGGER_KEYWORDS) {
            if (clean.contains(trigger)) {
                return trigger;
            }
        }
        return null;
    }

    public synchronized void startListening() {
        if (speechRecognizer == null) {
            initRecognizer();
        }
        isManualStop = false;
        isListening = true;
        try {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(recognizerIntent);
            }
            if (listener != null) {
                listener.onListeningStateChanged(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start speech recognizer: " + e.getMessage());
        }
    }

    public synchronized void stopListening() {
        isManualStop = true;
        isListening = false;
        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
            }
            if (listener != null) {
                listener.onListeningStateChanged(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recognizer: " + e.getMessage());
        }
    }

    public boolean isListening() {
        return isListening;
    }

    public void destroy() {
        stopListening();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
