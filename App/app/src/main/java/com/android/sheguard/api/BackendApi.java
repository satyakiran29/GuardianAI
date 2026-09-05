package com.android.sheguard.api;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BackendApi {

    @POST("auth/send-otp/")
    Call<JsonObject> sendOtp(@Body Map<String, Object> body);

    @POST("auth/verify-otp/")
    Call<JsonObject> verifyOtp(@Body Map<String, Object> body);

    @POST("auth/register/")
    Call<JsonObject> registerUser(@Body Map<String, Object> body);

    @POST("auth/profile/update/")
    Call<JsonObject> updateProfile(@Body Map<String, Object> body);

    @POST("auth/login/")
    Call<JsonObject> loginUser(@Body Map<String, Object> body);

    @POST("sos/trigger/")
    Call<JsonObject> triggerSos(@Body Map<String, Object> body);

    @POST("location/ping/")
    Call<JsonObject> pingLocation(@Body Map<String, Object> body);

    @GET("dashboard/stats/")
    Call<JsonObject> getDashboardStats();

    // Guardian Role & Tracking Endpoints
    @POST("guardians/link/")
    Call<JsonObject> linkGuardian(@Body Map<String, Object> body);

    @GET("guardians/my-guardians/")
    Call<JsonObject> getMyGuardians(@Query("phone") String phone);

    @GET("guardians/tracked-wards/")
    Call<JsonObject> getTrackedWards(@Query("guardian_phone") String guardianPhone);

    @POST("chat/send/")
    Call<JsonObject> sendChatMessage(@Body Map<String, Object> body);

    @GET("chat/messages/")
    Call<JsonObject> getChatMessages(@Query("user1") String user1, @Query("user2") String user2);

    @GET("location/history/")
    Call<JsonObject> getLocationHistory(
            @Query("ward_phone") String wardPhone,
            @Query("guardian_phone") String guardianPhone,
            @Query("hours") int hours
    );
}

