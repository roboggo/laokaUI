# 老卡桌面（InkDesktop）

一款为墨水屏电纸书打造的轻量 Android 桌面 Launcher。纯黑白高对比、零动画，可替代设备原厂桌面。

## 功能

- **时间日期**：大字号居中显示（时间 144sp、日期 36sp），占据屏幕顶部约 1/3。
- **首页**：8 个常用应用大图标（96dp，4×2 居中排列），可自由增删、可自定义排序，上限 8 个。
- **底部三块 tab**：左「系统设置」／中「全部应用」（大）／右「安卓设置」。
- **全部应用页**：网格列表、搜索过滤、一键刷新；顶部提供「返回 / 管理首页 / 排序首页 / 设为桌面」。
- **管理首页**：勾选式点选应用，加入或移出首页。
- **排序首页**：点应用上移调整顺序，自动保存。
- **设为默认桌面**：通过 Home 键选择或在应用内跳转系统设置完成，无需 root。
- **返回首页全屏强刷**：从应用列表返回首页时自动触发一次全屏刷新，消除残影。

## 墨水屏优化

- 纯白背景 + 纯黑文字，无渐变、无阴影、无动画。
- 应用图标做像素级灰度转换后缓存，保证纯灰阶显示。
- 原生 View 构建，不依赖 AndroidX / WebView，APK 约 29KB，内存占用低。
- 纯黑白四宫格自适应图标，线条粗、对比强。

## 安装

下载 APK 直接安装：

- 直接下载：https://github.com/roboggo/laokaUI/releases/download/v1.6/InkDesktop-v1.6.apk
- 或访问仓库 Releases 页面：https://github.com/roboggo/laokaUI

安装后设为默认桌面：按 Home 键，在弹窗中选择「老卡桌面」并勾选「始终」。

## 构建

需要 Android SDK（build-tools 36、platform android-34）与 JDK 17。`build.sh` 直接调用 aapt2 / javac / d8 / apksigner，无需 Gradle：

```bash
./build.sh
```

产物为 `build/InkDesktop-v1.6.apk`。签名密钥 `debug.jks` 不存在时会自动生成。

## 源码

https://github.com/roboggo/laokaUI
