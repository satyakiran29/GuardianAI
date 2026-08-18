package com.skdev.guardianai.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * 1x1 Compact Quick Glance AppWidget Provider.
 */
public class GlanceWidget1x1 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, WidgetUpdateHelper.buildGlance1x1Views(context));
        }
    }
}
