package com.skdev.guardianai.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages local offline authentication and session persistence without Firebase.
 */
public class AuthManager {

    private static final String PREF_NAME = "guardian_auth_pref";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTO_LOGIN = "auto_login_enabled";
    private static final String KEY_USERNAME = "current_username";
    private static final String KEY_USER_EMAIL = "current_email";

    public static final String DEFAULT_TEST_USERNAME = "admin";
    public static final String DEFAULT_TEST_PASSWORD = "admin";

    private static AuthManager instance;
    private final SharedPreferences prefs;

    private AuthManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public boolean shouldAutoLogin() {
        return prefs.getBoolean(KEY_AUTO_LOGIN, true) && prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean authenticate(String username, String password, boolean enableAutoLogin) {
        if (username == null || password == null) return false;
        String u = username.trim();
        String p = password.trim();

        // Testing credentials or any valid non-empty username/password for demo
        if ((u.equalsIgnoreCase(DEFAULT_TEST_USERNAME) && p.equals(DEFAULT_TEST_PASSWORD)) ||
            (!u.isEmpty() && p.length() >= 4)) {

            prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putBoolean(KEY_AUTO_LOGIN, enableAutoLogin)
                    .putString(KEY_USERNAME, u)
                    .putString(KEY_USER_EMAIL, u + "@guardianai.safe")
                    .apply();
            return true;
        }
        return false;
    }

    public void quickAdminLogin() {
        authenticate(DEFAULT_TEST_USERNAME, DEFAULT_TEST_PASSWORD, true);
    }

    public String getCurrentUsername() {
        return prefs.getString(KEY_USERNAME, "Admin");
    }

    public String getCurrentEmail() {
        return prefs.getString(KEY_USER_EMAIL, "admin@guardianai.safe");
    }

    public boolean isAutoLoginEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOGIN, true);
    }

    public void setAutoLoginEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply();
    }

    public void logout() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}
