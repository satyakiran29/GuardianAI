package com.android.sheguard.common;

public class Constants {

    // Firebase Firestore
    public static final String FIRESTORE_COLLECTION_USERLIST = "UserList";
    public static final String FIRESTORE_COLLECTION_PHONE2UID = "PhoneToUid";
    public static final String FIRESTORE_COLLECTION_TOKENS = "Tokens";

    // Settings
    public static final String SETTINGS_SHAKE_DETECTION = "shake_detection";
    public static final String SETTINGS_SAFE_MODE = "safe_mode";
    public static final String SETTINGS_SEND_SMS = "send_sms";
    public static final String SETTINGS_SEND_WHATSAPP = "send_whatsapp";
    public static final String SETTINGS_STOP_BACKGROUND_APPS = "stop_background_apps";
    public static final String SETTINGS_SEND_NOTIFICATION = "send_notification";
    public static final String SETTINGS_PLAY_SIREN = "play_siren";
    public static final String SETTINGS_CALL_EMERGENCY_SERVICE = "call_emergency_service";
    public static final String SETTINGS_HAPTIC_FEEDBACK = "haptic_feedback";
    public static final String SETTINGS_LOW_BATTERY_ALERT = "low_battery_alert";
    public static final String KEY_BATTERY_ALERT_SENT = "battery_alert_sent";
    public static final int BATTERY_ALERT_THRESHOLD = 15;
    public static final String SETTINGS_THEME_MODE = "settings_theme_mode";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_AMOLED = "amoled";
    public static final String SETTINGS_APP_LANGUAGE = "settings_app_language";
    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN = "en";
    public static final String LANG_HI = "hi";
    public static final String LANG_TE = "te";

    // Others
    public static final String CONTACTS_LIST = "contacts_list";
    public static final String PREFS_USER_NAME = "user_name";
    public static final String PREFS_USER_EMAIL = "user_email";
    public static final String PREFS_USER_PHONE = "user_phone";
    public static final String IS_DEMO_MODE = "is_demo_mode";
    public static final String DEMO_USER_NAME = "Sarah Connor";
    public static final String DEMO_USER_EMAIL = "demo.user@guardianai.app";
    public static final String DEMO_USER_PHONE = "+1 (555) 019-2834";
    public static final String EMERGENCY_NUMBER = "999";
    public static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/satyakiran29/GuardianAI/main/Apk/update.json";
    public static final String GROQ_API_KEY = "";
    public static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    // Hardware Buttons SOS Triggers
    public static final String SETTINGS_HARDWARE_BUTTON_SOS = "hardware_button_sos";
    public static final String SETTINGS_POWER_BUTTON_SOS = "power_button_sos";
    public static final String SETTINGS_VOLUME_BUTTON_SOS = "volume_button_sos";
    public static final String SETTINGS_HARDWARE_TRIGGER_MODE = "hardware_trigger_mode";
    public static final String HW_MODE_BOTH = "both";
    public static final String HW_MODE_POWER_ONLY = "power_only";
    public static final String HW_MODE_VOLUME_ONLY = "volume_only";
    public static final String HW_MODE_DISABLED = "disabled";
}
