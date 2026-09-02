package com.android.sheguard.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.sheguard.model.ContactModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern SMS Manager API Utility
 * Provides backward and forward compatibility across Android versions (API 21 - API 35+),
 * handles multi-part long text SMS dispatch for distress location links, and provides
 * intent fallback if background permissions are restricted.
 */
public final class SmsHelper {

    private static final String TAG = "SmsHelper";

    private SmsHelper() {
        // Private constructor for utility class
    }

    /**
     * Obtains the appropriate SmsManager instance based on the Android OS version.
     * Android 12 (API 31+) deprecates SmsManager.getDefault() in favor of Context.getSystemService(SmsManager.class).
     */
    @NonNull
    public static SmsManager getSmsManager(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            if (smsManager != null) {
                return smsManager;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return SmsManager.getSmsManagerForSubscriptionId(subId);
            }
        }
        return SmsManager.getDefault();
    }

    /**
     * Checks if SEND_SMS permission is granted by the user.
     */
    public static boolean isSmsPermissionGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if a phone number is an official emergency dispatch helpline (e.g. 112, 100, 911).
     * SMS messages should never be dispatched to emergency call center hotlines.
     */
    public static boolean isEmergencyHelplineNumber(@Nullable String rawPhone) {
        if (rawPhone == null) return false;
        String phone = sanitizePhoneNumber(rawPhone);
        return phone.equals("112") || phone.equals("911") || phone.equals("100") ||
               phone.equals("101") || phone.equals("102") || phone.equals("108") ||
               phone.equals("1090") || phone.equals("1091") || phone.equals("999") ||
               phone.equals("109");
    }

    /**
     * Sanitizes phone number by stripping unsupported special characters.
     */
    @NonNull
    public static String sanitizePhoneNumber(@Nullable String rawPhone) {
        if (rawPhone == null) return "";
        return rawPhone.replaceAll("[^0-9+]", "").trim();
    }

    /**
     * Sends an SMS message to a single phone number using multipart dispatch
     * to safely accommodate long distress messages and Google Maps URLs.
     */
    public static boolean sendSms(@NonNull Context context, @Nullable String rawPhoneNumber, @Nullable String message) {
        return sendSms(context, rawPhoneNumber, message, null, null);
    }

    /**
     * Sends an SMS message with optional sent and delivery tracking PendingIntents.
     */
    public static boolean sendSms(@NonNull Context context,
                                  @Nullable String rawPhoneNumber,
                                  @Nullable String message,
                                  @Nullable PendingIntent sentIntent,
                                  @Nullable PendingIntent deliveryIntent) {
        if (!isSmsPermissionGranted(context)) {
            Log.w(TAG, "sendSms: SEND_SMS permission is not granted!");
            return false;
        }

        String phoneNumber = sanitizePhoneNumber(rawPhoneNumber);
        if (phoneNumber.isEmpty()) {
            Log.e(TAG, "sendSms: Target phone number is empty or invalid!");
            return false;
        }

        // Never send automated text/check-in SMS to 112 / emergency dispatch lines
        if (isEmergencyHelplineNumber(phoneNumber)) {
            Log.w(TAG, "sendSms: Blocked SMS to emergency helpline hotline: " + phoneNumber);
            return false;
        }

        if (message == null || message.trim().isEmpty()) {
            Log.e(TAG, "sendSms: Message content is empty!");
            return false;
        }

        try {
            SmsManager smsManager = getSmsManager(context);
            ArrayList<String> parts = smsManager.divideMessage(message);

            if (parts.size() > 1) {
                ArrayList<PendingIntent> sentIntents = null;
                ArrayList<PendingIntent> deliveryIntents = null;

                if (sentIntent != null) {
                    sentIntents = new ArrayList<>();
                    for (int i = 0; i < parts.size(); i++) {
                        sentIntents.add(sentIntent);
                    }
                }

                if (deliveryIntent != null) {
                    deliveryIntents = new ArrayList<>();
                    for (int i = 0; i < parts.size(); i++) {
                        deliveryIntents.add(deliveryIntent);
                    }
                }

                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
                Log.i(TAG, "sendSms: Multi-part SMS (" + parts.size() + " parts) dispatched to: " + phoneNumber);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);
                Log.i(TAG, "sendSms: Single SMS dispatched to: " + phoneNumber);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sendSms: Error while dispatching SMS to " + phoneNumber, e);
            return false;
        }
    }

    /**
     * Sends emergency distress SMS to a list of guardian contacts.
     */
    public static int sendEmergencySmsToContacts(@NonNull Context context,
                                                 @Nullable List<ContactModel> contacts,
                                                 @NonNull String messageTemplateWithLocation) {
        if (!isSmsPermissionGranted(context) || contacts == null || contacts.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (ContactModel contact : contacts) {
            if (contact != null && contact.getPhone() != null) {
                String contactMsg = messageTemplateWithLocation;
                if (contact.getName() != null) {
                    contactMsg = contactMsg.replace("%1$s", contact.getName());
                }
                boolean sent = sendSms(context, contact.getPhone(), contactMsg);
                if (sent) {
                    successCount++;
                }
            }
        }
        return successCount;
    }

    /**
     * Attempts direct background SMS dispatch, and falls back to opening the native SMS app
     * if background permission is not granted or dispatch fails.
     */
    public static boolean sendSmsWithFallback(@NonNull Context context, @Nullable String rawPhoneNumber, @Nullable String message) {
        if (isSmsPermissionGranted(context)) {
            boolean sent = sendSms(context, rawPhoneNumber, message);
            if (sent) return true;
        }

        Log.w(TAG, "sendSmsWithFallback: Falling back to native SMS app...");
        return openSmsApp(context, rawPhoneNumber, message);
    }

    /**
     * Fallback intent launcher: opens the device default SMS messenger pre-filled
     * with recipient and message in case background dispatch fails or permissions are denied.
     */
    public static boolean openSmsApp(@NonNull Context context, @Nullable String rawPhoneNumber, @Nullable String message) {
        try {
            String phoneNumber = sanitizePhoneNumber(rawPhoneNumber);
            Uri smsUri = Uri.parse("smsto:" + phoneNumber);
            Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
            if (message != null && !message.isEmpty()) {
                intent.putExtra("sms_body", message);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "openSmsApp: Failed to launch SMS messenger", e);
            return false;
        }
    }
}
