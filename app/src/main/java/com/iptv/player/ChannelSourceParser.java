package com.iptv.player;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChannelSourceParser {
    private static final String TAG = "ChannelSourceParser";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ParseCallback {
        void onSuccess(List<Channel> channels);
        void onError(String error);
    }

    /**
     * 支持的网页源格式：
     * 
     * 1. 纯网页 URL (如 https://tv.cctv.com/live/cctv1/)
     *    - 应用会加载该网页，并注入 JS 自动全屏播放器
     *    - 适合单个频道或网页自带频道切换的情况
     * 
     * 2. JSON 格式频道列表
     *    {
     *      "channels": [
     *        {"name": "CCTV-1", "url": "https://tv.cctv.com/live/cctv1/", "logo": "https://example.com/logo1.png", "epg": "新闻联播", "number": 1},
     *        {"name": "CCTV-5", "url": "https://tv.cctv.com/live/cctv5/", "logo": "https://example.com/logo5.png", "epg": "体育赛事", "number": 2}
     *      ]
     *    }
     * 
     * 3. M3U 格式 (标准 IPTV 格式)
     *    #EXTM3U
     *    #EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV-1" tvg-logo="https://example.com/logo1.png" group-title="央视频道",CCTV-1
     *    https://tv.cctv.com/live/cctv1/
     *    #EXTINF:-1 tvg-id="CCTV5" tvg-name="CCTV-5" tvg-logo="https://example.com/logo5.png" group-title="央视频道",CCTV-5
     *    https://tv.cctv.com/live/cctv5/
     * 
     * 4. TXT 格式 (简单列表)
     *    CCTV-1,https://tv.cctv.com/live/cctv1/
     *    CCTV-5,https://tv.cctv.com/live/cctv5/
     */

    public static void parseSource(Context context, String sourceUrl, ParseCallback callback) {
        if (sourceUrl == null || sourceUrl.isEmpty()) {
            callback.onError("源地址为空");
            return;
        }

        // 本地 JSON 数据直接解析
        if (sourceUrl.trim().startsWith("[") || sourceUrl.trim().startsWith("{")) {
            parseJsonSource(sourceUrl, callback);
            return;
        }

        // 网络 URL，下载后解析
        executor.execute(() -> {
            try {
                String content = downloadContent(sourceUrl);
                if (content == null) {
                    // 如果下载失败，将 URL 作为单个频道处理
                    List<Channel> channels = new ArrayList<>();
                    channels.add(new Channel("频道 1", sourceUrl, "", "", 1));
                    callback.onSuccess(channels);
                    return;
                }

                // 根据内容格式解析
                String trimmed = content.trim();
                if (trimmed.startsWith("#EXTM3U")) {
                    parseM3U(content, callback);
                } else if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                    parseJsonSource(content, callback);
                } else if (trimmed.contains(",")) {
                    parseTxtSource(content, callback);
                } else {
                    // 无法识别格式，作为单个网页处理
                    List<Channel> channels = new ArrayList<>();
                    channels.add(new Channel("网页源", sourceUrl, "", "", 1));
                    callback.onSuccess(channels);
                }
            } catch (Exception e) {
                Log.e(TAG, "解析源失败", e);
                // 解析失败时，将 URL 作为单个频道
                List<Channel> channels = new ArrayList<>();
                channels.add(new Channel("网页源", sourceUrl, "", "", 1));
                callback.onSuccess(channels);
            }
        });
    }

    private static String downloadContent(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "IPTVPlayer/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "下载失败: " + urlString, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private static void parseJsonSource(String json, ParseCallback callback) {
        try {
            List<Channel> channels = new ArrayList<>();

            if (json.trim().startsWith("{")) {
                JSONObject root = new JSONObject(json);
                if (root.has("channels")) {
                    JSONArray array = root.getJSONArray("channels");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        channels.add(jsonToChannel(obj, i + 1));
                    }
                }
            } else if (json.trim().startsWith("[")) {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    channels.add(jsonToChannel(obj, i + 1));
                }
            }

            callback.onSuccess(channels);
        } catch (Exception e) {
            Log.e(TAG, "JSON 解析失败", e);
            callback.onError("JSON 格式错误: " + e.getMessage());
        }
    }

    private static Channel jsonToChannel(JSONObject obj, int defaultNumber) throws Exception {
        Channel channel = new Channel();
        channel.setName(obj.optString("name", "频道 " + defaultNumber));
        channel.setUrl(obj.optString("url", ""));
        channel.setLogo(obj.optString("logo", ""));
        channel.setEpg(obj.optString("epg", obj.optString("program", "")));
        channel.setNumber(obj.optInt("number", defaultNumber));
        return channel;
    }

    private static void parseM3U(String content, ParseCallback callback) {
        List<Channel> channels = new ArrayList<>();
        String[] lines = content.split("\n");

        Channel current = null;
        int number = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#EXTM3U")) continue;

            if (line.startsWith("#EXTINF:")) {
                current = new Channel();
                current.setNumber(number++);

                // 解析 tvg-name
                String name = extractAttribute(line, "tvg-name");
                if (name == null) {
                    // 从逗号后提取名称
                    int commaIndex = line.lastIndexOf(',');
                    if (commaIndex > 0) {
                        name = line.substring(commaIndex + 1).trim();
                    }
                }
                current.setName(name != null ? name : "频道 " + current.getNumber());

                // 解析 tvg-logo
                String logo = extractAttribute(line, "tvg-logo");
                current.setLogo(logo != null ? logo : "");

                // 解析 group-title 作为 EPG
                String group = extractAttribute(line, "group-title");
                current.setEpg(group != null ? group : "");

            } else if (current != null && !line.startsWith("#")) {
                current.setUrl(line);
                channels.add(current);
                current = null;
            }
        }

        callback.onSuccess(channels);
    }

    private static String extractAttribute(String line, String attr) {
        String pattern = attr + "=\";
        int start = line.indexOf(pattern);
        if (start >= 0) {
            start += pattern.length();
            int end = line.indexOf('"', start);
            if (end > start) {
                return line.substring(start, end);
            }
        }
        return null;
    }

    private static void parseTxtSource(String content, ParseCallback callback) {
        List<Channel> channels = new ArrayList<>();
        String[] lines = content.split("\n");
        int number = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",", 2);
            if (parts.length >= 2) {
                Channel channel = new Channel();
                channel.setName(parts[0].trim());
                channel.setUrl(parts[1].trim());
                channel.setNumber(number++);
                channels.add(channel);
            }
        }

        callback.onSuccess(channels);
    }
}
