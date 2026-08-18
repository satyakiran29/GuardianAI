package com.skdev.guardianai.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * 4x1 Horizontal Safety Bar AppWidget Provider.
 */
public class SafetyBarWidget4x1 extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, WidgetUpdateHelper.buildSafetyBar4x1Views(context));
        }
    }
}
