package com.android.sheguard.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.databinding.DialogOtpVerificationBinding;

import java.util.Locale;

public class OtpVerificationDialog {

    public interface OnOtpVerifiedListener {
        void onVerified(String target, String otpCode);
    }

    private final Context context;
    private Dialog dialog;
    private DialogOtpVerificationBinding binding;
    private CountDownTimer countDownTimer;
    private String currentTarget = "";
    private OnOtpVerifiedListener listener;

    public OtpVerificationDialog(@NonNull Context context) {
        this.context = context;
        initDialog();
    }

    private void initDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        binding = DialogOtpVerificationBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        setupOtpInputBoxes();

        binding.btnCancelOtp.setOnClickListener(v -> dismiss());

        binding.btnDemoFillOtp.setOnClickListener(v -> {
            fillOtp("123456");
            verifyEnteredOtp();
        });

        binding.btnVerifyOtp.setOnClickListener(v -> verifyEnteredOtp());

        binding.btnResendOtp.setOnClickListener(v -> {
            startTimer(60000);
            Toast.makeText(context, "New 6-digit OTP sent to " + currentTarget, Toast.LENGTH_SHORT).show();
            // Call backend send-otp
            ApiClient.sendOtp(currentTarget, "verification", null);
        });
    }

    private void setupOtpInputBoxes() {
        binding.etOtp1.addTextChangedListener(new AutoFocusWatcher(binding.etOtp1, binding.etOtp2, null));
        binding.etOtp2.addTextChangedListener(new AutoFocusWatcher(binding.etOtp2, binding.etOtp3, binding.etOtp1));
        binding.etOtp3.addTextChangedListener(new AutoFocusWatcher(binding.etOtp3, binding.etOtp4, binding.etOtp2));
        binding.etOtp4.addTextChangedListener(new AutoFocusWatcher(binding.etOtp4, binding.etOtp5, binding.etOtp3));
        binding.etOtp5.addTextChangedListener(new AutoFocusWatcher(binding.etOtp5, binding.etOtp6, binding.etOtp4));
        binding.etOtp6.addTextChangedListener(new AutoFocusWatcher(binding.etOtp6, null, binding.etOtp5));

        // Enter key triggers verification
        binding.etOtp6.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                verifyEnteredOtp();
                return true;
            }
            return false;
        });
    }

    public void show(String target, String purpose, OnOtpVerifiedListener listener) {
        this.currentTarget = target;
        this.listener = listener;

        if (target != null && target.contains("@")) {
            binding.tvOtpSubtitle.setText(String.format("Enter the 6-digit code sent from security@psatyakiran.in to %s", target));
        } else {
            binding.tvOtpSubtitle.setText(String.format("Enter the 6-digit verification code sent to %s", target));
        }
        clearOtpBoxes();
        startTimer(60000);

        // Async trigger backend OTP dispatch
        ApiClient.sendOtp(target, purpose != null ? purpose : "verification", (success, otpCode) -> {
            if (success && otpCode != null && !otpCode.isEmpty()) {
                // Hint demo users
                String prefix = (target != null && target.contains("@")) ? "📧 Email OTP sent to " : "📱 SMS OTP sent to ";
                Toast.makeText(context, prefix + target + " (Code: " + otpCode + ")", Toast.LENGTH_LONG).show();
            }
        });

        dialog.show();
        binding.etOtp1.requestFocus();
    }

    public void dismiss() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void clearOtpBoxes() {
        binding.etOtp1.setText("");
        binding.etOtp2.setText("");
        binding.etOtp3.setText("");
        binding.etOtp4.setText("");
        binding.etOtp5.setText("");
        binding.etOtp6.setText("");
    }

    private void fillOtp(String code) {
        if (code != null && code.length() == 6) {
            binding.etOtp1.setText(String.valueOf(code.charAt(0)));
            binding.etOtp2.setText(String.valueOf(code.charAt(1)));
            binding.etOtp3.setText(String.valueOf(code.charAt(2)));
            binding.etOtp4.setText(String.valueOf(code.charAt(3)));
            binding.etOtp5.setText(String.valueOf(code.charAt(4)));
            binding.etOtp6.setText(String.valueOf(code.charAt(5)));
        }
    }

    private void verifyEnteredOtp() {
        String code = getEnteredOtp();
        if (code.length() < 6) {
            Toast.makeText(context, "Please enter all 6 digits of the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnVerifyOtp.setEnabled(false);
        binding.btnVerifyOtp.setText("Verifying...");

        ApiClient.verifyOtp(currentTarget, code, (success, message) -> {
            binding.btnVerifyOtp.setEnabled(true);
            binding.btnVerifyOtp.setText("Verify & Continue");

            if (success || "123456".equals(code)) {
                Toast.makeText(context, "✅ OTP Verified Successfully!", Toast.LENGTH_SHORT).show();
                dismiss();
                if (listener != null) {
                    listener.onVerified(currentTarget, code);
                }
            } else {
                Toast.makeText(context, message != null ? message : "Invalid OTP Code. Use 123456 for demo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getEnteredOtp() {
        return (binding.etOtp1.getText() != null ? binding.etOtp1.getText().toString().trim() : "") +
                (binding.etOtp2.getText() != null ? binding.etOtp2.getText().toString().trim() : "") +
                (binding.etOtp3.getText() != null ? binding.etOtp3.getText().toString().trim() : "") +
                (binding.etOtp4.getText() != null ? binding.etOtp4.getText().toString().trim() : "") +
                (binding.etOtp5.getText() != null ? binding.etOtp5.getText().toString().trim() : "") +
                (binding.etOtp6.getText() != null ? binding.etOtp6.getText().toString().trim() : "");
    }

    private void startTimer(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        binding.tvResendLabel.setVisibility(View.VISIBLE);
        binding.tvOtpTimer.setVisibility(View.VISIBLE);
        binding.btnResendOtp.setVisibility(View.GONE);

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                binding.tvOtpTimer.setText(String.format(Locale.getDefault(), "00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                binding.tvResendLabel.setVisibility(View.GONE);
                binding.tvOtpTimer.setVisibility(View.GONE);
                binding.btnResendOtp.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private static class AutoFocusWatcher implements TextWatcher {
        private final View currentView;
        private final View nextView;
        private final View prevView;

        public AutoFocusWatcher(View currentView, View nextView, View prevView) {
            this.currentView = currentView;
            this.nextView = nextView;
            this.prevView = prevView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            } else if (s.length() == 0 && prevView != null) {
                prevView.requestFocus();
            }
        }
    }
}
