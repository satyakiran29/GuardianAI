package com.skdev.guardianai;

import android.app.Application;

import com.skdev.guardianai.data.EmergencyContactManager;
import com.skdev.guardianai.data.SafetyDataRepository;
import com.skdev.guardianai.voice.VoiceFeedbackManager;

/**
 * GuardianAI Application Base Class.
 * Initializes singleton managers, embedded safety datasets, and default emergency contacts.
 */
public class GuardianApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Pre-warm data repositories & TTS engine
        SafetyDataRepository.getInstance();
        EmergencyContactManager.getInstance(this);
        VoiceFeedbackManager.getInstance(this);
    }
}
