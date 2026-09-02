package com.android.sheguard.api;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface BackendApi {

    @POST("auth/send-otp/")
    Call<JsonObject> sendOtp(@Body Map<String, Object> body);

    @POST("auth/verify-otp/")
    Call<JsonObject> verifyOtp(@Body Map<String, Object> body);

    @POST("auth/register/")
    Call<JsonObject> registerUser(@Body Map<String, Object> body);

    @POST("auth/login/")
    Call<JsonObject> loginUser(@Body Map<String, Object> body);

    @POST("sos/trigger/")
    Call<JsonObject> triggerSos(@Body Map<String, Object> body);

    @POST("location/ping/")
    Call<JsonObject> pingLocation(@Body Map<String, Object> body);

    @GET("dashboard/stats/")
    Call<JsonObject> getDashboardStats();
}
