package com.iptv.player;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {

    private FrameLayout webViewContainer;
    private WebView webView;
    private ProgressBar loadingIndicator;
    private TextView channelNumberDisplay;
    private TextView speedDisplay;
    private TextView timeDisplay;
    private FrameLayout menuOverlay;
    private LinearLayout menuContainer;
    private LinearLayout menuItemSettings;
    private LinearLayout menuItemExit;
    private FrameLayout channelListOverlay;
    private LinearLayout channelListContainer;
    private RecyclerView channelRecyclerView;
    private LinearLayout infoBar;
    private ImageView channelLogo;
    private TextView channelName;
    private TextView channelEpg;

    private PreferenceManager prefs;
    private List<Channel> channels = new ArrayList<>();
    private ChannelAdapter channelAdapter;
    private int currentChannelIndex = 0;
    private int menuSelectedIndex = 0;
    private boolean isMenuOpen = false;
    private boolean isChannelListOpen = false;
    private boolean isInfoBarVisible = false;
    private StringBuilder channelInputBuffer = new StringBuilder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Timer speedTimer;
    private Timer timeTimer;
    private long lastBackPressTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000;
    private static final int CHANNEL_INPUT_TIMEOUT = 2000;
    private static final int INFO_BAR_TIMEOUT = 5000;
    private static final int CHANNEL_NUMBER_TIMEOUT = 2000;

    // JavaScript injection code for Aliplayer fullscreen
    private static final String INJECT_SCRIPT = 
        "javascript:(function() {" +
        "    const player = document.querySelector('.prism-player, #player-con, #J_prismPlayer, .player-container, video');" +
        "    if (!player) return;" +
        "    document.body.innerHTML = '';" +
        "    document.body.appendChild(player);" +
        "    document.body.style = 'margin:0;padding:0;overflow:hidden;background:#000;';" +
        "    player.style = 'width:100vw;height:100vh;position:fixed;top:0;left:0;z-index:999999;';" +
        "    const video = player.querySelector('video');" +
        "    if (video) { video.style = 'width:100%;height:100%;'; video.play(); }" +
        "    else if (player.tagName === 'VIDEO') { player.style = 'width:100%;height:100%;'; player.play(); }" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getInstance(this);
        initViews();
        initWebView();
        loadChannels();
        setupMenu();
        setupChannelList();
        startSpeedMonitor();
        startTimeMonitor();
        loadCurrentChannel();
    }

    private void initViews() {
        webViewContainer = findViewById(R.id.webview_container);
        loadingIndicator = findViewById(R.id.loading_indicator);
        channelNumberDisplay = findViewById(R.id.channel_number_display);
        speedDisplay = findViewById(R.id.speed_display);
        timeDisplay = findViewById(R.id.time_display);
        menuOverlay = findViewById(R.id.menu_overlay);
        menuContainer = findViewById(R.id.menu_container);
        menuItemSettings = findViewById(R.id.menu_item_settings);
        menuItemExit = findViewById(R.id.menu_item_exit);
        channelListOverlay = findViewById(R.id.channel_list_overlay);
        channelListContainer = findViewById(R.id.channel_list_container);
        channelRecyclerView = findViewById(R.id.channel_recycler_view);
        infoBar = findViewById(R.id.info_bar);
        channelLogo = findViewById(R.id.channel_logo);
        channelName = findViewById(R.id.channel_name);
        channelEpg = findViewById(R.id.channel_epg);
    }

    @SuppressLint("JavascriptInterface")
    private void initWebView() {
        webView = createWebView();
        webViewContainer.addView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUserAgentString(settings.getUserAgentString() + " IPTVPlayer/1.0");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    loadingIndicator.setVisibility(View.VISIBLE);
                } else {
                    loadingIndicator.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadingIndicator.setVisibility(View.GONE);
                // Inject fullscreen script after page load
                handler.postDelayed(() -> view.loadUrl(INJECT_SCRIPT), 1500);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        // Try to use custom engine if configured
        setupBrowserEngine();
    }

    private WebView createWebView() {
        return new WebView(this);
    }

    private void setupBrowserEngine() {
        int engineType = prefs.getEngineType();
        if (engineType == PreferenceManager.ENGINE_CUSTOM) {
            String customPath = prefs.getCustomEnginePath();
            if (customPath != null && !customPath.isEmpty()) {
                try {
                    File engineDir = new File(customPath);
                    if (engineDir.exists()) {
                        // Attempt to load custom chromium WebView
                        loadCustomWebView(engineDir);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadCustomWebView(File engineDir) {
        try {
            // Dynamic class loading for custom browser engine
            // This is a simplified approach - real implementation would load .so files
            ClassLoader classLoader = new dalvik.system.DexClassLoader(
                    engineDir.getAbsolutePath(),
                    getDir("dex", 0).getAbsolutePath(),
                    engineDir.getAbsolutePath(),
                    getClassLoader());
            // Custom engine loading logic would go here
            // For production, use WebViewFactoryProvider or similar
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChannels() {
        String json = prefs.getChannelList();
        if (!json.isEmpty() && !json.equals("[]")) {
            try {
                channels = new Gson().fromJson(json, new TypeToken<List<Channel>>(){}.getType());
            } catch (Exception e) {
                channels = getDefaultChannels();
            }
        } else {
            channels = getDefaultChannels();
        }
        currentChannelIndex = Math.min(prefs.getCurrentChannel(), channels.size() - 1);
    }

    private List<Channel> getDefaultChannels() {
        List<Channel> list = new ArrayList<>();
        // Default demo channels - users can configure via settings
        list.add(new Channel("CCTV-1 综合", "https://tv.cctv.com/live/cctv1/", "", "", 1));
        list.add(new Channel("CCTV-5 体育", "https://tv.cctv.com/live/cctv5/", "", "", 2));
        list.add(new Channel("CCTV-13 新闻", "https://tv.cctv.com/live/cctv13/", "", "", 3));
        return list;
    }

    private void setupMenu() {
        menuItemSettings.setOnClickListener(v -> {
            closeMenu();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        menuItemSettings.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) menuSelectedIndex = 0;
        });

        menuItemExit.setOnClickListener(v -> finish());
        menuItemExit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) menuSelectedIndex = 1;
        });

        menuOverlay.setOnClickListener(v -> closeMenu());
    }

    private void setupChannelList() {
        channelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        channelAdapter = new ChannelAdapter(channels, (channel, position) -> {
            switchChannel(position);
            closeChannelList();
        });
        channelRecyclerView.setAdapter(channelAdapter);
        channelListOverlay.setOnClickListener(v -> closeChannelList());
    }

    private void loadCurrentChannel() {
        if (channels.isEmpty()) return;
        Channel channel = channels.get(currentChannelIndex);
        String url = channel.getUrl();
        if (!url.isEmpty()) {
            webView.loadUrl(url);
            showInfoBar(channel);
        }
    }

    private void switchChannel(int index) {
        if (index < 0 || index >= channels.size()) return;
        currentChannelIndex = index;
        prefs.setCurrentChannel(index);
        loadCurrentChannel();
        showChannelNumber(channelNumberDisplay, String.valueOf(channels.get(index).getNumber()));
    }

    private void switchChannelRelative(int delta) {
        int newIndex = currentChannelIndex + delta;
        if (newIndex < 0) newIndex = channels.size() - 1;
        if (newIndex >= channels.size()) newIndex = 0;
        switchChannel(newIndex);
    }

    private void showInfoBar(Channel channel) {
        if (channel == null) return;
        channelName.setText(channel.getName());
        channelEpg.setText(channel.getEpg() != null ? channel.getEpg() : "");
        infoBar.setVisibility(View.VISIBLE);
        isInfoBarVisible = true;
        handler.removeCallbacks(hideInfoBarRunnable);
        handler.postDelayed(hideInfoBarRunnable, INFO_BAR_TIMEOUT);
    }

    private final Runnable hideInfoBarRunnable = () -> {
        infoBar.setVisibility(View.GONE);
        isInfoBarVisible = false;
    };

    private void showChannelNumber(TextView view, String number) {
        view.setText(number);
        view.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideChannelNumberRunnable);
        handler.postDelayed(hideChannelNumberRunnable, CHANNEL_NUMBER_TIMEOUT);
    }

    private final Runnable hideChannelNumberRunnable = () -> {
        channelNumberDisplay.setVisibility(View.GONE);
        channelInputBuffer.setLength(0);
    };

    // ===== Speed Monitor =====
    private void startSpeedMonitor() {
        if (!prefs.isShowSpeed()) {
            speedDisplay.setVisibility(View.GONE);
            return;
        }
        speedDisplay.setVisibility(View.VISIBLE);
        speedTimer = new Timer();
        speedTimer.scheduleAtFixedRate(new TimerTask() {
            private long lastRx = TrafficStats.getTotalRxBytes();
            private long lastTime = System.currentTimeMillis();

            @Override
            public void run() {
                long currentRx = TrafficStats.getTotalRxBytes();
                long currentTime = System.currentTimeMillis();
                long diffBytes = currentRx - lastRx;
                long diffTime = currentTime - lastTime;
                if (diffTime > 0) {
                    double speedKbps = (diffBytes * 8.0) / (diffTime * 1.0); // kbps
                    String speedText;
                    if (speedKbps > 1000) {
                        speedText = String.format(Locale.getDefault(), "%.1f Mbps", speedKbps / 1000);
                    } else {
                        speedText = String.format(Locale.getDefault(), "%.0f kbps", speedKbps);
                    }
                    handler.post(() -> speedDisplay.setText(speedText));
                }
                lastRx = currentRx;
                lastTime = currentTime;
            }
        }, 0, 1000);
    }

    // ===== Time Monitor =====
    private void startTimeMonitor() {
        if (!prefs.isShowTime()) {
            timeDisplay.setVisibility(View.GONE);
            return;
        }
        timeDisplay.setVisibility(View.VISIBLE);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeTimer = new Timer();
        timeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String time = sdf.format(new Date());
                handler.post(() -> timeDisplay.setText(time));
            }
        }, 0, 1000);
    }

    // ===== Menu Control =====
    private void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        isMenuOpen = true;
        menuOverlay.setVisibility(View.VISIBLE);
        menuSelectedIndex = 0;
        menuItemSettings.requestFocus();
    }

    private void closeMenu() {
        isMenuOpen = false;
        menuOverlay.setVisibility(View.GONE);
        if (webView != null) webView.requestFocus();
    }

    // ===== Channel List Control =====
    private void toggleChannelList() {
        if (isChannelListOpen) {
            closeChannelList();
        } else {
            openChannelList();
        }
    }

    private void openChannelList() {
        isChannelListOpen = true;
        channelListOverlay.setVisibility(View.VISIBLE);
        channelAdapter.setSelectedPosition(currentChannelIndex);
        channelRecyclerView.scrollToPosition(currentChannelIndex);
        // Focus on current item
        handler.post(() -> {
            View view = channelRecyclerView.getLayoutManager().findViewByPosition(currentChannelIndex);
            if (view != null) view.requestFocus();
        });
    }

    private void closeChannelList() {
        isChannelListOpen = false;
        channelListOverlay.setVisibility(View.GONE);
        if (webView != null) webView.requestFocus();
    }

    // ===== Key Handling =====
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        // D-pad / remote control keys
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (isMenuOpen) {
                    closeMenu();
                    return true;
                }
                if (isChannelListOpen) {
                    closeChannelList();
                    return true;
                }
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                    finish();
                } else {
                    lastBackPressTime = currentTime;
                    Toast.makeText(this, R.string.back_to_exit, Toast.LENGTH_SHORT).show();
                }
                return true;

            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SETTINGS:
                toggleMenu();
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (isMenuOpen) {
                    // Let menu item handle click
                    return false;
                }
                if (isChannelListOpen) {
                    // Let list handle click
                    return false;
                }
                toggleChannelList();
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (isChannelListOpen) {
                    return false; // Let RecyclerView handle
                }
                if (isMenuOpen) {
                    navigateMenu(-1);
                    return true;
                }
                switchChannelRelative(-1);
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isChannelListOpen) {
                    return false; // Let RecyclerView handle
                }
                if (isMenuOpen) {
                    navigateMenu(1);
                    return true;
                }
                switchChannelRelative(1);
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isChannelListOpen) {
                    closeChannelList();
                    return true;
                }
                return false;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!isChannelListOpen && !isMenuOpen) {
                    toggleChannelList();
                    return true;
                }
                return false;

            // Numeric keys for direct channel input
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
            case KeyEvent.KEYCODE_NUMPAD_0:
            case KeyEvent.KEYCODE_NUMPAD_1:
            case KeyEvent.KEYCODE_NUMPAD_2:
            case KeyEvent.KEYCODE_NUMPAD_3:
            case KeyEvent.KEYCODE_NUMPAD_4:
            case KeyEvent.KEYCODE_NUMPAD_5:
            case KeyEvent.KEYCODE_NUMPAD_6:
            case KeyEvent.KEYCODE_NUMPAD_7:
            case KeyEvent.KEYCODE_NUMPAD_8:
            case KeyEvent.KEYCODE_NUMPAD_9:
                handleChannelInput(keyCode);
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return false; // Let system handle volume
        }

        return super.onKeyDown(keyCode, event);
    }

    private void navigateMenu(int direction) {
        int count = menuContainer.getChildCount();
        menuSelectedIndex += direction;
        if (menuSelectedIndex < 0) menuSelectedIndex = count - 1;
        if (menuSelectedIndex >= count) menuSelectedIndex = 0;
        View child = menuContainer.getChildAt(menuSelectedIndex);
        if (child != null) child.requestFocus();
    }

    private void handleChannelInput(int keyCode) {
        int digit = keyCodeToDigit(keyCode);
        if (digit >= 0) {
            channelInputBuffer.append(digit);
            showChannelNumber(channelNumberDisplay, channelInputBuffer.toString());
            handler.removeCallbacks(channelInputRunnable);
            handler.postDelayed(channelInputRunnable, CHANNEL_INPUT_TIMEOUT);
        }
    }

    private final Runnable channelInputRunnable = () -> {
        try {
            int channelNum = Integer.parseInt(channelInputBuffer.toString());
            for (int i = 0; i < channels.size(); i++) {
                if (channels.get(i).getNumber() == channelNum) {
                    switchChannel(i);
                    break;
                }
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }
        channelInputBuffer.setLength(0);
    };

    private int keyCodeToDigit(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0: case KeyEvent.KEYCODE_NUMPAD_0: return 0;
            case KeyEvent.KEYCODE_1: case KeyEvent.KEYCODE_NUMPAD_1: return 1;
            case KeyEvent.KEYCODE_2: case KeyEvent.KEYCODE_NUMPAD_2: return 2;
            case KeyEvent.KEYCODE_3: case KeyEvent.KEYCODE_NUMPAD_3: return 3;
            case KeyEvent.KEYCODE_4: case KeyEvent.KEYCODE_NUMPAD_4: return 4;
            case KeyEvent.KEYCODE_5: case KeyEvent.KEYCODE_NUMPAD_5: return 5;
            case KeyEvent.KEYCODE_6: case KeyEvent.KEYCODE_NUMPAD_6: return 6;
            case KeyEvent.KEYCODE_7: case KeyEvent.KEYCODE_NUMPAD_7: return 7;
            case KeyEvent.KEYCODE_8: case KeyEvent.KEYCODE_NUMPAD_8: return 8;
            case KeyEvent.KEYCODE_9: case KeyEvent.KEYCODE_NUMPAD_9: return 9;
            default: return -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply settings that might have changed
        if (prefs.isShowSpeed() && speedTimer == null) {
            startSpeedMonitor();
        } else if (!prefs.isShowSpeed()) {
            speedDisplay.setVisibility(View.GONE);
        }
        if (prefs.isShowTime() && timeTimer == null) {
            startTimeMonitor();
        } else if (!prefs.isShowTime()) {
            timeDisplay.setVisibility(View.GONE);
        }
        // Reload channels if updated
        loadChannels();
        if (channelAdapter != null) {
            channelAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speedTimer != null) {
            speedTimer.cancel();
            speedTimer = null;
        }
        if (timeTimer != null) {
            timeTimer.cancel();
            timeTimer = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
    }
}
