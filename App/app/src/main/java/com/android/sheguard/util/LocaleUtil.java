package com.android.sheguard.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;

import java.util.Locale;

public class LocaleUtil {

    public static Context setLocale(Context context) {
        String lang = getLanguage();
        if (Constants.LANG_SYSTEM.equals(lang)) {
            return context;
        }
        return updateResources(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        Prefs.putString(Constants.SETTINGS_APP_LANGUAGE, language);
        if (Constants.LANG_SYSTEM.equals(language)) {
            return context;
        }
        return updateResources(context, language);
    }

    public static String getLanguage() {
        return Prefs.getString(Constants.SETTINGS_APP_LANGUAGE, Constants.LANG_SYSTEM);
    }

    public static int getLanguageIndex() {
        String lang = getLanguage();
        switch (lang) {
            case Constants.LANG_EN:
                return 1;
            case Constants.LANG_HI:
                return 2;
            case Constants.LANG_TE:
                return 3;
            case Constants.LANG_SYSTEM:
            default:
                return 0;
        }
    }

    public static String getLanguageName(Context context) {
        String lang = getLanguage();
        switch (lang) {
            case Constants.LANG_EN:
                return context.getString(R.string.language_english);
            case Constants.LANG_HI:
                return context.getString(R.string.language_hindi);
            case Constants.LANG_TE:
                return context.getString(R.string.language_telugu);
            case Constants.LANG_SYSTEM:
            default:
                return context.getString(R.string.language_system);
        }
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
