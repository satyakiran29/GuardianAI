package com.skdev.guardianai.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.skdev.guardianai.sos.SosAlertManager;

/**
 * 4x2 Comprehensive Safety Hub AppWidget Provider.
 */
public class SafetyHubWidget4x2 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, WidgetUpdateHelper.buildSafetyHub4x2Views(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (WidgetUpdateHelper.ACTION_TRIGGER_SOS.equals(intent.getAction())) {
            Toast.makeText(context, "🚨 WIDGET SOS TRIGGERED! Siren & Emergency SMS Sent!", Toast.LENGTH_LONG).show();
            SosAlertManager.getInstance(context).triggerEmergencySos(12.9784, 77.6408, "Indiranagar (Widget Trigger)");
            WidgetUpdateHelper.updateAllWidgets(context);
        }
    }
}
