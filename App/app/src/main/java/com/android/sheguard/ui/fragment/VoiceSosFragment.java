package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentVoiceSosBinding;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class VoiceSosFragment extends Fragment {

    private FragmentVoiceSosBinding binding;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVoiceSosBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.btnToggleVoiceSos.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                startListening();
            }
        });

        return view;
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Snackbar.make(binding.getRoot(), "Speech recognition not available on this device", Snackbar.LENGTH_SHORT).show();
            return;
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    if (isListening) {
                        try {
                            speechRecognizer.startListening(intent);
                        } catch (Exception ignored) {}
                    }
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null) {
                        for (String phrase : matches) {
                            String lower = phrase.toLowerCase();
                            if (lower.contains("help") || lower.contains("sos") || lower.contains("save me") || lower.contains("emergency")) {
                                triggerVoiceSos();
                                return;
                            }
                        }
                    }
                    if (isListening) {
                        try {
                            speechRecognizer.startListening(intent);
                        } catch (Exception ignored) {}
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            speechRecognizer.startListening(intent);
            isListening = true;
            binding.btnToggleVoiceSos.setText(getString(R.string.voice_sos_stop_listening));
            binding.btnToggleVoiceSos.setBackgroundColor(0xFFEF4444);
            binding.tvVoiceStatus.setText(getString(R.string.voice_sos_listening_active));
            SosUtil.vibrateDevice(requireContext());
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Microphone permission required", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void stopListening() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        binding.btnToggleVoiceSos.setText(getString(R.string.voice_sos_start_listening));
        binding.btnToggleVoiceSos.setBackgroundColor(0xFFEC4899);
        binding.tvVoiceStatus.setText("Tap below to activate voice detection");
    }

    private void triggerVoiceSos() {
        stopListening();
        SosUtil.vibrateDevice(requireContext());
        SosUtil.playSiren(requireContext());
        SosUtil.activateInstantSosMode(requireContext());
        Snackbar.make(binding.getRoot(), getString(R.string.voice_sos_keyword_detected), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        stopListening();
        super.onDestroyView();
        binding = null;
    }
}
