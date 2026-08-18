package com.skdev.guardianai.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.skdev.guardianai.R;
import com.skdev.guardianai.data.EmergencyContactManager;
import com.skdev.guardianai.data.SafetyDataRepository;
import com.skdev.guardianai.data.SafetyLocation;
import com.skdev.guardianai.data.SafetyModelEngine;
import com.skdev.guardianai.sos.EmergencyContactsActivity;
import com.skdev.guardianai.sos.SosAlertManager;
import com.skdev.guardianai.ui.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for updating all GuardianAI Home Screen Widgets with live data,
 * interactive PendingIntents (1-tap SOS, Safe Route, Live Location, Contacts).
 */
public class WidgetUpdateHelper {

    public static final String ACTION_TRIGGER_SOS = "com.skdev.guardianai.ACTION_TRIGGER_SOS";
    public static final String ACTION_OPEN_MAP = "com.skdev.guardianai.ACTION_OPEN_MAP";
    public static final String ACTION_OPEN_CONTACTS = "com.skdev.guardianai.ACTION_OPEN_CONTACTS";
    public static final String ACTION_OPEN_LIVE_LOC = "com.skdev.guardianai.ACTION_OPEN_LIVE_LOC";

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        // 1. Update 4x2 Hub
        ComponentName hubComponent = new ComponentName(context, SafetyHubWidget4x2.class);
        int[] hubIds = manager.getAppWidgetIds(hubComponent);
        if (hubIds != null && hubIds.length > 0) {
            for (int id : hubIds) {
                RemoteViews views = buildSafetyHub4x2Views(context);
                manager.updateAppWidget(id, views);
            }
        }

        // 2. Update 2x2 SOS
        ComponentName sosComponent = new ComponentName(context, SosQuickWidget2x2.class);
        int[] sosIds = manager.getAppWidgetIds(sosComponent);
        if (sosIds != null && sosIds.length > 0) {
            for (int id : sosIds) {
                RemoteViews views = buildSos2x2Views(context);
                manager.updateAppWidget(id, views);
            }
        }

        // 3. Update 4x1 Bar
        ComponentName barComponent = new ComponentName(context, SafetyBarWidget4x1.class);
        int[] barIds = manager.getAppWidgetIds(barComponent);
        if (barIds != null && barIds.length > 0) {
            for (int id : barIds) {
                RemoteViews views = buildSafetyBar4x1Views(context);
                manager.updateAppWidget(id, views);
            }
        }

        // 4. Update 1x1 Glance
        ComponentName glanceComponent = new ComponentName(context, GlanceWidget1x1.class);
        int[] glanceIds = manager.getAppWidgetIds(glanceComponent);
        if (glanceIds != null && glanceIds.length > 0) {
            for (int id : glanceIds) {
                RemoteViews views = buildGlance1x1Views(context);
                manager.updateAppWidget(id, views);
            }
        }
    }

    public static RemoteViews buildSafetyHub4x2Views(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_safety_hub_4x2);

        SafetyLocation loc = getPrimaryLocation();
        SafetyModelEngine.SafetyPrediction pred = loc != null ? loc.getPrediction() : SafetyModelEngine.evaluateSafety(8.5, 1.2, 12);

        views.setTextViewText(R.id.tv_widget_zone_title, "You are in\nSafe Zone");
        views.setTextViewText(R.id.tv_widget_risk_level, "Risk Level: " + pred.riskLevel.getLabel());
        views.setTextColor(R.id.tv_widget_risk_level, Color.parseColor(pred.riskLevel.getColorHex()));

        // Time & Date
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());
        String dateStr = new SimpleDateFormat("EEE, d MMM", Locale.US).format(new Date());
        views.setTextViewText(R.id.tv_widget_time, timeStr);
        views.setTextViewText(R.id.tv_widget_date, dateStr);
        views.setTextViewText(R.id.tv_widget_weather_temp, "28°C");
        views.setTextViewText(R.id.tv_widget_weather_desc, "Light Rain");

        // Action 1: Smart SOS
        Intent sosIntent = new Intent(context, SafetyHubWidget4x2.class);
        sosIntent.setAction(ACTION_TRIGGER_SOS);
        PendingIntent sosPi = PendingIntent.getBroadcast(context, 1001, sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_hub_sos, sosPi);

        // Action 2: Live Location
        Intent locIntent = new Intent(context, MainActivity.class);
        locIntent.putExtra("target_tab", R.id.nav_map);
        locIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent locPi = PendingIntent.getActivity(context, 1002, locIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_hub_location, locPi);

        // Action 3: Safe Route
        Intent routeIntent = new Intent(context, MainActivity.class);
        routeIntent.putExtra("target_tab", R.id.nav_map);
        routeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent routePi = PendingIntent.getActivity(context, 1003, routeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_hub_route, routePi);

        // Action 4: My Contacts
        Intent contactsIntent = new Intent(context, EmergencyContactsActivity.class);
        contactsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contactsPi = PendingIntent.getActivity(context, 1004, contactsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_hub_contacts, contactsPi);

        // Root Click -> Open Main
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 1000, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_safety_hub_root, mainPi);

        return views;
    }

    public static RemoteViews buildSos2x2Views(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_sos_2x2);

        SafetyLocation loc = getPrimaryLocation();
        if (loc != null) {
            views.setTextViewText(R.id.tv_widget_sos_location,
                    String.format(Locale.US, "Current: %.4f° N, %.4f° E", loc.getLatitude(), loc.getLongitude()));
        }

        int contactCount = EmergencyContactManager.getInstance(context).getContacts().size();
        views.setTextViewText(R.id.tv_widget_contacts_more_count, "+" + Math.max(1, contactCount - 1));

        // Big SOS Button Click -> Triggers Emergency SOS immediately!
        Intent sosIntent = new Intent(context, SosQuickWidget2x2.class);
        sosIntent.setAction(ACTION_TRIGGER_SOS);
        PendingIntent sosPi = PendingIntent.getBroadcast(context, 2001, sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_trigger_sos, sosPi);

        // Open Contacts
        Intent contactsIntent = new Intent(context, EmergencyContactsActivity.class);
        contactsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contactsPi = PendingIntent.getActivity(context, 2002, contactsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_sos_contacts_open, contactsPi);

        // Root Click
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra("target_tab", R.id.nav_sos);
        PendingIntent mainPi = PendingIntent.getActivity(context, 2000, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_sos_root, mainPi);

        return views;
    }

    public static RemoteViews buildSafetyBar4x1Views(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_safety_bar_4x1);

        SafetyLocation loc = getPrimaryLocation();
        SafetyModelEngine.SafetyPrediction pred = loc != null ? loc.getPrediction() : SafetyModelEngine.evaluateSafety(8.5, 1.2, 12);

        int score = pred != null ? pred.getScorePercentage() : 82;
        views.setTextViewText(R.id.tv_bar_score_val, String.valueOf(score));
        views.setTextViewText(R.id.tv_bar_score_caption, score > 75 ? "You are doing great!" : "Stay extra vigilant");
        views.setTextViewText(R.id.tv_bar_risk_level, pred != null ? pred.riskLevel.getLabel() : "Low");
        views.setTextViewText(R.id.tv_bar_people_count, (loc != null ? (loc.getCrowdDensity() / 20) : 23) + " Nearby");
        views.setTextViewText(R.id.tv_bar_safe_places_count, "5 Nearby");

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 3000, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_safety_bar_root, mainPi);

        return views;
    }

    public static RemoteViews buildGlance1x1Views(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_glance_1x1);

        SafetyLocation loc = getPrimaryLocation();
        SafetyModelEngine.SafetyPrediction pred = loc != null ? loc.getPrediction() : SafetyModelEngine.evaluateSafety(8.5, 1.2, 12);

        views.setTextViewText(R.id.tv_glance_risk_val, pred != null ? pred.riskLevel.getLabel() : "Low");
        if (pred != null) {
            views.setTextColor(R.id.tv_glance_risk_val, Color.parseColor(pred.riskLevel.getColorHex()));
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 4000, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_glance_root, mainPi);

        return views;
    }

    private static SafetyLocation getPrimaryLocation() {
        List<SafetyLocation> locs = SafetyDataRepository.getInstance().getLocationsForCity("Bengaluru");
        return (!locs.isEmpty()) ? locs.get(0) : null;
    }
}
