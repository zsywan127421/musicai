package com.example.musicai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LanguageManager {
    
    public static final String PREFS_NAME = "LanguagePrefs";
    public static final String KEY_LANGUAGE = "language";
    
    public static final int LANG_SYSTEM = 0;
    public static final int LANG_SIMPLIFIED = 1;
    public static final int LANG_TRADITIONAL = 2;
    public static final int LANG_ENGLISH = 3;
    
    private static LanguageManager instance;
    private SharedPreferences prefs;
    private int currentLang = LANG_SYSTEM;
    
    private LanguageManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentLang = prefs.getInt(KEY_LANGUAGE, LANG_SYSTEM);
    }
    
    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context);
        }
        return instance;
    }
    
    public int getLanguage() {
        return currentLang;
    }
    
    public void setLanguage(int lang) {
        currentLang = lang;
        prefs.edit().putInt(KEY_LANGUAGE, lang).apply();
    }
    
    public String getLanguageString() {
        switch (currentLang) {
            case LANG_SIMPLIFIED:
                return "简体中文";
            case LANG_TRADITIONAL:
                return "繁體中文";
            case LANG_ENGLISH:
                return "English";
            default:
                return "跟随系统";
        }
    }
    
    public Locale getLocale() {
        switch (currentLang) {
            case LANG_SIMPLIFIED:
                return Locale.SIMPLIFIED_CHINESE;
            case LANG_TRADITIONAL:
                return Locale.TRADITIONAL_CHINESE;
            case LANG_ENGLISH:
                return Locale.ENGLISH;
            default:
                return Locale.getDefault();
        }
    }
    
    public Context updateResources(Context context) {
        if (currentLang == LANG_SYSTEM) {
            return context;
        }
        
        Locale locale = getLocale();
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }
    
    public void applyLanguage(Context context) {
        if (currentLang == LANG_SYSTEM) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(getLocale().toLanguageTag());
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }
}
