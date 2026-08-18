package com.skdev.guardianai.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skdev.guardianai.R;
import com.skdev.guardianai.ui.MainActivity;

/**
 * Authentication screen with support for testing credentials (admin / admin),
 * auto-login session persistence, and instant 1-tap admin bypass.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private CheckBox cbAutoLogin;
    private Button btnLogin;
    private Button btnQuickAdmin;
    private AuthManager authManager;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.skdev.guardianai.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance(this);

        // Check if auto-login is active
        if (authManager.shouldAutoLogin()) {
            startMainActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbAutoLogin = findViewById(R.id.cb_auto_login);
        btnLogin = findViewById(R.id.btn_login);
        btnQuickAdmin = findViewById(R.id.btn_quick_admin);

        cbAutoLogin.setChecked(authManager.isAutoLoginEnabled());

        btnLogin.setOnClickListener(v -> handleLogin());
        btnQuickAdmin.setOnClickListener(v -> handleQuickAdmin());
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean autoLogin = cbAutoLogin.isChecked();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter testing username & password (admin / admin)", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = authManager.authenticate(username, password, autoLogin);
        if (success) {
            Toast.makeText(this, "Welcome to GuardianAI, " + username + "!", Toast.LENGTH_SHORT).show();
            startMainActivity();
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials. For testing, use admin / admin", Toast.LENGTH_LONG).show();
        }
    }

    private void handleQuickAdmin() {
        authManager.quickAdminLogin();
        Toast.makeText(this, "Quick Admin Access Granted!", Toast.LENGTH_SHORT).show();
        startMainActivity();
        finish();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
