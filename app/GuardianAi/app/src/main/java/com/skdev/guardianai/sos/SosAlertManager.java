package com.skdev.guardianai.sos;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.skdev.guardianai.data.EmergencyContact;
import com.skdev.guardianai.data.EmergencyContactManager;
import com.skdev.guardianai.voice.VoiceFeedbackManager;

import java.util.List;
import java.util.Locale;

/**
 * Orchestrates full Emergency SOS response:
 * 1. High-decibel dual-frequency synthetic siren generator
 * 2. Multi-contact SMS dispatch with live GPS coordinates & Maps URL
 * 3. Emergency phone call launcher
 * 4. Text-To-Speech audio confirmation
 * 5. Emergency haptic vibration
 */
public class SosAlertManager {

    private static final String TAG = "SosAlertManager";
    private static SosAlertManager instance;
    private final Context context;
    private boolean isSosActive = false;
    private AudioTrack sirenTrack;
    private Thread sirenThread;
    private volatile boolean isSirenRunning = false;

    private SosAlertManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SosAlertManager getInstance(Context context) {
        if (instance == null) {
            instance = new SosAlertManager(context);
        }
        return instance;
    }

    public boolean isSosActive() {
        return isSosActive;
    }

    /**
     * Executes the complete Emergency SOS sequence.
     */
    public void triggerEmergencySos(double latitude, double longitude, String localityName) {
        isSosActive = true;

        // 1. Play high-decibel synthetic alarm siren
        startEmergencySiren();

        // 2. Vibrate phone with SOS pattern (... --- ...)
        triggerSosVibration();

        // 3. Dispatch SMS to all registered emergency contacts
        EmergencyContactManager contactManager = EmergencyContactManager.getInstance(context);
        List<EmergencyContact> contacts = contactManager.getContacts();
        dispatchEmergencySms(contacts, latitude, longitude, localityName);

        // 4. Voice confirmation
        VoiceFeedbackManager.getInstance(context).announceSosTriggered(contacts.size());
    }

    /**
     * Synthesizes and plays a continuous emergency police/ambulance siren in a background thread.
     */
    public synchronized void startEmergencySiren() {
        if (isSirenRunning) return;
        isSirenRunning = true;

        sirenThread = new Thread(() -> {
            int sampleRate = 44100;
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            try {
                sirenTrack = new AudioTrack(AudioManager.STREAM_ALARM,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                        AudioTrack.MODE_STREAM);

                sirenTrack.play();
                short[] buffer = new short[bufferSize];

                double phase = 0;
                double time = 0;

                while (isSirenRunning) {
                    for (int i = 0; i < bufferSize; i++) {
                        // Dual tone sweep between 600Hz and 1200Hz every 0.6 seconds
                        double freq = 750 + 450 * Math.sin(2 * Math.PI * 1.6 * time);
                        phase += 2 * Math.PI * freq / sampleRate;
                        if (phase > 2 * Math.PI) phase -= 2 * Math.PI;

                        buffer[i] = (short) (Math.sin(phase) * Short.MAX_VALUE * 0.85);
                        time += 1.0 / sampleRate;
                    }
                    sirenTrack.write(buffer, 0, bufferSize);
                }
            } catch (Exception e) {
                Log.e(TAG, "Siren audio generation error: " + e.getMessage());
            } finally {
                if (sirenTrack != null) {
                    try {
                        sirenTrack.stop();
                        sirenTrack.release();
                    } catch (Exception ignored) {}
                    sirenTrack = null;
                }
            }
        });
        sirenThread.start();
    }

    public synchronized void stopEmergencySiren() {
        isSirenRunning = false;
        if (sirenThread != null) {
            sirenThread.interrupt();
            sirenThread = null;
        }
    }

    public void stopSos() {
        isSosActive = false;
        stopEmergencySiren();
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.cancel();
        }
        VoiceFeedbackManager.getInstance(context).speak("Emergency SOS cancelled. System returned to standby mode.");
    }

    private void triggerSosVibration() {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // SOS pattern: 3 short, 3 long, 3 short
                long[] pattern = {0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200};
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration exception: " + e.getMessage());
        }
    }

    /**
     * Broadcasts emergency SMS to all contacts.
     */
    private void dispatchEmergencySms(List<EmergencyContact> contacts, double lat, double lng, String locality) {
        String mapsUrl = String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", lat, lng);
        String message = String.format(Locale.US,
                "🚨 EMERGENCY SOS FROM GUARDIANAI!\nI need immediate assistance at %s.\nMy Live Location:\n%s",
                (locality != null ? locality : "Current Location"), mapsUrl);

        int sentCount = 0;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (EmergencyContact contact : contacts) {
                if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().trim().isEmpty()) {
                    try {
                        smsManager.sendTextMessage(contact.getPhoneNumber().trim(), null, message, null, null);
                        sentCount++;
                    } catch (Exception e) {
                        Log.w(TAG, "SMS send failed to " + contact.getPhoneNumber() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SMS Manager failed: " + e.getMessage());
        }

        Toast.makeText(context, "SOS Alert sent to " + sentCount + " emergency contacts!", Toast.LENGTH_LONG).show();
    }

    /**
     * Launches phone dialer to call primary contact or 112.
     */
    public void launchEmergencyCall(Context activityContext) {
        EmergencyContact primary = EmergencyContactManager.getInstance(context).getPrimaryContact();
        String number = (primary != null && primary.getPhoneNumber() != null) ? primary.getPhoneNumber() : "112";
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityContext.startActivity(intent);
    }
}
