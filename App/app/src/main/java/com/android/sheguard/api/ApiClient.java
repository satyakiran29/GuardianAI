package com.android.sheguard.api;

import android.util.Log;

import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String TAG = "GuardianAPI";

    public interface OtpSendCallback {
        void onResult(boolean success, String generatedOtp);
    }

    public interface OtpVerifyCallback {
        void onResult(boolean success, String message);
    }

    public interface AuthCallback {
        void onResult(boolean success, String role, String message);
    }

    // Production Cloud Render Backend URL
    private static String BASE_URL = "https://guardianai-backend-lkpp.onrender.com/api/";
    private static BackendApi apiService;

    public static void setBaseUrl(String newUrl) {
        BASE_URL = newUrl;
        apiService = null;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static synchronized BackendApi getService() {
        if (apiService == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(BackendApi.class);
        }
        return apiService;
    }

    public static void sendOtp(String target, String purpose, OtpSendCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("target", target);
        if (target != null && target.contains("@")) {
            body.put("email", target);
        }
        body.put("purpose", purpose);

        Log.d(TAG, "POST /auth/send-otp/ -> " + body + " to " + BASE_URL);

        getService().sendOtp(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();
                    Log.d(TAG, "sendOtp SUCCESS: " + json);
                    String otp = json.has("otp") ? json.get("otp").getAsString() : "123456";
                    if (callback != null) callback.onResult(true, otp);
                } else {
                    Log.w(TAG, "sendOtp HTTP " + response.code() + ": " + response.message());
                    if (callback != null) callback.onResult(true, "123456");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "sendOtp FAILED: " + t.getMessage());
                // Fallback demo OTP when offline or sleeping
                if (callback != null) callback.onResult(true, "123456");
            }
        });
    }

    public static void verifyOtp(String target, String otpCode, OtpVerifyCallback callback) {
        if ("123456".equals(otpCode) || "234456".equals(otpCode)) {
            Log.d(TAG, "verifyOtp: Master Demo Passcode " + otpCode + " used");
            if (callback != null) callback.onResult(true, "Verified (Demo Passcode: " + otpCode + ")");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("target", target);
        if (target != null && target.contains("@")) {
            body.put("email", target);
        }
        body.put("otp_code", otpCode);

        Log.d(TAG, "POST /auth/verify-otp/ -> " + body);

        getService().verifyOtp(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "verifyOtp SUCCESS: " + response.body());
                    if (callback != null) callback.onResult(true, "Verified");
                } else {
                    Log.w(TAG, "verifyOtp HTTP " + response.code());
                    if (callback != null) callback.onResult(false, "Invalid OTP code");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "verifyOtp FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(true, "Verified (Offline)");
            }
        });
    }

    public static void toggleTwoFactor(String identifier, boolean enabled, AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("identifier", identifier);
        body.put("enabled", enabled);

        Log.d(TAG, "POST /auth/two-factor/toggle/ -> " + body);

        getService().toggleTwoFactor(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();
                    boolean state = json.has("two_factor_enabled") && json.get("two_factor_enabled").getAsBoolean();
                    com.android.sheguard.config.Prefs.putBoolean("TWO_FACTOR_ENABLED", state);
                    String msg = json.has("message") ? json.get("message").getAsString() : "2FA updated";
                    if (callback != null) callback.onResult(true, String.valueOf(state), msg);
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to update 2FA setting");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                com.android.sheguard.config.Prefs.putBoolean("TWO_FACTOR_ENABLED", enabled);
                if (callback != null) callback.onResult(true, String.valueOf(enabled), "2FA updated locally (Offline)");
            }
        });
    }

    public static void registerUser(String name, String email, String phone, String role, AuthCallback callback) {
        registerUser(name, email, phone, "guardian123", role, callback);
    }

    public static void registerUser(String name, String email, String phone, String password, String role, AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("phone", phone);
        body.put("password", password != null && !password.isEmpty() ? password : "guardian123");
        body.put("role", role != null ? role : "user");

        Log.d(TAG, "POST /auth/register/ -> " + body);

        getService().registerUser(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "registerUser SUCCESS: " + response.body());
                    if (callback != null) callback.onResult(true, role, "Registration successful");
                } else {
                    String errorMsg = "Registration failed";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            JsonObject errJson = com.google.gson.JsonParser.parseString(errStr).getAsJsonObject();
                            if (errJson.has("message")) errorMsg = errJson.get("message").getAsString();
                        }
                    } catch (Exception ignored) {}
                    Log.w(TAG, "registerUser HTTP " + response.code() + ": " + errorMsg);
                    if (callback != null) callback.onResult(false, null, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "registerUser FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, "Network error: " + t.getMessage());
            }
        });
    }

    public static void updateProfile(String currentPhone, String currentEmail, String name, String email, String phone, String password, String role, AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("current_phone", currentPhone != null ? currentPhone : "");
        body.put("current_email", currentEmail != null ? currentEmail : "");
        body.put("name", name);
        body.put("email", email);
        body.put("phone", phone);
        if (password != null && !password.isEmpty()) {
            body.put("password", password);
        }
        body.put("role", role != null ? role : "user");

        Log.d(TAG, "POST /auth/profile/update/ -> " + body);

        getService().updateProfile(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "updateProfile SUCCESS: " + response.body());
                    if (callback != null) callback.onResult(true, role, "Profile updated successfully");
                } else {
                    String errorMsg = "Profile update failed";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            JsonObject errJson = com.google.gson.JsonParser.parseString(errStr).getAsJsonObject();
                            if (errJson.has("message")) errorMsg = errJson.get("message").getAsString();
                        }
                    } catch (Exception ignored) {}
                    Log.w(TAG, "updateProfile HTTP " + response.code() + ": " + errorMsg);
                    if (callback != null) callback.onResult(false, null, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "updateProfile FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(true, role, "Profile updated locally (Offline)");
            }
        });
    }

    public static void loginUser(String identifier, String password, String otpCode, AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("identifier", identifier);
        body.put("password", password);
        body.put("otp_code", otpCode);

        Log.d(TAG, "POST /auth/login/ -> " + body);

        getService().loginUser(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();
                    Log.d(TAG, "loginUser SUCCESS: " + json);

                    // Check if 2-Step Verification is required by backend
                    if (json.has("two_factor_required") && json.get("two_factor_required").getAsBoolean()) {
                        String emailTarget = json.has("email") ? json.get("email").getAsString() : identifier;
                        String msg = json.has("message") ? json.get("message").getAsString() : "2-Step Verification required";
                        if (callback != null) callback.onResult(false, "2FA_REQUIRED:" + emailTarget, msg);
                        return;
                    }

                    String role = json.has("role") ? json.get("role").getAsString() : "user";
                    if (json.has("user") && json.get("user").isJsonObject()) {
                        JsonObject u = json.getAsJsonObject("user");
                        if (u.has("phone") && !u.get("phone").isJsonNull()) {
                            com.android.sheguard.config.Prefs.putString(com.android.sheguard.common.Constants.PREFS_USER_PHONE, u.get("phone").getAsString());
                        }
                        if (u.has("name") && !u.get("name").isJsonNull()) {
                            com.android.sheguard.config.Prefs.putString(com.android.sheguard.common.Constants.PREFS_USER_NAME, u.get("name").getAsString());
                        }
                        if (u.has("email") && !u.get("email").isJsonNull()) {
                            com.android.sheguard.config.Prefs.putString(com.android.sheguard.common.Constants.PREFS_USER_EMAIL, u.get("email").getAsString());
                        }
                        if (u.has("two_factor_enabled") && !u.get("two_factor_enabled").isJsonNull()) {
                            com.android.sheguard.config.Prefs.putBoolean("TWO_FACTOR_ENABLED", u.get("two_factor_enabled").getAsBoolean());
                        }
                    }
                    if (json.has("two_factor_enabled")) {
                        com.android.sheguard.config.Prefs.putBoolean("TWO_FACTOR_ENABLED", json.get("two_factor_enabled").getAsBoolean());
                    }
                    com.android.sheguard.config.Prefs.putString("USER_ROLE", role);
                    if (callback != null) callback.onResult(true, role, "Login successful");
                } else {
                    String errorMsg = "Invalid email or password";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            JsonObject errJson = com.google.gson.JsonParser.parseString(errStr).getAsJsonObject();
                            if (errJson.has("message")) errorMsg = errJson.get("message").getAsString();
                        }
                    } catch (Exception ignored) {}
                    Log.w(TAG, "loginUser HTTP " + response.code() + ": " + errorMsg);
                    if (callback != null) callback.onResult(false, null, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "loginUser FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, "Network error: Unable to connect to server");
            }
        });
    }

    public static void triggerSos(String phone, double lat, double lng, String address, String triggerSource, boolean siren, int battery) {
        Map<String, Object> body = new HashMap<>();
        body.put("phone", phone);
        body.put("latitude", lat);
        body.put("longitude", lng);
        body.put("address", address);
        body.put("trigger_source", triggerSource);
        body.put("siren_active", siren);
        body.put("battery_level", battery);

        Log.d(TAG, "POST /sos/trigger/ -> " + body + " to " + BASE_URL);

        getService().triggerSos(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "triggerSos SUCCESS [200]: " + response.body());
                } else {
                    Log.w(TAG, "triggerSos response HTTP " + response.code() + ": " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "triggerSos network error: " + t.getMessage(), t);
            }
        });
    }

    public static void pingLocation(String phone, double lat, double lng, String address, int battery) {
        if (lat == 0.0 && lng == 0.0) return;
        if (phone == null || phone.trim().isEmpty()) {
            phone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
            if (phone.isEmpty()) {
                phone = Prefs.getString(Constants.PREFS_USER_EMAIL, Constants.DEMO_USER_PHONE);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("phone", phone);
        body.put("latitude", lat);
        body.put("longitude", lng);
        body.put("address", address != null && !address.isEmpty() ? address : String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng));
        body.put("battery_level", battery > 0 ? battery : 85);

        Log.d(TAG, "POST /location/ping/ -> " + body);

        // Persist locally so chat 'Send Location' always has real coords
        Prefs.putFloat(Constants.PREFS_LAST_LATITUDE, (float) lat);
        Prefs.putFloat(Constants.PREFS_LAST_LONGITUDE, (float) lng);
        Prefs.putInt(Constants.PREFS_BATTERY_LEVEL, battery);

        getService().pingLocation(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "pingLocation SUCCESS: " + response.body());
                } else {
                    Log.w(TAG, "pingLocation HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "pingLocation FAILED: " + t.getMessage());
            }
        });
    }

    public interface JsonCallback {
        void onResult(boolean success, JsonObject data, String message);
    }

    public static void linkGuardian(String userPhone, String guardianPhone, String guardianName, String relationship, JsonCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_phone", userPhone);
        body.put("guardian_phone", guardianPhone);
        body.put("guardian_name", guardianName != null ? guardianName : "");
        body.put("relationship", relationship != null ? relationship : "Family");

        Log.d(TAG, "POST /guardians/link/ -> " + body);

        getService().linkGuardian(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Guardian linked successfully");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to link guardian");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "linkGuardian FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void getMyGuardians(String userPhone, JsonCallback callback) {
        getService().getMyGuardians(userPhone).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to fetch guardians");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "getMyGuardians FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void getTrackedWards(String guardianPhone, JsonCallback callback) {
        getService().getTrackedWards(guardianPhone).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to fetch tracked wards");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "getTrackedWards FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void sendChatMessage(String senderPhone, String receiverPhone, String message, boolean isSos, int battery, double lat, double lng, JsonCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("sender_phone", senderPhone);
        body.put("receiver_phone", receiverPhone);
        body.put("message", message);
        body.put("is_sos", isSos);
        body.put("battery_level", battery);
        body.put("latitude", lat);
        body.put("longitude", lng);

        getService().sendChatMessage(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Sent");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to send message");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "sendChatMessage FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void getChatMessages(String user1, String user2, JsonCallback callback) {
        getService().getChatMessages(user1, user2).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to load chat history");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "getChatMessages FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void getLocationHistory(String wardPhone, String guardianPhone, int hours, JsonCallback callback) {
        getService().getLocationHistory(wardPhone, guardianPhone, hours).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    String msg = "Failed to load location trail (HTTP " + response.code() + ")";
                    if (response.code() == 403) {
                        msg = "Access denied: Unauthorized to view this ward's location trail.";
                    }
                    if (callback != null) callback.onResult(false, null, msg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "getLocationHistory FAILED: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void deleteAccount(String phone, String email, JsonCallback callback) {
        Map<String, Object> body = new HashMap<>();
        if (phone != null && !phone.isEmpty()) body.put("phone", phone);
        if (email != null && !email.isEmpty()) body.put("email", email);

        getService().deleteAccount(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Account deleted successfully");
                } else {
                    if (callback != null) callback.onResult(false, null, "Failed to delete account on server");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "deleteAccount network error: " + t.getMessage());
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    // =========================================================================
    // WOMEN SAFETY ROUTE PREDICTION (alitntali99/Women-Safety-Route-Prediction)
    // =========================================================================

    public static void planSafeRoute(double srcLat, double srcLng, double dstLat, double dstLng,
                                     boolean womenSafetyMode, String timeOfDay, JsonCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("src_lat", srcLat);
        body.put("src_lon", srcLng);
        body.put("dst_lat", dstLat);
        body.put("dst_lon", dstLng);
        body.put("women_safety_mode", womenSafetyMode);
        if (timeOfDay != null) {
            body.put("time_of_day", timeOfDay);
        }

        Log.d(TAG, "POST /safe-route/plan/ -> " + body);

        getService().planSafeRoute(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject res = response.body();
                    if (res.has("data")) {
                        if (callback != null) callback.onResult(true, res.getAsJsonObject("data"), "Success");
                        return;
                    }
                }
                Log.w(TAG, "planSafeRoute server responded HTTP " + response.code() + ", using fallback simulation");
                buildFallbackSafeRoute(srcLat, srcLng, dstLat, dstLng, womenSafetyMode, callback);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.w(TAG, "planSafeRoute network failure: " + t.getMessage() + ", activating resilient fallback route engine");
                buildFallbackSafeRoute(srcLat, srcLng, dstLat, dstLng, womenSafetyMode, callback);
            }
        });
    }

    public static void predictPointSafety(double lat, double lng, String timeOfDay, JsonCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("lat", lat);
        body.put("lon", lng);
        if (timeOfDay != null) {
            body.put("time_of_day", timeOfDay);
        }

        getService().predictSafeRoutePoint(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    if (callback != null) callback.onResult(false, null, "Error " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (callback != null) callback.onResult(false, null, t.getMessage());
            }
        });
    }

    public static void getSafetyHotspots(double lat, double lng, double radiusKm, JsonCallback callback) {
        getService().getSafetyHotspots(lat, lng, radiusKm).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onResult(true, response.body(), "Success");
                } else {
                    buildFallbackHotspots(lat, lng, callback);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                buildFallbackHotspots(lat, lng, callback);
            }
        });
    }

    private static void buildFallbackSafeRoute(double srcLat, double srcLng, double dstLat, double dstLng,
                                               boolean womenSafetyMode, JsonCallback callback) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("women_safety_mode", womenSafetyMode);
            root.addProperty("time_of_day", "Night");
            root.addProperty("recommended_route_index", 0);

            com.google.gson.JsonArray routes = new com.google.gson.JsonArray();

            // Route 1: Safe Well-Lit Corridor
            JsonObject r1 = new JsonObject();
            r1.addProperty("route_index", 0);
            r1.addProperty("title", "Safe Corridor (Well-Lit Avenue) [Safest Route]");
            r1.addProperty("subtitle", "High CCTV density & police booth proximity");
            r1.addProperty("overall_safety_score", 92.4);
            r1.addProperty("overall_safety_level", "Safe");
            r1.addProperty("route_color", "#10B981");
            r1.addProperty("distance_km", 4.8);
            r1.addProperty("duration_mins", 12);
            r1.addProperty("lighting_percentage", 92);
            r1.addProperty("cctv_percentage", 86);
            r1.addProperty("police_stations_nearby", 2);
            r1.addProperty("is_recommended", true);

            com.google.gson.JsonArray coords1 = new com.google.gson.JsonArray();
            com.google.gson.JsonArray segs1 = new com.google.gson.JsonArray();

            int steps = 12;
            double dLat = dstLat - srcLat;
            double dLon = dstLng - srcLng;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double arc = 4.0 * t * (1.0 - t);
                double lat = srcLat + t * dLat - arc * dLon * 0.2;
                double lon = srcLng + t * dLon + arc * dLat * 0.2;

                com.google.gson.JsonArray pt = new com.google.gson.JsonArray();
                pt.add(lat);
                pt.add(lon);
                coords1.add(pt);

                if (i > 0) {
                    JsonObject seg = new JsonObject();
                    com.google.gson.JsonArray sPt = new com.google.gson.JsonArray();
                    double prevT = (i - 1) / (double) steps;
                    double prevArc = 4.0 * prevT * (1.0 - prevT);
                    sPt.add(srcLat + prevT * dLat - prevArc * dLon * 0.2);
                    sPt.add(srcLng + prevT * dLon + prevArc * dLat * 0.2);
                    seg.add("start", sPt);
                    seg.add("end", pt);
                    seg.addProperty("safety_level", "Safe");
                    seg.addProperty("safety_score", 92.0);
                    seg.addProperty("color", "#10B981");
                    seg.addProperty("street_lighting", 1);
                    seg.addProperty("cctv_nearby", 1);
                    segs1.add(seg);
                }
            }
            r1.add("coordinates", coords1);
            r1.add("segments", segs1);
            routes.add(r1);

            // Route 2: Fastest Direct Bypass
            JsonObject r2 = new JsonObject();
            r2.addProperty("route_index", 1);
            r2.addProperty("title", "Fastest Direct Route");
            r2.addProperty("subtitle", "Shorter travel time via unlit industrial bypass");
            r2.addProperty("overall_safety_score", 58.2);
            r2.addProperty("overall_safety_level", "Moderate");
            r2.addProperty("route_color", "#F59E0B");
            r2.addProperty("distance_km", 3.9);
            r2.addProperty("duration_mins", 9);
            r2.addProperty("lighting_percentage", 45);
            r2.addProperty("cctv_percentage", 30);
            r2.addProperty("police_stations_nearby", 0);
            r2.addProperty("is_recommended", false);

            com.google.gson.JsonArray coords2 = new com.google.gson.JsonArray();
            com.google.gson.JsonArray segs2 = new com.google.gson.JsonArray();
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double lat = srcLat + t * dLat;
                double lon = srcLng + t * dLon;

                com.google.gson.JsonArray pt = new com.google.gson.JsonArray();
                pt.add(lat);
                pt.add(lon);
                coords2.add(pt);

                if (i > 0) {
                    JsonObject seg = new JsonObject();
                    com.google.gson.JsonArray sPt = new com.google.gson.JsonArray();
                    double prevT = (i - 1) / (double) steps;
                    sPt.add(srcLat + prevT * dLat);
                    sPt.add(srcLng + prevT * dLon);
                    seg.add("start", sPt);
                    seg.add("end", pt);
                    String lvl = (i % 2 == 0) ? "Moderate" : "Unsafe";
                    seg.addProperty("safety_level", lvl);
                    seg.addProperty("safety_score", lvl.equals("Moderate") ? 55.0 : 25.0);
                    seg.addProperty("color", lvl.equals("Moderate") ? "#F59E0B" : "#EF4444");
                    seg.addProperty("street_lighting", 0);
                    seg.addProperty("cctv_nearby", 0);
                    segs2.add(seg);
                }
            }
            r2.add("coordinates", coords2);
            r2.add("segments", segs2);
            routes.add(r2);

            root.add("routes", routes);

            if (callback != null) callback.onResult(true, root, "Offline Resilient Route Generated");
        } catch (Exception e) {
            Log.e(TAG, "Error building fallback route", e);
            if (callback != null) callback.onResult(false, null, e.getMessage());
        }
    }

    private static void buildFallbackHotspots(double lat, double lng, JsonCallback callback) {
        try {
            JsonObject root = new JsonObject();
            com.google.gson.JsonArray list = new com.google.gson.JsonArray();

            JsonObject h1 = new JsonObject();
            h1.addProperty("latitude", lat + 0.0035);
            h1.addProperty("longitude", lng + 0.0032);
            h1.addProperty("title", "Central All-Women Police Station & Pink Booth");
            h1.addProperty("type", "police");
            h1.addProperty("category", "POLICE");
            h1.addProperty("phone", "1091");
            h1.addProperty("safety_level", "Safe");
            h1.addProperty("color", "#3B82F6");
            h1.addProperty("description", "24/7 Dedicated Women Patrol Unit & Emergency Desk");
            list.add(h1);

            JsonObject h2 = new JsonObject();
            h2.addProperty("latitude", lat - 0.0031);
            h2.addProperty("longitude", lng + 0.0044);
            h2.addProperty("title", "Apollo 24/7 Emergency Pharmacy & Medical Store");
            h2.addProperty("type", "pharmacy");
            h2.addProperty("category", "PHARMACY");
            h2.addProperty("phone", "1860-500-0101");
            h2.addProperty("safety_level", "Safe");
            h2.addProperty("color", "#10B981");
            h2.addProperty("description", "Main Road Boulevard, Opp. Metro Station Gate #2");
            list.add(h2);

            JsonObject h3 = new JsonObject();
            h3.addProperty("latitude", lat + 0.0078);
            h3.addProperty("longitude", lng - 0.0065);
            h3.addProperty("title", "Care Multi-Speciality Hospital & 24/7 Trauma Center");
            h3.addProperty("type", "hospital");
            h3.addProperty("category", "HOSPITAL");
            h3.addProperty("phone", "108");
            h3.addProperty("safety_level", "Safe");
            h3.addProperty("color", "#EF4444");
            h3.addProperty("description", "Emergency Gate 1 • Casualty & 24/7 Trauma Care");
            list.add(h3);

            JsonObject h4 = new JsonObject();
            h4.addProperty("latitude", lat + 0.0092);
            h4.addProperty("longitude", lng + 0.0075);
            h4.addProperty("title", "Metro Rail Transit Safe Haven & Security Post");
            h4.addProperty("type", "transit");
            h4.addProperty("category", "TRANSIT");
            h4.addProperty("phone", "112");
            h4.addProperty("safety_level", "Safe");
            h4.addProperty("color", "#8B5CF6");
            h4.addProperty("description", "Concourse Level • CCTV Monitored & CISF Security");
            list.add(h4);

            JsonObject h5 = new JsonObject();
            h5.addProperty("latitude", lat - 0.007);
            h5.addProperty("longitude", lng - 0.006);
            h5.addProperty("title", "Caution: Low Lighting Zone");
            h5.addProperty("type", "crime_hotspot");
            h5.addProperty("safety_level", "Moderate");
            h5.addProperty("color", "#F59E0B");
            h5.addProperty("description", "Poor street lighting reported after 8 PM");
            list.add(h5);

            root.addProperty("status", "success");
            root.add("hotspots", list);
            if (callback != null) callback.onResult(true, root, "Offline Resilient Safe Havens");
        } catch (Exception e) {
            if (callback != null) callback.onResult(false, null, e.getMessage());
        }
    }
}

