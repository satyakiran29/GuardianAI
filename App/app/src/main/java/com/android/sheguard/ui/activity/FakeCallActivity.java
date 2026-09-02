package com.android.sheguard.ui.activity;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sheguard.R;
import com.android.sheguard.databinding.ActivityFakeCallBinding;
import com.android.sheguard.util.LocaleUtil;

public class FakeCallActivity extends AppCompatActivity {

    private ActivityFakeCallBinding binding;
    private Ringtone ringtone;
    private Vibrator vibrator;
    private Handler handler;
    private int callDurationSeconds = 0;
    private Runnable timerRunnable;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtil.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFakeCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Wake screen on lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        String callerName = getIntent().getStringExtra("caller_name");
        if (callerName != null && !callerName.isEmpty()) {
            binding.tvCallerName.setText(callerName);
        }

        handler = new Handler(Looper.getMainLooper());
        startRinging();

        binding.btnDecline.setOnClickListener(v -> endCall());
        binding.fabDecline.setOnClickListener(v -> endCall());
        binding.fabEndCall.setOnClickListener(v -> endCall());

        binding.btnAccept.setOnClickListener(v -> answerCall());
        binding.fabAccept.setOnClickListener(v -> answerCall());
    }

    private void startRinging() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) {
                ringtone.play();
            }

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopRinging() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {
        }
    }

    private void answerCall() {
        stopRinging();

        binding.layoutIncomingActions.setVisibility(View.GONE);
        binding.layoutInCall.setVisibility(View.VISIBLE);
        binding.layoutEndCall.setVisibility(View.VISIBLE);

        callDurationSeconds = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                callDurationSeconds++;
                binding.tvCallStatus.setText(getString(R.string.fake_call_active, callDurationSeconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void endCall() {
        stopRinging();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        stopRinging();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
        super.onDestroy();
    }
}
