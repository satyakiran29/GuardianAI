package com.android.sheguard.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.sheguard.R;
import com.android.sheguard.util.SosUtil;

public class QuickSosWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TRIGGER_SOS = "com.android.sheguard.widget.ACTION_TRIGGER_SOS";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_sos);

        Intent sosIntent = new Intent(context, QuickSosWidgetProvider.class);
        sosIntent.setAction(ACTION_TRIGGER_SOS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.btn_widget_sos_circle, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_sos_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TRIGGER_SOS.equals(intent.getAction())) {
            SosUtil.vibrateDevice(context);
            SosUtil.activateInstantSosMode(context);
            Toast.makeText(context, context.getString(R.string.widget_sos_triggered), Toast.LENGTH_SHORT).show();

            // Refresh widgets
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, QuickSosWidgetProvider.class));
            onUpdate(context, manager, ids);
        }
    }
}
