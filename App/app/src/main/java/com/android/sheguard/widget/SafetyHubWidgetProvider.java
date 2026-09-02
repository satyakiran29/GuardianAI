package com.android.sheguard.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.ui.fragment.ContactsFragment;
import com.android.sheguard.util.SosUtil;

import java.util.ArrayList;

public class SafetyHubWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TRIGGER_SOS = "com.android.sheguard.widget.ACTION_HUB_SOS";
    public static final String ACTION_TOGGLE_SAFE_MODE = "com.android.sheguard.widget.ACTION_HUB_SAFE_MODE";
    public static final String ACTION_SHARE_LOCATION = "com.android.sheguard.widget.ACTION_HUB_SHARE_LOCATION";
    public static final String ACTION_CALL_HELPLINE = "com.android.sheguard.widget.ACTION_HUB_HELPLINE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_safety_hub);

        boolean isSafeMode = Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false);
        views.setTextViewText(R.id.tv_hub_status_badge, isSafeMode ? "🛡️ SAFE MODE ACTIVE" : "🛡️ PROTECTED");
        views.setTextViewText(R.id.tv_hub_safe_mode_label, isSafeMode ? "Safe: ON" : "Safe: OFF");

        // 1. SOS Pending Intent
        Intent sosIntent = new Intent(context, SafetyHubWidgetProvider.class);
        sosIntent.setAction(ACTION_TRIGGER_SOS);
        PendingIntent sosPI = PendingIntent.getBroadcast(
                context, 101, sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_hub_sos, sosPI);

        // 2. Safe Mode Pending Intent
        Intent safeModeIntent = new Intent(context, SafetyHubWidgetProvider.class);
        safeModeIntent.setAction(ACTION_TOGGLE_SAFE_MODE);
        PendingIntent safeModePI = PendingIntent.getBroadcast(
                context, 102, safeModeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_hub_safe_mode, safeModePI);

        // 3. Share Location Pending Intent
        Intent shareIntent = new Intent(context, SafetyHubWidgetProvider.class);
        shareIntent.setAction(ACTION_SHARE_LOCATION);
        PendingIntent sharePI = PendingIntent.getBroadcast(
                context, 103, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_hub_share_location, sharePI);

        // 4. Helpline Call Pending Intent
        Intent helplineIntent = new Intent(context, SafetyHubWidgetProvider.class);
        helplineIntent.setAction(ACTION_CALL_HELPLINE);
        PendingIntent helplinePI = PendingIntent.getBroadcast(
                context, 104, helplineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_hub_helpline, helplinePI);

        // Root open app
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPI = PendingIntent.getActivity(
                context, 105, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_hub_root, openAppPI);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (ACTION_TRIGGER_SOS.equals(action)) {
            SosUtil.vibrateDevice(context);
            SosUtil.activateInstantSosMode(context);
            Toast.makeText(context, context.getString(R.string.widget_sos_triggered), Toast.LENGTH_SHORT).show();
            refreshAllWidgets(context);

        } else if (ACTION_TOGGLE_SAFE_MODE.equals(action)) {
            boolean current = Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false);
            boolean newMode = !current;
            Prefs.putBoolean(Constants.SETTINGS_SAFE_MODE, newMode);

            SosUtil.vibrateDevice(context);
            if (newMode) {
                SosUtil.stopBackgroundProcesses(context);
                Toast.makeText(context, context.getString(R.string.safe_mode_activated_toast), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getString(R.string.safe_mode_deactivated_toast), Toast.LENGTH_SHORT).show();
            }
            refreshAllWidgets(context);

        } else if (ACTION_SHARE_LOCATION.equals(action)) {
            SosUtil.sendWhatsAppWithLiveLocation(context);
            Toast.makeText(context, context.getString(R.string.widget_location_shared), Toast.LENGTH_SHORT).show();

        } else if (ACTION_CALL_HELPLINE.equals(action)) {
            ArrayList<com.android.sheguard.model.ContactModel> contacts = SosUtil.getStoredContacts(context);
            if (contacts != null && !contacts.isEmpty() && contacts.get(0).getPhone() != null) {
                String phone = contacts.get(0).getPhone().replaceAll("[^0-9+]", "");
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "No emergency contact found. Please add contacts in GuardianAI.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, SafetyHubWidgetProvider.class));
        for (int id : ids) {
            updateAppWidget(context, manager, id);
        }
    }
}
