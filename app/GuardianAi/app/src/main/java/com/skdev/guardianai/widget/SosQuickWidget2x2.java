package com.skdev.guardianai.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.skdev.guardianai.sos.SosAlertManager;

/**
 * 2x2 Quick Emergency SOS AppWidget Provider.
 */
public class SosQuickWidget2x2 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, WidgetUpdateHelper.buildSos2x2Views(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (WidgetUpdateHelper.ACTION_TRIGGER_SOS.equals(intent.getAction())) {
            Toast.makeText(context, "🚨 1-TAP WIDGET SOS ACTIVATED! Dispatched to all contacts!", Toast.LENGTH_LONG).show();
            SosAlertManager.getInstance(context).triggerEmergencySos(12.9784, 77.6408, "Indiranagar (1-Tap Widget)");
            WidgetUpdateHelper.updateAllWidgets(context);
        }
    }
}
