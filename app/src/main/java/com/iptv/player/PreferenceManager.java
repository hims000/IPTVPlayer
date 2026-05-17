package com.iptv.player;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "iptv_preferences";
    private static final String KEY_SOURCE_URL = "source_url";
    private static final String KEY_ENGINE_TYPE = "engine_type";
    private static final String KEY_CUSTOM_ENGINE_PATH = "custom_engine_path";
    private static final String KEY_SHOW_SPEED = "show_speed";
    private static final String KEY_SHOW_TIME = "show_time";
    private static final String KEY_EPG_URL = "epg_url";
    private static final String KEY_LOGO_URL = "logo_url";
    private static final String KEY_CURRENT_CHANNEL = "current_channel";
    private static final String KEY_CHANNEL_LIST = "channel_list";

    public static final int ENGINE_SYSTEM = 0;
    public static final int ENGINE_X5 = 1;
    public static final int ENGINE_CUSTOM = 2;

    private static PreferenceManager instance;
    private final SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    public void setSourceUrl(String url) {
        prefs.edit().putString(KEY_SOURCE_URL, url).apply();
    }

    public String getSourceUrl() {
        return prefs.getString(KEY_SOURCE_URL, "");
    }

    public void setEngineType(int type) {
        prefs.edit().putInt(KEY_ENGINE_TYPE, type).apply();
    }

    public int getEngineType() {
        return prefs.getInt(KEY_ENGINE_TYPE, ENGINE_SYSTEM);
    }

    public void setCustomEnginePath(String path) {
        prefs.edit().putString(KEY_CUSTOM_ENGINE_PATH, path).apply();
    }

    public String getCustomEnginePath() {
        return prefs.getString(KEY_CUSTOM_ENGINE_PATH, "");
    }

    public void setShowSpeed(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_SPEED, show).apply();
    }

    public boolean isShowSpeed() {
        return prefs.getBoolean(KEY_SHOW_SPEED, false);
    }

    public void setShowTime(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_TIME, show).apply();
    }

    public boolean isShowTime() {
        return prefs.getBoolean(KEY_SHOW_TIME, false);
    }

    public void setEpgUrl(String url) {
        prefs.edit().putString(KEY_EPG_URL, url).apply();
    }

    public String getEpgUrl() {
        return prefs.getString(KEY_EPG_URL, "");
    }

    public void setLogoUrl(String url) {
        prefs.edit().putString(KEY_LOGO_URL, url).apply();
    }

    public String getLogoUrl() {
        return prefs.getString(KEY_LOGO_URL, "");
    }

    public void setCurrentChannel(int index) {
        prefs.edit().putInt(KEY_CURRENT_CHANNEL, index).apply();
    }

    public int getCurrentChannel() {
        return prefs.getInt(KEY_CURRENT_CHANNEL, 0);
    }

    public void setChannelList(String json) {
        prefs.edit().putString(KEY_CHANNEL_LIST, json).apply();
    }

    public String getChannelList() {
        return prefs.getString(KEY_CHANNEL_LIST, "[]");
    }
}
