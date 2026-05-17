# IPTV Player

基于 Android WebView 的 IPTV 播放器，支持浏览器内核切换、EPG、台标显示等功能。

## 功能特性

- **浏览器内核切换**：支持系统 WebView、腾讯 X5 内核、外置自定义内核
- **网页源设置**：支持配置外置网页源地址
- **选台功能**：按确定键打开选台列表，上下键换台
- **台号显示**：换台时右上角显示当前台号
- **EPG 设置**：支持配置电子节目单
- **台标设置**：支持配置台标基础 URL
- **网速显示**：实时显示网络速度
- **时间显示**：显示当前时间
- **菜单系统**：按返回键打开/关闭菜单

## 遥控器按键说明

| 按键 | 功能 |
|------|------|
| 返回键 | 打开/关闭菜单 |
| 确定键 | 打开/关闭选台列表 |
| 上下键 | 换台（播放界面）/ 菜单导航 |
| 左右键 | 打开选台列表 / 关闭选台列表 |
| 数字键 | 直接输入台号跳转 |
| 音量键 | 系统音量控制 |

## 浏览器内核说明

### 系统 WebView
使用 Android 系统自带的 WebView，无需额外配置。

### 腾讯 X5 内核
需要在 `build.gradle` 中添加 X5 SDK 依赖，并在 `AndroidManifest.xml` 中配置相关权限。

### 外置内核
1. 将浏览器内核文件（如 Chromium 的 `.so` 库和资源配置）放入设备存储
2. 在设置中选择"外置内核"
3. 通过文件选择器选择内核目录
4. 重启应用生效

> **注意**：外置内核需要包含完整的 Chromium 运行时环境，包括 `libwebviewchromium.so` 及相关资源文件。

## 构建说明

### 本地构建

```bash
# 克隆仓库
git clone <repository-url>
cd IPTVPlayer

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

### GitHub Actions 自动构建

项目已配置 GitHub Actions 工作流，推送代码到 `main` 或 `master` 分支后会自动构建 APK。

构建产物可在 Actions 页面下载。

## 项目结构

```
IPTVPlayer/
├── .github/workflows/    # GitHub Actions 配置
├── app/
│   ├── src/main/
│   │   ├── java/com/iptv/player/
│   │   │   ├── MainActivity.java       # 主界面
│   │   │   ├── SettingsActivity.java   # 设置界面
│   │   │   ├── PreferenceManager.java  # 配置管理
│   │   │   ├── Channel.java            # 频道模型
│   │   │   ├── ChannelAdapter.java     # 频道列表适配器
│   │   │   └── IPTVApplication.java    # 应用入口
│   │   ├── res/                        # 资源文件
│   │   └── AndroidManifest.xml         # 清单文件
│   └── build.gradle                    # 模块构建配置
├── build.gradle                        # 项目构建配置
├── settings.gradle
└── gradle.properties
```

## 技术栈

- Android SDK 34
- Java 8
- WebView (支持自定义内核)
- RecyclerView (选台列表)
- Gson (JSON 解析)
- OkHttp (网络请求)

## 许可证

MIT License
