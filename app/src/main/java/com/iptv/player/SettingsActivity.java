package com.iptv.player;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_ENGINE = 1001;
    private static final int REQUEST_CODE_PICK_SOURCE = 1002;

    private PreferenceManager prefs;
    private LinearLayout settingSource;
    private LinearLayout settingEngine;
    private LinearLayout settingSpeed;
    private LinearLayout settingTime;
    private LinearLayout settingEpg;
    private LinearLayout settingLogo;
    private TextView sourceUrlText;
    private TextView engineText;
    private Switch switchSpeed;
    private Switch switchTime;
    private TextView epgText;
    private TextView logoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getInstance(this);
        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        settingSource = findViewById(R.id.setting_source);
        settingEngine = findViewById(R.id.setting_engine);
        settingSpeed = findViewById(R.id.setting_speed);
        settingTime = findViewById(R.id.setting_time);
        settingEpg = findViewById(R.id.setting_epg);
        settingLogo = findViewById(R.id.setting_logo);
        sourceUrlText = findViewById(R.id.source_url_text);
        engineText = findViewById(R.id.engine_text);
        switchSpeed = findViewById(R.id.switch_speed);
        switchTime = findViewById(R.id.switch_time);
        epgText = findViewById(R.id.epg_text);
        logoText = findViewById(R.id.logo_text);
    }

    private void loadSettings() {
        sourceUrlText.setText(prefs.getSourceUrl().isEmpty() ? "未设置" : prefs.getSourceUrl());
        updateEngineText();
        switchSpeed.setChecked(prefs.isShowSpeed());
        switchTime.setChecked(prefs.isShowTime());
        epgText.setText(prefs.getEpgUrl().isEmpty() ? "未设置" : prefs.getEpgUrl());
        logoText.setText(prefs.getLogoUrl().isEmpty() ? "未设置" : prefs.getLogoUrl());
    }

    private void updateEngineText() {
        int type = prefs.getEngineType();
        switch (type) {
            case PreferenceManager.ENGINE_SYSTEM:
                engineText.setText(R.string.system_webview);
                break;
            case PreferenceManager.ENGINE_X5:
                engineText.setText(R.string.x5_engine);
                break;
            case PreferenceManager.ENGINE_CUSTOM:
                String path = prefs.getCustomEnginePath();
                engineText.setText(path.isEmpty() ? "外置内核 (未选择)" : new File(path).getName());
                break;
        }
    }

    private void setupListeners() {
        settingSource.setOnClickListener(v -> showSourceUrlDialog());
        settingSource.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) v.setBackgroundResource(R.drawable.menu_item_focused);
        });

        settingEngine.setOnClickListener(v -> showEngineSelector());
        settingEngine.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) v.setBackgroundResource(R.drawable.menu_item_focused);
        });

        settingSpeed.setOnClickListener(v -> {
            boolean checked = !switchSpeed.isChecked();
            switchSpeed.setChecked(checked);
            prefs.setShowSpeed(checked);
        });

        settingTime.setOnClickListener(v -> {
            boolean checked = !switchTime.isChecked();
            switchTime.setChecked(checked);
            prefs.setShowTime(checked);
        });

        settingEpg.setOnClickListener(v -> showEpgDialog());
        settingLogo.setOnClickListener(v -> showLogoDialog());

        // Set initial focus
        settingSource.requestFocus();
    }

    private void showSourceUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.input_url);

        final EditText input = new EditText(this);
        input.setText(prefs.getSourceUrl());
        input.setHint("https://example.com/iptv.html");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            String url = input.getText().toString().trim();
            prefs.setSourceUrl(url);
            sourceUrlText.setText(url.isEmpty() ? "未设置" : url);
            // Parse and save channels from URL if needed
            if (!url.isEmpty()) {
                parseChannelsFromUrl(url);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void parseChannelsFromUrl(String url) {
        // In a real app, this would fetch and parse M3U or JSON
        // For now, we create a simple demo list
        List<Channel> list = new ArrayList<>();
        list.add(new Channel("频道 1", url, "", "", 1));
        list.add(new Channel("频道 2", url, "", "", 2));
        list.add(new Channel("频道 3", url, "", "", 3));
        prefs.setChannelList(new Gson().toJson(list));
        Toast.makeText(this, "已更新频道列表", Toast.LENGTH_SHORT).show();
    }

    private void showEngineSelector() {
        String[] items = {"系统 WebView", "腾讯 X5 内核", "外置内核"};
        int current = prefs.getEngineType();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_engine);
        builder.setSingleChoiceItems(items, current, (dialog, which) -> {
            if (which == PreferenceManager.ENGINE_CUSTOM) {
                dialog.dismiss();
                pickCustomEngine();
            } else {
                prefs.setEngineType(which);
                prefs.setCustomEnginePath("");
                updateEngineText();
                dialog.dismiss();
                Toast.makeText(this, "重启应用后生效", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void pickCustomEngine() {
        // Open file picker to select custom browser engine directory
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, REQUEST_CODE_PICK_ENGINE);
    }

    private void showEpgDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setting_epg);

        final EditText input = new EditText(this);
        input.setText(prefs.getEpgUrl());
        input.setHint("EPG XML 地址");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            String url = input.getText().toString().trim();
            prefs.setEpgUrl(url);
            epgText.setText(url.isEmpty() ? "未设置" : url);
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showLogoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setting_logo);

        final EditText input = new EditText(this);
        input.setText(prefs.getLogoUrl());
        input.setHint("台标基础 URL");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            String url = input.getText().toString().trim();
            prefs.setLogoUrl(url);
            logoText.setText(url.isEmpty() ? "未设置" : url);
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_PICK_ENGINE) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = uri.toString();
                    prefs.setEngineType(PreferenceManager.ENGINE_CUSTOM);
                    prefs.setCustomEnginePath(path);
                    updateEngineText();
                    Toast.makeText(this, "外置内核已选择，重启应用生效", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
