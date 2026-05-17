package com.iptv.player;

import android.app.Application;
import android.webkit.WebView;

public class IPTVApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable WebView debugging for development
        WebView.setWebContentsDebuggingEnabled(true);
    }
}
