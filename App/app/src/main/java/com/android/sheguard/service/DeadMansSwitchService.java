package com.android.sheguard.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.util.SosUtil;

import java.util.Locale;

/**
 * Dead Man's Switch — runs as a foreground service so it survives app backgrounding/killing.
 * If the user doesn't check in before the timer expires, auto-triggers SOS.
 * Shows a persistent notification with live countdown and "I'm Safe" cancel action.
 */
public class DeadMansSwitchService extends Service {

    private static final String TAG = "DeadMansSwitch";

    public static final String CHANNEL_ID       = "dead_mans_switch_channel";
    public static final int    NOTIF_ID         = 9001;
    public static final int    NOTIF_WARN_ID    = 9002;

    // Intent actions
    public static final String ACTION_START      = "com.android.sheguard.DMS_START";
    public static final String ACTION_CANCEL     = "com.android.sheguard.DMS_CANCEL";  // "I'm Safe"
    public static final String ACTION_STOP       = "com.android.sheguard.DMS_STOP";

    // Extras
    public static final String EXTRA_DURATION_MS = "duration_ms";

    // Broadcast sent to UI so SafetyTimerFragment can update its display
    public static final String BROADCAST_TICK    = "com.android.sheguard.DMS_TICK";
    public static final String BROADCAST_CANCELLED = "com.android.sheguard.DMS_CANCELLED";
    public static final String BROADCAST_EXPIRED   = "com.android.sheguard.DMS_EXPIRED";
    public static final String EXTRA_MILLIS_LEFT   = "millis_left";

    public static boolean isRunning = false;

    private static final long WARN_THRESHOLD_MS = 30_000L; // 30s warning before auto-SOS
    private boolean warnShown = false;

    private CountDownTimer countDownTimer;
    private long durationMs = 30 * 60 * 1000L;
    private long millisLeft = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ─────────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (action == null) action = ACTION_START;

        switch (action) {
            case ACTION_START:
                durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 30 * 60 * 1000L);
                startCountdown();
                break;

            case ACTION_CANCEL:
                onUserCheckedInSafe();
                break;

            case ACTION_STOP:
                stopSelf();
                break;
        }

        return START_STICKY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void startCountdown() {
        isRunning = true;
        warnShown = false;
        millisLeft = durationMs;

        startForeground(NOTIF_ID, buildNotification(durationMs, false));
        Log.i(TAG, "Dead Man's Switch started: " + durationMs / 60000 + "min");

        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long ms) {
                millisLeft = ms;
                updateNotification(ms, false);
                broadcastTick(ms);

                // 30-second escalation warning
                if (ms <= WARN_THRESHOLD_MS && !warnShown) {
                    warnShown = true;
                    showWarningNotification(ms);
                    Log.w(TAG, "⚠️ 30s warning — SOS imminent!");
                }
            }

            @Override
            public void onFinish() {
                millisLeft = 0;
                onTimerExpired();
            }
        }.start();
    }

    private void onUserCheckedInSafe() {
        Log.i(TAG, "User checked in safe — cancelling Dead Man's Switch");
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;

        // Dismiss both notifications
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_WARN_ID);
        }

        // Broadcast cancel so fragment can reset UI
        Intent broadcast = new Intent(BROADCAST_CANCELLED);
        broadcast.setPackage(getPackageName());
        sendBroadcast(broadcast);

        SosUtil.vibrateDevice(this);
        stopForeground(true);
        stopSelf();
    }

    private void onTimerExpired() {
        isRunning = false;
        Log.e(TAG, "💀 Dead Man's Switch EXPIRED — triggering auto-SOS!");

        // Broadcast expiry so fragment can show EXPIRED state
        Intent broadcast = new Intent(BROADCAST_EXPIRED);
        broadcast.setPackage(getPackageName());
        sendBroadcast(broadcast);

        // 🚨 AUTO SOS — runs on main thread
        handler.post(() -> {
            Prefs.putString(Constants.DMS_TRIGGER_SOURCE, "Dead Man's Switch — auto-triggered (no check-in)");
            SosUtil.activateInstantSosMode(getApplicationContext());
        });

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        super.onDestroy();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Dead Man's Switch",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Persistent safety countdown — auto-triggers SOS if you don't check in");
        channel.setShowBadge(true);
        channel.enableVibration(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(long ms, boolean warning) {
        long minutes = (ms / 1000) / 60;
        long seconds = (ms / 1000) % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        String title = warning
                ? "⚠️ SOS in " + timeStr + " — Check In Now!"
                : "🛡️ Dead Man's Switch Active";
        String body = warning
                ? "No check-in received. SOS will auto-fire in " + timeStr + "!"
                : "Countdown: " + timeStr + " remaining. Tap 'I'm Safe' to cancel.";

        // "I'm Safe" action — calls back to service
        Intent cancelIntent = new Intent(this, DeadMansSwitchService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent cancelPi = PendingIntent.getService(
                this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tap notification → open app
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int color = warning ? 0xFFEF4444 : 0xFF6366F1;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_safety_timer)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(color)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPi)
                .addAction(R.drawable.ic_contacts_safety, "✅ I'm Safe", cancelPi)
                .setPriority(warning ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build();
    }

    private void updateNotification(long ms, boolean warning) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(ms, warning));
    }

    private void showWarningNotification(long ms) {
        // Separate HIGH priority warning notification on top of the persistent one
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_WARN_ID, buildNotification(ms, true));

        // Also update the persistent foreground notification to warning state
        updateNotification(ms, true);
    }

    private void broadcastTick(long ms) {
        Intent tick = new Intent(BROADCAST_TICK);
        tick.setPackage(getPackageName());
        tick.putExtra(EXTRA_MILLIS_LEFT, ms);
        sendBroadcast(tick);
    }
}
